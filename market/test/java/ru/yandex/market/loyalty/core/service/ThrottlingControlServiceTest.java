package ru.yandex.market.loyalty.core.service;

import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.core.dao.ThrottlingControlDao;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.ThrottlingControlKey;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class ThrottlingControlServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    @After
    public void clean() {
        ThrottlingControlDao throttlingControlDao = throttlingControlDao(Clock.systemDefaultZone());

        throttlingControlDao.reset(ThrottlingControlKey.COINS);
        throttlingControlDao.reset(ThrottlingControlKey.COUPON);
    }

    @Test
    public void allKeysShouldExistsInDB() {
        ThrottlingControlDao throttlingControlDao = throttlingControlDao(Clock.systemDefaultZone());

        Set<ThrottlingControlKey> notExistsInDB = Arrays.stream(ThrottlingControlKey.values())
                .filter(k -> k != ThrottlingControlKey.UNKNOWN)
                .filter(k -> !throttlingControlDao.exists(k))
                .collect(Collectors.toSet());
        assertThat(notExistsInDB, is(empty()));
    }

    @Test
    public void requestTime() {
        ClockForTests clock = new ClockForTests();
        ThrottlingControlDao throttlingControlDao = throttlingControlDao(clock);

        assertThat(
                throttlingControlDao.requestTime(ThrottlingControlKey.COINS, Duration.ofSeconds(10)),
                success(Duration.ofSeconds(0))
        );

        clock.spendTime(Duration.ofSeconds(5));

        assertThat(
                throttlingControlDao.requestTime(ThrottlingControlKey.COINS, Duration.ofSeconds(10)),
                success(Duration.ofSeconds(5))
        );

        clock.spendTime(Duration.ofSeconds(2));

        assertThat(
                throttlingControlDao.requestTime(ThrottlingControlKey.COINS, Duration.ofSeconds(10)),
                notSuccess(Duration.ofSeconds(10))
        );
    }

    @Repeat(5)
    @Test
    public void shouldThrottleParallelProcesses() throws InterruptedException {
        Clock clock = Clock.systemDefaultZone();
        ThrottlingControlDao throttlingControlDao = throttlingControlDao(clock);

        ThrottlingControlService throttlingControlService = ThrottlingControlService.production(
                throttlingControlDao, clock,
                configurationService
        );

        assertEquals(30, ThrottlingControlKey.COUPON.maxCountPerSecond(LocalDateTime.now(clock),
                Collections.emptyMap()));

        Instant workUntil = clock.instant().plus(Duration.ofSeconds(2));
        AtomicLong counter = new AtomicLong();
        int batchSize = 5;
        testConcurrency(() -> () -> {
            while (clock.instant().isBefore(workUntil)) {
                if (throttlingControlService.requestBatch(ThrottlingControlKey.COUPON, batchSize, workUntil)) {
                    counter.addAndGet(batchSize);
                }
            }
        });
        assertThat(counter.get(), either(equalTo(60L)).or(equalTo(65L)));
    }

    @NotNull
    private static Matcher<ThrottlingControlDao.RequestResult> success(Duration timeToWait) {
        return allOf(
                hasProperty("success", equalTo(true)),
                hasProperty("timeToWait", equalTo(timeToWait))
        );
    }

    @NotNull
    private static Matcher<ThrottlingControlDao.RequestResult> notSuccess(Duration timeToWait) {
        return allOf(
                hasProperty("success", equalTo(false)),
                hasProperty("timeToWait", equalTo(timeToWait))
        );
    }

    @NotNull
    private ThrottlingControlDao throttlingControlDao(Clock clock) {
        return new ThrottlingControlDao(namedParameterJdbcTemplate, clock);
    }
}
