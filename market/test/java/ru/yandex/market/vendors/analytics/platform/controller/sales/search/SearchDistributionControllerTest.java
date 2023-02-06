package ru.yandex.market.vendors.analytics.platform.controller.sales.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;

/**
 * @author antipov93.
 */
@ClickhouseDbUnitDataSet(before = "SearchDistributionControllerTest.clickhouse.csv")
class SearchDistributionControllerTest extends CalculateFunctionalTest {

    @Test
    @DisplayName("Распределение поискового интереса по месяцам")
    void searchDistributionByMonths() {
        String body = ""
                + "{\n"
                + "  \"hid\": 1,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2020-01-01\",\n"
                + "    \"endDate\": \"2020-02-29\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"TABLE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";

        String actual = getSearchDistribution(body);
        String expected = loadFromFile("SearchDistributionControllerTest.searchDistributionByMonth.json");
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Суммарное распределение поискового интереса")
    void searchDistributionTotal() {
        String body = ""
                + "{\n"
                + "  \"hid\": 1,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2020-01-01\",\n"
                + "    \"endDate\": \"2020-02-29\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"visualization\": \"TABLE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";

        String actual = getSearchDistribution(body);
        String expected = loadFromFile("SearchDistributionControllerTest.searchDistributionTotal.json");
        assertEquals(expected, actual);
    }


    @Test
    @DisplayName("Суммарное распределение поискового интереса с фильтром по брендам")
    void searchDistributionWithBrandFilter() {
        String body = ""
                + "{\n"
                + "  \"hid\": 1,\n"
                + "  \"brands\": [10, 11],\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2020-01-01\",\n"
                + "    \"endDate\": \"2020-02-29\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"visualization\": \"TABLE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";

        String actual = getSearchDistribution(body);
        String expected = loadFromFile("SearchDistributionControllerTest.searchDistributionWithBrandFilter.json");
        assertEquals(expected, actual);
    }

    private String getSearchDistribution(String body) {
        var url = UriComponentsBuilder.fromUriString(getFullWidgetUrl("/search/distribution"))
                .toUriString();
        return FunctionalTestHelper.postForJson(url, body);
    }
}