package ru.yandex.market.antifraud.orders.service;

import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudItemLimitRule;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.antifraud.orders.service.AntifraudItemLimitRulesValidator.validatingRules;

/**
 * @author dzvyagin
 */
public class AntifraudItemLimitRulesValidatorTest {

    @Test
    public void testInvalidRule() {
        var rule = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7719")
                .msku(12345679L)
                .categoryId(91491)
                .maxCountPerUser(3L)
                .build();
        var validationResult = Stream.of(rule).collect(validatingRules(true));
        assertThat(validationResult.getResult()).isEmpty();
        assertThat(validationResult.getErrors()).hasSize(1);
    }

    @Test
    public void testValidationSuccesful() {
        var rule = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7719")
                .categoryId(91491)
                .maxCountPerUser(3L)
                .build();
        var validationResult = Stream.of(rule).collect(validatingRules(true));
        assertThat(validationResult.getResult()).containsExactly(rule);
        assertThat(validationResult.getErrors()).isEmpty();
    }

    @Test
    public void testDuplicates() {
        var rule1 = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7719")
                .categoryId(91491)
                .supplierId(123456L)
                .historyPeriod(50)
                .maxCountPerUser(3L)
                .maxAmountPerUser(3000L)
                .maxCountPerOrder(2L)
                .maxAmountPerOrder(2000L)
                .build();
        var rule2 = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7720")
                .categoryId(91491)
                .supplierId(123457L)
                .historyPeriod(51)
                .maxCountPerUser(5L)
                .maxAmountPerUser(5000L)
                .maxCountPerOrder(4L)
                .maxAmountPerOrder(4000L)
                .build();
        var validationResult = Stream.of(rule1, rule2)
                .collect(validatingRules(false));
        assertThat(validationResult.getResult()).containsExactly(rule1);
        assertThat(validationResult.getErrors()).hasSize(1);
    }

    @Test
    public void testDuplicatesAllowed() {
        var rule1 = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7719")
                .categoryId(91491)
                .supplierId(123456L)
                .historyPeriod(50)
                .maxCountPerUser(3L)
                .maxAmountPerUser(3000L)
                .maxCountPerOrder(2L)
                .maxAmountPerOrder(2000L)
                .build();
        var rule2 = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7720")
                .categoryId(91491)
                .supplierId(123457L)
                .historyPeriod(51)
                .maxCountPerUser(5L)
                .maxAmountPerUser(5000L)
                .maxCountPerOrder(4L)
                .maxAmountPerOrder(4000L)
                .build();
        var validationResult = Stream.of(rule1, rule2)
                .collect(validatingRules(true));
        assertThat(validationResult.getResult()).containsExactly(rule1, rule2);
        assertThat(validationResult.getErrors()).isEmpty();
    }
}
