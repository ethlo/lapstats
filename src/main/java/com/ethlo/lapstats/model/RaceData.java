package com.ethlo.lapstats.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.ethlo.lapstats.source.StatsReader;
import com.google.common.collect.Sets;

public class RaceData
{
    private final LocalDateTime date;

    private final String name;

    private final Map<Integer, Driver> driverData;

    private final Map<Integer, Duration> minLapTimes;

    private final Map<Integer, Duration> averageLapTimes;

    private final Map<Integer, Duration> maxLapTimes;
    private final Map<Integer, List<LapStatistics>> lapStatistics;

    private final Map<Duration, LapStatistics> ticks;

    public RaceData(Map<Integer, List<Timing>> lapTimes, final LocalDateTime date, final String name, Map<Integer, Driver> driverData)
    {
        this.date = date;
        this.name = name;
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
                this.ticks.put(lapData.getAccumulatedLapTime(), lapData);
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
                final List<Integer> driversFoundForLap = row.stream().map(LapStatistics::getDriverId).toList();

                final List<Integer> toCalculate = new ArrayList<>(initialDrivers);
                toCalculate.removeAll(driversFoundForLap);
                for (int driverToCalculate : toCalculate)
                {
                    final LapStatistics lastForDriver = driversLastLap.get(driverToCalculate);
                    final Duration avgRound = lastForDriver.getAccumulatedLapTime().dividedBy(lastForDriver.getLap());
                    final Duration accumulated = lastForDriver.getAccumulatedLapTime().plus(avgRound.multipliedBy(lap - lastForDriver.getLap()));
                    row.add(new LapStatistics(new Timing(lap, driverToCalculate, avgRound), accumulated, lastForDriver.getMinLapTime(), lastForDriver.getMaxLapTime(), Duration.ZERO, true));
                }
            }
        }

        // Calculate diff to leader
        ticks.forEach((timestamp, data) ->
        {
            final List<LapStatistics> forSameLap = getLap(data.getLap());
            forSameLap.sort(Comparator.comparing(LapStatistics::getAccumulatedLapTime));
            final LapStatistics firstPos = forSameLap.get(0);
            final Duration diffToCurrent = firstPos.getAccumulatedLapTime().minus(timestamp).abs();
            data.setDiffLeader(diffToCurrent);
        });

        // Calculate average lap time per driver
        driversLastLap.forEach((k, v) -> this.averageLapTimes.put(v.getDriverId(), v.getAccumulatedLapTime().dividedBy(v.getLap())));
    }

    public RaceData(StatsReader reader)
    {
        this(reader.getDriverLapTimes(), reader.getDate(), reader.getName(), reader.getDriverList());
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
        return new TreeSet<>(lapStatistics.values().iterator().next().stream().map(LapStatistics::getDriverId).toList());
    }

    private Map<Integer, LapStatistics> getDriversLastLap()
    {
        final Set<Integer> initialDrivers = getInitialDrivers();
        final Map<Integer, LapStatistics> result = new TreeMap<>();
        for (Map.Entry<Integer, List<LapStatistics>> entry : lapStatistics.entrySet())
        {
            final int lap = entry.getKey();
            final Sets.SetView<Integer> missing = Sets.difference(initialDrivers, entry.getValue().stream().map(LapStatistics::getDriverId).collect(Collectors.toSet()));
            for (int m : missing)
            {
                if (!result.containsKey(m))
                {
                    // Get previous timing
                    final LapStatistics lastLapForDriver = getLap(lap - 1).stream().filter(l -> l.getDriverId() == m).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not find driver " + m + " for timing " + lap));
                    result.put(lastLapForDriver.getDriverId(), lastLapForDriver);
                }
            }
        }
        for (LapStatistics lap : new ArrayList<>(lapStatistics.values()).get(lapStatistics.size() - 1))
        {
            result.putIfAbsent(lap.getDriverId(), lap);
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

    public LocalDateTime getDate()
    {
        return date;
    }

    public String getName()
    {
        return name;
    }
}
