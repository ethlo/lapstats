package com.ethlo.lapstats.render;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ethlo.lapstats.model.LapStatistics;
import com.ethlo.lapstats.model.RaceData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonStatusRenderer implements StatusRenderer
{
    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.CLOSE_CLOSEABLE);
    ;

    @Override
    public void render(RaceData raceData, OutputStream out) throws IOException
    {
        final List<Map<String, Object>> list = new LinkedList<>();
        final Map<Integer, LapStatistics> status = new TreeMap<>();
        for (Duration timestamp : raceData.getTicks())
        {
            final LapStatistics data = raceData.getLap(timestamp);
            final List<LapStatistics> forSameLap = raceData.getLap(data.getLap());
            forSameLap.sort(Comparator.comparing(LapStatistics::getAccumulatedLapTime));
            final LapStatistics firstPos = forSameLap.get(0);
            final Duration diffToCurrent = firstPos.getAccumulatedLapTime().minus(timestamp).abs();

            //final LapStatistics l = forSameLap.get(pos);
            status.put(data.getDriverId(), data);

            final List<Map<String, Object>> d = new ArrayList<>();
            status.forEach((driverId, l) ->
            {
                final String driverName = raceData.getDriverData(driverId).name();
                //final Duration diffFromLeader = data.accumulatedLapTime().minus(data.accumulatedLapTime()).plus(diffToCurrent);

                final Map<String, Object> row = new LinkedHashMap<>();
                row.put("pos", driverId);
                row.put("driver", driverName);
                row.put("lap", l.getLap());
                //row.put("diff", diffFromLeader);
                row.put("implicit", l.isImplicit());
                d.add(row);
            });
            list.add(Map.of("timestamp", timestamp, "data", d));
        }
        mapper.writeValue(out, list);
    }
}
