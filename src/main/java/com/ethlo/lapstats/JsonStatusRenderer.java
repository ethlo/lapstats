package com.ethlo.lapstats;

import com.ethlo.lapstats.model.LapData;
import com.ethlo.lapstats.model.RaceData;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

public class JsonStatusRenderer implements StatusRenderer {
    @Override
    public void render(RaceData raceData, OutputStream out) throws IOException {
        try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            for (LapData data : raceData.ticks().values()) {
                final List<LapData> forSameLap = raceData.lapToDriverLapList().get(data.lap());
                forSameLap.sort(Comparator.comparing(LapData::accumulatedLapTime));
                pw.println("\n------ # " + data.lap() + " (" + data.accumulatedLapTime() + ") ------");
                for (int pos = 0; pos < forSameLap.size(); pos++) {
                    final LapData l = forSameLap.get(pos);
                    final boolean current = data.driverId() == l.driverId() && data.lap() == l.lap();
                    pw.println((current ? "--> " : "") + (pos + 1) + " - " + raceData.driverData().get(l.driverId()).name() + " - " + l.accumulatedLapTime() + (!current ? (" - " + l.accumulatedLapTime().minus(data.accumulatedLapTime())) : ""));
                }
            }
        }
    }
}
