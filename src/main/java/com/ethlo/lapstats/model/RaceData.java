package com.ethlo.lapstats.model;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record RaceData(Map<Duration, LapData> ticks,
                       Map<Integer, List<LapData>> lapToDriverLapList,
                       Map<Integer, Driver> driverData) {
}
