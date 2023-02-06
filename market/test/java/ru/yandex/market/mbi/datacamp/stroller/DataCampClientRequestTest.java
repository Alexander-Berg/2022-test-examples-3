package ru.yandex.market.mbi.datacamp.stroller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import Market.DataCamp.API.Migration;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import Market.DataCamp.SyncAPI.SyncHideFeed;
import Market.DataCamp.SyncAPI.SyncSearch;
import NMarket.Common.Promo.Promo;
import com.google.common.base.Charsets;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.util.UriUtils;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.stroller.model.DataCampNotFoundException;
import ru.yandex.market.mbi.datacamp.stroller.model.FinishChangeSchemaDTO;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;
import ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferQuery;
import ru.yandex.market.mbi.datacamp.stroller.model.graphql.SearchQuery;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static ru.yandex.market.mbi.datacamp.model.search.filter.CpaContentStatus.CPA_CONTENT_NEED_CONTENT;
import static ru.yandex.market.mbi.datacamp.model.search.filter.CpaContentStatus.CPA_CONTENT_SUSPENDED;
import static ru.yandex.market.mbi.datacamp.model.search.filter.HidingSource.MARKET_PRICELABS;
import static ru.yandex.market.mbi.datacamp.model.search.filter.HidingSource.PUSH_PARTNER_API;
import static ru.yandex.market.mbi.datacamp.model.search.filter.PartnerContentStatus.AVAILABLE;
import static ru.yandex.market.mbi.datacamp.model.search.filter.PartnerContentStatus.HIDDEN;
import static ru.yandex.market.mbi.datacamp.model.search.filter.PartnerSupplyPlan.WILL_SUPPLY;
import static ru.yandex.market.mbi.datacamp.model.search.filter.PartnerSupplyPlan.WONT_SUPPLY;
import static ru.yandex.market.mbi.datacamp.model.search.filter.ResultContentStatus.HAS_CARD_MARKET;
import static ru.yandex.market.mbi.datacamp.model.search.filter.ResultContentStatus.NO_CARD_NEED_CONTENT;
import static ru.yandex.market.mbi.datacamp.model.search.filter.ResultOfferStatus.NOT_PUBLISHED_CHECKING;
import static ru.yandex.market.mbi.datacamp.model.search.filter.ResultOfferStatus.NOT_PUBLISHED_FINISH_PS_CHECK;
import static ru.yandex.market.mbi.datacamp.model.search.filter.ResultOfferStatus.NOT_PUBLISHED_PARTNER_IS_DISABLED;
import static ru.yandex.market.mbi.datacamp.model.search.filter.ResultOfferStatus.PUBLISHED;
import static ru.yandex.market.mbi.datacamp.model.search.filter.ResultOfferStatus.UNKNOWN_RESULT;

/**
 * Для проверки запросов в строллер.
 */
class DataCampClientRequestTest {

    private static final long BUSINESS_ID = 1000L;
    private static final long OLD_BUSINESS_ID = 2000L;
    private static final long NEW_BUSINESS_ID = 2001L;
    private static final long PARTNER_ID = 200L;
    private static final int WAREHOUSE_ID = 14;
    private static final long FEED_ID = 4006L;
    private static final long TARGET_FEED_ID = 4007L;
    private static final String OFFER_ID = "offerOK";
    private static final String OFFER_ID_2 = "offerOK2";
    private static final String TOKEN = "token_123";
    private static final SyncGetOffer.GetUnitedOffersResponse EMPTY_SEARCH_RESULT =
            SyncGetOffer.GetUnitedOffersResponse.newBuilder().build();

    private DataCampClient dataCampClient;
    private final ArgumentCaptor<HttpEntityEnclosingRequestBase> argumentCaptor =
            ArgumentCaptor.forClass(HttpEntityEnclosingRequestBase.class);
    private final ArgumentCaptor<URI> argumentURICaptor = ArgumentCaptor.forClass(URI.class);
    private final ArgumentCaptor<HttpPost> argumentJsonPostCaptor = ArgumentCaptor.forClass(HttpPost.class);
    private final ArgumentCaptor<GeneratedMessageV3> requestBodyCaptor =
            ArgumentCaptor.forClass(GeneratedMessageV3.class);

