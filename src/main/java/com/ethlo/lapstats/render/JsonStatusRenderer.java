package com.ethlo.lapstats.render;

import com.ethlo.lapstats.model.LapStatistics;
import com.ethlo.lapstats.model.RaceData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.*;

public class JsonStatusRenderer implements StatusRenderer
{
    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.CLOSE_CLOSEABLE);

    @Override
    public void render(RaceData raceData, OutputStream out) throws IOException
    {
        final List<Map<String, Object>> list = new LinkedList<>();
        final Map<Integer, LapStatistics> status = new TreeMap<>();
        for (Duration timestamp : raceData.getTicks())
        {
            final LapStatistics data = raceData.getLap(timestamp);
            status.put(data.getDriverId(), data);

            final List<Map<String, Object>> d = new ArrayList<>();
            final List<LapStatistics> r = new ArrayList<>(status.values());
            r.sort(Comparator.comparing(LapStatistics::getDiffLeader));
            for (int i = 0; i < r.size(); i++)
            {
                final LapStatistics l = r.get(i);
                final Map<String, Object> row = new LinkedHashMap<>();
                row.put("pos", i + 1);
                row.put("driver", raceData.getDriverData(l.getDriverId()).name());
                row.put("lap", l.getLap());
                row.put("diff", l.getDiffLeader());
                row.put("implicit", l.isImplicit());
                row.put("current", l.getAccumulatedLapTime().equals(timestamp));
                d.add(row);
            }
            list.add(Map.of("timestamp", timestamp, "data", d));
        }
        mapper.writeValue(out, list);
    }
}
