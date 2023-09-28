package com.ethlo.lapstats.source;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.Timing;

public interface StatsReader
{
    Map<Integer, List<Timing>> getDriverLapTimes();

    Map<Integer, Driver> getDriverList();

    LocalDateTime getDate();

    String getName();
}
