package ru.yandex.market.abo.core.startrek;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 14.08.18.
 */
public class StartrekQueryTest {

    @Test
    public void testBuildQuery() {
        String query = StartrekQuery.builder()
                .withQueue("some_queue")
                .withCreatedAfter(LocalDateTime.of(2019, 1, 1, 12, 30), false)
                .withUpdatedAfter(LocalDate.of(1995, Month.DECEMBER, 22), true)
                .withCreatedBefore(LocalDate.of(2020, 1, 1), true)
                .build()
                .buildStringQuery();

        assertEquals("" +
                        "\"updated\":>=\"1995-12-22 00:00:00\" \"queue\":some_queue " +
                        "\"created\":>\"2019-01-01 12:30:00\" \"created\":<\"2020-01-02 00:00:00\" ",
                query
        );
    }
}
