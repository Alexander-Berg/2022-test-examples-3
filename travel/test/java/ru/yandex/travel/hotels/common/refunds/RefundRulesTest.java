package ru.yandex.travel.hotels.common.refunds;

import java.time.Instant;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RefundRulesTest {
    @Test
    public void getRuleAtInstant_undeclaredPeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(Instant.parse("2020-07-07T17:00:00Z"))
                        .build())
                .build();
        assertThat(rules.getRuleAtInstant(Instant.parse("2020-07-07T17:23:00Z"))).satisfies(rule -> {
            assertThat(rule).isNotNull();
            assertThat(rule.getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        });
    }


    @Test
    public void testFullyRefundableTillForEmptyRules() {
        var rules = RefundRules.builder()
                .build();
        assertThat(rules.getFullyRefundableTill()).isNull();
    }

    @Test
    public void testFullyRefundableTillForNonRefRules() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .build())
                .build();
        assertThat(rules.getFullyRefundableTill()).isNull();
    }

    @Test
    public void testFullyRefundableTillForPartialThenNonRef() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100, ProtoCurrencyUnit.RUB))
                        .endsAt(Instant.parse("2020-07-07T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-07-07T17:00:00Z"))
                        .build())
                .build();
        assertThat(rules.getFullyRefundableTill()).isNull();
    }

    @Test
    public void testFullyRefundableTillForRefundableThenNonRef() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .penalty(Money.of(100, ProtoCurrencyUnit.RUB))
                        .endsAt(Instant.parse("2020-07-07T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-07-07T17:00:00Z"))
                        .build())
                .build();
        assertThat(rules.getFullyRefundableTill()).isEqualTo(Instant.parse("2020-07-07T17:00:00Z"));
    }

    @Test
    public void testFullyRefundableTillForRefundableThenRefundableThenNonRef() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .penalty(Money.of(100, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-07T17:00:00Z"))
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .penalty(Money.of(100, ProtoCurrencyUnit.RUB))
                        .endsAt(Instant.parse("2020-07-07T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .build();
        assertThat(rules.getFullyRefundableTill()).isEqualTo(Instant.parse("2020-07-08T17:00:00Z"));
    }

    @Test
    public void testAddNonRefExtraInFreePeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(null)
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100.0, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .endsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .build();
        var newRules = rules.withNonRefundableExtra(Money.of(500, ProtoCurrencyUnit.RUB), Instant.parse("2020-07" +
                "-07T17:00:00Z"));
        assertThat(newRules.getRules()).hasSize(3);
        assertThat(newRules.getRules().get(0).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(0).getPenalty()).isEqualTo(Money.of(500, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(1).getPenalty()).isEqualTo(Money.of(600, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testAddNonRefExtraInPenaltyPeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(null)
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100.0, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .endsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .build();
        var newRules = rules.withNonRefundableExtra(Money.of(500, ProtoCurrencyUnit.RUB), Instant.parse("2020-07" +
                "-09T17:00:00Z"));
        assertThat(newRules.getRules()).hasSize(3);
        assertThat(newRules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(newRules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(1).getPenalty()).isEqualTo(Money.of(600, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testAddNonRefExtraInNonRefPeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(null)
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100.0, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .endsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .build();
        var newRules = rules.withNonRefundableExtra(Money.of(500, ProtoCurrencyUnit.RUB), Instant.parse("2020-08" +
                "-09T17:00:00Z"));
        assertThat(newRules.getRules()).hasSize(3);
        assertThat(newRules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(newRules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(1).getPenalty()).isEqualTo(Money.of(100, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testAddFullyRefExtraInFreePeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(null)
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100.0, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .endsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .build();
        var newRules = rules.withFullyRefundableExtra(Money.of(500, ProtoCurrencyUnit.RUB), Instant.parse("2020-07" +
                "-07T17:00:00Z"));
        assertThat(newRules.getRules()).hasSize(3);
        assertThat(newRules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(newRules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(1).getPenalty()).isEqualTo(Money.of(100, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testAddFullyRefExtraInPenaltyPeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(null)
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100.0, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .endsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .build();
        var newRules = rules.withFullyRefundableExtra(Money.of(500, ProtoCurrencyUnit.RUB), Instant.parse("2020-07" +
                "-09T17:00:00Z"));
        assertThat(newRules.getRules()).hasSize(3);
        assertThat(newRules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(newRules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(1).getPenalty()).isEqualTo(Money.of(100, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testAddFullyRefExtraInNonRefPeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(null)
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100.0, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .endsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .build();
        var newRules = rules.withFullyRefundableExtra(Money.of(500, ProtoCurrencyUnit.RUB), Instant.parse("2020-08" +
                "-09T17:00:00Z"));
        assertThat(newRules.getRules()).hasSize(3);
        assertThat(newRules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(newRules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(1).getPenalty()).isEqualTo(Money.of(100, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testProportionallyRefExtraInFreePeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(null)
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100.0, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .endsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .build();
        var newRules = rules.withProportionallyRefundableExtra(Money.of(500, ProtoCurrencyUnit.RUB), Money.of(1200,
                ProtoCurrencyUnit.RUB), Instant.parse("2020-07-07T17:00:00Z"));
        assertThat(newRules.getRules()).hasSize(3);
        assertThat(newRules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(newRules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(1).getPenalty()).isEqualTo(Money.of(141.65, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testAddProportionallyRefExtraInPenaltyPeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(null)
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100.0, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .endsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .build();
        var newRules = rules.withProportionallyRefundableExtra(Money.of(500, ProtoCurrencyUnit.RUB), Money.of(1200,
                ProtoCurrencyUnit.RUB), Instant.parse("2020-07-09T17:00:00Z"));
        assertThat(newRules.getRules()).hasSize(3);
        assertThat(newRules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(newRules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(1).getPenalty()).isEqualTo(Money.of(141.65, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    @Test
    public void testAddProportionallyRefExtraInNonRefPeriod() {
        var rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(null)
                        .endsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .penalty(Money.of(100.0, ProtoCurrencyUnit.RUB))
                        .startsAt(Instant.parse("2020-07-08T17:00:00Z"))
                        .endsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(Instant.parse("2020-08-08T17:00:00Z"))
                        .build())
                .build();
        var newRules = rules.withProportionallyRefundableExtra(Money.of(500, ProtoCurrencyUnit.RUB), Money.of(1200,
                ProtoCurrencyUnit.RUB), Instant.parse("2020-08-09T17:00:00Z"));
        assertThat(newRules.getRules()).hasSize(3);
        assertThat(newRules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(newRules.getRules().get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newRules.getRules().get(1).getPenalty()).isEqualTo(Money.of(100, ProtoCurrencyUnit.RUB));
        assertThat(newRules.getRules().get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }
}
