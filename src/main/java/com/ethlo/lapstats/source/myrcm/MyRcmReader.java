package com.ethlo.lapstats.source.myrcm;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.Timing;
import com.ethlo.lapstats.source.StatsReader;

public class MyRcmReader implements StatsReader
{
    private final Pattern pattern = Pattern.compile("\\((\\d+)\\) (.*)");
    private final Document doc;

    public MyRcmReader(URL url) throws IOException
    {
        this.doc = Jsoup.parse(url, 15_000);
    }

    public MyRcmReader(final String html) throws IOException
    {
        this.doc = Jsoup.parse(html);
    }

    private static Duration getLapDuration(String elapsed)
    {
        final DateTimeFormatter df = elapsed.contains(":") ? DateTimeFormatter.ofPattern("m:ss.SSS") : DateTimeFormatter.ofPattern("ss.SSS");
        final TemporalAccessor temporalAccessor = df.parse(elapsed);
        final int minutes = temporalAccessor.isSupported(ChronoField.MINUTE_OF_HOUR) ? temporalAccessor.get(ChronoField.MINUTE_OF_HOUR) * 60 * 1000 : 0;
        final int secs = temporalAccessor.get(ChronoField.SECOND_OF_MINUTE) * 1000;
        final int millis = temporalAccessor.get(ChronoField.MILLI_OF_SECOND);
        return Duration.ofMillis(minutes + secs + millis);
    }

    @Override
    public Map<Integer, List<Timing>> getDriverLapTimes()
    {
        final Elements tables = doc.select("table");
        return extractLapTimes(tables.subList(1, tables.size() - 1));
    }

    @Override
    public Map<Integer, Driver> getDriverList()
    {
        final Elements tables = doc.select("table");
        return driverData(tables.get(0));
    }

    private Map<Integer, Driver> driverData(Element driverTable)
    {
        final Map<Integer, Driver> result = new TreeMap<>();
        final Collection<List<String>> rows = extractRows(List.of(driverTable), 0);
        for (List<String> row : rows)
        {
            final int driverId = Integer.parseInt(row.get(0));
            result.put(driverId, new Driver(driverId, row.get(3)));
        }
        return result;
    }

    private Map<Integer, List<Timing>> extractLapTimes(List<Element> tables)
    {
        final Collection<List<String>> rows = extractRows(tables, 1);
        final Map<Integer, List<Timing>> lapToDriverLapList = new HashMap<>();

        for (List<String> row : rows)
        {
            final int lap = Integer.parseInt(row.get(0));

            final List<Timing> driverList = new ArrayList<>(row.size());
            for (int driverIndex = 1; driverIndex < row.size(); driverIndex++)
            {
                final String placementAndTime = row.get(driverIndex);
                final Matcher matcher = pattern.matcher(placementAndTime);
                if (matcher.matches())
                {
                    final MatchResult result = matcher.toMatchResult();
                    final Duration lapTime = getLapDuration(result.group(2));
                    driverList.add(new Timing(lap, driverIndex, lapTime));
                }

                lapToDriverLapList.put(lap, driverList);
            }
        }
        return lapToDriverLapList;
    }

    private Collection<List<String>> extractRows(List<Element> tables, int skipColumns)
    {
        final Map<Integer, List<String>> result = new TreeMap<>();
        for (Element table : tables)
        {
            final Elements rows = table.select("tr");
            for (int i = 1; i < rows.size(); i++)
            {
                final Element row = rows.get(i);
                final Elements cols = row.select("td");
                result.compute(i, (k, v) -> {
                    if (v == null)
                    {
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
}