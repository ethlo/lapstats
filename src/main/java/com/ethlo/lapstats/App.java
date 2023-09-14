package com.ethlo.lapstats;

import com.ethlo.lapstats.model.RaceData;
import com.ethlo.lapstats.source.myrcm.MyRcmReader;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        final RaceData raceData = new MyRcmReader().load("https://www.myrcm.ch/myrcm/report/en/72922/317749?reportKey=1673");

        //new SrtRenderer().render(raceData, System.out);
        new JsonStatusRenderer().render(raceData, System.out);
    }
}
