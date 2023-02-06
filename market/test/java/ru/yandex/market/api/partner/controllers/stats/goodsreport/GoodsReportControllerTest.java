package ru.yandex.market.api.partner.controllers.stats.goodsreport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.stats.goodsreport.model.GoodsReportRequest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.fulfillment.tariff.TariffsIterator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsRow;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsRowWarehouse;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtStorage;
import ru.yandex.market.core.replenishment.supplier.PilotSupplierYtDao;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "GoodsReportControllerTest.before.csv")
class GoodsReportControllerTest extends FunctionalTest {

    private static final long CAMPAIGN_ID = 10150150L;
    private static final long BUSINESS_ID = 10150L;
    private static final long SUPPLIER_ID = 150L;
    private static final String CORRECT_SHOP_SKU_1 = "VP59136-3SK";
    private static final String CORRECT_SHOP_SKU_2 = "SPG333-111SK";
    private static final String INCORRECT_SHOP_SKU_1 = "!!!!!!&&&&&????";

    private static final String EXPECTED_ERROR_MESSAGE_EMPTY = "[{\"code\":\"BAD_REQUEST\",\"message\":" +
            "\"ShopSkus shouldn't be empty\"}]";
    private static final String EXPECTED_ERROR_MESSAGE_OVERSIZED = "[{\"code\":\"BAD_REQUEST\",\"message\":" +
            "\"ShopSkus size shouldn't be more than " + GoodsReportController.MAX_ITEMS_REQUEST_SIZE + "\"}]";
    private static final String EXPECTED_ERROR_MESSAGE_INVALID_SKU = "[{\"code\":\"INVALID_SHOP_SKU\",\"message\":" +
            "\"ShopSku at position 2 should contain ASCII graphical symbols no more than 80 length. Current is " +
            INCORRECT_SHOP_SKU_1 + "\"}]";

    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private MboMappingsService mboMappingsService;

    @Autowired
    private DataCampService dataCampService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private SalesDynamicsYtStorage salesDynamicsYtStorage;

    @Autowired
    private PilotSupplierYtDao pilotSupplierYtDao;

    @Autowired
    private TariffsService clientTariffsService;

