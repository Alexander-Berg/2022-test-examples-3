package ru.yandex.market.wms.common.service.validation.rule.shelflife.sku;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.dto.SkuLifetimesDTO;

import static org.junit.jupiter.api.Assertions.assertThrows;

class Susr4Susr5IsIntegerRuleTest {

    Susr4Susr5IsIntegerRule rule;

    @BeforeEach
    void init() {
        rule = new Susr4Susr5IsIntegerRule();
    }

    @Test
    void shouldAllowWhenSusr4AndSusr5Nulls() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldAllowWhenSusr4AndSusr5Valid() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr4("1");
        skuLifetimes.setSusr5("99");

        rule.validate(skuLifetimes);
    }

    @Test
    void shouldThrowExceptionWhenSusr4HasWrongFormat() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr4("9d");

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }

    @Test
    void shouldThrowExceptionWhenSusr5HasWrongFormat() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr5("9d");

        assertThrows(Exception.class, () -> rule.validate(skuLifetimes));
    }
}
