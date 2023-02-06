package ru.yandex.travel.hotels.common.partners.bnovo.utils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.bnovo.model.CancellationFineType;
import ru.yandex.travel.hotels.common.partners.bnovo.model.Offer;
import ru.yandex.travel.hotels.common.partners.bnovo.model.RatePlan;
import ru.yandex.travel.hotels.common.refunds.RefundType;

import static org.assertj.core.api.Assertions.assertThat;

public class BNovoRefundRulesBuilderTests {
    @Test
    public void testNonRefundableAsPercentage() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(10)
                .cancellationFineType(CancellationFineType.PERCENTAGE)
                .cancellationFineAmount(BigDecimal.valueOf(100))
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(1000))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isRefundable()).isFalse();
        assertThat(rules.isFullyRefundable()).isFalse();
    }

    @Test
    public void testNonRefundableAsNight() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(10)
                .cancellationFineType(CancellationFineType.FIRST_NIGHT)
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(1000))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isRefundable()).isFalse();
        assertThat(rules.isFullyRefundable()).isFalse();
    }

    @Test
    public void testNonRefundableAsFixedAmount() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(10)
                .cancellationFineType(CancellationFineType.FIXED_AMOUNT)
                .cancellationFineAmount(BigDecimal.valueOf(1000.0f))
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(1000))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isRefundable()).isFalse();
        assertThat(rules.isFullyRefundable()).isFalse();
    }

    @Test
    public void testNonRefundableAsFixedAmountExceedingPrice() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(10)
                .cancellationFineType(CancellationFineType.FIXED_AMOUNT)
                .cancellationFineAmount(BigDecimal.valueOf(10000.0f))
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(1000))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isRefundable()).isFalse();
        assertThat(rules.isFullyRefundable()).isFalse();
    }

    @Test
    public void testNonRefundableAsFullAmount() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(10)
                .cancellationFineType(CancellationFineType.FULL_AMOUNT)
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(1000))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isRefundable()).isFalse();
        assertThat(rules.isFullyRefundable()).isFalse();
    }

    @Test
    public void testPartiallyRefundableAsPercentage() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(10)
                .cancellationFineType(CancellationFineType.PERCENTAGE)
                .cancellationFineAmount(BigDecimal.valueOf(33.0f))
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(1000))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isFalse();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(0).getPenalty().getNumberStripped()).isEqualByComparingTo("330");
    }

    @Test
    public void testPartiallyRefundableAsNight() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(10)
                .cancellationFineType(CancellationFineType.FIRST_NIGHT)
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(1), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(2), BigDecimal.valueOf(400))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isFalse();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(0).getPenalty().getNumberStripped()).isEqualByComparingTo("300");
    }

    @Test
    public void testPartiallyRefundableAsFixedAmount() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(10)
                .cancellationFineType(CancellationFineType.FIXED_AMOUNT)
                .cancellationFineAmount(BigDecimal.valueOf(123.5f))
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(1), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(2), BigDecimal.valueOf(400))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isFalse();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(0).getPenalty().getNumberStripped()).isEqualByComparingTo("123.5");
    }

    @Test
    public void testFullyRefundableAsPercentage() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(3)
                .cancellationFineType(CancellationFineType.PERCENTAGE)
                .cancellationFineAmount(BigDecimal.valueOf(33.0f))
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(1000))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isTrue();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(3);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("330");
    }

    @Test
    public void testFullyRefundableAsNight() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(3)
                .cancellationFineType(CancellationFineType.FIRST_NIGHT)
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(1), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(2), BigDecimal.valueOf(400))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isTrue();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(3);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("300");
    }

    @Test
    public void testFullyRefundableAsFixedAmount() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(3)
                .cancellationFineType(CancellationFineType.FIXED_AMOUNT)
                .cancellationFineAmount(BigDecimal.valueOf(123.5f))
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(1), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(2), BigDecimal.valueOf(400))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isTrue();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(3);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("123.5");
    }

    @Test
    public void testFullyThenNonRefundableAsPercentage() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(3)
                .cancellationFineType(CancellationFineType.PERCENTAGE)
                .cancellationFineAmount(BigDecimal.valueOf(100.0f))
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(1000))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isTrue();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testFullyThenNonRefundableAsNight() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(3)
                .cancellationFineType(CancellationFineType.FIRST_NIGHT)
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(1000))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isTrue();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testFullyThenNonRefundableAsFixedAmount() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(3)
                .cancellationFineType(CancellationFineType.FIXED_AMOUNT)
                .cancellationFineAmount(BigDecimal.valueOf(12300.5f))
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(1), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(2), BigDecimal.valueOf(400))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(5, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isTrue();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testRatePlanWithNoPenaltyType() {
        var ratePlan = RatePlan.builder()
                .cancellationDeadline(0)
                .cancellationFineType(CancellationFineType.NO_PENALTY)
                .cancellationFineAmount(BigDecimal.ZERO)
                .build();
        var offer = Offer.builder()
                .price(BigDecimal.valueOf(1000))
                .pricesByDate(LocalDate.now(), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(1), BigDecimal.valueOf(300))
                .pricesByDate(LocalDate.now().plusDays(2), BigDecimal.valueOf(400))
                .build();
        var rules = BNovoRefundRulesBuilder.build(ratePlan, offer,
                Instant.now().plus(1, ChronoUnit.DAYS), Instant.now(), "RUB");
        assertThat(rules.isFullyRefundable()).isTrue();
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }
}
