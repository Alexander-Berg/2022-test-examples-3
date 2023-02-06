package ru.yandex.personal.mail.search.metrics.scraper.services.scraping.systems.yweb.search;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.personal.mail.search.metrics.scraper.utils.DateTimeUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YaDateParserTest {
    private YaDateParser parser = new YaDateParser();

    @BeforeAll
    static void setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
    }

    @Test
    void parseDateString() {
        assertEquals(DateTimeUtils.sameDayButCurrentYear(1540741140),
                parser.parseDateString("Отправлено 28 окт в 18:39"));
        assertEquals(Instant.ofEpochSecond(1409444580), parser.parseDateString("Получено 31.08.14 в 3:23"));
    }

    @Test
    void purifyDateString() {
        assertEquals("28 окт в 18:39", parser.purifyDateString("Отправлено 28 окт в 18:39"));
        assertEquals("28 сен в 14:58", parser.purifyDateString("Получено 28 сен в 14:58"));
        assertEquals("31.08.74 в 3:23", parser.purifyDateString("Получено 31.08.74 в 3:23"));
    }

    @Test
    void todayParser() {
        LocalDateTime ldt = LocalDateTime.now().withHour(12).withMinute(4).withSecond(0).withNano(0);

        assertEquals(ldt, LocalDateTime.from(
                YaDateParser.todayFormatterBuilder.get().toFormatter(new Locale("RU", "ru"))
                        .parse("сегодня в 12:04")
        ));
    }

    @Test
    void yesterdayParser() {
        LocalDateTime ldt = LocalDateTime.now().withHour(2).withMinute(14).withSecond(0).withNano(0).minusDays(1);

        assertEquals(ldt, LocalDateTime.from(
                YaDateParser.yesterdayFormatterBuilder.get().toFormatter(new Locale("RU", "ru"))
                        .parse("вчера в 2:14")
        ));
    }

    @Test
    void partialDateParser() {
        LocalDateTime ldt = LocalDateTime.now()
                .withMonth(9)
                .withDayOfMonth(28)
                .withHour(14)
                .withMinute(58)
                .withSecond(0)
                .withNano(0);

        assertEquals(ldt, LocalDateTime.from(
                YaDateParser.partialDateFormatterBuilder.get().toFormatter(new Locale("RU", "ru"))
                        .parse("28 сен в 14:58")
        ));
    }

    @Test
    void fullDateParser() {
        LocalDateTime ldt = LocalDateTime.of(2016, 6, 21, 0, 0);

        assertEquals(ldt, LocalDateTime.from(
                YaDateParser.fullDateFormatterBuilder.get().toFormatter(new Locale("RU", "ru"))
                        .parse("21.06.16 в 0:00")
        ));
    }
}
