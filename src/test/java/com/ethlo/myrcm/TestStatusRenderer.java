package com.ethlo.myrcm;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.RaceData;
import com.ethlo.lapstats.model.Timing;
import com.ethlo.lapstats.render.AsciiStatusRenderer;
import com.ethlo.lapstats.render.CsvPlotStatusRenderer;
import com.ethlo.lapstats.render.JsonStatusRenderer;
import com.ethlo.lapstats.render.PebbleRenderer;
import com.ethlo.lapstats.source.StatsReader;
import com.ethlo.lapstats.source.myrcm.MyRcmReader;

public class TestStatusRenderer
{
    private static final Logger logger = LoggerFactory.getLogger(TestStatusRenderer.class);
    private final StatsReader reader;

    public TestStatusRenderer() throws IOException
    {
        reader = new MyRcmReader(IoUtil.getClassPathResourceString("myrcm_sample.html"));
    }

    @Test
    void testRenderProgressAscii() throws IOException
    {
        final RaceData raceData = new RaceData(reader);
        new AsciiStatusRenderer().render(raceData, getFileOutputStream("lapstats_ascii.txt"));
    }

    @Test
    void testRenderProgressJson() throws IOException
    {
        final RaceData raceData = new RaceData(reader);
        new JsonStatusRenderer().render(raceData, getFileOutputStream("lapstats.json"));
    }

    @Test
    void testRenderCsvPlot() throws IOException
    {
        final RaceData raceData = new RaceData(reader);
        new CsvPlotStatusRenderer().render(raceData, getFileOutputStream("lapstats_csv_plot.csv"));
    }

    @Test
    void testRenderHtml() throws IOException
    {
        final RaceData raceData = new RaceData(reader);
        new PebbleRenderer("web.tpl.html").render(raceData, getFileOutputStream("lapstats_web.html"));
    }

    private OutputStream getFileOutputStream(String name) throws IOException
    {
        final String tmpDir = System.getProperty("java.io.tmpdir");
        final Path path = Paths.get(tmpDir).resolve(name);
        logger.info("Writing to {}", path);
        return Files.newOutputStream(path);
    }
}
