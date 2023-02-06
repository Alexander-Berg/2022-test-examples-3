package ru.yandex.market.vendors.analytics.platform.controller.sales.channels;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * @author ogonek.
 */

@DbUnitDataSet(before = "SalesChannelsControllerTest.before.csv")
public class SalesChannelsControllerTest extends CalculateFunctionalTest {

    @Test
    @DisplayName("Простой расчёт")
    @ClickhouseDbUnitDataSet(before = "SalesChannelsControllerTest.ch.before.csv")
    void simpleTest() {
        String body = "{"
                + "  \"hid\": 100,"
                + "  \"interval\": {"
                + "    \"startDate\": \"2020-01-01\","
                + "    \"endDate\": \"2020-03-15\""
                + "  },"
                + "  \"timeDetailing\": \"MONTH\","
                + "  \"visualization\": \"LINE\","
                + "  \"measure\": \"MONEY_PART\","
                + "  \"fullData\": true"
                + "}";

        String actual = calculate(body);
        String expected = loadFromFile("SalesChannelsControllerTest.simple.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Проверка фильтрации по каналам")
    @ClickhouseDbUnitDataSet(before = "SalesChannelsControllerTest.channelsFiltering.ch.before.csv")
    void сhannelsFilteringTest() {
        String body = "{"
                + "  \"hid\": 100,"
                + "  \"interval\": {"
                + "    \"startDate\": \"2020-01-01\","
                + "    \"endDate\": \"2020-03-15\""
                + "  },"
                + "  \"timeDetailing\": \"NONE\","
                + "  \"visualization\": \"LINE\","
                + "  \"measure\": \"MONEY_PART\","
                + "  \"fullData\": true"
                + "}";

        String actual = calculate(body);
        String expected = loadFromFile("SalesChannelsControllerTest.channelsFiltering.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    private String calculate(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl("/salesChannels", 1L), body);
    }
}
