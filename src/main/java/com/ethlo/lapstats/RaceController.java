package com.ethlo.lapstats;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.RaceData;
import com.ethlo.lapstats.model.Timing;
import com.ethlo.lapstats.render.JsonStatusRenderer;
import com.ethlo.lapstats.source.myrcm.MyRcmReader;
import com.ethlo.myrcm.IoUtil;

@Controller
public class RaceController
{
    private static final Logger logger = LoggerFactory.getLogger(RaceController.class);
    private final ConcurrentMap<String, RaceData> cache = new ConcurrentHashMap<>();

    @GetMapping("/results/myrcm/{eventId}/{raceId}/{reportKey}")
    public String myrcm(@PathVariable("eventId") String eventId, @PathVariable("raceId") String raceId, @PathVariable("reportKey") String reportKey, Model model) throws IOException
    {
        final String url = "https://www.myrcm.ch/myrcm/report/en/" + eventId + "/" + raceId + "?reportKey=" + reportKey;
        final RaceData raceData = cache.computeIfAbsent(url, u ->
        {
            logger.info(url);
            try
            {
                MyRcmReader reader;
                if ("0".equals(eventId) && "0".equals(raceId) && "0".equals(reportKey))
                {
                    reader = new MyRcmReader(IoUtil.getClassPathResourceString("myrcm_sample.html"));
                }
                else
                {
                    reader = new MyRcmReader(new URL(url));
                }
                final Map<Integer, List<Timing>> lapTimes = reader.getDriverLapTimes();
                final Map<Integer, Driver> driverList = reader.getDriverList();
                return new RaceData(lapTimes, driverList);
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        });
        model.addAttribute("data", raceData);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new JsonStatusRenderer().render(raceData, bout);
        model.addAttribute("json", bout.toString(StandardCharsets.UTF_8));
        return "web";
    }
}
