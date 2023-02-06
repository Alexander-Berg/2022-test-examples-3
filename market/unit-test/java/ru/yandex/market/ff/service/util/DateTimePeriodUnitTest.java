package ru.yandex.market.ff.service.util;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.enums.DatePeriodsNeighborshipType;
import ru.yandex.market.ff.service.util.dateTime.DateTimePeriod;

public class DateTimePeriodUnitTest {

    @Test
    void getNeighborshipTypeSameTimeTest() {
        LocalDateTime from = LocalDateTime.of(2018, 1, 1, 11, 0, 0);
        LocalDateTime to = LocalDateTime.of(2018, 1, 1, 12, 0, 0);
        DateTimePeriod dateTimePeriod = new DateTimePeriod(from, to);

        LocalDateTime from2 = LocalDateTime.of(2018, 1, 1, 11, 0, 0);
        LocalDateTime to2 = LocalDateTime.of(2018, 1, 1, 12, 0, 0);
        DateTimePeriod dateTimePeriod2 = new DateTimePeriod(from2, to2);

        Assertions.assertEquals(DatePeriodsNeighborshipType.SAME_TIME,
                dateTimePeriod.getNeighborshipType(dateTimePeriod2));
    }

    @Test
    void getNeighborshipTypeBeforeTest() {
        LocalDateTime from = LocalDateTime.of(2018, 1, 1, 11, 0, 0);
        LocalDateTime to = LocalDateTime.of(2018, 1, 1, 12, 0, 0);
        DateTimePeriod dateTimePeriod = new DateTimePeriod(from, to);

        LocalDateTime from2 = LocalDateTime.of(2018, 1, 1, 12, 0, 0);
        LocalDateTime to2 = LocalDateTime.of(2018, 1, 1, 13, 0, 0);
        DateTimePeriod dateTimePeriod2 = new DateTimePeriod(from2, to2);

        Assertions.assertEquals(DatePeriodsNeighborshipType.BEFORE,
                dateTimePeriod.getNeighborshipType(dateTimePeriod2));
    }

    @Test
    void getNeighborshipTypeAfterTest() {
        LocalDateTime from = LocalDateTime.of(2018, 1, 1, 11, 0, 0);
        LocalDateTime to = LocalDateTime.of(2018, 1, 1, 12, 0, 0);
        DateTimePeriod dateTimePeriod = new DateTimePeriod(from, to);

        LocalDateTime from2 = LocalDateTime.of(2018, 1, 1, 10, 0, 0);
        LocalDateTime to2 = LocalDateTime.of(2018, 1, 1, 11, 0, 0);
        DateTimePeriod dateTimePeriod2 = new DateTimePeriod(from2, to2);

        Assertions.assertEquals(DatePeriodsNeighborshipType.AFTER,
                dateTimePeriod.getNeighborshipType(dateTimePeriod2));
    }

    @Test
    void getNeighborshipTypeNoneTest() {
        LocalDateTime from = LocalDateTime.of(2018, 1, 1, 11, 0, 0);
        LocalDateTime to = LocalDateTime.of(2018, 1, 1, 12, 0, 0);
        DateTimePeriod dateTimePeriod = new DateTimePeriod(from, to);

        LocalDateTime from2 = LocalDateTime.of(2018, 1, 1, 9, 0, 0);
        LocalDateTime to2 = LocalDateTime.of(2018, 1, 1, 10, 0, 0);
        DateTimePeriod dateTimePeriod2 = new DateTimePeriod(from2, to2);

        Assertions.assertEquals(DatePeriodsNeighborshipType.NONE,
                dateTimePeriod.getNeighborshipType(dateTimePeriod2));
    }
}
