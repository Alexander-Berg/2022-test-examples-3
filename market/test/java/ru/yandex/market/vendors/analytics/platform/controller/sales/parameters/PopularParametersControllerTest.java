package ru.yandex.market.vendors.analytics.platform.controller.sales.parameters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * @author antipov93.
 */
@ClickhouseDbUnitDataSet(before = "PopularParametersControllerTest.before.csv")
@DbUnitDataSet(before = "PopularParameters.before.csv")
class PopularParametersControllerTest extends CalculateFunctionalTest {

    @Test
    @DisplayName("Простой расчёт")
    void simple() {
        String body = "{"
                + "  \"hid\": 100,"
                + "  \"interval\": {"
                + "    \"startDate\": \"2020-01-01\","
                + "    \"endDate\": \"2020-03-01\""
                + "  },"
                + "  \"timeDetailing\": \"MONTH\","
                + "  \"selectionInfo\": {"
                + "    \"strategy\": \"MONEY\","
                + "    \"limit\": 5"
                + "  },"
                + "  \"visualization\": \"LINE\","
                + "  \"measure\": \"MONEY_PART\","
                + "  \"fullData\": true"
                + "}";

        String actual = calculate(body);
        String expected = loadFromFile("PopularParametersControllerTest.simple.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Расчёт по модефикации")
    void modification() {
        String body = "{"
                + "  \"hid\": 200,"
                + "  \"interval\": {"
                + "    \"startDate\": \"2020-01-01\","
                + "    \"endDate\": \"2020-03-01\""
                + "  },"
                + "  \"timeDetailing\": \"MONTH\","
                + "  \"selectionInfo\": {"
                + "    \"strategy\": \"MONEY\","
                + "    \"limit\": 5"
                + "  },"
                + "  \"visualization\": \"LINE\","
                + "  \"measure\": \"MONEY_PART\","
                + "  \"fullData\": true"
                + "}";

        String actual = calculate(body);
        String expected = loadFromFile("PopularParametersControllerTest.modification.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Фильтр по параметру")
    void paramFilter() {
        String body = "{"
                + "  \"hid\": 100,"
                + "  \"interval\": {"
                + "    \"startDate\": \"2020-01-01\","
                + "    \"endDate\": \"2020-03-01\""
                + "  },"
                + "  \"timeDetailing\": \"MONTH\","
                + "  \"selectionInfo\": {"
                + "    \"strategy\": \"COUNT\","
                + "    \"limit\": 5"
                + "  },"
                + "  \"visualization\": \"LINE\","
                + "  \"measure\": \"COUNT\","
                + "  \"parameters\": [1000]"
                + "}";

        String actual = calculate(body);
        String expected = loadFromFile("PopularParametersControllerTest.paramFilter.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }


    private String calculate(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl("/parameters/top", 1L), body);
    }
}