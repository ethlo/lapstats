package com.ethlo.lapstats.render;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import com.ethlo.lapstats.model.RaceData;

public class CsvPlotStatusRenderer implements StatusRenderer
{
    @Override
    public void render(final RaceData raceData, final OutputStream out) throws IOException
    {
        final String result = new PebbleTemplateRenderer(true).renderFromTemplate(Map.of("data", raceData), "templates/csv_plot.tpl", Locale.ENGLISH, false);
        out.write(result.getBytes(StandardCharsets.UTF_8));
    }
}
