package ru.yandex.market.wms.common.service.validation.rule.shelflife.sku;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.dto.SkuLifetimesDTO;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrectDisabledStateRuleTest {

    CorrectDisabledStateRule rule;

    @BeforeEach
    void init() {
        rule = new CorrectDisabledStateRule();
    }

    @Test
    void shouldAllowEnabledShelflifeIndicator() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifeindicator("Y");

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldThrowExceptionWhenShelflifeIndicatorDisabledAndInboundShelflifePresent() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifeindicator("N");
        skuLifetimes.setShelflifeonreceiving(99);
        skuLifetimes.setShelflifeonreceivingPercentage(99);

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }

    @Test
    void shouldThrowExceptionWhenShelflifeIndicatorDisabledAndOutboundShelflifePresent() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifeindicator("N");
        skuLifetimes.setShelflife(33);
        skuLifetimes.setShelflifePercentage(33);

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }
}