    @BeforeEach
    void init() {
        dataCampClient = spy(
                new DataCampClient("url", HttpClientBuilder.create().build())
        );
        doReturn(null).when(dataCampClient)
                .sendRequest(argumentCaptor.capture(), requestBodyCaptor.capture(), any());
        doNothing().when(dataCampClient)
                .sendRequestWithoutResponse(argumentCaptor.capture(), requestBodyCaptor.capture());
        doNothing().when(dataCampClient).sendDelete(argumentURICaptor.capture(), any());
        doReturn(null).when(dataCampClient).sendJsonRequest(argumentJsonPostCaptor.capture(), any());
    }

    @Test
    void searchBusinessOffersCheckUrl() {
        SearchBusinessOffersRequest dataCampRequest = SearchBusinessOffersRequest.builder()
                .setBusinessId(BUSINESS_ID)
                .setPartnerId(PARTNER_ID)
                .setPageRequest(SeekSliceRequest.firstN(12))
                .setOffset(9)
                .build();
        doReturn(EMPTY_SEARCH_RESULT).when(dataCampClient)
                .sendRequest(argumentCaptor.capture(), any(), any());
        dataCampClient.searchBusinessOffers(dataCampRequest);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals("url/v1/partners/1000/offers?shop_id=200&limit=12&offset=9", requestValue.getURI().toString());
    }

