package ru.yandex.market.api.partner.controllers.price;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.SyncAPI.OffersBatch;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriTemplate;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.report.client.model.PriceRecommendationsDTO;
import ru.yandex.market.core.report.client.model.PriceRecommendationsDTO.PriceRecommendationDTO;
import ru.yandex.market.core.report.client.model.ReportRecommendationsResultDTO;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.context.FunctionalTestHelper.makeRequest;

/**
 * Тесты для {@link OfferPriceSuggestController}.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "OfferPriceControllerSuggestTest.before.csv")
class OfferPriceControllerSuggestTest extends FunctionalTest {

    // language=json
    private static final String EMPTY_RESPONSE_JSON = "{\"status\": \"OK\", \"result\":" +
            "  {\"offers\": []}" +
            "}";

    @Qualifier("marketReportService")
    @Autowired
    private AsyncMarketReportService reportService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Test
    void testSuggestCorrectSingleXml() {
        prepareReportMock(100L);

        // language=xml
        String request = "<price-suggestions><offers><offer market-sku='100'/></offers></price-suggestions>";

        ResponseEntity<String> response = makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.XML, request);

        // language=xml
        String expected = "<response><status>OK</status>" +
                "<result><offers><offer market-sku=\"100\">" +
                "  <price-suggestion type=\"MIN_PRICE_MARKET\" price=\"900\"/>" +
                "  <price-suggestion type=\"BUYBOX\" price=\"1000\"/>" +
                "  <price-suggestion type=\"DEFAULT_OFFER\" price=\"1100\"/>" +
                "  <price-suggestion type=\"MAX_DISCOUNT_BASE\" price=\"9000\"/>" +
                "  <price-suggestion type=\"MARKET_OUTLIER_PRICE\" price=\"18000\"/>" +
                "  <price-suggestion type=\"MAX_DISCOUNT_PRICE\" price=\"8500\"/>" +
                "</offer></offers></result>" +
                "</response>";

        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testSuggestCorrectSingleJson() {
        prepareReportMock(100L);
        // language=json
        String request = "{\"offers\": [{\"marketSku\": 100}]}";

        ResponseEntity<String> response = makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request);

        // language=json
        String expected = "{\"status\": \"OK\", \"result\":" +
                "  {\"offers\": [" +
                "    {\"marketSku\": 100, \"priceSuggestion\":[" +
                "      {\"type\": \"MIN_PRICE_MARKET\", \"price\": 900}," +
                "      {\"type\": \"BUYBOX\", \"price\": 1000}," +
                "      {\"type\": \"DEFAULT_OFFER\", \"price\": 1100}," +
                "      {\"type\": \"MAX_DISCOUNT_BASE\", \"price\": 9000}," +
                "      {\"type\": \"MARKET_OUTLIER_PRICE\", \"price\": 18000}," +
                "      {\"type\": \"MAX_DISCOUNT_PRICE\", \"price\": 8500}" +
                "    ]}" +
                "  ]}" +
                "}";

        JsonTestUtil.assertEquals(response.getBody(), expected);
    }

    @Test
    void testSuggestByOfferIdJson() {
        prepareReportMock(100L, 200L, 300L);
        prepareDataCampMock("datacamp/getMskuByOfferId.774.response.json");

        String request = StringTestUtil.getString(getClass(), "json/OfferPriceControllerSuggestTest.byOfferId.request.json");
        ResponseEntity<String> response = makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request);

        String expected = StringTestUtil.getString(getClass(), "json/OfferPriceControllerSuggestTest.byOfferId.response.json");

        JsonTestUtil.assertEquals(expected, response.getBody());
    }

    @Test
    void testSuggestCorrectMultiJson() {
        prepareReportMock(100L, 110L);
        // language=json
        String request = "{\"offers\": [{\"marketSku\": 100}, {\"marketSku\": 110}]}";

        ResponseEntity<String> response = makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request);

        // language=json
        String expected = "{\"status\": \"OK\", \"result\":" +
                "  {\"offers\": [" +
                "    {\"marketSku\": 100, \"priceSuggestion\":[" +
                "      {\"type\": \"MIN_PRICE_MARKET\", \"price\": 900}," +
                "      {\"type\": \"BUYBOX\", \"price\": 1000}," +
                "      {\"type\": \"DEFAULT_OFFER\", \"price\": 1100}," +
                "      {\"type\": \"MAX_DISCOUNT_BASE\", \"price\": 9000}," +
                "      {\"type\": \"MARKET_OUTLIER_PRICE\", \"price\": 18000}," +
                "      {\"type\": \"MAX_DISCOUNT_PRICE\", \"price\": 8500}" +
                "    ]}," +
                "    {\"marketSku\": 110, \"priceSuggestion\":[" +
                "      {\"type\": \"MIN_PRICE_MARKET\", \"price\": 990}," +
                "      {\"type\": \"BUYBOX\", \"price\": 1100}," +
                "      {\"type\": \"DEFAULT_OFFER\", \"price\": 1210}," +
                "      {\"type\": \"MAX_DISCOUNT_BASE\", \"price\": 9000}," +
                "      {\"type\": \"MARKET_OUTLIER_PRICE\", \"price\": 18000}," +
                "      {\"type\": \"MAX_DISCOUNT_PRICE\", \"price\": 8500}" +
                "    ]}" +
                "  ]}" +
                "}";

        JsonTestUtil.assertEquals(response.getBody(), expected);
    }

    @Test
    void testSuggestHasIncorrectJson() {
        prepareReportMock(100L);
        // language=json
        String request = "{\"offers\": [{\"marketSku\": 100}, {\"marketSku\": 110}]}";

        ResponseEntity<String> response = makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request);

        // language=json
        String expected = "{\"status\": \"OK\", \"result\":" +
                "  {\"offers\": [" +
                "      {\"marketSku\": 100, \"priceSuggestion\":[" +
                "        {\"type\": \"MIN_PRICE_MARKET\", \"price\": 900}," +
                "        {\"type\": \"BUYBOX\", \"price\": 1000}," +
                "        {\"type\": \"DEFAULT_OFFER\", \"price\": 1100}," +
                "        {\"type\": \"MAX_DISCOUNT_BASE\", \"price\": 9000}," +
                "        {\"type\": \"MARKET_OUTLIER_PRICE\", \"price\": 18000}," +
                "        {\"type\": \"MAX_DISCOUNT_PRICE\", \"price\": 8500}" +
                "      ]}" +
                "  ]}" +
                "}";

        JsonTestUtil.assertEquals(expected, response.getBody());
    }

    @Test
    void testSuggestNoCorrectJson() {
        prepareReportMock();
        // language=json
        String request = "{\"offers\": [{\"marketSku\": 100}, {\"marketSku\": 110}]}";

        ResponseEntity<String> response = makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request);

        JsonTestUtil.assertEquals(response.getBody(), EMPTY_RESPONSE_JSON);
    }

    @Test
    void testSuggestCorrectEmptyJson() {
        prepareReportMock();
        // language=json
        String request = "{\"offers\": []}";

        ResponseEntity<String> response = makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request);

        JsonTestUtil.assertEquals(response.getBody(), EMPTY_RESPONSE_JSON);
    }

    @Test
    void testSuggestCorrectNullJson() {
        prepareReportMock();
        // language=json
        String request = "{}";

        ResponseEntity<String> response = makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request);

        JsonTestUtil.assertEquals(response.getBody(), EMPTY_RESPONSE_JSON);
    }

    @Test
    void testSuggestDuplicatesMsku() {
        prepareReportMock();
        // language=json
        String request = "{\"offers\": [{\"marketSku\": 1000000}, {\"marketSku\": 1000000}]}";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request));

        // language=json
        String expected = "{\"status\": \"ERROR\"," +
                "  \"errors\": [" +
                "    {" +
                "      \"code\": \"DUPLICATE_MARKET_SKU\"," +
                "      \"message\": \"Duplicate market-sku in request: 1000000\"" +
                "    }" +
                "  ]" +
                "}";


        JsonTestUtil.assertEquals(exception.getResponseBodyAsString(), expected);
    }

    @Test
    void testSuggestDuplicatesOfferId() {
        prepareReportMock();
        String request = StringTestUtil.getString(getClass(), "json/OfferPriceControllerSuggestTest.duplicateOfferId.request.json");

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request));

        String expected = StringTestUtil.getString(getClass(), "json/OfferPriceControllerSuggestTest.duplicateOfferId.response.json");

        JsonTestUtil.assertEquals(exception.getResponseBodyAsString(), expected);
    }

    @Test
    void testSuggestEmptyMSKUAndOfferId() {
        prepareReportMock();
        // language=json
        String request = "{\"offers\": [{}]}";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request));

        String expected = StringTestUtil.getString(getClass(), "json/OfferPriceControllerSuggestTest.emptyOffer.response.json");

        JsonTestUtil.assertEquals(exception.getResponseBodyAsString(), expected);
    }

    @Test
    void testSuggestTooMany() {
        prepareReportMock();
        StringBuilder request = new StringBuilder();
        request.append("{\"offers\": [");
        for (int i = 1000, first = 1; i < 2001; i++, first = 0) {
            if (first == 0) {
                request.append(",");
            }
            request.append("{\"marketSku\":").append(i).append("}");
        }
        request.append("]}");

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request.toString()));

        // language=json
        String expected = "{\"status\": \"ERROR\"," +
                "  \"errors\": [" +
                "    {" +
                "      \"code\": \"REQUEST_LIMIT_EXCEEDED\"," +
                "      \"message\": \"Request too big: maximum of 1000 offers are allowed in single request: got 1001\"" +
                "    }" +
                "  ]" +
                "}";


        JsonTestUtil.assertEquals(exception.getResponseBodyAsString(), expected);
    }


    @Test
    void testSuggestMultiErrors() {
        prepareReportMock();
        StringBuilder request = new StringBuilder();
        request.append("{\"offers\": [");
        for (int i = 1000, first = 1; i < 2001; i++, first = 0) {
            if (first == 0) {
                request.append(",");
            }
            request.append("{\"marketSku\":").append(i / 500).append("}");
        }
        request.append("]}");

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> makeRequest(suggestionsUri(10774L), HttpMethod.POST, Format.JSON, request.toString()));

        // language=json
        String expected = "{\"status\": \"ERROR\"," +
                "  \"errors\": [" +
                "    {" +
                "      \"code\": \"DUPLICATE_MARKET_SKU\"," +
                "      \"message\": \"Duplicate market-sku in request: 2\"" +
                "    }," +
                "    {" +
                "      \"code\": \"DUPLICATE_MARKET_SKU\"," +
                "      \"message\": \"Duplicate market-sku in request: 3\"" +
                "    }," +
                "    {" +
                "      \"code\": \"REQUEST_LIMIT_EXCEEDED\"," +
                "      \"message\": \"Request too big: maximum of 1000 offers are allowed in single request: got 1001\"" +
                "    }" +
                "  ]" +
                "}";


        JsonTestUtil.assertEquals(exception.getResponseBodyAsString(), expected);
    }

    @Test
    void testInvalidCampaignId() {
        prepareReportMock();
        String request = StringTestUtil.getString(getClass(), "json/OfferPriceControllerSuggestTest.byOfferId.request.json");

        URI url = new UriTemplate("{base}/campaigns/123abc/offer-prices/suggestions")
                .expand(Map.of("base", urlBasePrefix));
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> makeRequest(url, HttpMethod.POST, Format.JSON, request));
        Assertions.assertEquals(400, exception.getRawStatusCode());

        String expected = StringTestUtil.getString(getClass(), "json/OfferPriceControllerSuggestTest.invalidCampaignId.response.json");

        JsonTestUtil.assertEquals(exception.getResponseBodyAsString(), expected);
    }

    private void prepareReportMock(Long... marketSkus) {
        Set<Long> skus = Stream.of(marketSkus).collect(Collectors.toSet());
        when(reportService.async(any(), any())).thenAnswer(invocation -> {
                    Long sku = Long.valueOf(((MarketSearchRequest) invocation.getArgument(0)).getMarketSku());
                    ReportRecommendationsResultDTO reportResult = new ReportRecommendationsResultDTO();
                    if (skus.contains(sku)) {
                        reportResult.setRecommendations(Collections.singletonList(
                                new PriceRecommendationsDTO(String.valueOf(sku), Arrays.asList(
                                        new PriceRecommendationDTO(BigDecimal.valueOf(sku * 10), BigDecimal.ZERO, "buybox"),
                                        new PriceRecommendationDTO(BigDecimal.valueOf(sku * 9), BigDecimal.ZERO, "minPriceMarket"),
                                        new PriceRecommendationDTO(BigDecimal.valueOf(sku * 11), BigDecimal.ZERO, "defaultOffer"),
                                        new PriceRecommendationDTO(BigDecimal.valueOf(9000), BigDecimal.ZERO, "maxOldPrice"),
                                        new PriceRecommendationDTO(BigDecimal.valueOf(18000), BigDecimal.ZERO, "priceLimit"),
                                        new PriceRecommendationDTO(BigDecimal.valueOf(8500), BigDecimal.ZERO, "maxDiscountPrice")
                                ))
                        ));
                    } else {
                        reportResult.setRecommendations(Collections.emptyList());
                    }
                    return CompletableFuture.completedFuture(reportResult);
                }
        );
    }

    private void prepareDataCampMock(String responsePath) {
        OffersBatch.UnitedOffersBatchResponse datcampResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                responsePath,
                getClass()
        );
        Mockito.doReturn(datcampResponse)
                .when(dataCampShopClient)
                .getBusinessUnitedOffers(anyLong(), any(), any());
    }

    private URI suggestionsUri(long campaignId) {
        return new UriTemplate("{base}/campaigns/{campaignId}/offer-prices/suggestions")
                .expand(ImmutableMap.of("base", urlBasePrefix, "campaignId", campaignId));
    }

}
