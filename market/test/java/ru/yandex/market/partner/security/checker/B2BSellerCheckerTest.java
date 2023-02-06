package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

public class B2BSellerCheckerTest extends FunctionalTest {

    @Autowired
    private B2BSellerChecker b2bSellerChecker;

    public static Stream<Arguments> data() {
        return Stream.of(
                data(10, "true", true),
                data(10, "false", false),
                data(20, "true", false),
                data(20, "false", true)
        );
    }

    private static Arguments data(long partnerId, String authorityValue, boolean expected) {
        return Arguments.of(partnerId, authorityValue, expected);
    }

    @ParameterizedTest
    @MethodSource("data")
    @DbUnitDataSet(before = "B2BSellerCheckerTest.before.csv")
    void test(long partnerId, String authorityValue, boolean expected) {
        Authority auth = new Authority("B2B_SELLER", authorityValue);
        MockPartnerRequest mockPartnerRequest = new MockPartnerRequest(1, partnerId, PartnerId.supplierId(partnerId));
        Assertions.assertEquals(expected, b2bSellerChecker.checkTyped(mockPartnerRequest, auth));
    }
}
