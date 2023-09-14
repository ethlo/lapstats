package com.ethlo.lapstats.model;

import java.time.Duration;

public record LapData(int lap, int driverId, Duration currentLapTime, Duration minLapTime,
                      Duration maxLapTime, Duration diffLastLap, Duration accumulatedLapTime,
                      Duration averageLapTime) {

}