    @Test
    void searchBusinessOffersWithOfferQueryCheckUrl() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withIdentifiers(true)
                .withPrice(true)
                .withIsDsbs(true)
                .build();
        var res = dataCampClient.buildSearchGraphqlQuery(offerQuery, SearchQuery.defaultInstance());
        String query = "{offers{actual{warehouse{partner_info{is_dsbs}}}," +
                "service{identifiers,price{basic{binary_price{price}}}},basic{identifiers}}," +
                "total,offset,current_page_position,limit,next_page_position,previous_page_position}";
        SearchBusinessOffersRequest dataCampRequest = SearchBusinessOffersRequest.builder()
                .setBusinessId(BUSINESS_ID)
                .setPartnerId(PARTNER_ID)
                .setPageRequest(SeekSliceRequest.firstN(12))
                .setOffset(9)
                .setOfferQuery(offerQuery)
                .build();
        doReturn(EMPTY_SEARCH_RESULT).when(dataCampClient)
                .sendRequest(argumentCaptor.capture(), any(), any());
        dataCampClient.searchBusinessOffers(dataCampRequest);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        String url = "url/v1/partners/1000/offers?shop_id=200&limit=12&offset=9&query=" +
                UriUtils.encode(query, Charsets.UTF_8);
        assertEquals("GET", requestValue.getMethod());
        assertEquals(url, requestValue.getURI().toString());
    }

    @Test
    void searchBusinessOffersCheckUrlWithScanLimit() {
        SearchBusinessOffersRequest dataCampRequest = SearchBusinessOffersRequest.builder()
                .setBusinessId(BUSINESS_ID)
                .setPartnerId(PARTNER_ID)
                .setPageRequest(SeekSliceRequest.firstN(12))
                .setOffset(9)
                .setScanLimit(10_000)
                .build();
        doReturn(EMPTY_SEARCH_RESULT).when(dataCampClient)
                .sendRequest(argumentCaptor.capture(), any(), any());
        dataCampClient.searchBusinessOffers(dataCampRequest);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals("url/v1/partners/1000/offers?shop_id=200&limit=12&offset=9&scan_limit=10000",
                requestValue.getURI().toString());
    }

    @Test
    void searchBusinessOffersWithOfferIdOffsetCheckUrl() {
        SearchBusinessOffersRequest dataCampRequest = SearchBusinessOffersRequest.builder()
                .setBusinessId(BUSINESS_ID)
                .setPartnerId(PARTNER_ID)
                .setPrefillWithServiceId(4321L)
                .setPageRequest(SeekSliceRequest.firstNAfter(12, "offer123"))
                .addOfferIds(Set.of("offer2", "offer1"))
                .addDisabledFlags(Set.of(PUSH_PARTNER_API, MARKET_PRICELABS))
                .addIncludePartnerIds(Set.of(234L, 567L))
                .addExcludePartnerIds(Set.of(777L, 888L))
                .addVendors(Set.of("partner1", "partner2"))
                .addCategoryIds(Set.of(141414L, 151515L))
                .addContentStatusesCPA(Set.of(CPA_CONTENT_SUSPENDED, CPA_CONTENT_NEED_CONTENT))
                .addMarketCategoryIds(Set.of(221L, 223L))
                .addContentStatusesPartner(Set.of(HIDDEN, AVAILABLE))
                .addResultOfferStatuses(Set.of(UNKNOWN_RESULT, PUBLISHED, NOT_PUBLISHED_CHECKING,
                        NOT_PUBLISHED_FINISH_PS_CHECK, NOT_PUBLISHED_PARTNER_IS_DISABLED))
                .addResultContentStatuses(Set.of(HAS_CARD_MARKET, NO_CARD_NEED_CONTENT))
                .addSupplyPlans(Set.of(WILL_SUPPLY, WONT_SUPPLY))
                .build();

        doReturn(EMPTY_SEARCH_RESULT).when(dataCampClient)
                .sendRequest(argumentCaptor.capture(), any(), any());
        dataCampClient.searchBusinessOffers(dataCampRequest);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals(
                "url/v1/partners/1000/offers?" +
                        "shop_id=200" +
                        "&return_shop_id=4321" +
                        "&limit=12" +
                        "&position=offer123" +
                        "&disabled_by=3&disabled_by=10" +
                        "&offer_id=offer1&offer_id=offer2" +
                        "&include_shop_id=234&include_shop_id=567" +
                        "&exclude_shop_id=777&exclude_shop_id=888" +
                        "&vendor=partner1&vendor=partner2" +
                        "&category_id=141414&category_id=151515" +
                        "&market_category_id=221&market_category_id=223" +
                        "&cpa=2&cpa=4" +
                        "&partner_status=1&partner_status=3" +
                        "&supply_plan=2&supply_plan=3" +
                        "&result_status=0&result_status=1&result_status=3&result_status=12&result_status=13" +
                        "&integral_content_status=1&integral_content_status=5",
                requestValue.getURI().toString()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getBusinessUnitedOfferCheckUrlData")
    void getBusinessUnitedOfferCheckUrl(String name, List<String> offerIds, @Nullable Long partnerId, String expected) {
        dataCampClient.getBusinessUnitedOffer(BUSINESS_ID, offerIds, partnerId);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals(expected, requestValue.getURI().toString());
    }

    private static Stream<Arguments> getBusinessUnitedOfferCheckUrlData() {
        return Stream.of(
                Arguments.of(
                        "1 оффер. Без услуги",
                        List.of(OFFER_ID),
                        null,
                        "url/v1/partners/1000/offers?offer_id=offerOK"
                ),
                Arguments.of(
                        "2 оффера. Без услуги",
                        List.of(OFFER_ID, OFFER_ID_2),
                        null,
                        "url/v1/partners/1000/offers?offer_id=offerOK&offer_id=offerOK2"
                ),
                Arguments.of(
                        "2 оффера. С услугой",
                        List.of(OFFER_ID, OFFER_ID_2),
                        123L,
                        "url/v1/partners/1000/offers?offer_id=offerOK&offer_id=offerOK2&shop_id=123"
                )
        );
    }

    @Test
    void getBusinessUnitedOffersCheckUrlAndRequestBody() {
        dataCampClient.getBusinessUnitedOffers(BUSINESS_ID, List.of(OFFER_ID, OFFER_ID_2), PARTNER_ID);
        checkUnitedOffersBatchRequestUrlAndBody();
    }

    @Test
    void getBusinessUnitedOffersWithOfferQueryCheckUrlAndRequestBody() {
        OfferQuery offerQuery = new OfferQuery.Builder()
                .withDynamicPricing(true)
                .withActiveAnaplanPromos(true)
                .withName(true)
                .build();
        dataCampClient.getBusinessUnitedOffers(BUSINESS_ID, List.of(OFFER_ID, OFFER_ID_2), PARTNER_ID, offerQuery);
        String query = "{entries{united_offer{service{price{dynamic_pricing}," +
                "promos{anaplan_promos{active_promos{promos}}}}," +
                "basic{content{partner{original{name{value}}}}}}}}";
        String url = "url/v1/offers/united/batch?query=" +
                UriUtils.encode(query, Charsets.UTF_8);
        checkUnitedOffersBatchRequestUrlAndBody(url);
    }

    @Test
    void changeBusinessUnitedOffersCheckUrlAndRequestBody() {
        DataCampUnitedOffer.UnitedOffer offer =
                loadOfferFrom("../proto/DataCampClientTest.changeBusinessUnitedOffers.offerOK.json");
        DataCampUnitedOffer.UnitedOffer offer2 =
                loadOfferFrom("../proto/DataCampClientTest.changeBusinessUnitedOffers.offerOK2.json");
        dataCampClient.changeBusinessUnitedOffers(BUSINESS_ID, PARTNER_ID, List.of(offer, offer2));
        checkUnitedOffersBatchRequestUrlAndBody(offer, offer2);
    }

    private DataCampUnitedOffer.UnitedOffer loadOfferFrom(String jsonPath) {
        return ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                jsonPath,
                getClass()
        );
    }

    private void checkUnitedOffersBatchRequestUrlAndBody(DataCampUnitedOffer.UnitedOffer... unitedOffers) {
        checkUnitedOffersBatchRequestUrlAndBody("url/v1/offers/united/batch", unitedOffers);
    }

    private void checkUnitedOffersBatchRequestUrlAndBody(String url, DataCampUnitedOffer.UnitedOffer... unitedOffers) {
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        OffersBatch.UnitedOffersBatchRequest requestBody =
                (OffersBatch.UnitedOffersBatchRequest) requestBodyCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals(url, requestValue.getURI().toString());
        List<OffersBatch.UnitedOffersBatchRequest.Entry> entries = requestBody.getEntriesList();
        List<OffersBatch.UnitedOffersBatchRequest.Entry> expected = getExpectedBatchRequestEntries(unitedOffers);
        Assertions.assertThat(entries)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    private List<OffersBatch.UnitedOffersBatchRequest.Entry> getExpectedBatchRequestEntries(
            DataCampUnitedOffer.UnitedOffer... offers
    ) {
        if (ArrayUtils.getLength(offers) == 2) {
            return List.of(
                    getBatchRequestEntry((int) PARTNER_ID, (int) BUSINESS_ID, OFFER_ID, offers[0]),
                    getBatchRequestEntry((int) PARTNER_ID, (int) BUSINESS_ID, OFFER_ID_2, offers[1])
            );
        } else {
            return List.of(
                    getBatchRequestEntry((int) PARTNER_ID, (int) BUSINESS_ID, OFFER_ID),
                    getBatchRequestEntry((int) PARTNER_ID, (int) BUSINESS_ID, OFFER_ID_2)
            );
        }
    }

    private OffersBatch.UnitedOffersBatchRequest.Entry getBatchRequestEntry(int partnerId, int businessId,
                                                                            String offerId) {
        return getBatchRequestEntryBuilder(partnerId, businessId, offerId).build();
    }

    private OffersBatch.UnitedOffersBatchRequest.Entry getBatchRequestEntry(int partnerId, int businessId,
                                                                            String offerId,
                                                                            DataCampUnitedOffer.UnitedOffer offer) {
        return getBatchRequestEntryBuilder(partnerId, businessId, offerId)
                .setOffer(offer)
                .setMethod(OffersBatch.RequestMethod.POST)
                .build();
    }

    private OffersBatch.UnitedOffersBatchRequest.Entry.Builder getBatchRequestEntryBuilder(int partnerId,
                                                                                           int businessId,
                                                                                           String offerId) {
        return OffersBatch.UnitedOffersBatchRequest.Entry.newBuilder()
                .setShopId(partnerId)
                .setBusinessId(businessId)
                .setOfferId(offerId);
    }

    @Test
    @DisplayName("getBusinessUnitedOffers. Индексатор вернул фейковый оффер. Клиент должен вернуть пустой список")
    void getBusinessUnitedOffersFakeEmptyResponse() {
        checkGetBusinessUnitedOffersResponse(
                PARTNER_ID,
                "../proto/DataCampClientTest.getBusinessUnitedOffers.fake_empty.json",
                "../proto/DataCampClientTest.getBusinessUnitedOffers.real_empty.json"
        );
    }

    @Test
    @DisplayName("getBusinessUnitedOffers. Индексатор вернул пустой список. Клиент должен вернуть пустой список")
    void getBusinessUnitedOffersRealEmptyResponse() {
        checkGetBusinessUnitedOffersResponse(
                PARTNER_ID,
                "../proto/DataCampClientTest.getBusinessUnitedOffers.real_empty.json",
                "../proto/DataCampClientTest.getBusinessUnitedOffers.real_empty.json"
        );
    }

    @Test
    @DisplayName("getBusinessUnitedOffers. Индексатор вернул список офферов. Клиент должен вернуть список офферов")
    void getBusinessUnitedOffersNotEmptyResponse() {
        checkGetBusinessUnitedOffersResponse(
                PARTNER_ID,
                "../proto/DataCampClientTest.getBusinessUnitedOffers.not_empty.json",
                "../proto/DataCampClientTest.getBusinessUnitedOffers.not_empty.json"
        );
    }

    @Test
    @DisplayName("getBusinessUnitedOffers. Индексатор вернул список офферов, в котором есть пустые. " +
            "Клиент должен вернуть список офферов без пропусков")
    void getBusinessUnitedOffersWithEmptyResponse() {
        checkGetBusinessUnitedOffersResponse(
                PARTNER_ID,
                "../proto/DataCampClientTest.getBusinessUnitedOffers.with_empty.json",
                "../proto/DataCampClientTest.getBusinessUnitedOffers.not_empty.json"
        );
    }

    @Test
    @DisplayName("getBusinessUnitedOffers. Запросили оффер, который есть в бизнесе, но нет в услуге. " +
            "Услуга есть в запросе. Вернулся только базовый оффер. Клиент должен вернуть пустой список")
    void getBusinessUnitedOffersWithEmptyServiceWithPartnerResponse() {
        checkGetBusinessUnitedOffersResponse(
                PARTNER_ID,
                "../proto/DataCampClientTest.getBusinessUnitedOffers.with_empty_service.json",
                "../proto/DataCampClientTest.getBusinessUnitedOffers.real_empty.json"
        );
    }

    @Test
    @DisplayName("getBusinessUnitedOffers. Запросили оффер, который есть в бизнесе, но нет в услуге. " +
            "Услуги нет в запросе. Вернулся только базовый оффер. Клиент должен вернуть оффер as-is")
    void getBusinessUnitedOffersWithEmptyServiceWithoutPartnerResponse() {
        checkGetBusinessUnitedOffersResponse(
                null,
                "../proto/DataCampClientTest.getBusinessUnitedOffers.with_empty_service.json",
                "../proto/DataCampClientTest.getBusinessUnitedOffers.with_empty_service.json"
        );
    }

    private void checkGetBusinessUnitedOffersResponse(@Nullable Long partnerId,
                                                      String idxResponse,
                                                      String clientResponse) {
        Mockito.reset(dataCampClient);
        OffersBatch.UnitedOffersBatchResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                idxResponse,
                getClass()
        );
        doReturn(mockedResponse).when(dataCampClient)
                .sendRequest(argumentCaptor.capture(), requestBodyCaptor.capture(), any());

        OffersBatch.UnitedOffersBatchResponse actualResponse = dataCampClient.getBusinessUnitedOffers(BUSINESS_ID,
                List.of(OFFER_ID, OFFER_ID_2), partnerId);

        OffersBatch.UnitedOffersBatchResponse expected = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                clientResponse,
                getClass()
        );

        ProtoTestUtil.assertThat(actualResponse)
                .isEqualTo(expected);
    }

    @Test
    void getBusinessBasicOfferCheckUrl() {
        dataCampClient.getBusinessBasicOffer(BUSINESS_ID, OFFER_ID);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals("url/v1/partners/1000/offers/basic?offer_id=offerOK", requestValue.getURI().toString());
    }

    @Test
    void getBusinessServiceOfferCheckUrl() {
        dataCampClient.getBusinessServiceOffer(BUSINESS_ID, OFFER_ID, PARTNER_ID);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals("url/v1/partners/1000/offers/services/200?offer_id=offerOK", requestValue.getURI().toString());
    }

    @Test
    void changeBusinessBasicOfferCheckUrl() {
        DataCampOffer.Offer request = DataCampOffer.Offer.getDefaultInstance();
        dataCampClient.changeBusinessBasicOffer(BUSINESS_ID, OFFER_ID, request);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/v1/partners/1000/offers/basic?offer_id=offerOK", requestValue.getURI().toString());
    }

    @Test
    void changeBusinessServiceOfferCheckUrl() {
        DataCampOffer.Offer request = DataCampOffer.Offer.getDefaultInstance();
        dataCampClient.changeBusinessServiceOffer(BUSINESS_ID, OFFER_ID, PARTNER_ID, request);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/v1/partners/1000/offers/services/200?offer_id=offerOK", requestValue.getURI().toString());
    }

    @Test
    @DisplayName("hideOffersByFeed с указанием токена")
    void hideOffersByFeedCheckUrlWithToken() {
        checkHideOffersByFeed(
                "/v1/partners/1000/shops/200/feeds/4006/hide?page_token=token_123&batch_size=100",
                TOKEN
        );
    }

    @Test
    @DisplayName("hideOffersByFeed без указания токена")
    void hideOffersByFeedCheckUrlWithoutToken() {
        checkHideOffersByFeed(
                "/v1/partners/1000/shops/200/feeds/4006/hide?batch_size=100",
                null
        );
    }

    private void checkHideOffersByFeed(String expectedRequestUrl, @Nullable String token) {
        Instant currentTimestamp = DateTimes.toInstant(2019, 4, 5);

        dataCampClient.hideOffersByFeed(BUSINESS_ID, PARTNER_ID, FEED_ID, currentTimestamp, 100, token);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url" + expectedRequestUrl, requestValue.getURI().toString());

        SyncHideFeed.HideFeedRequest expectedRequest = ProtoTestUtil.getProtoMessageByJson(
                SyncHideFeed.HideFeedRequest.class,
                "../proto/DataCampClientTest.hideOffersByFeed.request.json",
                getClass()
        );
        GeneratedMessageV3 actualRequest = requestBodyCaptor.getValue();
        ProtoTestUtil.assertThat(actualRequest)
                .isEqualTo(expectedRequest);
    }

    @Test
    @DisplayName("migrateOffersByFeed с указанием токена")
    void migrateOffersByFeedCheckUrlWithToken() {
        checkMigrateOffersByFeed(
                "/v1/partners/1000/shops/200/feeds/4006/migrate" +
                        "?target_feed_id=4007&page_token=token_123&batch_size=100",
                TOKEN
        );
    }

    @Test
    @DisplayName("migrateOffersByFeed без указания токена")
    void migrateOffersByFeedCheckUrlWithoutToken() {
        checkMigrateOffersByFeed(
                "/v1/partners/1000/shops/200/feeds/4006/migrate?target_feed_id=4007&batch_size=100",
                null
        );
    }

    private void checkMigrateOffersByFeed(String expectedRequestUrl, @Nullable String token) {
        dataCampClient.migrateOffersByFeed(BUSINESS_ID, PARTNER_ID, FEED_ID, TARGET_FEED_ID, 100, token);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url" + expectedRequestUrl, requestValue.getURI().toString());
    }

    @Test
    void searchOffersCheckUrl() {
        SyncSearch.SearchRequest request = SyncSearch.SearchRequest.newBuilder().build();
        dataCampClient.searchOffers(PARTNER_ID, BUSINESS_ID, true, request);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/shops/200/offers/search?original=1&business_id=1000", requestValue.getURI().toString());
    }

    @Test
    void getOfferCheckUrl() {
        dataCampClient.getOffer(PARTNER_ID, OFFER_ID, WAREHOUSE_ID, BUSINESS_ID);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals("url/shops/200/offers?offer_id=" + OFFER_ID + "&warehouse_id=" + WAREHOUSE_ID
                + "&business_id=" + BUSINESS_ID, requestValue.getURI().toString());
    }

    @Test
    void changeOfferStatusCheckUrl() {
        dataCampClient.changeOfferStatus(PARTNER_ID, OFFER_ID, BUSINESS_ID,
                DataCampOfferStatus.OfferStatus.newBuilder().build());
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("PUT", requestValue.getMethod());
        assertEquals("url/shops/200/offers/disabled?offer_id=" + OFFER_ID + "&business_id=" + BUSINESS_ID,
                requestValue.getURI().toString());
    }

    @Test
    void changeOfferPriceCheckUrl() {
        dataCampClient.changeOfferPrice(PARTNER_ID, OFFER_ID, BUSINESS_ID,
                DataCampOfferPrice.OfferPrice.newBuilder().build());
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("PUT", requestValue.getMethod());
        assertEquals("url/shops/200/offers/price?offer_id=" + OFFER_ID + "&business_id=" + BUSINESS_ID,
                requestValue.getURI().toString());
    }

    @Test
    void changeOfferPriceShopCheckUrl() {
        dataCampClient.changeOfferPrice(PARTNER_ID, OFFER_ID, null,
                DataCampOfferPrice.OfferPrice.newBuilder().build());
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("PUT", requestValue.getMethod());
        assertEquals("url/shops/200/offers/price?offer_id=" + OFFER_ID,
                requestValue.getURI().toString());
    }

    @Test
    void deleteOfferCheckUrl() {
        dataCampClient.deleteOffer(PARTNER_ID, OFFER_ID, WAREHOUSE_ID, BUSINESS_ID);
        URI uriValue = argumentURICaptor.getValue();
        assertEquals("url/shops/200/offers?offer_id=" + OFFER_ID + "&warehouse_id=" + WAREHOUSE_ID
                + "&business_id=" + BUSINESS_ID, uriValue.toString());
    }

    @Test
    void changeSchemaCheckUrl() {
        FinishChangeSchemaDTO finishChangeSchemaDTO = new FinishChangeSchemaDTO();
        finishChangeSchemaDTO.setFinishTsSec(10L);
        finishChangeSchemaDTO.setIsPushPartner(true);

        dataCampClient.changeSchema(PARTNER_ID, BUSINESS_ID, finishChangeSchemaDTO);

        HttpPost requestValue = argumentJsonPostCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/shops/200/change_schema?business_id=" + BUSINESS_ID, requestValue.getURI().toString());

    }

    @Test
    void getOffersCheckUrl() {
        dataCampClient.getOffers(PARTNER_ID, BUSINESS_ID,
                SyncChangeOffer.ChangeOfferRequest.newBuilder()
                        .addOffer(
                                DataCampOffer.Offer.newBuilder()
                                        .setIdentifiers(
                                                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                        .setOfferId("2101920")
                                                        .build())
                                        .build())
                        .build());
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals("url/shops/200/offers?business_id=" + BUSINESS_ID, requestValue.getURI().toString());
    }

    @Test
    void addWarehouseTest() {
        dataCampClient.addWarehouse(PARTNER_ID, 200344511L, 145L, BUSINESS_ID);

        HttpEntityEnclosingRequestBase requestValue = argumentJsonPostCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/shops/200/add_warehouse?feed_id=200344511&warehouse_id=145&format=json&business_id=1000",
                requestValue.getURI().toString());
    }

    @Test
    void addWarehouse404Test() {
        doThrow(new DataCampNotFoundException("Something wrong"))
                .when(dataCampClient).sendJsonRequest(argumentJsonPostCaptor.capture(), any());

        assertThrows(DataCampNotFoundException.class,
                () -> dataCampClient.addWarehouse(PARTNER_ID, 200344511L, 145L, BUSINESS_ID));

        HttpEntityEnclosingRequestBase requestValue = argumentJsonPostCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/shops/200/add_warehouse?feed_id=200344511&warehouse_id=145&format=json&business_id=1000",
                requestValue.getURI().toString());
    }

    @Test
    void removeWarehouseTest() {
        dataCampClient.removeWarehouse(PARTNER_ID, 145L, BUSINESS_ID);

        HttpEntityEnclosingRequestBase requestValue = argumentJsonPostCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/v1/partners/1000/services/200/remove_warehouse?warehouse_id=145&format=json",
                requestValue.getURI().toString());
    }

    @Test
    void removeWarehouse404Test() {
        doThrow(new DataCampNotFoundException("Something wrong"))
                .when(dataCampClient).sendJsonRequest(argumentJsonPostCaptor.capture(), any());

        assertThrows(DataCampNotFoundException.class,
                () -> dataCampClient.removeWarehouse(PARTNER_ID, 145L, BUSINESS_ID));

        HttpEntityEnclosingRequestBase requestValue = argumentJsonPostCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/v1/partners/1000/services/200/remove_warehouse?warehouse_id=145&format=json",
                requestValue.getURI().toString());
    }

    @Test
    void startMigration200Test() {
        Migration.MigrationStatus migrationStatus = Migration.MigrationStatus.newBuilder()
                .setStatus(Migration.MigrationStatus.EStatus.STARTED)
                .build();
        doReturn(migrationStatus)
                .when(dataCampClient).sendMigrationRequest(argumentJsonPostCaptor.capture(), any(), any());

        assertEquals(
                migrationStatus,
                dataCampClient.startMigration(PARTNER_ID, OLD_BUSINESS_ID, NEW_BUSINESS_ID)
        );
        checkStartMigrationRequest();
    }

    @Test
    void startMigration404Test() {
        Migration.MigrationStatus migrationStatus = Migration.MigrationStatus.newBuilder()
                .build();
        doReturn(migrationStatus)
                .when(dataCampClient).sendMigrationRequest(argumentJsonPostCaptor.capture(), any(), any());

        assertEquals(
                migrationStatus,
                dataCampClient.startMigration(PARTNER_ID, OLD_BUSINESS_ID, NEW_BUSINESS_ID)
        );
        checkStartMigrationRequest();
    }

    @Test
    void startMigrationExceptionTest() {
        doThrow(new UncheckedIOException("Something wrong", new IOException()))
                .when(dataCampClient).sendMigrationRequest(argumentJsonPostCaptor.capture(), any(), any());

        assertThrows(UncheckedIOException.class,
                () -> dataCampClient.startMigration(PARTNER_ID, OLD_BUSINESS_ID, NEW_BUSINESS_ID));
        checkStartMigrationRequest();
    }


    private void checkStartMigrationRequest() {
        HttpEntityEnclosingRequestBase requestValue = argumentJsonPostCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/v1/migration/200/start?src_business_id=2000&dst_business_id=2001",
                requestValue.getURI().toString());
    }

    @Test
    void migrationStatusTest() {
        dataCampClient.migrationStatus(PARTNER_ID);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals("url/v1/migration/200/status",
                requestValue.getURI().toString());
    }

    @Test
    void getBusinessOfferStocks() {
        long businessId = 123L;
        long partnerId = 456L;
        long warehouseId = 9000L;
        List<String> offers = IntStream.rangeClosed(1, 750).boxed().map(i -> "offer" + i).collect(Collectors.toList());

        ArgumentCaptor<SyncGetOffer.GetOffersRequest> bodyCaptor =
                ArgumentCaptor.forClass(SyncGetOffer.GetOffersRequest.class);

        doReturn(SyncGetOffer.GetOffersResponse.newBuilder().build())
                .when(dataCampClient)
                .sendRequest(argumentCaptor.capture(), bodyCaptor.capture(), any());

        dataCampClient.getBusinessOfferStocks(businessId, partnerId,
                warehouseId, offers);

        List<SyncGetOffer.GetOffersRequest> requests = bodyCaptor.getAllValues();
        List<Integer> sizes = requests.stream()
                .map(SyncGetOffer.GetOffersRequest::getIdentifiersList)
                .map(List::size)
                .collect(Collectors.toList());
        assertEquals(requests.size(), 2);
        MatcherAssert.assertThat(sizes, containsInAnyOrder(250, 500));
    }

    @Test
    void getPromosTest() {
        dataCampClient.getPromos(
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(
                                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setBusinessId((int) BUSINESS_ID)
                                                        .setSource(
                                                                NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE
                                                        )
                                                        .setPromoId("somePromoId")
                                                        .build()
                                        )
                                        .build()
                        )
                        .setEnabled(true)
                        .setOnlyUnfinished(true)
                        .setPromoType(Collections.singleton(DataCampPromo.PromoType.DIRECT_DISCOUNT))
                        .build()
        );
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/v1/promo/get?enabled=true&only_unfinished=true&type=1", requestValue.getURI().toString());
    }

    @Test
    void getPartnerPromosTest() {
        long businessId = 111;
        PromoDatacampRequest request =
                new PromoDatacampRequest.Builder(111L)
                        .withEnabled(true)
                        .withOnlyUnfinished(true)
                        .withPartnerId(1L)
                        .withPromoType(Set.of(DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK))
                        .withSourceTypes(new TreeSet<>(
                                Set.of(
                                        Promo.ESourceType.ANAPLAN,
                                        Promo.ESourceType.PARTNER_SOURCE)
                                )
                        )
                        .withLimit(10)
                        .withPosition("promo_id")
                        .build();
        dataCampClient.getPromos(request);
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("GET", requestValue.getMethod());
        assertEquals(
                "url/v1/partners/" + businessId + "/promos?" +
                        "enabled=true&" +
                        "only_unfinished=true&" +
                        "partner_id=1&" +
                        "type=19&" +
                        "source=1&source=2&" +
                        "limit=10&" +
                        "position=promo_id",
                requestValue.getURI().toString()
        );
    }

    @Test
    void addPromoTest() {
        dataCampClient.addPromo(
                DataCampPromo.PromoDescription.getDefaultInstance()
        );
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/v1/add_promo", requestValue.getURI().toString());
    }

    @Test
    void addPartnerPromoTest() {
        long businessId = 111;
        dataCampClient.addPromo(
                SyncGetPromo.UpdatePromoBatchRequest.getDefaultInstance(),
                businessId
        );
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/v1/partners/" + businessId + "/promos", requestValue.getURI().toString());
    }

    @Test
    void deletePromoTest() {
        dataCampClient.deletePromo(
                SyncGetPromo.DeletePromoBatchRequest.getDefaultInstance()
        );
        HttpEntityEnclosingRequestBase requestValue = argumentCaptor.getValue();
        assertEquals("POST", requestValue.getMethod());
        assertEquals("url/v1/promo/delete", requestValue.getURI().toString());
    }
}
