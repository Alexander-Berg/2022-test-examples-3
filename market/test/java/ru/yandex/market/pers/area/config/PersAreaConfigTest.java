package ru.yandex.market.pers.area.config;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 09.11.17
 */
public class PersAreaConfigTest {
    @Test
    public void testApiDateTimeFormat() {
        ZonedDateTime localDateTime = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PersAreaConfig.API_DATE_TIME_FORMAT);
        String formattedString = formatter.format(localDateTime);
        System.out.println(formattedString);
        assertNotNull(formattedString);
        assertNotNull(formatter.parse(formattedString));
    }
}
