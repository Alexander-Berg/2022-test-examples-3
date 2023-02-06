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
 * Javasec-чекер. Проверка наличие успешной поставки
 */
@DbUnitDataSet(before = "firstSupplyCheckerTest.csv")
public class FirstSupplyCheckerTest extends FunctionalTest {
    @Autowired
    private FirstSupplyChecker firstSupplyChecker;

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(1001L, 1L, "fulfillment", true),
                Arguments.of(1002L, 2L, "fulfillment", false)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void test(long campaignId, long datasourceId, String authorityValue, boolean expected) {
        Assertions.assertEquals(expected, firstSupplyChecker.checkTyped(new MockPartnerRequest(-1, -1L, datasourceId, campaignId), new Authority("test", authorityValue)));
    }
}
