package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.widget;

import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.BRAND_MARKET_SHARE_BY_SHOPS;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.BRAND_PERCENT_BY_SHOPS;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.CATEGORY_BRANDS_SHARE_BY_REGIONS;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.CATEGORY_GROWTH_WATERFALL;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.CATEGORY_MARKET_SHARE;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.CATEGORY_MARKET_SHARE_WITH_BRAND;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.CATEGORY_SOC_DEM_DISTRIBUTION;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.PARENT_CATEGORY_MARKET_SHARE;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "WidgetCheckerTest.before.csv")
class WidgetTypeCreationCheckerTest extends AbstractCheckerTest {

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("widgetCreateCheckerArguments")
    @DisplayName("Проверка доступа к виджету")
    void widgetCreateChecker(long uid, long hid, WidgetType widgetType, boolean hasAccess) {
        var requestBody = requestBody(uid, hid, widgetType);
        var response = check("widgetTypeCreationChecker", requestBody);
        var expected = responseBody(hasAccess);
        assertJsonEquals(expected, response);
    }

    private static Stream<Arguments> widgetCreateCheckerArguments() {
        return StreamEx.of(
                Arguments.of(1500L, 10L, CATEGORY_MARKET_SHARE, true),
                Arguments.of(1500L, 10L, PARENT_CATEGORY_MARKET_SHARE, true),
                Arguments.of(1500L, 10L, BRAND_PERCENT_BY_SHOPS, true),
                Arguments.of(1500L, 15L, CATEGORY_MARKET_SHARE_WITH_BRAND, false),
                Arguments.of(3000L, 10L, CATEGORY_GROWTH_WATERFALL, false),
                // низкий уровень доступа у пользоваетля 5000 (один партнёр: магазин 100)
                Arguments.of(5000L, 20L, CATEGORY_GROWTH_WATERFALL, false),
                Arguments.of(5000L, 30L, CATEGORY_BRANDS_SHARE_BY_REGIONS, false),
                Arguments.of(5000L, 40L, CATEGORY_SOC_DEM_DISTRIBUTION, false),
                Arguments.of(5000L, 50L, BRAND_MARKET_SHARE_BY_SHOPS, false),
                // достаточный уровень доступа у пользователя 5000 (один партнёр: магазин 100)
                Arguments.of(5000L, 10L, PARENT_CATEGORY_MARKET_SHARE, true),
                // достаточный уровень доступа у пользователя 5001 (партнёры: магазины 100 и 110, вендор 111)
                Arguments.of(5001L, 10L, PARENT_CATEGORY_MARKET_SHARE, true),
                Arguments.of(5001L, 20L, CATEGORY_GROWTH_WATERFALL, true),
                Arguments.of(5001L, 30L, CATEGORY_BRANDS_SHARE_BY_REGIONS, true),
                Arguments.of(5001L, 40L, CATEGORY_SOC_DEM_DISTRIBUTION, true),
                Arguments.of(5001L, 50L, BRAND_MARKET_SHARE_BY_SHOPS, true)
        );
    }

    private static String requestBody(long uid, long hid, WidgetType widgetType) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s,\n"
                        + "   \"hid\": %s,\n"
                        + "   \"widgetType\": \"%s\"\n"
                        + "}",
                uid,
                hid,
                widgetType.name()
        );
    }

}
