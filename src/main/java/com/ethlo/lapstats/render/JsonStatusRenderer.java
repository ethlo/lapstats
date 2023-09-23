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
        for (Duration timestamp : raceData.getTicks())
        {
            final LapStatistics data = raceData.getLap(timestamp);
            //final List<LapStatistics> forSameLap = raceData.getLap(data.timing().lap());
            //forSameLap.sort(Comparator.comparing(LapStatistics::accumulatedLapTime));
            //pw.println("\n" + formatDiff(data.accumulatedLapTime()));
            final LapStatistics firstPos = forSameLap.get(0);
            final Duration diffToCurrent = firstPos.accumulatedLapTime().minus(timestamp).abs();

            final List<Map<String, Object>> d = new ArrayList<>();
            for (int pos = 0; pos < forSameLap.size(); pos++)
            {
                final LapStatistics l = forSameLap.get(pos);
                final String driverName = raceData.getDriverData(l.timing().driverId()).name();
                final Duration diffFromLeader = l.accumulatedLapTime().minus(data.accumulatedLapTime()).plus(diffToCurrent);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("pos", pos + 1);
                row.put("driver", driverName);
                row.put("lap", l.timing().lap());
                row.put("diff", diffFromLeader);
                row.put("implicit", l.implicit());
                d.add(row);
            }
            list.add(Map.of("timestamp", timestamp, "data", d));
        }
        mapper.writeValue(out, list);
    }
}
