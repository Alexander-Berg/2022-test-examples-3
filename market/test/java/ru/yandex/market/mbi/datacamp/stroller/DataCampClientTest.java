package ru.yandex.market.mbi.datacamp.stroller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import Market.DataCamp.API.CopyOffers;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferStockInfo;
import Market.DataCamp.SyncAPI.GetVerdicts;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncSelect;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.util.retry.DataCampServiceUnavailableRetryStrategy;
import ru.yandex.market.mbi.datacamp.model.category.CategoryInfo;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOfferStocksRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOfferStocksResult;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.stroller.model.DataCampException;
import ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferQuery;
import ru.yandex.market.mbi.datacamp.stroller.model.graphql.SearchQuery;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataCampClientTest {
    private static final String BASE_URL = "http://localhost";
    private static final int MAX_OFFERS_COUNT_TO_GET = 500;
    private static final int SERVICE_UNAVAILABLE_RETRY_COUNT = 5;
    private static final long SERVICE_UNAVAILABLE_RETRY_TIMEOUT = 50;

    private static WireMockServer wireMockServer;

    private static DataCampClient dataCampClient;

    @BeforeAll
    public static void setup() {
        wireMockServer = new WireMockServer(new WireMockConfiguration()
                .dynamicPort()
                .notifier(new ConsoleNotifier(false))
        );

        wireMockServer.start();

        dataCampClient = new DataCampClient(
                String.format("%s:%s", BASE_URL, wireMockServer.port()),
                HttpClientBuilder.create()
                        .setServiceUnavailableRetryStrategy(
                                new DataCampServiceUnavailableRetryStrategy(
                                        SERVICE_UNAVAILABLE_RETRY_COUNT,
                                        SERVICE_UNAVAILABLE_RETRY_TIMEOUT
                                )
                        )
                        .build());
    }

    /**
     * Перед каждым тестом приводит wiremock server в исходное состояние
     */
    @BeforeEach
    public void initTest() {
        wireMockServer.resetAll();
    }

    @AfterAll
    public static void teardown() {
        wireMockServer.stop();
    }

    @Test
    void testGraphqlQuerySearch_PricingAndCategories() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withPrice(true)
                .withDynamicPricing(true)
                .withCategoryId(true)
                .build();
        SearchQuery searchQuery = new SearchQuery.Builder()
                .withLimit(true)
                .withNextPagePosition(true)
                .build();

        String expected = "{offers{service{price{dynamic_pricing,basic{binary_price{price}}}}," +
                "basic{content{market{category_id},binding{approved{market_category_id}}}}},limit,next_page_position}";
        String query = dataCampClient.buildSearchGraphqlQuery(offerQuery, searchQuery);
        assertEquals(expected, query);
    }

    @Test
    void testGraphqlQuerySearch_Empty() {
        OfferQuery offerQuery = new OfferQuery.Builder().build();
        SearchQuery searchQuery = new SearchQuery.Builder().build();
        String expected = "";

        String query = dataCampClient.buildSearchGraphqlQuery(offerQuery, searchQuery);
        assertEquals(expected, query);

        offerQuery = new OfferQuery.Builder()
                .withAllFields(false)
                .build();
        query = dataCampClient.buildSearchGraphqlQuery(offerQuery, searchQuery);
        assertEquals(expected, query);
    }

    @Test
    void testGraphqlQuerySearch_AllFlags() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withAllFields(true)
                .build();
        SearchQuery searchQuery = new SearchQuery.Builder()
                .withAllFields(true)
                .build();

        String expected = "{offers{actual{warehouse{partner_info{is_dsbs},stock_info{market_stocks{count}}}}," +
                "service{identifiers,price{dynamic_pricing,basic{binary_price{price}}}," +
                "promos{partner_promos{meta,promos},partner_cashback_promos{meta,promos}," +
                "anaplan_promos{all_promos{promos},active_promos{promos}}}," +
                "content{partner{original_terms{supply_plan{value}}}}}," +
                "basic{identifiers,content{market{category_id,vendor_id,market_category,vendor_name}," +
                "partner{original{vendor{value},name{value}}}," +
                "binding{approved{market_category_id,market_sku_id,market_category_name}," +
                "uc_mapping{market_sku_id}}}}}," +
                "total,offset,current_page_position,limit,next_page_position,previous_page_position}";
        String query = dataCampClient.buildSearchGraphqlQuery(offerQuery, searchQuery);
        assertEquals(expected, query);
    }

    @Test
    void testGraphqlQuerySearch_ActivePromosAndStockCount() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withActiveAnaplanPromos(true)
                .withPartnerPromos(true)
                .withPartnerCashbackPromos(true)
                .withStockCount(true)
                .build();
        SearchQuery searchQuery = new SearchQuery.Builder()
                .withTotal(true)
                .withCurrentPagePosition(true)
                .build();

        String expected = "{offers{actual{warehouse{stock_info{market_stocks{count}}}}," +
                "service{promos{partner_promos{promos},partner_cashback_promos{promos}," +
                "anaplan_promos{active_promos{promos}}}}},total,current_page_position}";
        String query = dataCampClient.buildSearchGraphqlQuery(offerQuery, searchQuery);
        assertEquals(expected, query);
    }

    @Test
    void testGraphqlQuerySearch_AllPromosAndOfferName() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withActiveAnaplanPromos(true)
                .withPartnerPromos(true)
                .withPartnerCashbackPromos(true)
                .withAllAnaplanPromos(true)
                .withName(true)
                .build();
        SearchQuery searchQuery = new SearchQuery.Builder()
                .withOffset(true)
                .withPreviousPagePosition(true)
                .build();

        String expected = "{offers{service{promos{partner_promos{promos},partner_cashback_promos{promos}," +
                "anaplan_promos{all_promos{promos},active_promos{promos}}}}," +
                "basic{content{partner{original{name{value}}}}}},offset,previous_page_position}";
        String query = dataCampClient.buildSearchGraphqlQuery(offerQuery, searchQuery);
        assertEquals(expected, query);
    }

    @Test
    void testGraphqlQueryBatch_PricingAndCategories() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withPrice(true)
                .withDynamicPricing(true)
                .withCategoryId(true)
                .build();

        String expected = "{entries{united_offer{service{price{dynamic_pricing,basic{binary_price{price}}}}," +
                "basic{content{market{category_id},binding{approved{market_category_id}}}}}}}";
        String query = dataCampClient.buildBatchGraphqlQuery(offerQuery);
        assertEquals(expected, query);
    }

    @Test
    void testGraphqlQueryBatch_Empty() {
        OfferQuery offerQuery = new OfferQuery.Builder().build();
        String expected = "";

        String query = dataCampClient.buildBatchGraphqlQuery(offerQuery);
        assertEquals(expected, query);

        offerQuery = new OfferQuery.Builder()
                .withAllFields(false)
                .build();
        query = dataCampClient.buildBatchGraphqlQuery(offerQuery);
        assertEquals(expected, query);
    }

    @Test
    void testGraphqlQueryBatch_AllFlags() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withAllFields(true)
                .build();

        String expected = "{entries{united_offer{actual{warehouse{partner_info{is_dsbs}," +
                "stock_info{market_stocks{count}}}}," +
                "service{identifiers,price{dynamic_pricing,basic{binary_price{price}}}," +
                "promos{partner_promos{meta,promos},partner_cashback_promos{meta,promos}," +
                "anaplan_promos{all_promos{promos},active_promos{promos}}}," +
                "content{partner{original_terms{supply_plan{value}}}}}," +
                "basic{identifiers,content{market{category_id,vendor_id,market_category,vendor_name}," +
                "partner{original{vendor{value},name{value}}}," +
                "binding{approved{market_category_id,market_sku_id,market_category_name}," +
                "uc_mapping{market_sku_id}}}}}}}";
        String query = dataCampClient.buildBatchGraphqlQuery(offerQuery);
        assertEquals(expected, query);
    }

    @Test
    void testGraphqlQueryBatch_ActivePromosAndStockCount() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withActiveAnaplanPromos(true)
                .withPartnerPromos(true)
                .withPartnerCashbackPromos(true)
                .withStockCount(true)
                .build();

        String expected = "{entries{united_offer{actual{warehouse{stock_info{market_stocks{count}}}}," +
                "service{promos{partner_promos{promos},partner_cashback_promos{promos}," +
                "anaplan_promos{active_promos{promos}}}}}}}";
        String query = dataCampClient.buildBatchGraphqlQuery(offerQuery);
        assertEquals(expected, query);
    }

    @Test
    void testGraphqlQueryBatch_AllPromosAndOfferName() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withActiveAnaplanPromos(true)
                .withPartnerPromos(true)
                .withPartnerCashbackPromos(true)
                .withAllAnaplanPromos(true)
                .withName(true)
                .build();

        String expected = "{entries{united_offer{service{promos{partner_promos{promos}," +
                "partner_cashback_promos{promos},anaplan_promos{all_promos{promos},active_promos{promos}}}}," +
                "basic{content{partner{original{name{value}}}}}}}}";
        String query = dataCampClient.buildBatchGraphqlQuery(offerQuery);
        assertEquals(expected, query);
    }

    /**
     * Проверка генерации запроса на получение стоков из stroller'а и
     * парсинга ответа
     */
    @DisplayName("Проверка генерации запроса на получение стоков из stroller'а")
    @Test
    void testSearchBusinessOfferStocks() {
        long businessId = 123L;
        long partnerId = 456L;
        long warehouseId = 9000L;
        int limit = 50;
        String pageToken = "pageToken";
        String nextPageToken = "nextPageToken";

        int firstShopId = 1;
        String firstOfferId = "123";
        int firstOfferStocksCount = 100;
        int secondShopId = 2;
        String secondOfferId = "456";
        int secondOfferStocksCount = 700;

        SearchBusinessOfferStocksRequest searchRequest = SearchBusinessOfferStocksRequest.builder()
                .setBusinessId(businessId)
                .setPartnerId(partnerId)
                .setWarehouseId(warehouseId)
                .setPageRequest(SeekSliceRequest.firstNAfter(limit, pageToken))
                .build();

        SyncSelect.SelectRequest selectRequest = SyncSelect.SelectRequest.newBuilder()
                .setPageToken(pageToken)
                .setLimit(limit)
                .setFilters(SyncSelect.SelectFilters.newBuilder()
                        .setWarehouseId(warehouseId).build())
                .build();

        SyncSelect.SelectResponse selectResponse = createSelectResponse(nextPageToken,
                createTestOffer(
                        createTestIdentifier(firstShopId, firstOfferId),
                        createTestStockInfo(firstOfferStocksCount)),
                createTestOffer(
                        createTestIdentifier(secondShopId, secondOfferId),
                        createTestStockInfo(secondOfferStocksCount))
        );

        wireMockServer.stubFor(post(
                urlEqualTo(String.format(
                                "/v1/partners/%s/offers/services/%s/stocks",
                                businessId,
                                partnerId
                        )
                ))
                .withRequestBody(containing(new String(selectRequest.toByteArray())))
                .willReturn(aResponse().withBody(selectResponse.toByteArray()))
        );

        SearchBusinessOfferStocksResult actualResult = dataCampClient.searchBusinessOfferStocks(searchRequest);

        List<DataCampOffer.Offer> expectedOffers = Arrays.asList(
                createTestOffer(
                        createTestIdentifier(firstShopId, firstOfferId),
                        createTestStockInfo(firstOfferStocksCount)),
                createTestOffer(
                        createTestIdentifier(secondShopId, secondOfferId),
                        createTestStockInfo(secondOfferStocksCount))
        );

        assertEquals(2, actualResult.getOffersCount());
        assertEquals(Optional.of(nextPageToken), actualResult.getResult().nextSliceKey());
        assertEquals(expectedOffers, actualResult.getResult().entries());
    }

    /**
     * Проверка генерации запроса на получение идентификаторов офферов из stroller'а и
     * парсинга ответа
     */
    @DisplayName("Проверка генерации запроса на получение идентификаторов офферов из stroller'а")
    @Test
    void testSearchBusinessOfferIdentifiers() {
        long businessId = 123L;
        long partnerId = 456L;
        long warehouseId = 9000L;
        int limit = 50;
        String pageToken = "offer1";
        String nextPageToken = "offer2";

        int shopId = 1;
        String offerId = "123";

        SearchBusinessOfferStocksRequest searchRequest = SearchBusinessOfferStocksRequest.builder()
                .setBusinessId(businessId)
                .setPartnerId(partnerId)
                .setWarehouseId(warehouseId)
                .setPageRequest(SeekSliceRequest.firstNAfter(limit, pageToken))
                .build();

        SyncSelect.SelectRequest selectRequest = SyncSelect.SelectRequest.newBuilder()
                .setPageToken(pageToken)
                .setLimit(limit)
                .setFilters(SyncSelect.SelectFilters.newBuilder().setHasMapping(true).build())
                .build();

        SyncSelect.SelectResponse selectResponse = createSelectResponse(
                nextPageToken,
                DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(createTestIdentifier(shopId, offerId))
                        .build()
        );

        wireMockServer.stubFor(post(
                urlEqualTo(String.format(
                                "/v1/partners/%s/offers/services/%s/identifiers",
                                businessId,
                                partnerId
                        )
                ))
                .withRequestBody(containing(new String(selectRequest.toByteArray()).trim()))
                .willReturn(aResponse().withBody(selectResponse.toByteArray()))
        );

        SearchBusinessOfferStocksResult actualResult = dataCampClient.searchBusinessOfferIdentifiers(searchRequest);

        List<DataCampOffer.Offer> expectedOffers = Collections.singletonList(
                DataCampOffer.Offer.newBuilder().setIdentifiers(createTestIdentifier(shopId, offerId)).build());

        assertEquals(1, actualResult.getOffersCount());
        assertEquals(Optional.of(nextPageToken), actualResult.getResult().nextSliceKey());
        assertEquals(expectedOffers, actualResult.getResult().entries());
    }

    /**
     * Проверка генерации запроса на получение идентификаторов офферов из stroller'а и
     * валидной обработки ответа с пустым списком оферов
     */
    @DisplayName("Проверка генерации запроса на получение идентификаторов офферов из stroller'а")
    @Test
    void testSearchEmptyResponseOfferIdentifiers() {
        long businessId = 123L;
        long partnerId = 456L;
        long warehouseId = 9000L;
        int limit = 50;
        String pageToken = "pageToken";

        var searchRequest = SearchBusinessOfferStocksRequest.builder()
                .setBusinessId(businessId)
                .setPartnerId(partnerId)
                .setWarehouseId(warehouseId)
                .setPageRequest(SeekSliceRequest.firstNAfter(limit, pageToken))
                .build();

        SyncSelect.SelectRequest selectRequest = SyncSelect.SelectRequest.newBuilder()
                .setPageToken(pageToken)
                .setLimit(limit)
                .setFilters(SyncSelect.SelectFilters.newBuilder().build())
                .build();

        SyncSelect.SelectResponse selectResponse = SyncSelect.SelectResponse.newBuilder().build();

        wireMockServer.stubFor(post(
                urlEqualTo(String.format(
                        "/v1/partners/%s/offers/services/%s/identifiers",
                        businessId,
                        partnerId
                )))
                .withRequestBody(containing(new String(selectRequest.toByteArray())))
                .willReturn(aResponse().withBody(selectResponse.toByteArray()))
        );

        assertThrows(DataCampException.class, () -> dataCampClient.searchBusinessOfferIdentifiers(searchRequest));
    }

    /**
     * Проверка генерации запроса на получение стоков по индексам офферов
     */
    @DisplayName("Проверка генерации запроса на получение стоков по индексам офферов")
    @Test
    void testGetBusinessOfferStocks() {
        long businessId = 123L;
        long partnerId = 456L;
        long warehouseId = 9000L;
        List<String> offerIds = List.of("100", "200");

        int firstShopId = 100;
        String firstOfferId = "123";
        int firstOfferStocksCount = 100;
        int secondShopId = 200;
        String secondOfferId = "456";
        int secondOfferStocksCount = 700;

        List<DataCampOfferIdentifiers.OfferIdentifiers> expectedRequestIdentifiers = Arrays.asList(
                createRequestIdentifier(businessId, partnerId, warehouseId, offerIds.get(0)),
                createRequestIdentifier(businessId, partnerId, warehouseId, offerIds.get(1))
        );

        SyncGetOffer.GetOffersRequest request = SyncGetOffer.GetOffersRequest.newBuilder()
                .addAllIdentifiers(expectedRequestIdentifiers)
                .build();

        SyncGetOffer.GetOffersResponse offersResponse = SyncGetOffer.GetOffersResponse.newBuilder()
                .addAllOffers(Arrays.asList(
                        createTestOffer(
                                createTestIdentifier(firstShopId, firstOfferId),
                                createTestStockInfo(firstOfferStocksCount)),
                        createTestOffer(
                                createTestIdentifier(secondShopId, secondOfferId),
                                createTestStockInfo(secondOfferStocksCount))
                ))
                .build();

        wireMockServer.stubFor(post(
                urlEqualTo(String.format("/v1/partners/%s/offers/services/%s/stocks/get", businessId, partnerId)))
                .withRequestBody(containing(new String(request.toByteArray())))
                .willReturn(aResponse().withBody(offersResponse.toByteArray()))
        );

        List<DataCampOffer.Offer> actualOffers = dataCampClient
                .getBusinessOfferStocks(businessId, partnerId, warehouseId, offerIds);

        List<DataCampOffer.Offer> expectedOffers = Arrays.asList(
                createTestOffer(
                        createTestIdentifier(firstShopId, firstOfferId),
                        createTestStockInfo(firstOfferStocksCount)),
                createTestOffer(
                        createTestIdentifier(secondShopId, secondOfferId),
                        createTestStockInfo(secondOfferStocksCount))
        );

        assertEquals(2, actualOffers.size());
        assertEquals(expectedOffers, actualOffers);
    }

    /**
     * Проверка генерации нескольких запросов на получение стоков по индексам офферов,
     * если офферов большое количество
     */
    @DisplayName("Проверка генерации нескольких запросов на получение стоков по индексам офферов")
    @Test
    void testGetBusinessOfferStocksOffersCountPartition() {
        int offersCount = 800;
        long businessId = 123L;
        long partnerId = 456L;
        long warehouseId = 9000L;
        int shopId = 1000;

        List<String> offerIds = IntStream.rangeClosed(1, offersCount)
                .boxed().map(Object::toString)
                .collect(Collectors.toList());

        List<DataCampOfferIdentifiers.OfferIdentifiers> expectedRequestIdentifiers = offerIds.stream()
                .map(offerId -> createRequestIdentifier(businessId, partnerId, warehouseId, offerId))
                .collect(Collectors.toList());

        SyncGetOffer.GetOffersRequest firstRequest = SyncGetOffer.GetOffersRequest.newBuilder()
                .addAllIdentifiers(expectedRequestIdentifiers.subList(0, MAX_OFFERS_COUNT_TO_GET))
                .build();

        SyncGetOffer.GetOffersResponse firstOfferResponse = SyncGetOffer.GetOffersResponse.newBuilder()
                .addAllOffers(mapIdsToOffers(offerIds, 0, MAX_OFFERS_COUNT_TO_GET, shopId))
                .build();

        SyncGetOffer.GetOffersRequest secondRequest = SyncGetOffer.GetOffersRequest.newBuilder()
                .addAllIdentifiers(expectedRequestIdentifiers.subList(MAX_OFFERS_COUNT_TO_GET, offersCount))
                .build();

        SyncGetOffer.GetOffersResponse secondOfferResponse = SyncGetOffer.GetOffersResponse.newBuilder()
                .addAllOffers(mapIdsToOffers(offerIds, MAX_OFFERS_COUNT_TO_GET, offersCount, shopId))
                .build();

        wireMockServer.stubFor(post(
                urlEqualTo(String.format("/v1/partners/%s/offers/services/%s/stocks/get", businessId, partnerId)))
                .withRequestBody(containing(new String(firstRequest.toByteArray())))
                .willReturn(aResponse().withBody(firstOfferResponse.toByteArray()))
        );

        wireMockServer.stubFor(post(
                urlEqualTo(String.format("/v1/partners/%s/offers/services/%s/stocks/get", businessId, partnerId)))
                .withRequestBody(containing(new String(secondRequest.toByteArray())))
                .willReturn(aResponse().withBody(secondOfferResponse.toByteArray()))
        );

        List<DataCampOffer.Offer> actualOffers = dataCampClient
                .getBusinessOfferStocks(businessId, partnerId, warehouseId, offerIds);

        List<DataCampOffer.Offer> expectedOffers = offerIds.stream().map(offerId -> DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId(offerId)
                        .setShopId(shopId)
                        .build())
                .build()).collect(Collectors.toList());

        assertEquals(offersCount, actualOffers.size());
        assertTrue(actualOffers.containsAll(expectedOffers));
    }

    private List<DataCampOffer.Offer> mapIdsToOffers(List<String> offerIds, int from, int to, int shopId) {
        return offerIds.subList(from, to).stream()
                .map(offerId -> DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId(offerId)
                                .setShopId(shopId)
                                .build())
                        .build()).collect(Collectors.toList());
    }

    /**
     * Проверка, что запрос с пустыми офферами не обрабатывается
     */
    @DisplayName("Проверка, что запрос с пустыми офферами не обрабатывается")
    @Test
    void testGetBusinessOfferStocksEmptyOffers() {
        long businessId = 123L;
        long partnerId = 456L;
        long warehouseId = 9000L;

        List<DataCampOffer.Offer> actualOffers = dataCampClient
                .getBusinessOfferStocks(businessId, partnerId, warehouseId, Collections.emptyList());

        wireMockServer.verify(0, postRequestedFor(
                urlEqualTo(String.format("/v1/partners/%s/offers/services/%s/stocks/get", businessId, partnerId)))
        );

        assertTrue(actualOffers.isEmpty());
    }

    @DisplayName("Запрос офферов с ценами по бизнесу и партнеру")
    @Test
    public void testGetBusinessOfferWithPriceFlag() {
        //given
        long businessId = 123L;
        long partnerId = 456L;

        boolean hasPrice = true;
        int limitNeeded = 1;

        //when
        SearchBusinessOffersRequest req = SearchBusinessOffersRequest.builder()
                .setPageRequest(SeekSliceRequest.firstN(1))
                .setBusinessId(businessId)
                .setPartnerId(partnerId)
                .setPricePresence(hasPrice)
                .build();

        assertThrows(DataCampException.class, () -> dataCampClient.searchBusinessOffers(req));

        //then
        wireMockServer.verify(1, getRequestedFor(
                urlEqualTo(String.format("/v1/partners/%s/offers?shop_id=%s&limit=%s&has_price=%s",
                        businessId, partnerId, limitNeeded, hasPrice))
        ));

    }

    @DisplayName("Запрос офферов со стоками по бизнесу и партнеру")
    @Test
    public void testGetBusinessOfferWithStocksFlag() {
        //given
        long businessId = 123L;
        long partnerId = 456L;

        boolean hasStocks = true;
        long warehouseId = 3000L;
        int limitNeeded = 1;

        //when
        SearchBusinessOffersRequest req = SearchBusinessOffersRequest.builder()
                .setPageRequest(SeekSliceRequest.firstN(1))
                .setBusinessId(businessId)
                .setPartnerId(partnerId)
                .setMarketStocksPresence(hasStocks)
                .setWarehouseId(warehouseId)
                .build();

        assertThrows(DataCampException.class, () -> dataCampClient.searchBusinessOffers(req));

        //then
        wireMockServer.verify(1, getRequestedFor(
                urlEqualTo(String.format(
                        "/v1/partners/%s/offers?shop_id=%s&limit=%s&has_market_stocks=%s&warehouse_id=%s",
                        businessId, partnerId, limitNeeded, hasStocks, warehouseId
                ))
        ));

    }

    @Test
    public void testWarehouseIdNotSpecified() {
        assertThrows(IllegalArgumentException.class, () -> {
            SearchBusinessOffersRequest.builder()
                    .setPageRequest(SeekSliceRequest.firstN(1))
                    .setMarketStocksPresence(true)
                    .build();
        });
    }

    @DisplayName("Проверка запуска копирования сервисной части в оффферном хранилище")
    @Test
    public void testStartCopyOffers() {
        //given
        long businessId = 123L;
        long partnerId = 456L;
        long warehouseId = 3000L;
        long srcPartnerId = 1;

        var copyRequest = CopyOffers.OffersCopyTask.newBuilder()
                .setBusinessId((int) businessId)
                .setDstShopId((int) partnerId)
                .addSrcShopIds((int) srcPartnerId)
                .setDstWarehouseId((int) warehouseId)
                .setCopyContentFromShop(1)
                .build();

        var copyResponse = CopyOffers.OffersCopyTaskStatus.newBuilder()
                .setShopId((int) partnerId)
                .setId(100)
                .setStatus(CopyOffers.OffersCopyTaskStatus.EStatus.STARTED)
                .build();

        wireMockServer.stubFor(post(
                urlEqualTo(String.format("/v1/partners/%s/offers/services/%s/copy", businessId, partnerId)))
                .withRequestBody(containing(new String(copyRequest.toByteArray()).trim()))
                .willReturn(aResponse().withBody(copyResponse.toByteArray()))
        );

        var status = dataCampClient.startCopyOffers(businessId, partnerId, warehouseId, srcPartnerId);

        //then
        wireMockServer.verify(1, postRequestedFor(
                urlEqualTo(String.format("/v1/partners/%s/offers/services/%s/copy", businessId, partnerId))
        ));
        assertEquals(copyResponse, status);
    }


    @DisplayName("Проверка получения статуса копирования сервисной части в оффферном хранилище")
    @Test
    public void testGetCopyOffersStatus() {
        //given
        long businessId = 123L;
        long partnerId = 456L;
        int taskId = 100;

        var copyResponse = CopyOffers.OffersCopyTaskStatus.newBuilder()
                .setShopId((int) partnerId)
                .setId(100)
                .setStatus(CopyOffers.OffersCopyTaskStatus.EStatus.FINISHED)
                .build();

        wireMockServer.stubFor(get(
                urlEqualTo(String.format("/v1/partners/%s/offers/services/%s/copy/%s", businessId, partnerId, taskId)))
                .willReturn(aResponse().withBody(copyResponse.toByteArray()))
        );

        var status = dataCampClient.getCopyTaskStatus(businessId, partnerId, taskId);

        //then
        wireMockServer.verify(1, getRequestedFor(
                urlEqualTo(String.format("/v1/partners/%s/offers/services/%s/copy/%s", businessId, partnerId, taskId)))
        );
        assertEquals(copyResponse, status);
    }

    @Test
    public void testGetVerdicts() {
        long businessId = 123L;
        long partnerId = 345L;

        var verdictsResponse = ProtoTestUtil.getProtoMessageByJson(GetVerdicts.GetVerdictsBatchResponse.class,
                StringTestUtil.getString(this.getClass(), "../proto/DataCampClientTest.getVerdictsResponse.json"));

        wireMockServer.stubFor(post(
                urlEqualTo(String.format("/v1/partners/%s/offers/verdicts?shop_id=%s", businessId, partnerId)))
                .willReturn(aResponse().withBody(verdictsResponse.toByteArray())));

        var verdictsRequest = GetVerdicts.GetVerdictsBatchRequest.newBuilder()
                .addOfferIds("1")
                .addOfferIds("2")
                .build();

        var response = dataCampClient.getVerdicts(verdictsRequest, businessId, partnerId);
        assertEquals(verdictsResponse, response);
    }


    @Test
    public void testGet503() {
        long businessId = 123L;
        long partnerId = 345L;

        wireMockServer.stubFor(post(
                urlEqualTo(String.format("/v1/partners/%s/offers/verdicts?shop_id=%s", businessId, partnerId)))
                .willReturn(aResponse().withStatus(503)));

        boolean exceptionReceived = false;
        try {
            dataCampClient.getVerdicts(
                    GetVerdicts.GetVerdictsBatchRequest.newBuilder().build(), businessId, partnerId
            );
        } catch (DataCampException e) {
            exceptionReceived = true;
        }

        if (!exceptionReceived) {
            Assertions.fail("Should receive exception, but had not.");
        }

        wireMockServer.verify(SERVICE_UNAVAILABLE_RETRY_COUNT + 1, postRequestedFor(
                urlEqualTo(String.format("/v1/partners/%s/offers/verdicts?shop_id=%s", businessId, partnerId)))
        );
    }

    @DisplayName("Проверка получения категорий по бизнесу")
    @Test
    void testGetBusinessCategories() throws IOException {
        long businessId = 123L;

        String datacampResponse = IOUtils.toString(
                this.getClass().getResourceAsStream("../json/getCategories.response.json")
        );

        wireMockServer.stubFor(get(
                urlEqualTo(String.format("/v1/partners/%s/categories", businessId)))
                .willReturn(aResponse().withBody(datacampResponse))
        );

        Map<String, CategoryInfo> actualResult = dataCampClient
                .getBusinessCategories(businessId);

        assertEquals(Map.of(
                "1027", createTestCategoryInfo1(),
                "1024", createTestCategoryInfo2()
        ), actualResult);
    }

    private CategoryInfo createTestCategoryInfo1() {
        CategoryInfo catInfo = new CategoryInfo();
        catInfo.setId("1027");
        catInfo.setName("leaf");
        catInfo.setChildren(new ArrayList<>());
        catInfo.setLeaf(true);
        catInfo.setParentId("1024");

        return catInfo;
    }

    private CategoryInfo createTestCategoryInfo2() {
        CategoryInfo catInfo = new CategoryInfo();
        catInfo.setId("1024");
        catInfo.setName("parent");
        catInfo.setChildren(List.of("1027"));
        catInfo.setLeaf(false);
        catInfo.setParentId("rootId");

        return catInfo;
    }

    private SyncSelect.SelectResponse createSelectResponse(String nextPageToken, DataCampOffer.Offer... offers) {
        return SyncSelect.SelectResponse.newBuilder()
                .setMeta(SyncSelect.SelectMeta.newBuilder().setNextPageToken(nextPageToken).build())
                .addAllOffers(Arrays.asList(offers))
                .build();
    }

    private DataCampOffer.Offer createTestOffer(DataCampOfferIdentifiers.OfferIdentifiers identifiers,
                                                DataCampOfferStockInfo.OfferStockInfo stocks) {
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(identifiers)
                .setStockInfo(stocks)
                .build();
    }

    private DataCampOfferIdentifiers.OfferIdentifiers createTestIdentifier(int shopId, String offerId) {
        return DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setShopId(shopId)
                .setOfferId(offerId)
                .build();
    }

    private DataCampOfferStockInfo.OfferStockInfo createTestStockInfo(int stocksCount) {
        return DataCampOfferStockInfo.OfferStockInfo.newBuilder()
                .setPartnerStocks(DataCampOfferStockInfo.OfferStocks.newBuilder()
                        .setCount(stocksCount)
                        .build())
                .build();
    }

    private DataCampOfferIdentifiers.OfferIdentifiers createRequestIdentifier(long businessId, long partnerId,
                                                                              long warehouseId, String offerId) {
        return DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(Long.valueOf(businessId).intValue())
                .setShopId(Long.valueOf(partnerId).intValue())
                .setWarehouseId(Long.valueOf(warehouseId).intValue())
                .setOfferId(offerId)
                .build();
    }
}
