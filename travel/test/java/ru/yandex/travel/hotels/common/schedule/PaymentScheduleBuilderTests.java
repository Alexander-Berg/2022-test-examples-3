package ru.yandex.travel.hotels.common.schedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.assertj.core.data.Offset;
import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.hotels.common.refunds.RefundRule;
import ru.yandex.travel.hotels.common.refunds.RefundRules;
import ru.yandex.travel.hotels.common.refunds.RefundType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;


public class PaymentScheduleBuilderTests {
    @Test
    public void testNonMonotonousSequence() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-20T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(1000, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-11-20T17:00:00Z"))
                        .endsAt(Instant.parse("2020-11-22T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(800, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-11-22T17:00:00Z"))
                        .endsAt(Instant.parse("2020-11-24T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-11-24T17:00:00Z"))
                        .build())
                .build();
        assertThatThrownBy(() -> PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-15T17:00:00Z"),
                LocalDate.of(2020, 11, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(2000, ProtoCurrencyUnit.RUB), Money.of(200, ProtoCurrencyUnit.RUB),
                Money.of(1000, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3),
                Duration.ofMinutes(10))).isInstanceOf(IllegalStateException.class);
    }


    @Test
    public void testNonMonotonousSequenceDueToFullAmount() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-20T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(1000, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-11-20T17:00:00Z"))
                        .endsAt(Instant.parse("2020-11-22T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-11-22T17:00:00Z"))
                        .build())
                .build();
        assertThatThrownBy(() -> PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-15T17:00:00Z"),
                LocalDate.of(2020, 11, 16).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(800, ProtoCurrencyUnit.RUB), Money.of(80, ProtoCurrencyUnit.RUB),
                Money.of(400, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3),
                Duration.ofMinutes(10))).isInstanceOf(IllegalStateException.class);
    }

    // a test-case from real-world scenario in Muscatel-hotel (TL=5827)
    @Test
    public void testMuscatel() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-25T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(1125, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-11-25T11:00:00Z"))
                        .endsAt(Instant.parse("2020-12-05T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(2250, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-12-05T11:00:00Z"))
                        .endsAt(Instant.parse("2020-12-15T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(3375, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-12-15T11:00:00Z"))
                        .endsAt(Instant.parse("2020-12-20T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-12-20T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 21).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(4500, ProtoCurrencyUnit.RUB), Money.of(450, ProtoCurrencyUnit.RUB),
                Money.of(2250, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3),
                Duration.ofHours(1)).toProto();
        assertThat(result.hasInitialPayment()).isTrue();
        assertThat(result.getInitialPayment().getAmount()).isEqualTo(ProtoUtils.toTPrice(Money.of(2250,
                ProtoCurrencyUnit.RUB)));
        assertThat(result.getInitialPayment().getRate()).isEqualTo(0.5);
        assertThat(result.getDeferredPaymentsList()).hasSize(1)
                .extracting("amount", "paymentEndsAt", "rate")
                .containsExactly(
                        tuple(ProtoUtils.toTPrice(Money.of(2250, ProtoCurrencyUnit.RUB)),
                                ProtoUtils.fromInstant(Instant.parse("2020-12-15T10:00:00Z")),
                                0.5)
                );
    }

    // a test-case from real-world scenario in Muscatel-hotel (TL=5827), but with an applied promo code (200 rub)
    // As a result, 2nd penalty (2250) becomes > 50% of total cost, so the initial payment will be using 1125
    @Test
    public void testMuscatelWithPromocode() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-25T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(1125, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-11-25T11:00:00Z"))
                        .endsAt(Instant.parse("2020-12-05T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(2250, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-12-05T11:00:00Z"))
                        .endsAt(Instant.parse("2020-12-15T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(3375, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-12-15T11:00:00Z"))
                        .endsAt(Instant.parse("2020-12-20T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-12-20T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 21).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(4300, ProtoCurrencyUnit.RUB), Money.of(450, ProtoCurrencyUnit.RUB),
                Money.of(2250, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1)
        ).toProto();
        assertThat(result.getInitialPayment().getRate()).isEqualTo(0.2616, Offset.offset(0.0001));
        assertThat(result.hasInitialPayment()).isTrue();
        assertThat(result.getInitialPayment().getAmount()).isEqualTo(ProtoUtils.toTPrice(Money.of(1125,
                ProtoCurrencyUnit.RUB)));
        assertThat(result.getDeferredPaymentsList()).hasSize(1)
                .extracting("amount", "paymentEndsAt")
                .containsExactly(
                        tuple(ProtoUtils.toTPrice(Money.of(3175, ProtoCurrencyUnit.RUB)),
                                ProtoUtils.fromInstant(Instant.parse("2020-12-05T10:00:00Z"))));
    }


    // common case: fully refundable, penalty refundable, non-refundable rules.
    @Test
    public void testStandard3Rules() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-25T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(1000, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-11-25T11:00:00Z"))
                        .endsAt(Instant.parse("2020-12-05T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-12-05T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 21).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(3000, ProtoCurrencyUnit.RUB), Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1)
        ).toProto();
        assertThat(result.getInitialPayment().getRate()).isEqualTo(1.0 / 3.0);
        assertThat(result.hasInitialPayment()).isTrue();
        assertThat(result.getInitialPayment().getAmount()).isEqualTo(ProtoUtils.toTPrice(Money.of(1000,
                ProtoCurrencyUnit.RUB)));
        assertThat(result.getDeferredPaymentsList()).hasSize(1)
                .extracting("amount", "paymentEndsAt")
                .containsExactly(
                        tuple(ProtoUtils.toTPrice(Money.of(2000, ProtoCurrencyUnit.RUB)),
                                ProtoUtils.fromInstant(Instant.parse("2020-12-05T10:00:00Z"))));
    }

    // common case: fully refundable, penalty refundable, non-refundable rules.
    // Same as previous, but no-penalty period is much longer, so it's better to take minimal first payment and
    // charge 100% of first penalty instant
    @Test
    public void testStandard3RulesLongNoPenaltyPeriod() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-12-20T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(1000, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-12-20T11:00:00Z"))
                        .endsAt(Instant.parse("2020-12-23T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-12-23T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 24).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(3000, ProtoCurrencyUnit.RUB), Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1)
        ).toProto();
        assertThat(result.hasInitialPayment()).isTrue();
        assertThat(result.getInitialPayment().getAmount()).isEqualTo(ProtoUtils.toTPrice(Money.of(300,
                ProtoCurrencyUnit.RUB)));
        assertThat(result.getDeferredPaymentsList()).hasSize(1)
                .extracting("amount", "paymentEndsAt")
                .containsExactly(
                        tuple(ProtoUtils.toTPrice(Money.of(2700, ProtoCurrencyUnit.RUB)),
                                ProtoUtils.fromInstant(Instant.parse("2020-12-20T10:00:00Z"))));
    }

    // common case: fully refundable, penalty refundable, non-refundable rules.
    // Same as previous, but no-penalty period starts very soon, so the first payment after the window has penalty
    @Test
    public void testStandard3RulesVeryShortNoPenaltyPeriod() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-17T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(1000, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-11-17T11:00:00Z"))
                        .endsAt(Instant.parse("2020-12-23T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-12-23T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 24).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(3000, ProtoCurrencyUnit.RUB), Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1)
        ).toProto();
        assertThat(result.hasInitialPayment()).isTrue();
        assertThat(result.getInitialPayment().getAmount()).isEqualTo(ProtoUtils.toTPrice(Money.of(1000,
                ProtoCurrencyUnit.RUB)));
        assertThat(result.getDeferredPaymentsList()).hasSize(1)
                .extracting("amount", "paymentEndsAt")
                .containsExactly(
                        tuple(ProtoUtils.toTPrice(Money.of(2000, ProtoCurrencyUnit.RUB)),
                                ProtoUtils.fromInstant(Instant.parse("2020-12-23T10:00:00Z"))));
    }

    @Test
    public void testStandard2RulesWithPenalty() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(1000, ProtoCurrencyUnit.RUB))
                        .endsAt(Instant.parse("2020-12-23T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-12-23T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 24).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(3000, ProtoCurrencyUnit.RUB), Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1)
        ).toProto();
        assertThat(result.hasInitialPayment()).isTrue();
        assertThat(result.getInitialPayment().getAmount()).isEqualTo(ProtoUtils.toTPrice(Money.of(1000,
                ProtoCurrencyUnit.RUB)));
        assertThat(result.getDeferredPaymentsList()).hasSize(1)
                .extracting("amount", "paymentEndsAt")
                .containsExactly(
                        tuple(ProtoUtils.toTPrice(Money.of(2000, ProtoCurrencyUnit.RUB)),
                                ProtoUtils.fromInstant(Instant.parse("2020-12-23T10:00:00Z"))));
    }

    @Test
    public void testStandard2RulesNoPenalty() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-20T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-11-20T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 24).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(3000, ProtoCurrencyUnit.RUB), Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1)
        ).toProto();
        assertThat(result.hasInitialPayment()).isTrue();
        assertThat(result.getInitialPayment().getAmount()).isEqualTo(ProtoUtils.toTPrice(Money.of(300,
                ProtoCurrencyUnit.RUB)));
        assertThat(result.getDeferredPaymentsList()).hasSize(1)
                .extracting("amount", "paymentEndsAt")
                .containsExactly(
                        tuple(ProtoUtils.toTPrice(Money.of(2700, ProtoCurrencyUnit.RUB)),
                                ProtoUtils.fromInstant(Instant.parse("2020-11-20T10:00:00Z"))));
    }

    @Test
    public void testStandard2RulesNoPenaltyVeryShort() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-17T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-11-17T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 21).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(3000, ProtoCurrencyUnit.RUB), Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1));
        assertThat(result).isNull();
    }

    @Test
    public void testStandard2RulesNoPenaltyVeryShortWithIgnore() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-17T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-11-17T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 21).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(3000, ProtoCurrencyUnit.RUB), Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofSeconds(0), Duration.ofHours(1)
        );
        assertThat(result).isNotNull();
    }

    @Test
    public void testNonRefundable() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                LocalDate.of(2020, 12, 21).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                Money.of(3000, ProtoCurrencyUnit.RUB), Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1));
        assertThat(result).isNull();
    }

    @Test
    public void testCheckinStartsBeforeLastRule() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-20T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-11-20T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-16T17:00:00Z"),
                Instant.parse("2020-11-19T21:00:00Z"),
                Money.of(3000, ProtoCurrencyUnit.RUB), Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1)).toProto();
        assertThat(result.hasInitialPayment()).isTrue();
        assertThat(result.getInitialPayment().getAmount()).isEqualTo(ProtoUtils.toTPrice(Money.of(300,
                ProtoCurrencyUnit.RUB)));
        assertThat(result.getDeferredPaymentsList()).hasSize(1)
                .extracting("amount", "paymentEndsAt")
                .containsExactly(
                        tuple(ProtoUtils.toTPrice(Money.of(2700, ProtoCurrencyUnit.RUB)),
                                ProtoUtils.fromInstant(Instant.parse("2020-11-19T20:59:00Z"))));
    }

    @Test
    public void test2RulesWithClosePenalty() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-11-20T14:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-11-20T14:00:00Z"))
                        .endsAt(Instant.parse("2020-11-20T15:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-19T17:00:00Z"),
                Instant.parse("2020-11-19T21:00:00Z"), Money.of(3000, ProtoCurrencyUnit.RUB),
                Money.of(300, ProtoCurrencyUnit.RUB),
                Money.of(1500, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1));
        assertThat(result).isNull();
    }

    @Test
    public void test50to50WithPromoCode() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(1000, ProtoCurrencyUnit.RUB))
                        .endsAt(Instant.parse("2020-12-23T11:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-12-23T11:00:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(Instant.parse("2020-11-19T17:00:00Z"),
                Instant.parse("2020-12-23T21:00:00Z"), Money.of(1800, ProtoCurrencyUnit.RUB),
                Money.of(200, ProtoCurrencyUnit.RUB),  Money.of(1000, ProtoCurrencyUnit.RUB), rules,
                Duration.ofDays(3), Duration.ofHours(1));
        assertThat(result).isNotNull();
        assertThat(result.getInitialPayment().getAmount()
                .add(result.getDeferredPayments().get(0).getAmount()))
                .isEqualTo(Money.of(1800, ProtoCurrencyUnit.RUB));
        assertThat(result.getInitialPayment().getRatio()
                .add(result.getDeferredPayments().get(0).getRatio()).setScale(4, RoundingMode.HALF_UP))
                .isEqualTo(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
    }

    @Test
    public void testLowPenalty() {
        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2021-02-08T17:59:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(18526, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2021-02-08T17:59:00Z"))
                        .endsAt(Instant.parse("2021-02-10T17:59:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2021-02-10T17:59:00Z"))
                        .build())
                .build();
        var result = PaymentScheduleBuilder.twoPaymentSchedule(
                Instant.parse("2021-01-12T09:12:58Z"),
                Instant.parse("2021-02-09T21:00:00Z"),
                Money.of(270167, ProtoCurrencyUnit.RUB), Money.of(27016.7, ProtoCurrencyUnit.RUB),
                Money.of(135083.5, ProtoCurrencyUnit.RUB), rules, Duration.ofDays(3), Duration.ofHours(1));
        assertThat(result).isNotNull();
        assertThat(result.getInitialPayment().getRatio()).isGreaterThanOrEqualTo(BigDecimal.valueOf(0.1));
    }
}
