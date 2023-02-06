package ru.yandex.market.clab.common.test.asset;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 26.04.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class Dates {
    // https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
    public static final ZoneId MSK_ZONE = ZoneId.of("Europe/Moscow");

    public static final ZonedDateTime SUMMER_DAY = ZonedDateTime.of(LocalDate.of(2007, Month.JULY, 4)
        .atTime(16, 31), MSK_ZONE);

    private Dates() {
    }
}
