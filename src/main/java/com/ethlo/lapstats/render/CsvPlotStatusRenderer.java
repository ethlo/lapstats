package com.ethlo.lapstats.render;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import com.ethlo.lapstats.model.ExtendedLapData;
import com.ethlo.lapstats.model.RaceData;

public class CsvPlotStatusRenderer implements StatusRenderer
{
    @Override
    public void render(final RaceData raceData, final OutputStream out) throws IOException
    {
        try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))
        {
            for (Duration timestamp : raceData.getTicks())
            {
                final ExtendedLapData data = raceData.getLapData(timestamp);
                final List<ExtendedLapData> forSameLap = raceData.getLap(data.lap().lap());
                forSameLap.sort(Comparator.comparing(ExtendedLapData::accumulatedLapTime));
                pw.print(data.accumulatedLapTime());
                pw.print(",");
                final ExtendedLapData firstPos = forSameLap.get(0);
                final Duration diffToCurrent = firstPos.accumulatedLapTime().minus(timestamp).abs();
                for (int pos = 0; pos < forSameLap.size(); pos++)
                {
                    final ExtendedLapData l = forSameLap.get(pos);
                    final String driverName = raceData.getDriverData(l.lap().driverId()).name();
                    final Duration diffFromLeader = l.accumulatedLapTime().minus(data.accumulatedLapTime()).plus(diffToCurrent);
                    pw.print(pos + "," + driverName + "," + diffFromLeader.toMillis());
                }
                pw.println();
            }
        }
    }
}
