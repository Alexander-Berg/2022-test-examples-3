package ru.yandex.market.partner.mvc.controller.price;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import com.amazonaws.util.StringInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.mvc.controller.util.ResponseJsonUtil.getResult;

@DbUnitDataSet(before = "csv/OfferPriceInfoControllerFunctionalTest.before.csv")
class OfferPriceInfoControllerFunctionalTest extends FunctionalTest {
    private static final Map<String, String> MSKU_RECOMMENDATIONS_RESPONSES = new HashMap<>() {{
        put("1", "json/getRecommendedPrice_1.reportResponse.json");
        put("23", "json/getRecommendedPrice_23.reportResponse.json");
    }};
    private static final Map<Long, String> GET_OFFERS_RESPONSES = new HashMap<>() {{
        put(1101L, "json/getOffersPrices_1101.reportResponse.json");
        put(1102L, "json/getOffersPrices_1102.reportResponse.json");
        put(10671634L, "json/getOffersPrices_10671634.reportResponse.json");
        put(1104L, "json/getOffersPrices_1104.reportResponse.json");
    }};

    @Autowired
    private DataCampClient dataCampShopClient;

    @Autowired
    @Qualifier("marketReportService")
    private AsyncMarketReportService marketReportService;

    @BeforeEach
    void init() {
        mockDataCampOffers();
        mockEmptyRecommendationsReportResponse(Set.of("1", "23"));
        mockEmptyOffersPricesReportResponse();
    }

