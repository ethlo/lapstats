package com.ethlo.lapstats;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.RaceData;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SrtRenderer implements StatusRenderer {
    public void render(RaceData raceData, OutputStream outputStream) {
        final AtomicInteger index = new AtomicInteger(1);
        final Duration ttl = Duration.ofSeconds(1);
        final Map<Integer, Driver> driverData = raceData.driverData();
        try (final PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            raceData.ticks().forEach((time, lapData) ->
            {
                out.println(index.getAndIncrement());
                out.print(formatSrtTimestamp(time));
                out.print(" --> ");
                out.print(formatSrtTimestamp(time.plus(ttl)));
                out.println();
                out.print(Optional.ofNullable(driverData.get(lapData.driverId())).orElseThrow(() -> new IllegalArgumentException("No driver id: " + lapData.driverId())).name());
                //out.print(" | Position " + lapData.position());
                out.print(" | Lap " + lapData.lap());
                out.print(" | Time " + formatIntervalWithSeconds(lapData.currentLapTime(), false) + " (" + (lapData.diffLastLap() != null ? formatIntervalWithSeconds(lapData.diffLastLap(), true) : " - ") + ")");
                out.println();
                out.println();
            });
        }
    }

    public static String formatSrtTimestamp(final Duration duration) {
        final long interval = duration.toMillis();
        final long hour = TimeUnit.MILLISECONDS.toHours(interval) % 60;
        final long min = TimeUnit.MILLISECONDS.toMinutes(interval) % 60;
        final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) % 60;
        final long ms = TimeUnit.MILLISECONDS.toMillis(interval) % 1000;
        return String.format("%02d:%02d:%02d,%03d", hour, min, sec, ms);
    }

    public static String formatIntervalWithSeconds(final Duration duration, boolean sign) {
        final long interval = Math.abs(duration.toMillis());
        final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) % 60;
        final long ms = TimeUnit.MILLISECONDS.toMillis(interval) % 1000;
        return (sign ? (duration.isNegative() ? "-" : "+") : "") + String.format("%02d.%03d", sec, ms);
    }
}