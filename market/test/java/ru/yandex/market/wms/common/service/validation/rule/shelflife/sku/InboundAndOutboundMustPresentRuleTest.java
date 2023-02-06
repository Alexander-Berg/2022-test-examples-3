package ru.yandex.market.wms.common.service.validation.rule.shelflife.sku;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.dto.SkuLifetimesDTO;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InboundAndOutboundMustPresentRuleTest {

    InboundAndOutboundMustPresentRule rule;

    @BeforeEach
    void init() {
        rule = new InboundAndOutboundMustPresentRule();
    }


    @Test
    void shouldAllowNullShelflifeIndicator() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldAllowDisabledShelflifeIndicator() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifeindicator("N");

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldThrowExceptionWhenEnabledIndicatorAndInboundLifetimesMissed() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifeindicator("Y");
        skuLifetimes.setShelflife(10);
        skuLifetimes.setShelflifePercentage(10);

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }

    @Test
    void shouldThrowExceptionWhenEnabledIndicatorAndOutboundLifetimesMissed() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifeindicator("Y");
        skuLifetimes.setShelflifeonreceiving(10);
        skuLifetimes.setShelflifeonreceivingPercentage(10);

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }
}
