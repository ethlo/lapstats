package com.ethlo.myrcm;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageLoader {
    private final Pattern pattern = Pattern.compile("\\((\\d+)\\) (.*)");

    public void load(String url) throws IOException {
        final Document doc = Jsoup.parse(new URL(url), 15_000);
        final Elements tables = doc.select("table");
        final Map<Integer, Driver> driverData = driverData(tables.get(0));
        final RaceData raceData = extractLapTimes(tables.subList(1, tables.size() - 1));

        outputPositions(raceData, driverData);
        outputSrtText(raceData, driverData, Paths.get("e:/test.srt"));
    }

    private static void outputSrtText(RaceData raceData, Map<Integer, Driver> driverData, Path file) throws IOException {
        final AtomicInteger index = new AtomicInteger(1);
        final Duration ttl = Duration.ofSeconds(1);
        try (final PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            raceData.ticks.forEach((time, lapData) ->
            {
                out.println(index.getAndIncrement());
                out.print(formatSrtTimestamp(time));
                out.print(" --> ");
                out.print(formatSrtTimestamp(time.plus(ttl)));
                out.println();
                out.print(Optional.ofNullable(driverData.get(lapData.driverId)).orElseThrow(() -> new IllegalArgumentException("No driver id: " + lapData.driverId)).name);
                out.print(" | Position " + lapData.position);
                out.print(" | Lap " + lapData.lap);
                out.print(" | Time " + formatIntervalWithSeconds(lapData.currentLapTime, false) + " (" + (lapData.diffLastLap != null ? formatIntervalWithSeconds(lapData.diffLastLap, true) : " - ") + ")");
                out.println();
                out.println();
            });
        }
    }

    private void outputPositions(RaceData raceData, Map<Integer, Driver> driverData) {
        for (LapData data : raceData.ticks.values()) {
            final List<LapData> forSameLap = raceData.lapToDriverLapList.get(data.lap);
            forSameLap.sort(Comparator.comparing(a -> a.accumulatedLapTime));
            System.out.println("\n------ # " + data.lap + " (" + data.accumulatedLapTime + ") ------");
            for (int pos = 0; pos < forSameLap.size(); pos++) {
                final LapData l = forSameLap.get(pos);
                final boolean current = data.driverId == l.driverId && data.lap == l.lap;
                System.out.println((current ? "--> " : "") + (pos + 1) + " - " + driverData.get(l.driverId).name + " - " + l.accumulatedLapTime + (!current ? (" - " + l.accumulatedLapTime.minus(data.accumulatedLapTime)) : ""));
            }
        }
    }

    private Map<Integer, Driver> driverData(Element driverTable) {
        final Map<Integer, Driver> result = new TreeMap<>();
        final Collection<List<String>> rows = extractRows(List.of(driverTable), 0);
        for (List<String> row : rows) {
            final int driverId = Integer.parseInt(row.get(0));
            result.put(driverId, new Driver(driverId, row.get(3)));
        }
        return result;
    }

    private RaceData extractLapTimes(List<Element> tables) {
        final Collection<List<String>> rows = extractRows(tables, 1);

        final Map<Integer, Duration> accumulatedLapTimes = new HashMap<>();
        final Map<Integer, Duration> minLapTimes = new HashMap<>();
        final Map<Integer, Duration> maxLapTimes = new HashMap<>();
        final Map<Integer, List<LapData>> lapToDriverLapList = new HashMap<>();
        final Map<Duration, LapData> changeTicks = new TreeMap<>();

        for (List<String> row : rows) {
            final int lap = Integer.parseInt(row.get(0));

            final List<LapData> driverList = new ArrayList<>(row.size());
            for (int driverIndex = 1; driverIndex < row.size(); driverIndex++) {
                final int a = driverIndex;
                final Optional<LapData> lapData = getDriverLapData(lap, driverIndex, row.get(driverIndex), accumulatedLapTimes, minLapTimes, maxLapTimes, lapToDriverLapList);
                lapData.ifPresent(l ->
                {
                    driverList.add(l);
                    changeTicks.put(accumulatedLapTimes.get(a), l);
                });
            }

            lapToDriverLapList.put(lap, driverList);
        }

        return new RaceData(changeTicks, lapToDriverLapList);
    }

    private Optional<LapData> getDriverLapData(int lap, int driverIndex, String placementAndTime, Map<Integer, Duration> accumulatedLapTimes, Map<Integer, Duration> minLapTimes, Map<Integer, Duration> maxLapTimes, Map<Integer, List<LapData>> lapToDriverLapList) {
        final Matcher matcher = pattern.matcher(placementAndTime);
        if (matcher.matches()) {
            final MatchResult result = matcher.toMatchResult();
            final int position = Integer.parseInt(result.group(1));
            final Duration lapTime = getLapDuration(result);

            final Duration accumulatedLapTime = accumulatedLapTimes.compute(driverIndex, (k, v) -> {
                if (v == null) {
                    return lapTime;
                } else {
                    return v.plus(lapTime);
                }
            });

            final Duration minLapTime = minLapTimes.compute(driverIndex, (k, v) -> {
                if (lap < 2) {
                    return null;
                } else if (v == null) {
                    return lapTime;
                } else {
                    return v.compareTo(lapTime) > 0 ? lapTime : v;
                }
            });

            final Duration maxLapTime = maxLapTimes.compute(driverIndex, (k, v) -> {
                if (v == null) {
                    return lapTime;
                } else {
                    return v.compareTo(lapTime) < 0 ? lapTime : v;
                }
            });

            final Duration diffLastLap = getDiffToLastLap(lapToDriverLapList, lap, driverIndex, lapTime);
            return Optional.of(new LapData(lap, position, driverIndex, lapTime, minLapTime, maxLapTime, diffLastLap, accumulatedLapTime, accumulatedLapTime.dividedBy(lap)));
        }
        return Optional.empty();
    }

    private Collection<List<String>> extractRows(List<Element> tables, int skipColumns) {
        final Map<Integer, List<String>> result = new TreeMap<>();
        for (Element table : tables) {
            final Elements rows = table.select("tr");
            for (int i = 1; i < rows.size(); i++) {
                final Element row = rows.get(i);
                final Elements cols = row.select("td");
                result.compute(i, (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.addAll(cols.stream()
                            .skip(table == tables.get(0) ? 0 : skipColumns)
                            .map(Element::text).toList());
                    return v;
                });
            }
        }
        return result.values();
    }

    private static Duration getDiffToLastLap(Map<Integer, List<LapData>> lapToDriverLapList, int lap, int driverId, Duration lapTime) {
        if (lap <= 2) {
            return null;
        }
        final Optional<LapData> lastLap = Optional.ofNullable(lapToDriverLapList.get(lap - 1)).map(driverLaps ->
        {
            for (LapData l : driverLaps) {
                if (l.driverId == driverId) {
                    return l;
                }
            }
            return null;
        });
        return lastLap.map(l -> lapTime.minus(l.currentLapTime)).orElse(null);
    }

    private static Duration getLapDuration(MatchResult result) {
        final DateTimeFormatter df = result.group(2).contains(":") ? DateTimeFormatter.ofPattern("m:ss.SSS") : DateTimeFormatter.ofPattern("ss.SSS");
        final TemporalAccessor temporalAccessor = df.parse(result.group(2));
        final int minutes = temporalAccessor.isSupported(ChronoField.MINUTE_OF_HOUR) ? temporalAccessor.get(ChronoField.MINUTE_OF_HOUR) * 60 * 1000 : 0;
        final int secs = temporalAccessor.get(ChronoField.SECOND_OF_MINUTE) * 1000;
        final int millis = temporalAccessor.get(ChronoField.MILLI_OF_SECOND);
        return Duration.ofMillis(minutes + secs + millis);
    }

    public static String formatSrtTimestamp(final Duration duration) {
        final long interval = duration.toMillis();
        final long hour = TimeUnit.MILLISECONDS.toHours(interval) % 60;
        final long min = TimeUnit.MILLISECONDS.toMinutes(interval) % 60;
        final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) % 60;
        final long ms = TimeUnit.MILLISECONDS.toMillis(interval) % 1000;
        return String.format("%02d:%02d:%02d,%03d", hour, min, sec, ms);
    }

    public static String formatIntervalWithSeconds(final Duration duration, boolean sign) {
        final long interval = Math.abs(duration.toMillis());
        final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) % 60;
        final long ms = TimeUnit.MILLISECONDS.toMillis(interval) % 1000;
        return (sign ? (duration.isNegative() ? "-" : "+") : "") + String.format("%02d.%03d", sec, ms);
    }


    public record LapData(int lap, int position, int driverId, Duration currentLapTime, Duration minLapTime,
                          Duration maxLapTime, Duration diffLastLap, Duration accumulatedLapTime,
                          Duration averageLapTime) {

    }

    public record Driver(int driverId, String name) {
    }

    public record RaceData(Map<Duration, LapData> ticks, Map<Integer, List<LapData>> lapToDriverLapList) {
    }
}
