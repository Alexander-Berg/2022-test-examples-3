package ru.yandex.market.api.partner.controllers.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.model.model.ModelIds;
import ru.yandex.market.api.partner.controllers.model.model.Models;
import ru.yandex.market.api.partner.controllers.util.CurrencyAndRegionHelper;
import ru.yandex.market.api.partner.model.PapiModelService;
import ru.yandex.market.api.partner.report.ProductOffersReportParser;
import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.parser.Parsers;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.Model;
import ru.yandex.market.common.report.model.Offer;
import ru.yandex.market.common.report.model.Offers;
import ru.yandex.market.common.report.model.Prices;
import ru.yandex.market.common.report.parser.xml.BulkModelOfferCountsReportXmlParser;
import ru.yandex.market.common.report.parser.xml.BulkModelOffersReportXmlParser;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.io.MbiFiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Проверяем {@link ModelControllerV1}.
 */
class ModelControllerV1Test extends FunctionalTest {
    private static final int MODEL_ID = 624180294;
    private static final int UID = 67282295;
    @Qualifier("marketReportService")
    @Autowired
    private AsyncMarketReportService marketReportService;

    private ModelControllerV1 controller = new ModelControllerV1();

    private void mockReportService() {

        controller.setMarketReportService(marketReportService);
        controller.setPapiModelService(mock(PapiModelService.class));
        controller.setRegionService(mock(RegionService.class));
        controller.setCurrencyAndRegionHelper(mock(CurrencyAndRegionHelper.class));
        Mockito.when(marketReportService.async(
                Mockito.argThat(arg -> arg != null
                        && arg.getPlace() == MarketReportPlace.PARTNER_OFFER_COUNTS), any())
        ).thenAnswer(inv -> {
            BulkModelOfferCountsReportXmlParser parserCount = new BulkModelOfferCountsReportXmlParser();
            LiteInputStreamParser<BulkModelOfferCountsReportXmlParser> liteParserCount = Parsers.itself(parserCount);
            BulkModelOfferCountsReportXmlParser searchResultCount = liteParserCount.parse(
                    this.getClass().getResourceAsStream("partner_model_count_report_response.xml"));
            return CompletableFuture.completedFuture(searchResultCount);
        });

        Mockito.when(marketReportService.async(
                Mockito.argThat(arg -> arg != null
                        && arg.getPlace() == MarketReportPlace.PRODUCT_OFFERS),
                any()))
                .thenAnswer(inv -> {
                    ProductOffersReportParser parser = new ProductOffersReportParser();
                    LiteInputStreamParser<ProductOffersReportParser> liteParser = Parsers.itself(parser);
                    ProductOffersReportParser searchResult = liteParser.parse(
                            this.getClass().getResourceAsStream("partner_productoffers_report_response.json"));
                    return CompletableFuture.completedFuture(searchResult);
                });

        Mockito.when(marketReportService.async(
                Mockito.argThat(arg -> arg != null
                        && arg.getPlace() == MarketReportPlace.PARTNER_MODEL_OFFERS),
                any()))
                .thenAnswer(inv -> {
                    BulkModelOffersReportXmlParser parser = new BulkModelOffersReportXmlParser();
                    LiteInputStreamParser<BulkModelOffersReportXmlParser> liteParser = Parsers.itself(parser);
                    BulkModelOffersReportXmlParser searchResult = liteParser.parse(
                            this.getClass().getResourceAsStream("partner_model_offers_report_response.xml"));
                    return CompletableFuture.completedFuture(searchResult);
                });
    }

    @BeforeEach
    void setUp() {
        mockReportService();
    }

    @Test
    void getModelOffers() {
        Model expected = getExpectedResponse();
        ModelIds modelIds = toModelId(624180294);
        ModelControllerV1.PriceSortOrder order = ModelControllerV1.PriceSortOrder.ASC;
        Models models = controller.getModelOffers(213, ru.yandex.common.util.currency.Currency.RUR, modelIds, 10, 1,
                order);

        assertThat(models.getModels()).hasSize(1);

        Model model = models.getModels().get(0);
        assertThat(model).usingRecursiveComparison().isEqualTo(expected);
    }


