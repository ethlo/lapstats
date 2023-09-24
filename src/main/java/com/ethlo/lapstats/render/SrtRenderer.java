package com.ethlo.lapstats.render;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.LapStatistics;
import com.ethlo.lapstats.model.RaceData;

public class SrtRenderer implements StatusRenderer
{
    public static String formatSrtTimestamp(final Duration duration)
    {
        final long interval = duration.toMillis();
        final long hour = TimeUnit.MILLISECONDS.toHours(interval) % 60;
        final long min = TimeUnit.MILLISECONDS.toMinutes(interval) % 60;
        final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) % 60;
        final long ms = TimeUnit.MILLISECONDS.toMillis(interval) % 1000;
        return String.format("%02d:%02d:%02d,%03d", hour, min, sec, ms);
    }

    public static String formatIntervalWithSeconds(final Duration duration, boolean sign)
    {
        final long interval = Math.abs(duration.toMillis());
        final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) % 60;
        final long ms = TimeUnit.MILLISECONDS.toMillis(interval) % 1000;
        return (sign ? (duration.isNegative() ? "-" : "+") : "") + String.format("%02d.%03d", sec, ms);
    }

    public void render(RaceData raceData, OutputStream outputStream)
    {
        final AtomicInteger index = new AtomicInteger(1);
        final Duration ttl = Duration.ofSeconds(1);
        try (final PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)))
        {
            raceData.getTicks().forEach(time ->
            {
                final LapStatistics lapData = raceData.getLap(time);
                final Driver driver = raceData.getDriverData(lapData.getDriverId());
                out.println(index.getAndIncrement());
                out.print(formatSrtTimestamp(time));
                out.print(" --> ");
                out.print(formatSrtTimestamp(time.plus(ttl)));
                out.println();
                out.print(driver.name());
                out.print(" | Lap " + lapData.getLap());
                out.print(" | Time " + formatIntervalWithSeconds(lapData.getTime(), false)); // + " (" + (lapData.diffLastLap() != null ? formatIntervalWithSeconds(lapData.diffLastLap(), true) : " - ") + ")");
                out.println();
                out.println();
            });
        }
    }
}
