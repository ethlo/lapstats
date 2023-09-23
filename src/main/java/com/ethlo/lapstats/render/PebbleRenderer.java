package com.ethlo.lapstats.render;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import com.ethlo.lapstats.model.RaceData;

public class PebbleRenderer implements StatusRenderer
{
    private final String template;

    public PebbleRenderer(final String template)
    {
        this.template = template;
    }

    @Override
    public void render(final RaceData raceData, final OutputStream out) throws IOException
    {
        final String result = new PebbleTemplateRenderer(true).renderFromTemplate(Map.of("data", raceData), "templates/" + template, Locale.ENGLISH, false);
        out.write(result.getBytes(StandardCharsets.UTF_8));
    }
}
