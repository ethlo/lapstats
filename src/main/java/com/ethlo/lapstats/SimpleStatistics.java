package com.ethlo.lapstats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.ExtendedLapData;
import com.ethlo.lapstats.model.LapData;
import com.ethlo.lapstats.model.RaceData;

public class SimpleStatistics
{
    public RaceData process(Map<Integer, List<LapData>> lapTimes, Map<Integer, Driver> driverList)
    {
        final Map<Integer, List<ExtendedLapData>> extendedLapData = new TreeMap<>();
        final Map<Integer, Duration> accumulatedLapTimes = new HashMap<>();
        final Map<Integer, Duration> minLapTimes = new HashMap<>();
        final Map<Integer, Duration> maxLapTimes = new HashMap<>();

        lapTimes.forEach((lap, times) ->
        {
            final List<ExtendedLapData> extendedRow = new ArrayList<>(times.size());
            for (int driverIndex = 0; driverIndex < times.size(); driverIndex++)
            {
                final LapData lapData = times.get(driverIndex);
                final Duration lapTime = lapData.time();
                final Duration accumulatedLapTime = accumulatedLapTimes.compute(driverIndex, (k, v) -> {
                    if (v == null)
                    {
                        return lapTime;
                    }
                    else
                    {
                        return v.plus(lapTime);
                    }
                });

                final Duration minLapTime = minLapTimes.compute(driverIndex, (k, v) -> {
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

                final Duration maxLapTime = maxLapTimes.compute(driverIndex, (k, v) -> {
                    if (v == null)
                    {
                        return lapTime;
                    }
                    else
                    {
                        return v.compareTo(lapTime) < 0 ? lapTime : v;
                    }
                });

                final Duration diffLastLap = getDiffToLastLap(lapTimes, lap, driverIndex, lapTime);

                extendedRow.add(new ExtendedLapData(lapData, accumulatedLapTime, minLapTime, maxLapTime, diffLastLap, false));
            }
            extendedLapData.put(lap, extendedRow);
        });

        return new RaceData(extendedLapData, driverList);
    }

    private static Duration getDiffToLastLap(Map<Integer, List<LapData>> lapToDriverLapList, int lap, int driverId, Duration lapTime)
    {
        if (lap <= 2)
        {
            return null;
        }
        final Optional<LapData> lastLap = Optional.ofNullable(lapToDriverLapList.get(lap - 1)).map(driverLaps ->
        {
            for (LapData l : driverLaps)
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
}