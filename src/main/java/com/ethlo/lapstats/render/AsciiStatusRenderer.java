package com.ethlo.lapstats.render;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import com.ethlo.lapstats.model.LapStatistics;
import com.ethlo.lapstats.model.RaceData;
import com.google.common.base.Strings;

public class AsciiStatusRenderer implements StatusRenderer
{
    @Override
    public void render(RaceData raceData, OutputStream out)
    {
        try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))
        {
            final int maxDriverNameLength = raceData.getDrivers().stream().max(Comparator.comparingInt(a -> a.name().length())).orElseThrow().name().length();
            for (Duration timestamp : raceData.getTicks())
            {
                final LapStatistics data = raceData.getLap(timestamp);
                final List<LapStatistics> forSameLap = raceData.getLap(data.getLap());
                forSameLap.sort(Comparator.comparing(LapStatistics::getAccumulatedLapTime));
                pw.println("\n" + formatDiff(data.getAccumulatedLapTime()));
                final LapStatistics firstPos = forSameLap.get(0);
                final Duration diffToCurrent = firstPos.getAccumulatedLapTime().minus(timestamp).abs();
                for (int pos = 0; pos < forSameLap.size(); pos++)
                {
                    final LapStatistics l = forSameLap.get(pos);
                    final String driverName = raceData.getDriverById(l.getDriverId()).orElseThrow().name();
                    final String paddedPos = Strings.padStart(Integer.toString(pos + 1), 2, '0');
                    final String paddedDriverName = Strings.padEnd(driverName, maxDriverNameLength, ' ');
                    final Duration diffFromLeader = l.getAccumulatedLapTime().minus(data.getAccumulatedLapTime()).plus(diffToCurrent);
                    pw.println(paddedPos + " - " + paddedDriverName + " " + formatDiff(diffFromLeader) + (l.isImplicit() ? " *" : ""));
                }
            }
        }
    }

    private String formatDiff(Duration duration)
    {
        final Duration abs = duration.abs();
        return String.format("%s%2d:%2d:%3d", duration.isNegative() ? "-" : "+", abs.toMinutesPart(), abs.toSecondsPart(), abs.toMillisPart()).replace(" ", "0");
    }
}
