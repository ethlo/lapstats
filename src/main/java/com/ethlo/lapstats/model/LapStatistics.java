package com.ethlo.lapstats.model;

import java.time.Duration;

public record LapStatistics(Timing timing, Duration accumulatedLapTime, Duration minLapTime, Duration maxLapTime,
                            Duration diffLastLap, boolean implicit)
{
}
