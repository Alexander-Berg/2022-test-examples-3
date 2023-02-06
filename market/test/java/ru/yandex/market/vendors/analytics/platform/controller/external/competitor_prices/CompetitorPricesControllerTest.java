package ru.yandex.market.vendors.analytics.platform.controller.external.competitor_prices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.model.common.language.Language;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DbUnitDataSet(before = "competitorPrices.before.csv")
public class CompetitorPricesControllerTest extends FunctionalTest {
    private static final String COMPETITOR_PRICES_BASE_PATH = "/reports/competitorPrices/";
    private static final String SHOP_OFFERS_SUFFIX = "shopOffers";

    private static final String BASE_REPORT_PATTERN = "http:\\/\\/report.tst.vs.market.yandex.net:17051\\/yandsearch"
            + "\\?place=productoffers&client=analytics-platform&pp=18";

    @Autowired
    protected RestTemplate reportRestTemplate;

    protected MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void resetMocks() {
        mockRestServiceServer = MockRestServiceServer.createServer(reportRestTemplate);
    }

    @ClickhouseDbUnitDataSet(before = "competitorPrices.ch.before.csv")
    @Test
    @DisplayName("Проверка метода shopOffers")
    public void getShopOffers() {
        String reportResponse = loadFromFile("competitorPrices.report.response.json");
        var requestUrl = BASE_REPORT_PATTERN
                + "&rids=213&page=1&numdoc=1000&cpa=real&cpa-category-filter=1&hideglfilters=1&hidenonglfilters=1&adult=1"
                + "&market-sku=.*"
                + "&hyperid=&fesh=";

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(matchesPattern(requestUrl)))
                .andExpect(queryParam("market-sku", allOf(
                        containsString("2000001"),
                        containsString("2000002"),
                        containsString("2000003"),
                        containsString("2000004"))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        String body =
                "{\n" +
                        "  \"shopId\": 100000,\n" +
                        "  \"regionId\": 1,\n" +
                        "  \"filters\": {\n" +
                        "    \"categories\": [\n" +
                        "    ],\n" +
                        "    \"priceLevel\": \"ANY\"," +
                        "    \"textFilter\": \"\"," +
                        "    \"vendors\": [\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"paging\": {\n" +
                        "    \"pageNumber\": 0,\n" +
                        "    \"pageSize\": 20\n" +
                        "  },\n" +
                        "  \"campaignId\" : \"505\",\n" +
                        "  \"platformType\": \"shop\",\n" +
                        "  \"shopName\": \"Тестовый магазин\"" +
                        "}";

        String actual = getShopOffers(body);
        String expected = loadFromFile("competitorPrices.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @ClickhouseDbUnitDataSet(before = "competitorPrices.ch.before.csv")
    @Test
    @DisplayName("Проверка, что метод shopOffers возвращает цену = null, если магазин не продает товар в регионе")
    public void priceIsNullable() {
        String reportResponse = loadFromFile("priceIsNullable.report.response.json");
        var requestUrl = BASE_REPORT_PATTERN
                + "&rids=213&page=1&numdoc=1000&cpa=real&cpa-category-filter=1&hideglfilters=1&hidenonglfilters=1&adult=1"
                + "&market-sku=.*"
                + "&hyperid=&fesh=";

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(matchesPattern(requestUrl)))
                .andExpect(queryParam("market-sku", allOf(containsString("2000002"))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        String body =
                "{\n" +
                        "  \"shopId\": 100001,\n" +
                        "  \"regionId\": 1,\n" +
                        "  \"filters\": {\n" +
                        "    \"categories\": [\n" +
                        "    ],\n" +
                        "    \"priceLevel\": \"ANY\"," +
                        "    \"textFilter\": \"\"," +
                        "    \"vendors\": [\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"paging\": {\n" +
                        "    \"pageNumber\": 0,\n" +
                        "    \"pageSize\": 20\n" +
                        "  },\n" +
                        "  \"campaignId\" : \"500\",\n" +
                        "  \"platformType\": \"shop\",\n" +
                        "  \"shopName\": \"Тестовый магазин\"" +
                        "}";

        String actual = getShopOffers(body);
        String expected = loadFromFile("priceIsNullable.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @ClickhouseDbUnitDataSet(before = { "referencePrices.ch.before.csv" })
    @Test
    @DisplayName("Проверка, что метод shopOffers возвращает референсные цены")
    public void testReferencePrices() {
        String reportResponse = loadFromFile("referencePrices.report.response.json");
        var requestUrl = BASE_REPORT_PATTERN
                + "&rids=213&page=1&numdoc=1000&cpa=real&cpa-category-filter=1&hideglfilters=1&hidenonglfilters=1&adult=1"
                + "&market-sku=.*"
                + "&hyperid=&fesh=";

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(matchesPattern(requestUrl)))
                .andExpect(queryParam("market-sku", allOf(
                        containsString("2000001"),
                        containsString("2000002"),
                        containsString("2000003"),
                        containsString("2000004"),
                        containsString("2000005"),
                        containsString("2000006"))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        String body =
                "{\n" +
                        "  \"shopId\": 100000,\n" +
                        "  \"regionId\": 1,\n" +
                        "  \"filters\": {\n" +
                        "    \"categories\": [\n" +
                        "    ],\n" +
                        "    \"priceLevel\": \"ANY\"," +
                        "    \"textFilter\": \"\"," +
                        "    \"vendors\": [\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"paging\": {\n" +
                        "    \"pageNumber\": 0,\n" +
                        "    \"pageSize\": 20\n" +
                        "  },\n" +
                        "  \"campaignId\" : \"505\",\n" +
                        "  \"platformType\": \"shop\",\n" +
                        "  \"shopName\": \"Тестовый магазин\"" +
                        "}";

        String actual = getShopOffers(body);
        String expected = loadFromFile("referencePrices.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    private String getShopOffers(String body) {
        return FunctionalTestHelper.postForJson(getFullUrl(SHOP_OFFERS_SUFFIX).toUriString(), body);
    }

    private UriComponentsBuilder getFullUrl(String urlSuffix) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path(COMPETITOR_PRICES_BASE_PATH)
                .path(urlSuffix)
                .queryParam("language", Language.RU);
    }
}