    @Test
    void checkPageAndCount() {
        ModelIds modelIds = toModelId(624180294);
        ModelControllerV1.PriceSortOrder order = ModelControllerV1.PriceSortOrder.ASC;
        controller.getModelOffers(213, ru.yandex.common.util.currency.Currency.RUR, modelIds, 100, 2, null);
        ArgumentCaptor<MarketSearchRequest> argumentCaptor = ArgumentCaptor.forClass(MarketSearchRequest.class);
        verify(marketReportService, times(3)).async(argumentCaptor.capture(), any());
        List<MarketSearchRequest> capturedArgument = argumentCaptor.getAllValues();
        assertThat(capturedArgument).hasSize(3);
        assertThat(capturedArgument).anySatisfy(marketSearchRequest -> {
            Collection<String> page = marketSearchRequest.getParams().get("page");
            Collection<String> count = marketSearchRequest.getParams().get("numdoc");
            assertThat(page).hasSize(1);
            assertThat(count).hasSize(1);
            assertThat(page).contains("2");
            assertThat(count).contains("100");
        });
    }


    @Test
    @DbUnitDataSet(before = "GetModelOffers.before.csv")
    void checkJson() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpUriRequest request = getModelOffersRequest(MODEL_ID, "json",
                    Map.of("regionId", "157"),
                    UID);
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                String bodyString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                String expected = "" +
                        "{\n" +
                        "    \"regionId\": 157,\n" +
                        "    \"models\": [\n" +
                        "        {\n" +
                        "            \"id\": 624180294,\n" +
                        "            \"name\": \"Телевизор /Asano\\\\ 40LF7030S 40\\\\\\\" (2019)\",\n" +
                        "            \"prices\": {\n" +
                        "                \"min\": 29185,\n" +
                        "                \"max\": 40990,\n" +
                        "                \"avg\": 581.2\n" +
                        "            },\n" +
                        "          \"offers\": [\n" +
                        "            {\n" +
                        "              \"pos\": 1,\n" +
                        "              \"name\": \"Телевизор /Asano\\\\ 40LF7030S 40\\\\\\\" (2019)\",\n" +
                        "              \"price\": 29185,\n" +
                        "              \"regionId\": 157,\n" +
                        "              \"shippingCost\": 590,\n" +
                        "              \"inStock\": 1,\n" +
                        "              \"shopName\": \"Бытовая-Техника.МСК\",\n" +
                        "              \"shopRating\": 5\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"pos\": 2,\n" +
                        "              \"name\": \"Телевизор /Asano\\\\ 40LF7030S 40\\\\\\\" (2019)\",\n" +
                        "              \"price\": 29185,\n" +
                        "              \"regionId\": 157,\n" +
                        "              \"shippingCost\": 590,\n" +
                        "              \"inStock\": 1,\n" +
                        "              \"shopName\": \"Бытовая-Техника.МСК\",\n" +
                        "              \"shopRating\": 5\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"pos\": 3,\n" +
                        "              \"name\": \"Телевизор /Asano\\\\ 40LF7030S 40\\\\\\\" (2019)\",\n" +
                        "              \"price\": 29190,\n" +
                        "              \"preDiscountPrice\": 31840,\n" +
                        "              \"discount\": 8,\n" +
                        "              \"regionId\": 157,\n" +
                        "              \"shippingCost\": 500,\n" +
                        "              \"inStock\": 1,\n" +
                        "              \"shopName\": \"SKIDKI-BT\",\n" +
                        "              \"shopRating\": 5\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"onlineOffers\": 41,\n" +
                        "          \"offlineOffers\": 41\n" +
                        "        }\n" +
                        "  ],\n" +
                        "  \"currency\": \"BYN\"\n" +
                        "}";
                MbiAsserts.assertJsonEquals(
                        expected,
                        bodyString
                );
            }
        }
    }

    @Test
    @DbUnitDataSet(before = "GetModelOffers.before.csv")
    void checkXml() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpUriRequest request = getModelOffersRequest(MODEL_ID, "xml",
                    Map.of("regionId", "157"),
                    UID);
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                String bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);

                MbiAsserts.assertXmlEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<response>\n" +
                                "    <models region-id=\"157\" currency=\"BYN\">\n" +
                                "        <model id=\"624180294\" name=\"Телевизор /Asano\\ 40LF7030S 40&quot; (2019)" +
                                "\">\n" +
                                "            <prices min=\"29185\" max=\"40990\" avg=\"581.2\"/>\n" +
                                "            <offers online=\"41\" offline=\"41\">\n" +
                                "                <offer pos=\"1\" name=\"Телевизор /Asano\\ 40LF7030S 40\\&quot; " +
                                "(2019)\" price=\"29185\" region-id=\"157\"\n" +
                                "                       shipping-cost=\"590\" in-stock=\"1\" " +
                                "shop-name=\"Бытовая-Техника.МСК\" shop-rating=\"5\"/>\n" +
                                "                <offer pos=\"2\" name=\"Телевизор /Asano\\ 40LF7030S 40\\&quot; " +
                                "(2019)\" price=\"29185\" region-id=\"157\"\n" +
                                "                       shipping-cost=\"590\" in-stock=\"1\" " +
                                "shop-name=\"Бытовая-Техника.МСК\" shop-rating=\"5\"/>\n" +
                                "                <offer pos=\"3\" name=\"Телевизор /Asano\\ 40LF7030S 40\\&quot; " +
                                "(2019)\" price=\"29190\"\n" +
                                "                       pre-discount-price=\"31840\" discount=\"8\" region-id=\"157\"" +
                                " shipping-cost=\"500\" in-stock=\"1\"\n" +
                                "                       shop-name=\"SKIDKI-BT\" shop-rating=\"5\"/>\n" +
                                "            </offers>\n" +
                                "        </model>\n" +
                                "    </models>\n" +
                                "</response>"
                        ,
                        bodyString
                );
            }
        }
    }

    HttpGet getModelOffersRequest(
            long modelId,
            String format,
            Map<String, String> queryParameters,
            long uid) {
        HttpGet request = new HttpGet(modelOffersURI(modelId, format, queryParameters));
        request.setHeader("X-AuthorizationService", "Mock");
        request.setHeader("Cookie", String.format("yandexuid = %d;", uid));
        return request;
    }

    protected URI modelOffersURI(
            long modelId,
            String format,
            Map<String, String> queryParams) {
        try {
            URIBuilder uriBuilder =
                    new URIBuilder(
                            String.format(Locale.US, "%s/v2/models/%d/offers.%s",
                                    urlBasePrefix, modelId, format));
            for (Map.Entry<String, String> argument : queryParams.entrySet()) {
                uriBuilder.addParameter(argument.getKey(), argument.getValue());
            }
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Expecting to be valid URI", e);
        }
    }

    private Model getExpectedResponse() {
        Model model = new Model();
        model.setId(624180294);
        model.setName("Телевизор /Asano\\ 40LF7030S 40\" (2019)");
        model.setCategoryId(0);

        Prices prices = new Prices();
        prices.setAvg(BigDecimal.valueOf(581.2));
        prices.setMax(BigDecimal.valueOf(40990));
        prices.setMin(BigDecimal.valueOf(29185));
        model.setPrices(prices);
        Offers offers = new Offers();
        offers.setOnline(41);
        offers.setOffline(41);
        Offer offer3 = new Offer();
        offer3.setPos(3);
        offer3.setPreDiscountPrice(BigDecimal.valueOf(31840));
        offer3.setShippingCost(BigDecimal.valueOf(500));
        offer3.setDiscount(8);
        offer3.setInStock(1);
        offer3.setShopName("SKIDKI-BT");
        offer3.setShopRating(5);
        offer3.setName("Телевизор /Asano\\ 40LF7030S 40\\\" (2019)");
        offer3.setRegionId(213L);
        offer3.setPrice(BigDecimal.valueOf(29190));

        Offer offer2 = new Offer();
        offer2.setPos(2);
        offer2.setShippingCost(BigDecimal.valueOf(590));
        offer2.setInStock(1);
        offer2.setShopName("Бытовая-Техника.МСК");
        offer2.setShopRating(5);
        offer2.setName("Телевизор /Asano\\ 40LF7030S 40\\\" (2019)");
        offer2.setRegionId(213L);
        offer2.setPrice(BigDecimal.valueOf(29185));

        Offer offer1 = new Offer();
        offer1.setPos(1);
        offer1.setShippingCost(BigDecimal.valueOf(590));
        offer1.setInStock(1);
        offer1.setShopName("Бытовая-Техника.МСК");
        offer1.setShopRating(5);
        offer1.setName("Телевизор /Asano\\ 40LF7030S 40\\\" (2019)");
        offer1.setRegionId(213L);
        offer1.setPrice(BigDecimal.valueOf(29185));
        offers.setOffers(Arrays.asList(offer1, offer2, offer3));
        model.setOffers(offers);
        return model;
    }

    protected ModelIds toModelId(long modelId) {
        ModelIds modelIds = new ModelIds();
        modelIds.setModels(Collections.singletonList(modelId));
        return modelIds;
    }
}
