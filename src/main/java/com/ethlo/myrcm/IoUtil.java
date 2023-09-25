package com.ethlo.myrcm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class IoUtil
{
    public static String getClassPathResourceString(final String path) throws IOException
    {
        final URL resource = IoUtil.class.getClassLoader().getResource(path);
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
