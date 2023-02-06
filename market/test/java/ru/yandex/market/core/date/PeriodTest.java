package ru.yandex.market.core.date;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodTest {

    private static final Instant FROM = Instant.parse("2018-10-23T10:12:35Z");
    private static final Instant TO = Instant.parse("2018-10-27T10:12:35Z");

    private static final Period PERIOD = new Period(FROM, TO);

    @Test
    void testDateFromEqualsToDateTo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Period(FROM, FROM)
        );

        assertEquals(
                "Start time should be before the end time",
                exception.getMessage()
        );
    }

    @Test
    void testDateFromIsAfterDateTo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Period(TO, FROM)
        );

        assertEquals(
                "Start time should be before the end time",
                exception.getMessage()
        );
    }

    @Test
    void testInPeriod() {
        assertTrue(PERIOD.inPeriod(Instant.parse("2018-10-25T10:12:35Z")));
    }

    @Test
    void testDateFromIsInPeriod() {
        assertTrue(PERIOD.inPeriod(FROM));
    }

    @Test
    void testDateToNotInPeriod() {
        assertFalse(PERIOD.inPeriod(TO));
    }

    @Test
    void testDateBeforeDateFromNotInPeriod() {
        assertFalse(PERIOD.inPeriod(Instant.parse("2018-10-23T10:12:34Z")));
    }

    @Test
    void testDateAfterDateToNotInPeriod() {
        assertFalse(PERIOD.inPeriod(Instant.parse("2018-10-27T10:12:36Z")));
    }

    @Test
    void testIntersectionLeft() {
        assertTrue(PERIOD.intersection(
                new Period(
                        Instant.parse("2018-10-20T10:12:35Z"),
                        Instant.parse("2018-10-24T10:12:35Z")
                )
        ));
    }

    @Test
    void testIntersectionRight() {
        assertTrue(PERIOD.intersection(
                new Period(
                        Instant.parse("2018-10-24T10:12:35Z"),
                        Instant.parse("2018-10-29T10:12:35Z")
                )
        ));
    }

    @Test
    void testTheFirstPeriodWithinTheSecondPeriod() {
        assertTrue(PERIOD.intersection(
                new Period(
                        Instant.parse("2018-10-20T10:12:35Z"),
                        Instant.parse("2018-10-29T10:12:35Z")
                )
        ));
    }

    @Test
    void testTheSecondPeriodIsBeforeTheFirstPeriod() {
        assertFalse(PERIOD.intersection(
                new Period(
                        Instant.parse("2018-10-20T10:12:35Z"),
                        Instant.parse("2018-10-23T10:12:34Z")
                )
        ));
    }

    @Test
    void testTheSecondPeriodIsAfterTheFirstPeriod() {
        assertFalse(PERIOD.intersection(
                new Period(
                        TO,
                        Instant.parse("2018-10-29T10:12:35Z")
                )
        ));
    }

    @Test
    void testTheSecondPeriodWithinTheFirstPeriod() {
        assertTrue(PERIOD.intersection(
                new Period(
                        Instant.parse("2018-10-24T10:12:35Z"),
                        Instant.parse("2018-10-25T10:12:35Z")
                )
        ));
    }
}
