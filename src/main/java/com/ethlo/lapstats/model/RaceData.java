package com.ethlo.lapstats.model;

import com.google.common.collect.Sets;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class RaceData
{
    private final Map<Integer, List<ExtendedLapData>> lapToDriverLapList;
    private final Map<Integer, Driver> driverData;
    private Map<Duration, ExtendedLapData> ticks;

    public RaceData(Map<Integer, List<ExtendedLapData>> lapToDriverLapList,
                    Map<Integer, Driver> driverData)
    {
        this.ticks = new TreeMap<>();
        for (Map.Entry<Integer, List<ExtendedLapData>> e : lapToDriverLapList.entrySet())
        {
            for (ExtendedLapData lapData : e.getValue())
            {
                this.ticks.put(lapData.accumulatedLapTime(), lapData);
            }
        }

        this.lapToDriverLapList = lapToDriverLapList;
        final Set<Integer> initialDrivers = getInitialDrivers();
        final Map<Integer, ExtendedLapData> driversLastLap = getDriversLastLap();
        for (Map.Entry<Integer, List<ExtendedLapData>> entry : lapToDriverLapList.entrySet())
        {
            final int lap = entry.getKey();
            final List<ExtendedLapData> row = entry.getValue();
            if (row.size() < initialDrivers.size())
            {
                // We are missing data for the row
                final List<Integer> driversFoundForLap = row.stream().map(el -> el.lap().driverId()).toList();

                final List<Integer> toCalculate = new ArrayList<>(initialDrivers);
                toCalculate.removeAll(driversFoundForLap);
                for (int driverToCalculate : toCalculate)
                {
                    final ExtendedLapData lastForDriver = driversLastLap.get(driverToCalculate);
                    final Duration avgRound = lastForDriver.accumulatedLapTime().dividedBy(lastForDriver.lap().lap());
                    final Duration accumulated = lastForDriver.accumulatedLapTime().plus(avgRound.multipliedBy(lap - lastForDriver.lap().lap()));
                    row.add(new ExtendedLapData(new LapData(lap, driverToCalculate, avgRound), accumulated, lastForDriver.minLapTime(), lastForDriver.maxLapTime(), Duration.ZERO, true));
                }
            }
        }

        this.driverData = driverData;
    }

    private Set<Integer> getInitialDrivers()
    {
        return new TreeSet<>(lapToDriverLapList.values().iterator().next().stream().map(el -> el.lap().driverId()).toList());
    }

    private Map<Integer, ExtendedLapData> getDriversLastLap()
    {
        final Set<Integer> initialDrivers = getInitialDrivers();
        final Map<Integer, ExtendedLapData> result = new TreeMap<>();
        for (Map.Entry<Integer, List<ExtendedLapData>> entry : lapToDriverLapList.entrySet())
        {
            final int lap = entry.getKey();
            final Sets.SetView<Integer> missing = Sets.difference(initialDrivers, entry.getValue().stream().map(el -> el.lap().driverId()).collect(Collectors.toSet()));
            for (int m : missing)
            {
                if (!result.containsKey(m))
                {
                    // Get previous lap
                    final ExtendedLapData lastLapForDriver = getLap(lap - 1).stream().filter(l -> l.lap().driverId() == m).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not find driver " + m + " for lap " + lap));
                    result.put(lastLapForDriver.lap().driverId(), lastLapForDriver);
                }
            }
        }
        return result;
    }

    public ExtendedLapData getLapData(Duration timestamp)
    {
        return ticks.get(timestamp);
    }

    public List<ExtendedLapData> getLap(int lap)
    {
        return lapToDriverLapList.get(lap);
    }

    public Driver getDriverData(int driverId)
    {
        return driverData.get(driverId);
    }

    public Collection<Duration> getTicks()
    {
        return ticks.keySet();
    }

    public Collection<Driver> getDrivers()
    {
        return driverData.values();
    }
}
