package ru.yandex.market.wms.common.service.validation.rule.shelflife.sku;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.dto.SkuLifetimesDTO;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PercentageValidationRuleTest {

    PercentageValidationRule rule;

    @BeforeEach
    void init() {
        rule = new PercentageValidationRule();
    }

    @Test
    void shouldAllowWhenInboundPercentageAndOutboundPercentageNulls() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldAllowWhenInboundAndOutboundPercentagesValid() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifePercentage(1);
        skuLifetimes.setShelflifeonreceivingPercentage(99);

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldThrowExceptionWhenPercentageNegative() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifePercentage(-1);

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }

    @Test
    void shouldThrowExceptionWhenPercentageMoreThan100() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifeonreceivingPercentage(101);

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }
}
