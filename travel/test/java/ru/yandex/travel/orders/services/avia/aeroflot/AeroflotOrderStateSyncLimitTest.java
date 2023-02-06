package ru.yandex.travel.orders.services.avia.aeroflot;

import java.time.Duration;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.testing.time.SettableClock;

import static org.assertj.core.api.Assertions.assertThat;

public class AeroflotOrderStateSyncLimitTest {
    private AeroflotOrderStateSyncLimit aeroflotOrderStateSyncLimit;
    private SettableClock rateLimiterClock;

    private final static int LIMIT = 24 * 10;
    private final static Duration LIMIT_REFRESH = Duration.ofHours(24);
    private final static Instant NOW = Instant.now();
    private final static Duration DEFAULT_DELAY = Duration.ofMillis(700);


    @Before
    public void init() {
        rateLimiterClock = new SettableClock();
        rateLimiterClock.setCurrentTime(NOW);
        var properties = AeroflotOrderStateSyncProperties.builder();
        properties.requestLimitCount(LIMIT);
        properties.requestLimitPeriod(LIMIT_REFRESH);
        properties.defaultDelay(DEFAULT_DELAY);

        aeroflotOrderStateSyncLimit = new AeroflotOrderStateSyncLimit(properties.build(), rateLimiterClock);
    }

    @Test
    public void nextRefreshLimitSeconds_dateUpdateIsNull() {
        rateLimiterClock.setCurrentTime(NOW);

        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(Duration.ZERO);
    }

    @Test
    public void nextRefreshLimitSeconds_oneSecondBefore() {
        rateLimiterClock.setCurrentTime(NOW.plusSeconds(100));
        assertThat(aeroflotOrderStateSyncLimit.need(0)).isTrue();
        rateLimiterClock.setCurrentTime(NOW.plusSeconds(LIMIT_REFRESH.toSeconds() - 1));

        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(Duration.ofSeconds(1 + 100));
    }

    @Test
    public void nextRefreshLimitSeconds_oneSecondAfter() {
        aeroflotOrderStateSyncLimit.need(0); // for initialization
        rateLimiterClock.setCurrentTime(NOW.plusSeconds(LIMIT_REFRESH.toSeconds() + 1));

        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(Duration.ofSeconds(-1));
    }

    @Test
    public void nextRefreshLimitSeconds_updateNextRefresh() {
        rateLimiterClock.setCurrentTime(NOW.plusSeconds(LIMIT_REFRESH.toSeconds()));
        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(Duration.ZERO);

        assertThat(aeroflotOrderStateSyncLimit.need(0)).isTrue();
        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(LIMIT_REFRESH);
    }

    @Test
    public void defaultLimit() {
        assertThat(aeroflotOrderStateSyncLimit.getUnusedLimit()).isEqualTo(LIMIT);
    }

    @Test
    public void defaultRefreshLimit() {
        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(Duration.ZERO);
    }

    @Test
    public void checkInitValue() {
        assertThat(aeroflotOrderStateSyncLimit.getUnusedLimit()).isEqualTo(LIMIT);
        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(LIMIT_REFRESH);
    }

    @Test
    public void need_equalLimit_updateLimit() {
        var sec = 1;

        assertThat(aeroflotOrderStateSyncLimit.need(LIMIT)).isTrue();

        assertThat(aeroflotOrderStateSyncLimit.getUnusedLimit()).isEqualTo(0);
        rateLimiterClock.setCurrentTime(NOW.plusSeconds(sec));
        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(LIMIT_REFRESH.minusSeconds(sec));
    }

    @Test
    public void need_overLimit_notUpdateLimit() {
        var sec = 1;

        assertThat(aeroflotOrderStateSyncLimit.need(LIMIT + 1)).isFalse();

        assertThat(aeroflotOrderStateSyncLimit.getUnusedLimit()).isEqualTo(LIMIT);
        rateLimiterClock.setCurrentTime(NOW.plusSeconds(sec));
        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(LIMIT_REFRESH.minusSeconds(sec));
    }

    @Test
    public void getLimit_withInit() {
        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(Duration.ZERO);
        assertThat(aeroflotOrderStateSyncLimit.getUnusedLimit()).isEqualTo(LIMIT);
        assertThat(aeroflotOrderStateSyncLimit.durationUntilNextRateLimitPeriod()).isEqualTo(LIMIT_REFRESH);
    }

    @Test
    public void getLimit_withoutInit() {
        assertThat(aeroflotOrderStateSyncLimit.need(0)).isTrue();
        assertThat(aeroflotOrderStateSyncLimit.getUnusedLimit()).isEqualTo(LIMIT);
        assertThat(aeroflotOrderStateSyncLimit.need(1)).isTrue();
        assertThat(aeroflotOrderStateSyncLimit.getUnusedLimit()).isEqualTo(LIMIT - 1);
    }


    @Test
    public void getSleepSeconds_zeroOrders_defaultValue() {
        assertThat(aeroflotOrderStateSyncLimit.estimateIntervalBetweenOrderUpdates(0)).isEqualTo(DEFAULT_DELAY);
    }

    @Test
    public void getSleepSeconds() {
        // leave 10 requests
        aeroflotOrderStateSyncLimit.need(LIMIT - 10);
        // 30 seconds until next window
        rateLimiterClock.setCurrentTime(rateLimiterClock.instant().plus(LIMIT_REFRESH).minusSeconds(30));

        // more or less fitting into default rate of 1/3 rps
        assertThat(aeroflotOrderStateSyncLimit.estimateIntervalBetweenOrderUpdates(1)).isEqualTo(Duration.ofSeconds(3));
        assertThat(aeroflotOrderStateSyncLimit.estimateIntervalBetweenOrderUpdates(2)).isEqualTo(Duration.ofSeconds(3));
        // ...
        assertThat(aeroflotOrderStateSyncLimit.estimateIntervalBetweenOrderUpdates(10)).isEqualTo(Duration.ofSeconds(3));
        // falling back to config-driven interval if we do not fit
        assertThat(aeroflotOrderStateSyncLimit.estimateIntervalBetweenOrderUpdates(20)).isEqualTo(LIMIT_REFRESH.dividedBy(LIMIT));
        assertThat(aeroflotOrderStateSyncLimit.estimateIntervalBetweenOrderUpdates(200)).isEqualTo(LIMIT_REFRESH.dividedBy(LIMIT));
    }

    @Test
    public void getSleepSeconds_zeroLimit_refreshLimit() {
        // use all the limit
        aeroflotOrderStateSyncLimit.need(LIMIT);
        // 30 seconds until next window
        rateLimiterClock.setCurrentTime(rateLimiterClock.instant().plus(LIMIT_REFRESH).minusSeconds(30));

        assertThat(aeroflotOrderStateSyncLimit.estimateIntervalBetweenOrderUpdates(1)).isEqualTo(LIMIT_REFRESH.dividedBy(LIMIT));
        assertThat(aeroflotOrderStateSyncLimit.estimateIntervalBetweenOrderUpdates(2)).isEqualTo(LIMIT_REFRESH.dividedBy(LIMIT));
    }
}
