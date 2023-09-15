package com.ethlo.lapstats.model;

public record ExtendedLapData(LapData lap, java.time.Duration accumulatedLapTime, java.time.Duration minLapTime, java.time.Duration maxLapTime,
                              java.time.Duration diffLastLap, boolean implicit)
{
}
