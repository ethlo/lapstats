package com.ethlo.lapstats.model;

import java.time.Duration;

public class LapStatistics
{
    private final Timing timing;
    private final Duration accumulatedLapTime;

    private final Duration minLapTime;

    private final Duration maxLapTime;

    private final Duration diffLastLap;
    private Duration diffLeader;

    private final boolean implicit;

    public LapStatistics(final Timing timing, final Duration accumulatedLapTime, final Duration minLapTime, final Duration maxLapTime, final Duration diffLastLap, final boolean implicit)
    {
        this.timing = timing;
        this.accumulatedLapTime = accumulatedLapTime;
        this.minLapTime = minLapTime;
        this.maxLapTime = maxLapTime;
        this.diffLastLap = diffLastLap;
        this.implicit = implicit;
    }

    public Duration getAccumulatedLapTime()
    {
        return accumulatedLapTime;
    }

    public Duration getMinLapTime()
    {
        return minLapTime;
    }

    public Duration getMaxLapTime()
    {
        return maxLapTime;
    }

    public Duration getDiffLastLap()
    {
        return diffLastLap;
    }

    public Duration getDiffLeader()
    {
        return diffLeader;
    }

    public boolean isImplicit()
    {
        return implicit;
    }

    public int getDriverId()
    {
        return timing.driverId();
    }

    public int getLap()
    {
        return timing.lap();
    }

    public Duration getTime()
    {
        return timing.time();
    }
}


