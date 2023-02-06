package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.widget;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * Чекер на права доступа к виджетам {@link WidgetReaderChecker}
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "WidgetCheckerTest.before.csv")
public class WidgetReaderCheckerTest extends AbstractCheckerTest {

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("widgetReaderCheckerArguments")
    @DisplayName("Проверка доступа к виджету")
    void widgetReaderChecker(long uid, long widgetId, boolean expectedHasAccess, String testName) {
        var requestBody = widgetCheckRequestBody(uid, widgetId);
        var response = check("widgetReaderChecker", requestBody);
        var expected = responseBody(expectedHasAccess);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static Stream<Arguments> widgetReaderCheckerArguments() {
        return Stream.of(
                Arguments.of(1500L, 10L, true, "Есть доступ к виджету (categoryFilter)"),
                Arguments.of(1500L, 60L, true, "Есть доступ к виджету (hid)"),
                Arguments.of(1500L, 70L, true, "Есть доступ к виджету (categoriesFilter)"),
                Arguments.of(1500L, 40L, false, "Нет доступа к виджету (categoriesFilter)"),
                Arguments.of(3000L, 20L, false, "Нет доступа к виджету (отключен партнёр-вендор)"),
                Arguments.of(1500L, 156L, false, "Нет доступа к виджету (видежет не найден)"),
                // низкий уровень доступа у пользоваетля 5000 (один партнёр: магазин 100)
                Arguments.of(5000L, 1002L, false, "Нет доступа к виджету CATEGORY_GROWTH_WATERFALL"),
                Arguments.of(5000L, 1003L, false, "Нет доступа к виджету CATEGORY_BRANDS_SHARE_BY_REGIONS"),
                Arguments.of(5000L, 1004L, false, "Нет доступа к виджету CATEGORY_SOC_DEM_DISTRIBUTION"),
                Arguments.of(5000L, 1005L, false, "Нет доступа к виджету BRAND_MARKET_SHARE_BY_SHOPS"),
                // достаточный уровень доступа у пользователя 5000 (один партнёр: магазин 100)
                Arguments.of(5000L, 1001L, true, "Есть доступ к виджету PARENT_CATEGORY_MARKET_SHARE"),
                // достаточный уровень доступа у пользователя 5001 (партнёры: магазины 100 и 110, вендор 111)
                Arguments.of(5001L, 1001L, true, "Есть доступ к виджету PARENT_CATEGORY_MARKET_SHARE"),
                Arguments.of(5001L, 1002L, true, "Есть доступ к виджету CATEGORY_GROWTH_WATERFALL"),
                Arguments.of(5001L, 1003L, true, "Есть доступ к виджету CATEGORY_BRANDS_SHARE_BY_REGIONS"),
                Arguments.of(5001L, 1004L, true, "Есть доступ к виджету CATEGORY_SOC_DEM_DISTRIBUTION"),
                Arguments.of(5001L, 1005L, true, "Есть доступ к виджету BRAND_MARKET_SHARE_BY_SHOPS"),
                // user 5002, partner SHOP 112
                Arguments.of(5001L, 1001L, true, "У пользователя магазина есть доступ к PARENT_CATEGORY_MARKET_SHARE"),
                Arguments.of(5001L, 1002L, true, "У пользователя магазина есть доступ к CATEGORY_GROWTH_WATERFALL"),
                Arguments.of(5001L, 1003L, true, "У пользователя магазина есть доступ к CATEGORY_BRANDS_SHARE_BY_REGIONS"),
                Arguments.of(5001L, 1004L, true, "У пользователя магазина есть доступ к CATEGORY_SOC_DEM_DISTRIBUTION"),
                Arguments.of(5001L, 1005L, true, "У пользователя магазина нет доступа к BRAND_MARKET_SHARE_BY_SHOPS"),
                // user 5001, compare
                Arguments.of(5001L, 110L, true, "Есть доступ к виджету сравнению"),
                Arguments.of(5001L, 120L, false, "Нет доступа к виджету сравнению")
        );
    }

    private static String widgetCheckRequestBody(long uid, long widgetId) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s,\n"
                        + "   \"widgetId\": %s\n"
                        + "}",
                uid,
                widgetId
        );
    }
}