    private void mock() {
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            return new TariffsIterator((pageNumber, batchSize) -> List.of());
        }).when(clientTariffsService).findTariffs(Mockito.any(TariffFindQuery.class));
        given(dataCampShopClient.searchBusinessOffers(any())).willReturn(
                SearchBusinessOffersResult.builder().build());
    }

    @BeforeEach
    void setUp() {
        when(pilotSupplierYtDao.getPilotSupplierIds()).thenReturn(List.of());
    }

    @Test
    @DisplayName("Проверяем корректные shopSku (одно с ВГХ, второе без)")
    void testCorrectResponse() throws Exception {
        mock();
        mockMbo("proto/testCorrectResponse.datacamp.json", "proto/testCorrectResponse.mbo.json");
        mockSalesDynamics();

        List<String> shopSkus = Arrays.asList(CORRECT_SHOP_SKU_1, CORRECT_SHOP_SKU_2);
        GoodsReportRequest goodsReportRequest = new GoodsReportRequest(shopSkus);
        ResponseEntity<String> response = doRequest(goodsReportRequest);
        checkResponse(response, "GoodsReportControllerTest.mbocCorrectData.json");
    }

    @Test
    @DisplayName("Проверяем корректные shopSku через ЕОХ")
    @DbUnitDataSet(before = "GoodsReportControllerTest.catalog.before.csv")
    void testCorrectDatacampResponse() throws Exception {
        mock();
        mockUnitedDatacamp("proto/testCorrectResponse.united_datacamp.json");
        mockSalesDynamics();

        List<String> shopSkus = Arrays.asList(CORRECT_SHOP_SKU_1, CORRECT_SHOP_SKU_2);
        GoodsReportRequest goodsReportRequest = new GoodsReportRequest(shopSkus);
        ResponseEntity<String> response = doRequest(goodsReportRequest);
        checkResponse(response, "GoodsReportControllerTest.unitedCorrectData.json");
    }

    @Test
    @DisplayName("Запрошенный ssku, которого нет у партнера. Ассортимент в MBO")
    void testNotExistedSskuMbo() throws Exception {
        mock();
        mockMbo("proto/testEmptyResponse.datacamp.json", "proto/testEmptyResponse.mbo.json");

        List<String> shopSkus = Arrays.asList(CORRECT_SHOP_SKU_1, CORRECT_SHOP_SKU_2);
        GoodsReportRequest goodsReportRequest = new GoodsReportRequest(shopSkus);
        ResponseEntity<String> response = doRequest(goodsReportRequest);
        checkResponse(response, "GoodsReportControllerTest.emptyData.json");
    }

    @Test
    @DisplayName("Запрошенный ssku, которого нет у партнера. Ассортимент в ЕОХ. ЕКат")
    @DbUnitDataSet(before = "GoodsReportControllerTest.catalog.before.csv")
    void testNotExistedSskuUnitedDatacamp() throws Exception {
        mock();
        mockUnitedDatacamp("proto/testEmptyResponse.united_datacamp.json");

        List<String> shopSkus = Arrays.asList(CORRECT_SHOP_SKU_1, CORRECT_SHOP_SKU_2);
        GoodsReportRequest goodsReportRequest = new GoodsReportRequest(shopSkus);
        ResponseEntity<String> response = doRequest(goodsReportRequest);
        checkResponse(response, "GoodsReportControllerTest.emptyData.json");
    }

    @Test
    void testEmptySkuListPassed() {
        GoodsReportRequest emptyRequest = new GoodsReportRequest(Collections.emptyList());
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> doRequest(emptyRequest)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage(EXPECTED_ERROR_MESSAGE_EMPTY, exception.getResponseBodyAsString());
    }

    @Test
    void testOversizedSkuListPassed() {
        List<String> overSizedList = new ArrayList<>();
        for (int i = 0; i < 510; i++) {
            overSizedList.add(String.valueOf(i));
        }
        GoodsReportRequest overSizedRequest = new GoodsReportRequest(overSizedList);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> doRequest(overSizedRequest)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage(EXPECTED_ERROR_MESSAGE_OVERSIZED, exception.getResponseBodyAsString());
    }

    @Test
    void testInvalidSkuPassed() {
        List<String> skuList = Arrays.asList(CORRECT_SHOP_SKU_1, INCORRECT_SHOP_SKU_1);
        GoodsReportRequest invalidRequest = new GoodsReportRequest(skuList);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> doRequest(invalidRequest)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage(EXPECTED_ERROR_MESSAGE_INVALID_SKU,
                exception.getResponseBodyAsString());
    }

    private void mockMbo(String datacampData, String mboData) {
        SyncChangeOffer.FullOfferResponse datacampResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.FullOfferResponse.class,
                datacampData,
                getClass()
        );
        doReturn(datacampResponse)
                .when(dataCampService).getOffers(eq(SUPPLIER_ID), anyCollection());

        MboMappings.SearchMappingsResponse mboResponse = ProtoTestUtil.getProtoMessageByJson(
                MboMappings.SearchMappingsResponse.class,
                mboData,
                getClass()
        );
        doReturn(mboResponse)
                .when(mboMappingsService).searchMappingsByKeys(any());
    }

    private void mockUnitedDatacamp(String data) {
        OffersBatch.UnitedOffersBatchResponse datacampResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                data,
                getClass()
        );
        doReturn(datacampResponse)
                .when(dataCampShopClient).getBusinessUnitedOffers(eq(BUSINESS_ID), anyCollection(), any());
    }

    private void mockSalesDynamics() {
        List<SalesDynamicsRow> rows = List.of(
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku(CORRECT_SHOP_SKU_1)
                        .addWarehouse(147, SalesDynamicsRowWarehouse.builder()
                                .withDemandDynamicRecWeek2(10.0)
                                .build())
                        .withPrice(50.0)
                        .build()
        );
        doAnswer(invocation -> {
            Consumer<Iterator<SalesDynamicsRow>> consumer = invocation.getArgument(2);
            consumer.accept(rows.iterator());
            return null;
        }).when(salesDynamicsYtStorage).getSalesDynamicsReport(any(), any(), any());
    }

    private ResponseEntity<String> doRequest(
            GoodsReportRequest request
    ) throws Exception {
        URIBuilder uriBuilder =
                new URIBuilder(
                        String.format(Locale.US, "%s/campaigns/%d/stats/skus.%s",
                                urlBasePrefix, CAMPAIGN_ID, Format.JSON));

        return FunctionalTestHelper.makeRequest(uriBuilder.build(), HttpMethod.POST, Format.JSON, toJson(request));
    }

    protected String toJson(GoodsReportRequest request) throws JsonProcessingException {
        return JSON_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(request);
    }

    private void checkResponse(final ResponseEntity<String> response, final String fileName) throws IOException {
        //noinspection ConstantConditions
        String expected = IOUtils.toString(this.getClass().getResourceAsStream(fileName), UTF_8);
        JSONObject actual = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
    }
}
