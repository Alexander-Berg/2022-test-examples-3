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
@DbUnitDataSet(before = "ShopUserManagerCheckerTest.before.csv")
class ShopUserManagementCheckerTest extends AbstractCheckerTest {

    @ParameterizedTest(name = "{index}")
    @MethodSource("checkShopUserManagementArguments")
    @DisplayName("Проверка доступа к странице управления пользователями магазина")
    void checkShopUserManagement(long uid, boolean expectedAccess) {
        var response = check("shopUserManagementChecker", shopUserManagementCheckerBody(uid));
        var expected = responseBody(expectedAccess);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static Stream<Arguments> checkShopUserManagementArguments() {
        return Stream.of(
                Arguments.of(100L, false),
                Arguments.of(200L, false),
                Arguments.of(300L, false),
                Arguments.of(400L, true),
                Arguments.of(500L, true),
                Arguments.of(600L, true)
        );
    }

    private static String shopUserManagementCheckerBody(long uid) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s\n"
                        + "}",
                uid
        );
    }
}