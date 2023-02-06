package ru.yandex.market.vendors.analytics.platform.controller.sales.socdem;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Functional tests for {@link SocdemSalesController}.
 *
 * @author antipov93.
 */
@ClickhouseDbUnitDataSet(before = "SocdemSalesControllerTest.clickhouse.csv")
public class SocdemSalesControllerTest extends CalculateFunctionalTest {

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
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        String actual = getSocdemDistribution(body);
        String expected = loadFromFile("SocdemSalesControllerTest.socdemDistribution.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Распределение продаж по соцдему с новыми скрытиями")
    @DbUnitDataSet(before = "SocdemDistributionNewHiding.csv")
    @ClickhouseDbUnitDataSet(before = "SocdemDistributionNewHiding.ch.csv")
    void socdemDistributionNewHiding() {
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
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getSocdemDistribution(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\n"
                        + "  \"code\": \"BAD_FILTERS\",\n"
                        + "  \"message\": \"Hiding exception with type BRAND\",\n"
                        + "  "
                        + "\"hidingType\": \"BRAND\"\n" +
                        "}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Распределение продаж по соцдему: выбраны несовместимые фильтры")
    void incompatibleFilters() {
        String body = ""
                + "{\n"
                + "  \"modelIds\": [1000, 2000],\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-10\",\n"
                + "    \"endDate\": \"2019-06-25\"\n"
                + "  },\n"
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {\n"
                + "      \"201\": [1, 2],\n"
                + "      \"202\": [100]\n"
                + "    }\n"
                + "  },\n"
                + "  \"timeDetailing\": \"QUARTER\",\n"
                + "  \"visualization\": \"AFFINITY\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getSocdemDistribution(body)
        );

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(""
                        + "{\n"
                        + "  \"code\": \"BAD_REQUEST\",\n"
                        + "  \"message\": \"Don't pass models and report filters at the same time.\"\n"
                        + "}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Если не был передан таймдетейлинг - кидать 400ку")
    void noTimeDetailing() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-10\",\n"
                + "    \"endDate\": \"2019-06-25\"\n"
                + "  },\n"
                + "  \"dashboardId\": 100,\n"
                + "  \"visualization\": \"AFFINITY\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getSocdemDistribution(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        String expected = "{\n"
                + "  \"code\": \"BAD_REQUEST\",\n"
                + "  \"message\": \"[Time detailing should be present in request]\"\n"
                + "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    private String getSocdemDistribution(String body) {
        String url = UriComponentsBuilder.fromUriString(getFullWidgetUrl(SOCDEM_DISTRIBUTION_PATH))
                .toUriString();
        return FunctionalTestHelper.postForJson(url, body);
    }
}
