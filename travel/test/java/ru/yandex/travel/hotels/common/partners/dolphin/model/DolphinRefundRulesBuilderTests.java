package ru.yandex.travel.hotels.common.partners.dolphin.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.dolphin.utils.DolphinRefundRulesBuilder;
import ru.yandex.travel.hotels.common.refunds.RefundType;

import static org.assertj.core.api.Assertions.assertThat;

public class DolphinRefundRulesBuilderTests {
    private static final Map<Integer, BigDecimal> RULES = ImmutableMap.of(
            5, BigDecimal.ZERO,
            0, BigDecimal.valueOf(0.8)
    );
    private static final FixedPercentFeeRefundParams FIXED_REFUND_STRATEGY = new FixedPercentFeeRefundParams(RULES);
    private static final AverageNightStayFeeRefundParams AVERAGE_REFUND_STRATEGY = new AverageNightStayFeeRefundParams(10, 5);
    private static final AverageNightStayFeeRefundParams AVERAGE_REFUND_STRATEGY_ONE_NIGHT = new AverageNightStayFeeRefundParams(1, 5);

    @Test
    public void testCheckinTomorrowAlreadyHappened() {
        // User at UTC is searching for checkin "tomorrow", but hotel's timezone is 10 hours ahead of them, so
        // "tomorrow" is already there.
        // So the cancellation should indicate FULL_PENALTY
        LocalDate checkin = LocalDate.of(2019, 8, 2);
        LocalDateTime searchingAt = LocalDateTime.of(2019, 8, 1, 22, 0);
        Instant checkinMoment = checkin.atStartOfDay().toInstant(ZoneOffset.ofHours(10)); // UTC+10 - Vladivostok and
        // around
        Instant searchingAtMoment = searchingAt.toInstant(ZoneOffset.UTC); // UTC
        var info = DolphinRefundRulesBuilder.build(searchingAtMoment, checkinMoment,
                BigDecimal.valueOf(100), "RUB", FIXED_REFUND_STRATEGY);
        assertThat(info.isRefundable()).isFalse();
        assertThat(info.getRules()).isEmpty();
    }

    @Test
    public void testCheckinTodayIsNotThereYet() {
        // User at UTC is searching for checkin "today", but hotel's timezone is 10 hours behind of them, so it is
        // still "tomorrow" for the hotel
        // So the cancellation should indicate 80% penalty
        LocalDate checkin = LocalDate.of(2019, 8, 1);
        LocalDateTime searchingAt = LocalDateTime.of(2019, 8, 1, 8, 0);
        Instant checkinMoment = checkin.atStartOfDay().toInstant(ZoneOffset.ofHours(-10)); // UTC-10 - Honolulu and
        // around
        Instant searchingAtMoment = searchingAt.toInstant(ZoneOffset.UTC); // UTC
        var info = DolphinRefundRulesBuilder.build(searchingAtMoment, checkinMoment,
                BigDecimal.valueOf(100), "RUB", FIXED_REFUND_STRATEGY);
        assertThat(info.isRefundable()).isTrue();
        assertThat(info.getRules().size()).isEqualTo(2);
        assertThat(info.getRules().get(0).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(info.getRules().get(0).getPenalty().getNumberStripped()).isEqualByComparingTo("80");
    }

    @Test
    public void testCheckinInSlightlyMoreThen5Days() {
        LocalDate checkin = LocalDate.of(2019, 8, 7);
        LocalDateTime searchingAt = LocalDateTime.of(2019, 8, 1, 23, 59);
        var info = DolphinRefundRulesBuilder.build(
                searchingAt.toInstant(ZoneOffset.UTC), checkin.atStartOfDay().toInstant(ZoneOffset.UTC),
                BigDecimal.valueOf(100), "RUB", FIXED_REFUND_STRATEGY);
        assertThat(info.isRefundable()).isTrue();
        assertThat(info.getRules().size()).isEqualTo(3);
        assertThat(info.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(info.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(info.getRules().get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("80");
        assertThat(info.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testCheckinInSlightlyMoreThen5DaysWithNewStrategy() {
        LocalDate checkin = LocalDate.of(2019, 8, 7);
        LocalDateTime searchingAt = LocalDateTime.of(2019, 8, 1, 23, 59);
        var info = DolphinRefundRulesBuilder.build(
            searchingAt.toInstant(ZoneOffset.UTC), checkin.atStartOfDay().toInstant(ZoneOffset.UTC),
            BigDecimal.valueOf(100), "RUB", AVERAGE_REFUND_STRATEGY);
        assertThat(info.isRefundable()).isTrue();
        assertThat(info.getRules().size()).isEqualTo(3);
        assertThat(info.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(info.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(info.getRules().get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("10");
        assertThat(info.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testCheckinInSlightlyLessThen5Days() {
        LocalDate checkin = LocalDate.of(2019, 8, 7);
        LocalDateTime searchingAt = LocalDateTime.of(2019, 8, 2, 0, 01);
        var info = DolphinRefundRulesBuilder.build(
                searchingAt.toInstant(ZoneOffset.UTC), checkin.atStartOfDay().toInstant(ZoneOffset.UTC),
                BigDecimal.valueOf(100), "RUB", FIXED_REFUND_STRATEGY);
        assertThat(info.isRefundable()).isTrue();
        assertThat(info.getRules().size()).isEqualTo(2);
        assertThat(info.getRules().get(0).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(info.getRules().get(0).getPenalty().getNumberStripped()).isEqualByComparingTo("80");
        assertThat(info.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testDolphinRefundPropertiesSerialization() throws IOException {
        DolphinRefundParams props = AVERAGE_REFUND_STRATEGY;
        var objMapper = new ObjectMapper();
        DolphinRefundParams newProps = objMapper.readValue(objMapper.writeValueAsString(props), DolphinRefundParams.class);
        assertThat(newProps.getClass()).isEqualTo(props.getClass());
    }

    @Test
    public void testOneNightRules() {
        LocalDate checkin = LocalDate.of(2019, 8, 7);
        LocalDateTime searchingAt = LocalDateTime.of(2019, 8, 1, 23, 59);
        var info = DolphinRefundRulesBuilder.build(
                searchingAt.toInstant(ZoneOffset.UTC), checkin.atStartOfDay().toInstant(ZoneOffset.UTC),
                BigDecimal.valueOf(100), "RUB", AVERAGE_REFUND_STRATEGY_ONE_NIGHT);
        assertThat(info.isRefundable()).isTrue();
        assertThat(info.getRules().size()).isEqualTo(2);
        assertThat(info.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(info.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }
}
