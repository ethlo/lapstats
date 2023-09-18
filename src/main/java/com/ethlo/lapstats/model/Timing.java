package com.ethlo.lapstats.model;

import java.time.Duration;

public record Timing(int lap, int driverId, Duration time)
{

}
