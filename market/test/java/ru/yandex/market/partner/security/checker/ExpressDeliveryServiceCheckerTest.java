package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

/**
 * Тесты для {@link ExpressDeliveryServiceChecker}
 */
@DbUnitDataSet(before = "HasExpressDeliveryServiceCheckerTest.before.csv")
class ExpressDeliveryServiceCheckerTest extends FunctionalTest {

    @Autowired
    private ExpressDeliveryServiceChecker expressDeliveryServiceChecker;

    public static Stream<Arguments> data() {
        return Stream.of(
                data("Белый", 101101, false),
                data("Фулфиллмент", 202202, false),
                data("Дропшип с экспрессом", 303303, true),
                data("Дропшип без экспресса", 404404, false),
                data("Дропшип без склада", 505505, false)
        );
    }

    private static Arguments data(String description, long campaignId, boolean expected) {
        return Arguments.of(description, campaignId, expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void test(String description, long campaignId, boolean expected) {
        var mockPartnerRequest = new MockPartnerRequest(-1, -1L, campaignId, campaignId);
        Assertions.assertEquals(
                expected,
                expressDeliveryServiceChecker.checkTyped(mockPartnerRequest, new Authority())
        );
    }
}