    /**
     * Для ssku_1 есть только рекомендации по байбоксу и минрефу.
     * Для ssku_2 и ssku_3 нет совсем никакой информации.
     */
    @Test
    void recommendationsWithoutReportPriceAndStrategyAndPromosTest() {
        mockNotEmptyRecommendationsReportResponse(Set.of("1"));
        String requestBody = StringTestUtil.getString(this.getClass(), "json/ssku-set-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(1103, requestBody);

        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-all-with-disabled-strategy.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Аналогичный предыдущему тест, только при передаче msku с фронта.
     */
    @Test
    void recommendationsWithoutReportPriceAndStrategyAndPromos_withMskuTest() {
        mockNotEmptyRecommendationsReportResponse(Set.of("1"));
        String requestBody = StringTestUtil.getString(this.getClass(), "json/sku-full-info-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(1103, requestBody);

        verify(dataCampShopClient, never())
                .getOffers(anyLong(), any(), any(SyncChangeOffer.ChangeOfferRequest.class));
        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-all-with-disabled-strategy.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Для ssku_1 есть только стратегия по байбоксу.
     * Для ssku_2 и ssku_3 есть стратегия по байбоксу, цена из репорта и байбокс.
     * Промок ни у кого нет.
     */
    @Test
    void reportPriceAndRecommendationsAndBuyboxStrategyWithoutPromosTest() {
        long partnerId = 1101;
        mockNotEmptyRecommendationsReportResponse(Set.of("23"));
        mockNotEmptyOffersPricesReportResponse(partnerId);
        String requestBody = StringTestUtil.getString(this.getClass(), "json/ssku-set-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(partnerId, requestBody);

        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-all-with-enabled-buybox-strategy.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Аналогичный предыдущему тест, только при передаче msku с фронта.
     */
    @Test
    void reportPriceAndRecommendationsAndBuyboxStrategyWithoutPromos_withMskuTest() {
        long partnerId = 1101;
        mockNotEmptyRecommendationsReportResponse(Set.of("23"));
        mockNotEmptyOffersPricesReportResponse(partnerId);
        String requestBody = StringTestUtil.getString(this.getClass(), "json/sku-full-info-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(partnerId, requestBody);

        verify(dataCampShopClient, never())
                .getOffers(anyLong(), any(), any(SyncChangeOffer.ChangeOfferRequest.class));
        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-all-with-enabled-buybox-strategy.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Для ssku_1 есть стратегия по минрефу, цена из репорта, обе рекомендации.
     * Для ssku_2 и ssku_3 есть стратегия по минрефу, цена из репорта и байбокс.
     * Промок ни у кого нет.
     */
    @Test
    void reportPriceAndRecommendationsAndReferenceStrategyWithoutPromosTest() {
        long partnerId = 1102;
        mockNotEmptyRecommendationsReportResponse(Set.of("1", "23"));
        mockNotEmptyOffersPricesReportResponse(partnerId);
        String requestBody = StringTestUtil.getString(this.getClass(), "json/ssku-set-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(partnerId, requestBody);

        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-all-with-enabled-reference-strategy.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Аналогичный предыдущему тест, только при передаче msku с фронта.
     */
    @Test
    void reportPriceAndRecommendationsAndReferenceStrategyWithoutPromos_withMskuTest() {
        long partnerId = 1102;
        mockNotEmptyRecommendationsReportResponse(Set.of("1", "23"));
        mockNotEmptyOffersPricesReportResponse(partnerId);
        String requestBody = StringTestUtil.getString(this.getClass(), "json/sku-full-info-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(partnerId, requestBody);

        verify(dataCampShopClient, never())
                .getOffers(anyLong(), any(), any(SyncChangeOffer.ChangeOfferRequest.class));
        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-all-with-enabled-reference-strategy.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Проверяет участие промо в субсидируемой прямой скидке, обычной ПС и акции с неизвестной механикой.
     */
    @Test
    void promosInCheckPricesResponseTest() {
        long partnerId = 10671634;
        mockDataCampOffersForPromoRequests();
        mockNotEmptyRecommendationsReportResponse(Set.of("23"));
        mockNotEmptyOffersPricesReportResponse(partnerId);
        String requestBody = StringTestUtil.getString(this.getClass(), "json/ssku-promo-set-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(partnerId, requestBody);

        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-with-direct-discount-promo.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Аналогичный предыдущему тест, только при передаче msku с фронта.
     */
    @Test
    void promosInCheckPricesResponse_withMskuTest() {
        long partnerId = 10671634;
        mockDataCampOffersForPromoRequests();
        mockNotEmptyRecommendationsReportResponse(Set.of("23"));
        mockNotEmptyOffersPricesReportResponse(partnerId);
        String requestBody = StringTestUtil.getString(this.getClass(), "json/sku-promo-full-info-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(partnerId, requestBody);

        verify(dataCampShopClient, never())
                .getOffers(anyLong(), any(), any(SyncChangeOffer.ChangeOfferRequest.class));
        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-with-direct-discount-promo.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Проверяет участие промо в акциях, которые напрямую не влияют на снижение цены оффера на выдаче.
     */
    @Test
    void promosWithoutDiscountInCheckPricesResponseTest() {
        long partnerId = 1104;
        mockDataCampOffersForPromoRequests();
        mockNotEmptyRecommendationsReportResponse(Set.of("23"));
        mockNotEmptyOffersPricesReportResponse(partnerId);
        String requestBody = StringTestUtil.getString(this.getClass(), "json/ssku-promo-set-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(partnerId, requestBody);

        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-with-other-promos-promo.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    @Test
    void dynamicPricingStrategyTest() {
        long partnerId = 1101;
        mockNotEmptyRecommendationsReportResponse(Set.of("23"));
        mockNotEmptyOffersPricesReportResponse(partnerId);
        String requestBody = StringTestUtil.getString(this.getClass(), "json/ssku-set-request.json");
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(partnerId, requestBody);

        String expected = StringTestUtil.getString(this.getClass(), "json/ssku-with-dynamic-pricing-strategy.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    private ResponseEntity<String> sendGetFullPriceInfoForSskuListRequest(
            long campaignId,
            Object body
    ) {
        return FunctionalTestHelper.post(baseUrl + "/partner/price-info/by-ssku?campaign_id=" + campaignId, body);
    }

    private void mockDataCampOffers() {
        DataCampOffer.Offer dataCampOffer_1 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("ssku_1")
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(1L)
                                )
                        )
                )
                .build();
        DataCampOffer.Offer dataCampOffer_2 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("ssku_2")
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(23L)
                                )
                        )
                )
                .build();
        DataCampOffer.Offer dataCampOffer_3 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("ssku_3")
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(23L)
                                )
                        )
                )
                .build();
        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(
                                Arrays.asList(
                                        dataCampOffer_1,
                                        dataCampOffer_2,
                                        dataCampOffer_3
                                )
                        )
                        .build()
        ).when(dataCampShopClient).getOffers(anyLong(), any(), any(SyncChangeOffer.ChangeOfferRequest.class));
    }

    private void mockDataCampOffersForPromoRequests() {
        DataCampOffer.Offer dataCampOffer_10967179 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("hid.10967179")
                                .build()
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(23L)
                                )
                        )
                )
                .build();
        DataCampOffer.Offer dataCampOffer_8344905 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("hid.8344905")
                                .build()
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(1L)
                                )
                        )
                )
                .build();
        DataCampOffer.Offer dataCampOffer_13451747 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("hid.13451747")
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(23L)
                                )
                        )
                )
                .build();
        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(
                                Arrays.asList(
                                        dataCampOffer_10967179,
                                        dataCampOffer_8344905,
                                        dataCampOffer_13451747
                                )
                        )
                        .build()
        ).when(dataCampShopClient).getOffers(anyLong(), any(), any(SyncChangeOffer.ChangeOfferRequest.class));
    }

    private void mockEmptyRecommendationsReportResponse(Set<String> mskus) {
        mskus.forEach(msku ->
                mockRecommendationsReportResponse(
                        msku,
                        String.format("{\"results\": [{\"priceRecommendations\": [], \"marketSKU\": \"%s\"}]}", msku)
                )
        );
    }

    private void mockNotEmptyRecommendationsReportResponse(Set<String> mskus) {
        mskus.forEach(msku -> {
            String reportResponse = StringTestUtil.getString(this.getClass(), MSKU_RECOMMENDATIONS_RESPONSES.get(msku));
            mockRecommendationsReportResponse(msku, reportResponse);
        });
    }

    private void mockRecommendationsReportResponse(String msku, String reportResponse) {
        when(marketReportService.async(
                ArgumentMatchers.argThat(request ->
                        request != null && request.getPlace() == MarketReportPlace.PRICE_RECOMMENDER &&
                                msku.equals(request.getMarketSku())
                ),
                Mockito.any()
        )).then(invocation -> {
            LiteInputStreamParser<?> parser = invocation.getArgument(1);
            CompletableFuture<Object> future = new CompletableFuture<>();
            Object result = parser == null ? null : parser.parse(new StringInputStream(reportResponse));
            future.complete(result);
            return future;
        });
    }

    private void mockEmptyOffersPricesReportResponse() {
        String reportResponse = StringTestUtil.getString(this.getClass(), "json/emptyOffersPrices.reportResponse.json");
        mockOffersPricesReportResponse(reportResponse);
    }

    private void mockNotEmptyOffersPricesReportResponse(long partnerId) {
        String reportResponse = StringTestUtil.getString(this.getClass(), GET_OFFERS_RESPONSES.get(partnerId));
        mockOffersPricesReportResponse(reportResponse);
    }

    private void mockOffersPricesReportResponse(String reportResponse) {
        when(marketReportService.async(
                ArgumentMatchers.argThat(request ->
                        request != null && request.getPlace() == MarketReportPlace.CHECK_PRICES_LOW_LATENCY
                ),
                Mockito.any()
        )).then(invocation -> {
            LiteInputStreamParser<?> parser = invocation.getArgument(1);
            CompletableFuture<Object> future = new CompletableFuture<>();
            Object result = parser == null ? null : parser.parse(new StringInputStream(reportResponse));
            future.complete(result);
            return future;
        });
    }
}
