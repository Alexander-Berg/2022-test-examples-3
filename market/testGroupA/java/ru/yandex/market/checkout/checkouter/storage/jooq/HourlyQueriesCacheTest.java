package ru.yandex.market.checkout.checkouter.storage.jooq;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HourlyQueriesCacheTest {

    @Test
    public void isNeedWriteFullSqlQuery() {
        final ZoneId zoneId = ZoneId.systemDefault();
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(CheckouterDateFormats.DEFAULT);
        final Instant instant = LocalDateTime.parse("11-12-2019 11:00:59", dateTimeFormatter)
                .atZone(zoneId).toInstant();
        final Clock clock = Clock.fixed(instant, zoneId);
        assertTrue(HourlyQueriesCache.cacheAndCheckIfNeededToWriteFullSqlQuery("1", clock));
        assertFalse(HourlyQueriesCache.cacheAndCheckIfNeededToWriteFullSqlQuery("1", clock));
        assertFalse(HourlyQueriesCache.cacheAndCheckIfNeededToWriteFullSqlQuery("1", Clock.offset(clock,
                Duration.ofMinutes(59L))));
        assertTrue(HourlyQueriesCache.cacheAndCheckIfNeededToWriteFullSqlQuery("1", Clock.offset(clock,
                Duration.ofHours(1L))));
        assertTrue(HourlyQueriesCache.cacheAndCheckIfNeededToWriteFullSqlQuery("1", Clock.offset(clock,
                Duration.ofHours(2L))));
        assertTrue(HourlyQueriesCache.cacheAndCheckIfNeededToWriteFullSqlQuery("1", Clock.offset(clock,
                Duration.ofDays(1L))));
        assertTrue(HourlyQueriesCache.cacheAndCheckIfNeededToWriteFullSqlQuery("2", clock));
        assertFalse(HourlyQueriesCache.cacheAndCheckIfNeededToWriteFullSqlQuery("2", clock));
        assertFalse(HourlyQueriesCache.cacheAndCheckIfNeededToWriteFullSqlQuery("2", clock));
    }

    @Test
    public void getHash() {
        final String query = "select * from orders where id = 1000::bigint";
        assertEquals(String.valueOf(query.hashCode()), HourlyQueriesCache.getHash(query));
    }
}
