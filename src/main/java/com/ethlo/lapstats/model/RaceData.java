package com.ethlo.lapstats.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

public class RaceData
{
    private final Map<Integer, Driver> driverData;

    private final Map<Integer, Duration> minLapTimes;

    private final Map<Integer, Duration> averageLapTimes;

    private final Map<Integer, Duration> maxLapTimes;
    private final Map<Integer, List<LapStatistics>> lapStatistics;

    private Map<Duration, LapStatistics> ticks;

    public RaceData(Map<Integer, List<Timing>> lapTimes, Map<Integer, Driver> driverData)
    {
        this.minLapTimes = new LinkedHashMap<>();
        this.averageLapTimes = new LinkedHashMap<>();
        this.maxLapTimes = new LinkedHashMap<>();
        this.ticks = new TreeMap<>();
        this.lapStatistics = new TreeMap<>();
        final Map<Integer, Duration> accumulatedLapTimes = new HashMap<>();

        lapTimes.forEach((lap, times) ->
        {
            for (int driverIndex = 0; driverIndex < times.size(); driverIndex++)
            {
                final Timing lapData = times.get(driverIndex);
                final Duration lapTime = lapData.time();

                minLapTimes.compute(driverIndex, (k, v) -> {
                    if (lap < 2)
                    {
                        return null;
                    }
                    else if (v == null)
                    {
                        return lapTime;
                    }
                    else
                    {
                        return v.compareTo(lapTime) > 0 ? lapTime : v;
                    }
                });

                maxLapTimes.compute(driverIndex, (k, v) -> {
                    if (v == null)
                    {
                        return lapTime;
                    }
                    else
                    {
                        return v.compareTo(lapTime) < 0 ? lapTime : v;
                    }
                });
            }
        });

        lapTimes.forEach((lap, times) ->
        {
            final List<LapStatistics> lapStatistics = new ArrayList<>(times.size());
            for (int driverIndex = 0; driverIndex < times.size(); driverIndex++)
            {
                final Timing lapData = times.get(driverIndex);
                final Duration lapTime = lapData.time();
                final Duration accumulatedLapsTime = accumulatedLapTimes.compute(driverIndex, (k, v) ->
                {
                    if (v == null)
                    {
                        return lapTime;
                    }
                    else
                    {
                        return v.plus(lapTime);
                    }
                });
                final Duration diffLastLap = getDiffToLastLap(lapTimes, lap, driverIndex, lapTime);

                lapStatistics.add(new LapStatistics(lapData, accumulatedLapsTime, minLapTimes.get(driverIndex), maxLapTimes.get(driverIndex), diffLastLap, false));
            }
            this.lapStatistics.put(lap, lapStatistics);
        });

        for (Map.Entry<Integer, List<LapStatistics>> e : this.lapStatistics.entrySet())
        {
            for (LapStatistics lapData : e.getValue())
            {
                this.ticks.put(lapData.accumulatedLapTime(), lapData);
            }
        }

        this.driverData = driverData;
        final Set<Integer> initialDrivers = getInitialDrivers();
        final Map<Integer, LapStatistics> driversLastLap = getDriversLastLap();
        for (Map.Entry<Integer, List<LapStatistics>> entry : lapStatistics.entrySet())
        {
            final int lap = entry.getKey();
            final List<LapStatistics> row = entry.getValue();
            if (row.size() < initialDrivers.size())
            {
                // We are missing data for the row
                final List<Integer> driversFoundForLap = row.stream().map(el -> el.timing().driverId()).toList();

                final List<Integer> toCalculate = new ArrayList<>(initialDrivers);
                toCalculate.removeAll(driversFoundForLap);
                for (int driverToCalculate : toCalculate)
                {
                    final LapStatistics lastForDriver = driversLastLap.get(driverToCalculate);
                    final Duration avgRound = lastForDriver.accumulatedLapTime().dividedBy(lastForDriver.timing().lap());
                    final Duration accumulated = lastForDriver.accumulatedLapTime().plus(avgRound.multipliedBy(lap - lastForDriver.timing().lap()));
                    row.add(new LapStatistics(new Timing(lap, driverToCalculate, avgRound), accumulated, lastForDriver.minLapTime(), lastForDriver.maxLapTime(), Duration.ZERO, true));
                }
            }
        }

        // Calculate average lap time per driver
        driversLastLap.forEach((k, v) -> this.averageLapTimes.put(v.timing().driverId(), v.accumulatedLapTime().dividedBy(v.timing().lap())));
    }

    private static Duration getDiffToLastLap(Map<Integer, List<Timing>> lapToDriverLapList, int lap, int driverId, Duration lapTime)
    {
        if (lap <= 2)
        {
            return null;
        }
        final Optional<Timing> lastLap = Optional.ofNullable(lapToDriverLapList.get(lap - 1)).map(driverLaps ->
        {
            for (Timing l : driverLaps)
            {
                if (l.driverId() == driverId)
                {
                    return l;
                }
            }
            return null;
        });
        return lastLap.map(l -> lapTime.minus(l.time())).orElse(null);
    }

    private Set<Integer> getInitialDrivers()
    {
        return new TreeSet<>(lapStatistics.values().iterator().next().stream().map(el -> el.timing().driverId()).toList());
    }

    private Map<Integer, LapStatistics> getDriversLastLap()
    {
        final Set<Integer> initialDrivers = getInitialDrivers();
        final Map<Integer, LapStatistics> result = new TreeMap<>();
        for (Map.Entry<Integer, List<LapStatistics>> entry : lapStatistics.entrySet())
        {
            final int lap = entry.getKey();
            final Sets.SetView<Integer> missing = Sets.difference(initialDrivers, entry.getValue().stream().map(el -> el.timing().driverId()).collect(Collectors.toSet()));
            for (int m : missing)
            {
                if (!result.containsKey(m))
                {
                    // Get previous timing
                    final LapStatistics lastLapForDriver = getLap(lap - 1).stream().filter(l -> l.timing().driverId() == m).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not find driver " + m + " for timing " + lap));
                    result.put(lastLapForDriver.timing().driverId(), lastLapForDriver);
                }
            }
        }
        for (LapStatistics lap : new ArrayList<>(lapStatistics.values()).get(lapStatistics.size() - 1))
        {
            result.putIfAbsent(lap.timing().driverId(), lap);
        }
        return result;
    }

    public LapStatistics getLap(Duration timestamp)
    {
        return ticks.get(timestamp);
    }

    public List<LapStatistics> getLap(int lap)
    {
        return lapStatistics.get(lap);
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

    public Map<Integer, List<LapStatistics>> getLaps()
    {
        return lapStatistics;
    }

    public Map<Integer, Duration> getMinLapTimes()
    {
        return minLapTimes;
    }

    public Map<Integer, Duration> getAverageLapTimes()
    {
        return averageLapTimes;
    }

    public Map<Integer, Duration> getMaxLapTimes()
    {
        return maxLapTimes;
    }
}
