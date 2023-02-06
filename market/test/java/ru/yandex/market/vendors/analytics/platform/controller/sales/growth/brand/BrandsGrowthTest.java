package ru.yandex.market.vendors.analytics.platform.controller.sales.growth.brand;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.growth.GrowthDriversController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.model.ModelFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Functional tests for {@link GrowthDriversController}}.
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "BrandsGrowthTest.before.csv")
@ClickhouseDbUnitDataSet(before = "BrandsGrowthTest.ch.before.csv")
public class BrandsGrowthTest extends ModelFunctionalTest {

    private static final long DASHBOARD_ID = 100;
    private static final String BRAND_GROWTH_DRIVERS_PATH = "/growthDrivers/brands";

    @Test
    @DisplayName("Рост продаж брендов в категории")
    void brandsGrowth() {
        String reportResponse = loadFromFile("ReportBrandsGrowth.response.json");
        var requestUrl = BASE_REPORT_URL + "&hid=91491"
                + "&modelid=1,4,10,11,13"
                + "&glfilter=201:1,2&glfilter=202:100";

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        String expected = loadFromFile("BrandGrowthTest.response.json");

        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491,\n"
                + "    \"priceSegments\": [\n"
                + "      7,\n"
                + "      8\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-02-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {\n"
                + "      \"423423\": [],"
                + "      \"22342342\": null,"
                + "      \"201\": [1, 2],\n"
                + "      \"202\": [100]\n"
                + "    }\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getBrandGrowth(body);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Рост брендов в категории, нет данных")
    void brandsGrowthNoSales() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491,\n"
                + "    \"priceSegments\": [\n"
                + "      7,\n"
                + "      8\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-03-01\",\n"
                + "    \"endDate\": \"2019-03-31\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getBrandGrowth(body)
        );

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\"code\":\"NO_DATA\",\"message\":\"There are no sales for this periods\"}",
                clientException.getResponseBodyAsString()
        );
    }

    private String getBrandGrowth(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(BRAND_GROWTH_DRIVERS_PATH, DASHBOARD_ID), body);
    }
}
