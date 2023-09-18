package com.ethlo.myrcm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.RaceData;
import com.ethlo.lapstats.model.Timing;
import com.ethlo.lapstats.render.AsciiStatusRenderer;
import com.ethlo.lapstats.render.CsvPlotStatusRenderer;
import com.ethlo.lapstats.render.StaticHtmlRenderer;
import com.ethlo.lapstats.source.StatsReader;
import com.ethlo.lapstats.source.myrcm.MyRcmReader;

public class TestStatusRenderer
{
    @Test
    void testRenderProgressAscii() throws IOException
    {
        //final StatsReader reader = new MyRcmReader(new URL("https://www.myrcm.ch/myrcm/report/en/72922/317749?reportKey=1673"));
        final StatsReader reader = new MyRcmReader(getClassPathResourceString("myrcm_sample.html"));
        final Map<Integer, List<Timing>> lapTimes = reader.getDriverLapTimes();
        final Map<Integer, Driver> driverList = reader.getDriverList();
        final RaceData raceData = new RaceData(lapTimes, driverList);
        new AsciiStatusRenderer().render(raceData, Files.newOutputStream(Files.createTempFile("lapstats_ascii_", ".txt")));
    }

    @Test
    void testRenderCsvPlot() throws IOException
    {
        final StatsReader reader = new MyRcmReader(getClassPathResourceString("myrcm_sample.html"));
        final Map<Integer, List<Timing>> lapTimes = reader.getDriverLapTimes();
        final Map<Integer, Driver> driverList = reader.getDriverList();
        final RaceData raceData = new RaceData(lapTimes, driverList);
        new CsvPlotStatusRenderer().render(raceData, Files.newOutputStream(Files.createTempFile("lapstats_csv_plot_", ".csv")));
    }

    @Test
    void testRenderHtml() throws IOException
    {
        final StatsReader reader = new MyRcmReader(getClassPathResourceString("myrcm_sample.html"));
        final Map<Integer, List<Timing>> lapTimes = reader.getDriverLapTimes();
        final Map<Integer, Driver> driverList = reader.getDriverList();
        final RaceData raceData = new RaceData(lapTimes, driverList);
        new StaticHtmlRenderer().render(raceData, Files.newOutputStream(Files.createTempFile("lapstats_web_", ".html")));
    }

    private String getClassPathResourceString(final String path) throws IOException
    {
        final URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null)
        {
            try (final InputStream in = resource.openStream())
            {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
