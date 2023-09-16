package com.ethlo.lapstats.render;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.RaceData;

public class CsvPlotStatusRenderer implements StatusRenderer
{
    @Override
    public void render(final RaceData raceData, final OutputStream out) throws IOException
    {
        try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))
        {
            pw.print("Lap,");
            pw.print(raceData.getDrivers().stream().map(Driver::name).collect(Collectors.joining(",")));
            pw.println();

            raceData.getLaps().forEach((lap, lapData) ->
            {
                pw.print(lapData.get(0).lap().lap() + ",");
                pw.print(lapData.stream().map(l -> formatTimestamp(l.accumulatedLapTime())).collect(Collectors.joining(",")));
                pw.println();
            });
        }
    }

    public static String formatTimestamp(final Duration duration)
    {
        final long interval = duration.toMillis();
        final long hour = TimeUnit.MILLISECONDS.toHours(interval) % 60;
        final long min = TimeUnit.MILLISECONDS.toMinutes(interval) % 60;
        final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) % 60;
        final long ms = TimeUnit.MILLISECONDS.toMillis(interval) % 1000;
        return String.format("%02d:%02d:%02d.%03d", hour, min, sec, ms);
    }
}
