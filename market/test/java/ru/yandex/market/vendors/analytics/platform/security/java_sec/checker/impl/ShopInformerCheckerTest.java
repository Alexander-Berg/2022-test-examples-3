package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "ShopInformerCheckerTest.before.csv")
public class ShopInformerCheckerTest extends AbstractCheckerTest {

    @ParameterizedTest(name = "{index}")
    @MethodSource("checkShopInformerArguments")
    @DisplayName("Проверка доступа к информеру магазина")
    void checkShopInformer(long uid, long shopId, boolean expectedAccess) {
        var response = check("shopInformerChecker", informerUserCheckRequestBody(uid, shopId));
        var expected = responseBody(expectedAccess);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static Stream<Arguments> checkShopInformerArguments() {
        return Stream.of(
                Arguments.of(1001L, 1L, true),
                Arguments.of(1002L, 1L, false),
                Arguments.of(1001L, 2L, false)
        );
    }

    private static String informerUserCheckRequestBody(long uid, long shopId) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s,\n"
                        + "   \"hid\": %s\n"
                        + "}",
                uid, shopId
        );
    }
}
