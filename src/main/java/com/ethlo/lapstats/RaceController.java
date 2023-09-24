package com.ethlo.lapstats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.ethlo.lapstats.render.JsonStatusRenderer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ethlo.lapstats.model.Driver;
import com.ethlo.lapstats.model.RaceData;
import com.ethlo.lapstats.model.Timing;
import com.ethlo.lapstats.source.myrcm.MyRcmReader;
import jakarta.websocket.server.PathParam;

@Controller
public class RaceController
{
    @GetMapping("/results/myrcm/{eventId}/{raceId}")
    public String myrcm(@PathParam("eventId") String eventId, @PathParam("raceId") String raceId, Model model) throws IOException
    {
        final MyRcmReader reader = new MyRcmReader(new URL("https://www.myrcm.ch/myrcm/report/en/72922/317749?reportKey=1673"));
        final Map<Integer, List<Timing>> lapTimes = reader.getDriverLapTimes();
        final Map<Integer, Driver> driverList = reader.getDriverList();
        final RaceData raceData = new RaceData(lapTimes, driverList);
        model.addAttribute("data", raceData);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new JsonStatusRenderer().render(raceData, bout);
        model.addAttribute("json", bout.toString(StandardCharsets.UTF_8));
        return "web";
    }
}
