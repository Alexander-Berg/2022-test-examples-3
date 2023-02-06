package ru.yandex.market.wms.common.service.validation.rule.shelflife.sku;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.dto.SkuLifetimesDTO;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrectIndicatorRuleTest {

    CorrectIndicatorRule rule;

    @BeforeEach
    void init() {
        rule = new CorrectIndicatorRule();
    }

    @Test
    void shouldAllowNullShelflifeIndicator() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldAllowEnabledShelflifeIndicator() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifeindicator("Y");

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldThrowExceptionWhenShelflifeIndicatorHasWrongValue() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setShelflifeindicator("WRONG_VALUE");

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }
}
