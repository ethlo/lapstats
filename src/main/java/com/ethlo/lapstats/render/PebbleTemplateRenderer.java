package com.ethlo.lapstats.render;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.loader.StringLoader;

public class PebbleTemplateRenderer
{
    private final PebbleEngine engine;

    public PebbleTemplateRenderer(boolean strict)
    {
        final Map<String, Filter> filters = new TreeMap<>();
        //filters.put("date", new PebbleDateFilter());

        engine = new PebbleEngine.Builder()
                .strictVariables(strict)
                .loader(new StringLoader())
                .extension(new AbstractExtension()
                {
                    @Override
                    public Map<String, Filter> getFilters()
                    {
                        return filters;
                    }
                }).build();
    }

    public String renderFromTemplate(Map<String, Object> data, String template, Locale locale, boolean disableEscaping) throws PebbleException, IOException
    {
        final String message = new String(getClass().getClassLoader().getResourceAsStream(template).readAllBytes(), StandardCharsets.UTF_8.name());
        return render(data, message, locale, disableEscaping);
    }

    public String render(Map<String, Object> data, String message, Locale locale, boolean disableEscaping) throws PebbleException, IOException
    {
        if (disableEscaping)
        {
            message = "{% autoescape false %}" + message + "{% endautoescape %}";
        }

        final StringWriter sw = new StringWriter();
        engine.getTemplate(message).evaluate(sw, data, locale);
        return sw.toString();
    }

    public String render(Map<String, Object> data, String message, Locale locale) throws PebbleException, IOException
    {
        return render(data, message, locale, false);
    }
}
