package ru.yandex.market.wms.common.service.validation.rule.shelflife.sku;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.dto.SkuLifetimesDTO;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InboundMoreThenOutboundRuleTest {

    InboundMoreThenOutboundRule rule;

    @BeforeEach
    void init() {
        rule = new InboundMoreThenOutboundRule();
    }

    @Test
    void shouldAllowWhenInboundAndOutboundNulls() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldAllowWhenInboundMoreThenOutbound() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr5("10");
        skuLifetimes.setSusr4("30");

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldAllowWhenInboundPercentageMoreThenOutboundPercentage() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifePercentage(20);
        skuLifetimes.setShelflifeonreceivingPercentage(80);

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldThrowExceptionWhenOutboundMoreThenInbound() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr5("50");
        skuLifetimes.setSusr4("10");

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }

    @Test
    void shouldThrowExceptionWhenOutboundPercentageMoreThenInboundPercentage() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifePercentage(80);
        skuLifetimes.setShelflifeonreceivingPercentage(10);

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }
}
