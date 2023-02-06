package ru.yandex.travel.hotels.common.partners.expedia;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.ShoppingRate;
import ru.yandex.travel.hotels.common.refunds.RefundRule;
import ru.yandex.travel.hotels.common.refunds.RefundRules;
import ru.yandex.travel.hotels.common.refunds.RefundType;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpediaRefundBuilderTests {
    private static final ObjectMapper mapper = DefaultExpediaClient.createObjectMapper();

    @Test
    public void testRefundableGraceThenSomePercentThenFullV24() throws IOException {
        ShoppingRate rate = loadRate("CancelPercentage");
        RefundRules rules = ExpediaRefundRulesBuilder.build(rate, "1-15", Duration.ofHours(1));
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(3);
        assertThat(rules.getRules()).extracting(RefundRule::getType).containsExactly(
                RefundType.FULLY_REFUNDABLE,
                RefundType.REFUNDABLE_WITH_PENALTY,
                RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("306.00");   // 1021.23 USD total inclusive * 65.72 fxRate * 30% refund = 20134.57 ~= 20135
        assertThat(rules.getRules().get(1).getPenalty().getCurrency().getCurrencyCode()).isEqualTo("USD");
        assertThat(rules.getRules().get(0).getPenalty()).isNull();
        assertThat(rules.getRules().get(2).getPenalty()).isNull();
    }

    @Test
    public void testRefundableGraceThenSomeAmountThenFull() throws IOException {
        ShoppingRate rate = loadRate("CancelAmount");
        RefundRules rules = ExpediaRefundRulesBuilder.build(rate, "2", Duration.ofHours(1));
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(3);
        assertThat(rules.getRules()).extracting(RefundRule::getType).containsExactly(
                RefundType.FULLY_REFUNDABLE,
                RefundType.REFUNDABLE_WITH_PENALTY,
                RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("20.00");   // 20 USD penalty * 65.72 fxRate = 1314.4, round -> 1314
        assertThat(rules.getRules().get(1).getPenalty().getCurrency().getCurrencyCode()).isEqualTo("USD");
        assertThat(rules.getRules().get(0).getPenalty()).isNull();
        assertThat(rules.getRules().get(2).getPenalty()).isNull();
    }

    @Test
    public void testRefundableGraceThenFullNights() throws IOException {
        ShoppingRate rate = loadRate("CancelNightsFull");
        RefundRules rules = ExpediaRefundRulesBuilder.build(rate, "2", Duration.ofHours(1));
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules()).extracting(RefundRule::getType).containsExactly(
                RefundType.FULLY_REFUNDABLE,
                RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules()).allMatch(p -> p.getPenalty() == null);
    }

    @Test
    public void testRefundableGraceThenSomeNightsThenFull() throws IOException {
        ShoppingRate rate = loadRate("CancelNightsSome");
        RefundRules rules = ExpediaRefundRulesBuilder.build(rate, "2", Duration.ofHours(1));
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(3);
        assertThat(rules.getRules()).extracting(RefundRule::getType).containsExactly(
                RefundType.FULLY_REFUNDABLE,
                RefundType.REFUNDABLE_WITH_PENALTY,
                RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("452.00");
        assertThat(rules.getRules().get(1).getPenalty().getCurrency().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    public void testAmountTooLargeSoFull() throws IOException {
        ShoppingRate rate = loadRate("CancelAmountTooLarge");
        RefundRules rules = ExpediaRefundRulesBuilder.build(rate, "2", Duration.ofHours(1));
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules()).extracting(RefundRule::getType).containsExactly(
                RefundType.FULLY_REFUNDABLE,
                RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(0).getPenalty()).isNull();
        assertThat(rules.getRules().get(1).getPenalty()).isNull();
    }

    @Test
    public void testGraceAlreadyOver() throws IOException {
        ShoppingRate rate = loadRate("CancelNightsSomeAlreadyStarted");
        RefundRules rules = ExpediaRefundRulesBuilder.build(rate, "2", Duration.ofHours(1));
        assertThat(rules.isRefundable()).isTrue();
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules()).extracting(RefundRule::getType).containsExactly(
                RefundType.REFUNDABLE_WITH_PENALTY,
                RefundType.NON_REFUNDABLE);
        assertThat(rules.getRules().get(0).getPenalty().getNumberStripped()).isEqualByComparingTo("452");
    }

    @Test
    public void testRemoveObsolete() {
        Instant now = Instant.now();
        RefundRules initial = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(now.minus(1, ChronoUnit.DAYS))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .startsAt(now.minus(1, ChronoUnit.DAYS))
                        .endsAt(now.plus(1, ChronoUnit.DAYS))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(now.plus(1, ChronoUnit.DAYS))
                        .build())
                .build();
        RefundRules newInfo = initial.actualize();
        assertThat(newInfo.isRefundable()).isTrue();
        assertThat(newInfo.getRules()).hasSize(2);
        assertThat(newInfo.getRules().get(0).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(newInfo.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }


    @Test
    public void testRemoveObsoleteMakeNonRefundable() {
        Instant now = Instant.now();
        RefundRules initial = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .endsAt(now.minus(2, ChronoUnit.DAYS))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .startsAt(now.minus(2, ChronoUnit.DAYS))
                        .endsAt(now.minus(1, ChronoUnit.DAYS))
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.NON_REFUNDABLE)
                        .startsAt(now.minus(1, ChronoUnit.DAYS))
                        .build())
                .build();
        RefundRules newInfo = initial.actualize();
        assertThat(newInfo.isRefundable()).isFalse();
        assertThat(newInfo.getRules()).hasSize(1);
    }

    private ShoppingRate loadRate(String resourceName) throws IOException {
        if (!resourceName.endsWith(".json")) {
            resourceName += ".json";
        }
        String data = Resources.toString(Resources.getResource("expediaResponses/" + resourceName), Charset.defaultCharset());
        return mapper.readerFor(ShoppingRate.class).readValue(data);
    }
}
