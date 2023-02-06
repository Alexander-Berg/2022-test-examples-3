/*
 * (C) 2018 Yandex Market LLC
 */
package ru.yandex.market.partner.mvc.controller.supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@ParametersAreNonnullByDefault
class SupplierDomainNormalizationTest {

    @ParameterizedTest
    @ValueSource(strings = {"http://www.shop.test.com/", "www.shop.test.com", "http://shop.test.com/"})
    void testDomainNormalization(String domainBeforeNormalization) {
        Assertions.assertEquals("shop.test.com", SupplierRegistrationDTO.normalizeDomain(domainBeforeNormalization));
    }
}
