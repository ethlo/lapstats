package com.ethlo.lapstats.source.myrcm;

import com.ethlo.lapstats.source.StatsReader;
import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.LapData;
import com.ethlo.lapstats.model.RaceData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyRcmReader implements StatsReader {
    private final Pattern pattern = Pattern.compile("\\((\\d+)\\) (.*)");

    @Override
    public RaceData load(String url) throws IOException {
        final Document doc = Jsoup.parse(new URL(url), 15_000);
        final Elements tables = doc.select("table");
        final Map<Integer, Driver> driverData = driverData(tables.get(0));
        return extractLapTimes(tables.subList(1, tables.size() - 1), driverData);
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

    private RaceData extractLapTimes(List<Element> tables, Map<Integer, Driver> driverData) {
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

        return new RaceData(changeTicks, lapToDriverLapList, driverData);
    }

    private Optional<LapData> getDriverLapData(int lap, int driverIndex, String placementAndTime, Map<Integer, Duration> accumulatedLapTimes, Map<Integer, Duration> minLapTimes, Map<Integer, Duration> maxLapTimes, Map<Integer, List<LapData>> lapToDriverLapList) {
        final Matcher matcher = pattern.matcher(placementAndTime);
        if (matcher.matches()) {
            final MatchResult result = matcher.toMatchResult();
            //final int position = Integer.parseInt(result.group(1));
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
            return Optional.of(new LapData(lap, driverIndex, lapTime, minLapTime, maxLapTime, diffLastLap, accumulatedLapTime, accumulatedLapTime.dividedBy(lap)));
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
                if (l.driverId() == driverId) {
                    return l;
                }
            }
            return null;
        });
        return lastLap.map(l -> lapTime.minus(l.currentLapTime())).orElse(null);
    }

    private static Duration getLapDuration(MatchResult result) {
        final DateTimeFormatter df = result.group(2).contains(":") ? DateTimeFormatter.ofPattern("m:ss.SSS") : DateTimeFormatter.ofPattern("ss.SSS");
        final TemporalAccessor temporalAccessor = df.parse(result.group(2));
        final int minutes = temporalAccessor.isSupported(ChronoField.MINUTE_OF_HOUR) ? temporalAccessor.get(ChronoField.MINUTE_OF_HOUR) * 60 * 1000 : 0;
        final int secs = temporalAccessor.get(ChronoField.SECOND_OF_MINUTE) * 1000;
        final int millis = temporalAccessor.get(ChronoField.MILLI_OF_SECOND);
        return Duration.ofMillis(minutes + secs + millis);
    }

}
