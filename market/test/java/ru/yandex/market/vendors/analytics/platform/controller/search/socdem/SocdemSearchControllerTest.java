package ru.yandex.market.vendors.analytics.platform.controller.search.socdem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.socdem.SocdemSalesController;

/**
 * Functional tests for {@link SocdemSalesController}.
 *
 * @author antipov93.
 */
@ClickhouseDbUnitDataSet(before = "SocdemSearchControllerTest.clickhouse.csv")
public class SocdemSearchControllerTest extends CalculateFunctionalTest {

    private static final String SOCDEM_DISTRIBUTION_PATH = "/socdem/distribution";

    @Test
    @DisplayName("Распределение продаж по соцдему")
    void socdemDistribution() {
        String body = ""
                + "{\n"
                + "  \"brands\": [1, 2, 3],\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-10\",\n"
                + "    \"endDate\": \"2019-06-25\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"QUARTER\",\n"
                + "  \"visualization\": \"AFFINITY\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";

        String actual = getSocdemDistribution(body);
        String expected = loadFromFile("SocdemSearchControllerTest.socdemDistribution.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    private String getSocdemDistribution(String body) {
        String url = UriComponentsBuilder.fromUriString(getFullWidgetUrl(SOCDEM_DISTRIBUTION_PATH))
                .toUriString();
        return FunctionalTestHelper.postForJson(url, body);
    }
}
