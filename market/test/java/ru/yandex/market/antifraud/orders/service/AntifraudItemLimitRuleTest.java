package ru.yandex.market.antifraud.orders.service;

import java.time.LocalDate;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudItemLimitRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author dzvyagin
 */
public class AntifraudItemLimitRuleTest {

    @Test
    public void testTagValidation() {
        var rule = AntifraudItemLimitRule.builder()
                .categoryId(91491)
                .maxCountPerUser(3L)
                .build();
        assertThatCode(rule::validate)
                .hasMessageContaining("tag");
    }

    @Test
    public void testConstraintsValidation() {
        var rule = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7719")
                .categoryId(91491)
                .maxAmountPerUser(0L)
                .build();
        assertThatCode(rule::validate)
                .hasMessageContaining("constraint");
    }

    @Test
    public void testNoFiltersValidation() {
        var rule = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7719")
                .maxCountPerUser(3L)
                .build();
        assertThatCode(rule::validate)
                .hasMessageContaining("exactly one of msku, modelId, categoryId");
    }

    @Test
    public void testManyFiltersValidation() {
        var rule = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7719")
                .msku(12345679L)
                .categoryId(91491)
                .maxCountPerUser(3L)
                .build();
        assertThatCode(rule::validate)
                .hasMessageContaining("exactly one of msku, modelId, categoryId");
    }

    @Test
    public void testInvalidPeriod() {
        var rule = AntifraudItemLimitRule.builder()
            .tag("MARKETCHECKOUT-7719")
            .categoryId(91491)
            .maxCountPerUser(3L)
            .periodFrom(LocalDate.now().plusDays(2))
            .periodTo(LocalDate.now().plusDays(1))
            .build();
        assertThatCode(rule::validate)
                .hasMessageContaining("periodFrom is after periodTo");
    }

    @Test
    public void testEndedPeriod() {
        var rule = AntifraudItemLimitRule.builder()
            .tag("MARKETCHECKOUT-7719")
            .categoryId(91491)
            .maxCountPerUser(3L)
            .periodTo(LocalDate.of(2022, 6, 1))
            .build();
        assertThatCode(rule::validate)
                .hasMessageContaining("Rule period");
    }

    @Test
    public void testValidationSuccesful() {
        var rule = AntifraudItemLimitRule.builder()
                .tag("MARKETCHECKOUT-7719")
                .categoryId(91491)
                .maxCountPerUser(3L)
                .build();
        assertThatCode(rule::validate)
                .doesNotThrowAnyException();
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
        assertThat(rule1.getIdentityKey()).isEqualTo(rule2.getIdentityKey());
    }
}
