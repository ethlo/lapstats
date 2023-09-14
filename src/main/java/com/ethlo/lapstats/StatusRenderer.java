package com.ethlo.lapstats;

import com.ethlo.lapstats.model.RaceData;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public interface StatusRenderer {
    void render(RaceData raceData, OutputStream out) throws IOException;
}
