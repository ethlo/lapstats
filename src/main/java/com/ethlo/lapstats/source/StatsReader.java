package com.ethlo.lapstats.source;

import com.ethlo.lapstats.model.RaceData;

import java.io.IOException;

public interface StatsReader {
    RaceData load(String url) throws IOException;
}
