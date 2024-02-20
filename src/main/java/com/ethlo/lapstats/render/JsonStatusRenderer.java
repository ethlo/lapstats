package com.ethlo.lapstats.render;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ethlo.lapstats.model.LapStatistics;
import com.ethlo.lapstats.model.RaceData;
import com.ethlo.lapstats.model.Timing;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ComparisonChain;

public class JsonStatusRenderer implements StatusRenderer
{
    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.CLOSE_CLOSEABLE)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    @Override
    public void render(RaceData raceData, OutputStream out) throws IOException
    {
        final List<Map<String, Object>> list = new LinkedList<>();
        final Map<Integer, LapStatistics> currentRow = new TreeMap<>();

        // Put initial standings at start
        raceData.getDrivers()
                .forEach(driver -> currentRow.put(driver.id(),
                        new LapStatistics(new Timing(0, driver.id(), Duration.ZERO), Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, false)
                ));
        addToEventStream(raceData, list, currentRow, Duration.ZERO);

        // Iterate through all laps for all users sorted by time of passing
        for (Duration timestamp : raceData.getTicks())
        {
            final LapStatistics data = raceData.getLap(timestamp);

            // Replace data when it occurs
            currentRow.put(data.getDriverId(), data);
            addToEventStream(raceData, list, currentRow, timestamp);
        }
        mapper.writeValue(out, list);
    }

    private static void addToEventStream(final RaceData raceData, final List<Map<String, Object>> list, final Map<Integer, LapStatistics> currentRow, final Duration timestamp)
    {
        final List<Map<String, Object>> d = new ArrayList<>();
        final List<LapStatistics> currentRowSortedByPos = new ArrayList<>(currentRow.values());
        currentRowSortedByPos.sort((a, b) -> ComparisonChain.start().compare(b.getLap(), a.getLap()).compare(a.getDiffLeader(), b.getDiffLeader()).result());

        int pos = 1;
        for (LapStatistics l : currentRowSortedByPos)
        {
            final Long diff = l.getDiffLeader().isZero() && pos != 1 ? (timestamp.toMillis() - l.getAccumulatedLapTime().toMillis()) : l.getDiffLeader().toMillis();
            final Map<String, Object> row = new LinkedHashMap<>();
            row.put("pos", pos);
            row.put("driver", raceData.getDriverById(l.getDriverId()).orElseThrow().name());
            row.put("lap", l.getLap());
            row.put("diff", diff);
            row.put("implicit", l.isImplicit());
            row.put("current", l.getAccumulatedLapTime().equals(timestamp));
            d.add(row);
            pos++;
        }
        list.add(Map.of("timestamp", timestamp, "data", d));
    }
}
