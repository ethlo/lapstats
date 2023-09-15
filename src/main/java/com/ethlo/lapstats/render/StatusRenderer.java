package com.ethlo.lapstats.render;

import java.io.IOException;
import java.io.OutputStream;

import com.ethlo.lapstats.model.RaceData;

public interface StatusRenderer
{
    void render(RaceData raceData, OutputStream out) throws IOException;
}
