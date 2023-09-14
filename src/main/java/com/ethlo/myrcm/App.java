package com.ethlo.myrcm;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        new PageLoader().load("https://www.myrcm.ch/myrcm/report/en/72922/317749?reportKey=1673");
    }
}
