package ru.yandex.market.core.periodic_survey.service.scheduler;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodicSurveyPartnersDayFilterTest {

    @Test
    void filter() {
        var todayFilter = getFilter("2021-08-13T10:15:30.00Z");
        var tomorrowFilter = getFilter("2021-08-14T03:15:30.00Z");

        assertFalse(todayFilter.test(131L));
        assertFalse(todayFilter.test(131L + 180*1234));
        assertFalse(tomorrowFilter.test(131L));
        assertFalse(tomorrowFilter.test(131L + 180*2345));

        assertTrue(todayFilter.test(132L));
        assertTrue(todayFilter.test(132L + 180*4321));
        assertFalse(tomorrowFilter.test(132L));
        assertFalse(tomorrowFilter.test(132L + 180*3456));

        assertFalse(todayFilter.test(133L));
        assertFalse(todayFilter.test(133L + 180*2222));
        assertTrue(tomorrowFilter.test(133L));
        assertTrue(tomorrowFilter.test(133L + 180*3333));

        assertFalse(todayFilter.test(134L));
        assertFalse(todayFilter.test(134L + 180*5432));
        assertFalse(tomorrowFilter.test(134L));
        assertFalse(tomorrowFilter.test(134L + 180*9999));
    }

    private static Predicate<Long> getFilter(String datetime) {
        Clock fixedClock = Clock.fixed(Instant.parse(datetime), ZoneId.of("UTC"));
        return new PeriodicSurveyPartnersDayFilter(180, fixedClock).shouldBeSurveyedTodayPredicate();
    }
}
