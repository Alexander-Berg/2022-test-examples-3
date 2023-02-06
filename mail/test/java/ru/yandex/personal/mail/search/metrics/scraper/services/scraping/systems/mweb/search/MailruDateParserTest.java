package ru.yandex.personal.mail.search.metrics.scraper.services.scraping.systems.mweb.search;

import java.time.Instant;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.personal.mail.search.metrics.scraper.utils.DateTimeUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;


class MailruDateParserTest {
    private final MailruDateParser parser = new MailruDateParser();

    @BeforeAll
    static void setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
    }

    @Test
    void parseStringDate() {
        assertEquals(DateTimeUtils.sameDayButCurrentYear(1538135100), parser.parseStringDate("28 сентября, 14:45"));
        assertEquals(Instant.ofEpochSecond(147140580), parser.parseStringDate("31 августа 1974, 3:23"));
        assertEquals(DateTimeUtils.sameDayButCurrentYear(1525122720), parser.parseStringDate("1 мая, 0:12"));
    }
}
