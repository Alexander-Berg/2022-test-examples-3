package ru.yandex.market.wms.common.service.validation.rule.shelflife.sku;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.dto.SkuLifetimesDTO;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NonNegativeDaysValidationRuleTest {

    NonNegativeDaysValidationRule rule;

    @BeforeEach
    void init() {
        rule = new NonNegativeDaysValidationRule();
    }

    @Test
    void shouldAllowWhenInboundAndOutboundNulls() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldAllowWhenInboundAndOutboundNonNegative() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr5("1");
        skuLifetimes.setSusr4("1");

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldThrowExceptionWhenOutboundNegative() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr5("-1");

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }

    @Test
    void shouldThrowExceptionWhenInboundNegative() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr4("-1");

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }
}
