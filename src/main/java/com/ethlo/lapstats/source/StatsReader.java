package com.ethlo.lapstats.source;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.LapData;

public interface StatsReader
{
    Map<Integer, List<LapData>> getDriverLapTimes() throws IOException;

    Map<Integer, Driver> getDriverList() throws IOException;
}
