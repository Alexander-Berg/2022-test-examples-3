package ru.yandex.market.vendors.analytics.platform.controller.sales.common.interval;

import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.CATEGORY_MARKET_SHARE;
import static ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType.CATEGORY_PRICE_SEGMENTS;

/**
 * Функциональные тесты для {@link WidgetDataIntervalController}.
 *
 * @author ogonek.
 */
@SuppressWarnings("ConstantConditions")
@DbUnitDataSet(before = "ResearchIntervalControllerTest.before.csv")
public class WidgetDataIntervalControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Детализация месяц")
    void monthDetailing() {
        String response = getSalesWidgetInterval(CATEGORY_MARKET_SHARE, TimeDetailing.MONTH);
        String expected = "[{"
                + "    \"widgetType\":\"CATEGORY_MARKET_SHARE\","
                + "    \"interval\":{\"startDate\":\"2017-07-01\",\"endDate\":\"2019-03-31\"}"
                + "}]";
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Детализация None")
    void noneDetailing() {
        String response = getSalesWidgetInterval(CATEGORY_MARKET_SHARE, TimeDetailing.NONE);
        String expected = "[{"
                + "\"widgetType\":\"CATEGORY_MARKET_SHARE\","
                + "\"interval\":{\"startDate\":\"2017-07-01\",\"endDate\":\"2019-03-05\"}"
                + "}]";
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Несколько типов виджета")
    void severalWidgetTypes() {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("widget/data/intervals")
                .queryParam("widgetType", CATEGORY_MARKET_SHARE, CATEGORY_PRICE_SEGMENTS)
                .queryParam("timeDetailing", TimeDetailing.MONTH)
                .toUriString();

        String response = FunctionalTestHelper.get(url).getBody();

        String expected = "[\n" +
                "   {\n" +
                "      \"widgetType\":\"CATEGORY_MARKET_SHARE\",\n" +
                "      \"interval\":{\n" +
                "         \"startDate\":\"2017-07-01\",\n" +
                "         \"endDate\":\"2019-03-31\"\n" +
                "      }\n" +
                "   },\n" +
                "   {\n" +
                "      \"widgetType\":\"CATEGORY_PRICE_SEGMENTS\",\n" +
                "      \"interval\":{\n" +
                "         \"startDate\":\"2017-07-01\",\n" +
                "         \"endDate\":\"2019-03-31\"\n" +
                "      }\n" +
                "   }\n" +
                "]";
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Продажи в нескольких типах виджетов")
    void severalWidgetTypesSales() {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("widget/sales/intervals")
                .queryParam("widgetType", CATEGORY_MARKET_SHARE, CATEGORY_PRICE_SEGMENTS, CATEGORY_MARKET_SHARE)
                .queryParam("uid", 1)
                .queryParam("hid", 1)
                .toUriString();

        String response = FunctionalTestHelper.get(url).getBody();

        String expected = "[\n" +
                "   {\n" +
                "      \"widgetType\":\"CATEGORY_MARKET_SHARE\",\n" +
                "      \"interval\":{\n" +
                "         \"startDate\":\"2017-07-01\",\n" +
                "         \"endDate\":\"2019-03-05\"\n" +
                "      }\n" +
                "   },\n" +
                "   {\n" +
                "      \"widgetType\":\"CATEGORY_PRICE_SEGMENTS\",\n" +
                "      \"interval\":{\n" +
                "         \"startDate\":\"2017-07-01\",\n" +
                "         \"endDate\":\"2019-03-05\"\n" +
                "      }\n" +
                "   }\n" +
                "]";
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    void promoVendorInterval() {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("widget/sales/intervals")
                .queryParam("widgetType", CATEGORY_MARKET_SHARE)
                .queryParam("uid", 2)
                .queryParam("hid", 1)
                .toUriString();

        String response = FunctionalTestHelper.get(url).getBody();

        String expected = "[\n" +
                "   {\n" +
                "      \"widgetType\":\"CATEGORY_MARKET_SHARE\",\n" +
                "      \"interval\":{\n" +
                "         \"startDate\":\"2019-12-30\",\n" +
                "         \"endDate\":\"2020-02-02\"\n" +
                "      }\n" +
                "   }" +
                "]";
        JsonTestUtil.assertEquals(expected, response);
    }

    @ParameterizedTest
    @MethodSource("checkAllWidgetsAreSupportedArguments")
    @DisplayName("Поддержаны все типы виджетов")
    void checkAllWidgetsAreSupported(WidgetType widgetType, TimeDetailing timeDetailing) {
        var response = getSalesWidgetInterval(widgetType, timeDetailing);
        assertNotNull(response);
    }

    private static Stream<Arguments> checkAllWidgetsAreSupportedArguments() {
        return StreamEx.of(WidgetType.values())
                .cross(TimeDetailing.values())
                .mapKeyValue(Arguments::of);
    }

    private String getSalesWidgetInterval(WidgetType widgetType, TimeDetailing timeDetailing) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("widget", "data", "intervals")
                .queryParam("widgetType", widgetType)
                .queryParam("timeDetailing", timeDetailing.getId())
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }
}
