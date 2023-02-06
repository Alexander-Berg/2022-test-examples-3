package ru.yandex.market.sre.services.tms.eventdetector.service.startrek.format;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GrafanaTicketFormatterTest {

    @Test
    public void format() {
        String input = "\n#|" +
                "\n|| **" + "https://ya.ru" + "**" +
                "\n" +
                "\n" + "https://ya.ru" +
                "\n||" +
                "\n|#";
        if (input.endsWith("|#")) {
            input = input.substring(0, input.length() - 6);
        }
        assertEquals("\n" +
                "#|\n" +
                "|| **https://ya.ru**\n" +
                "\n" +
                "https://ya.ru", input);
    }
}
