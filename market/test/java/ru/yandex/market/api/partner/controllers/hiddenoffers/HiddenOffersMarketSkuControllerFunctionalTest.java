package ru.yandex.market.api.partner.controllers.hiddenoffers;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.PapiFeedMarketSkuHiding;
import ru.yandex.market.core.offer.PapiHidingDetails;
import ru.yandex.market.core.offer.PapiHidingEvent;
import ru.yandex.market.core.offer.PapiMarketSkuOfferService;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerServiceStub;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.web.paging.Paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "HiddenOffersMarketSkuControllerFunctionalTest.csv")
class HiddenOffersMarketSkuControllerFunctionalTest extends AbstractHiddenOffersControllerFunctionalTest {
    private static final long CLIENT_ID = 325076;
    private static final long DATASOURCE_ID = 10263825;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    @Qualifier("papiMarketSkuOfferSeparatePoolService")
    private PapiMarketSkuOfferService papiMarketSkuOfferService;

    @Autowired
    private UltraControllerServiceStub ultraControllerClient;

    @Autowired
    @Qualifier("marketQuickLogbrokerService")
    private LogbrokerEventPublisher<SyncChangeOfferLogbrokerEvent> logbrokerService;

    @BeforeEach
    void initEnv() {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
    }

    @Test
    void testPostBasicXml() throws IOException {
        mockUltraControllerClient();
        String content =
                "<hidden-offers>" +
                        "   <hidden-offer market-sku=\"1\" offer-id=\"offer1\" ttl-in-hours=\"10\"/>" +
                        "   <hidden-offer market-sku=\"3\" offer-id=\"offer3\" ttl-in-hours=\"2\"/>" +
                        "   <hidden-offer market-sku=\"5\" offer-id=\"offer5\" comment=\"test\"/>" +
                        "</hidden-offers>";
        String response = performPost(10661, content, 200, Format.XML);
        String expectedResponse = "<response><status>OK</status></response>";
        MbiAsserts.assertXmlEquals(expectedResponse, response);
        Map<IndexerOfferKey, PapiHidingDetails> hiddenOffers =
                papiMarketSkuOfferService.hiddenOffersEntries(Collections.singleton(661661L), Paging.firstN(1000))
                        .stream()
                        .collect(Collectors.toMap(
                                PapiFeedMarketSkuHiding::feedMarketSku,
                                PapiFeedMarketSkuHiding::details));
        assertEquals(3, hiddenOffers.size());
        assertTrue(hiddenOffers.containsKey(IndexerOfferKey.offerId(661661L, "offer1")));
        assertTrue(hiddenOffers.containsKey(IndexerOfferKey.offerId(661661L, "offer3")));
        assertTrue(hiddenOffers.containsKey(IndexerOfferKey.offerId(661661L, "offer5")));
        assertEquals(Optional.empty(), hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer1")).comment());
        assertEquals(CLIENT_ID, hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer1")).clientId());
        assertEquals(Optional.empty(), hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer1")).comment());
        assertEquals(CLIENT_ID, hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer3")).clientId());
        assertEquals(Optional.of("test"), hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer5")).comment());
        assertEquals(CLIENT_ID, hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer5")).clientId());
    }


    @Test
    void testPostBasicXmlWithLogbrokerEnabled() throws IOException {
        mockUltraControllerClient();
        environmentService.setValue("market.quick.partner-api.send.to.logbroker", "true");
        String content =
                "<hidden-offers>" +
                        "   <hidden-offer market-sku=\"1\" offer-id=\"offer1\" ttl-in-hours=\"10\"/>" +
                        "   <hidden-offer market-sku=\"3\" offer-id=\"offer3\" ttl-in-hours=\"2\"/>" +
                        "   <hidden-offer market-sku=\"5\" offer-id=\"offer5\" comment=\"test\"/>" +
                        "</hidden-offers>";
        String response = performPost(10661, content, 200, Format.XML);
        String expectedResponse = "<response><status>OK</status></response>";
        MbiAsserts.assertXmlEquals(expectedResponse, response);
        Map<IndexerOfferKey, PapiHidingDetails> hiddenOffers =
                papiMarketSkuOfferService.hiddenOffersEntries(Collections.singleton(661661L), Paging.firstN(1000))
                        .stream()
                        .collect(Collectors.toMap(
                                PapiFeedMarketSkuHiding::feedMarketSku,
                                PapiFeedMarketSkuHiding::details));
        assertEquals(3, hiddenOffers.size());
        assertTrue(hiddenOffers.containsKey(IndexerOfferKey.offerId(661661L, "offer1")));
        assertTrue(hiddenOffers.containsKey(IndexerOfferKey.offerId(661661L, "offer3")));
        assertTrue(hiddenOffers.containsKey(IndexerOfferKey.offerId(661661L, "offer5")));
        assertEquals(Optional.empty(), hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer1")).comment());
        assertEquals(CLIENT_ID, hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer1")).clientId());
        assertEquals(Optional.empty(), hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer3")).comment());
        assertEquals(CLIENT_ID, hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer3")).clientId());
        assertEquals(Optional.of("test"), hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer5")).comment());
        assertEquals(CLIENT_ID, hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer5")).clientId());
    }

    @Test
    void testPostBasicJson() throws IOException {
        mockUltraControllerClient();
        String content =
                "{\n" +
                        "  \"hiddenOffers\" : [\n" +
                        "    {\n" +
                        "      \"marketSku\" : \"1\",\n" +
                        "      \"offerId\" : \"offer1\",\n" +
                        "      \"ttlInHours\" : 10\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"marketSku\" : \"3\",\n" +
                        "      \"offerId\" : \"offer3\",\n" +
                        "      \"ttlInHours\" : 2\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"marketSku\" : \"5\",\n" +
                        "      \"offerId\" : \"offer5\",\n" +
                        "      \"comment\" : \"test\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
        String response = performPost(10661, content, 200, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "  \"status\" : \"OK\"\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
        Map<IndexerOfferKey, PapiHidingDetails> hiddenOffers =
                papiMarketSkuOfferService.hiddenOffersEntries(Collections.singleton(661661L), Paging.firstN(1000))
                        .stream()
                        .collect(Collectors.toMap(
                                PapiFeedMarketSkuHiding::feedMarketSku,
                                PapiFeedMarketSkuHiding::details));
        assertEquals(3, hiddenOffers.size());
        assertTrue(hiddenOffers.containsKey(IndexerOfferKey.offerId(661661L, "offer1")));
        assertTrue(hiddenOffers.containsKey(IndexerOfferKey.offerId(661661L, "offer3")));
        assertTrue(hiddenOffers.containsKey(IndexerOfferKey.offerId(661661L, "offer5")));
        assertEquals(Optional.empty(), hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer1")).comment());
        assertEquals(325076, hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer1")).clientId());
        assertEquals(Optional.empty(), hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer3")).comment());
        assertEquals(325076, hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer3")).clientId());
        assertEquals(Optional.of("test"), hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer5")).comment());
        assertEquals(325076, hiddenOffers.get(IndexerOfferKey.offerId(661661L, "offer5")).clientId());
    }

    @Test
    @DbUnitDataSet(before = "hiddenOffersController.pushPartner.before.csv")
    void testPostBasicJsonForPushPartnerFeedExpansionDisabled() throws IOException {
        prepareMappingsForPostTests();
        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> logbrokerServiceEventCaptor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        doNothing().when(logbrokerService).publishEvent(logbrokerServiceEventCaptor.capture());
        String content = getContentForPushPartnerPostTests();
        String response = performPost(10661, content, 200, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "  \"status\" : \"OK\"\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
        Map<IndexerOfferKey, PapiHidingDetails> hiddenOffers =
                papiMarketSkuOfferService.hiddenOffersEntries(Collections.singleton(661661L), Paging.firstN(1000))
                        .stream()
                        .collect(Collectors.toMap(
                                PapiFeedMarketSkuHiding::feedMarketSku,
                                PapiFeedMarketSkuHiding::details));
        assertEquals(3, hiddenOffers.size());
        //Проверяем, что эвент был отправлен в Логброкер
        verify(logbrokerService, times(1)).publishEvent(any());
        var dataCampEvents = logbrokerServiceEventCaptor.getValue().getPayload();
        assertEquals(3, dataCampEvents.size());
    }

    private void prepareMappingsForPostTests() {
        List<UltraController.SKUMappingResponse.SKUMapping> MAPPING_LIST =
                List.of(
                        UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                .addShopSku("101")
                                .setMarketSkuId(1241412)
                                .build(),
                        UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                .addShopSku("111")
                                .addShopSku("112")
                                .setMarketSkuId(1241413)
                                .build()
                );
        Mockito.when(ultraControllerClient.getShopSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addAllSkuMapping(MAPPING_LIST)
                        .build());
    }

    private String getContentForPushPartnerPostTests() {
        return "{" +
                "   \"hiddenOffers\" : [" +
                "       {" +
                "           \"feedId\" : 661661," +
                "           \"marketSku\": 1241412" +
                "       }," +
                "       {" +
                "           \"feedId\" : 661661," +
                "           \"marketSku\": 1241413" +
                "       }" +
                "   ]" +
                "}";
    }

    @Test
    void testPostJsonErrors() throws IOException {
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"feedId\" : 661662," +
                        "           \"marketSku\" : 1" +
                        "       }," +
                        "       {" +
                        "           \"feedId\" : 661661," +
                        "           \"marketSku\" : 2" +
                        "       }," +
                        "       {" +
                        "           \"feedId\" : 661661," +
                        "           \"marketSku\" : 2" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performPost(10661, content, 400, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "   \"status\":\"ERROR\",\n" +
                        "   \"errors\":[\n" +
                        "      {\n" +
                        "         \"code\":\"INVALID_FEED_ID\",\n" +
                        "         \"message\":\"invalid feedId: 661662\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "         \"code\":\"DUPLICATE_OFFER\",\n" +
                        "         \"message\":\"duplicate {feedId=661661, marketSku=2} pair at positions [1, 2]\"\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    void testMissingFeedIdJsonErrors() throws IOException {
        mockUltraControllerClient();
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"marketSku\" : 1" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performPost(10661, content, 200, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "  \"status\" : \"OK\"\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    void testMissingIdentifiersJsonErrors() throws IOException {
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"feedId\" : 661661" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performPost(10661, content, 400, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "   \"status\":\"ERROR\",\n" +
                        "   \"errors\":[\n" +
                        "      {\n" +
                        "         \"code\":\"INVALID_MARKET_SKU\",\n" +
                        "         \"message\":\"invalid marketSku at position 0\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "         \"code\":\"MISSING_OFFER_ID\",\n" +
                        "         \"message\":\"missing offer-id at position 0\"\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    void testNoFeedsFoundJsonErrors() throws IOException {
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"marketSku\": 1732697839" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performPost(10662, content, 400, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "   \"status\":\"ERROR\",\n" +
                        "   \"errors\":[\n" +
                        "      {\n" +
                        "         \"code\":\"NOT_FOUND\",\n" +
                        "         \"message\":\"no feeds found\"\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    void testOfferIdInsteadOfMarketSku() throws IOException {
        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> logbrokerServiceEventCaptor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        doNothing().when(logbrokerService).publishEvent(logbrokerServiceEventCaptor.capture());

        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"feedId\" : 661661," +
                        "           \"offerId\": \"offer1\"" +
                        "       }" +
                        "   ]" +
                        "}";

        String response = performPost(10661, content, 200, Format.JSON);
        String expectedResponse = "{\"status\":\"OK\"}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
        verify(logbrokerService, times(1)).publishEvent(any());
        var dataCampEvents = logbrokerServiceEventCaptor.getValue().getPayload();
        Assertions.assertEquals(1, dataCampEvents.size());
        DataCampOffer.Offer offer = dataCampEvents.iterator().next().convertToDataCampOffer();
        Assertions.assertEquals("offer1", offer.getIdentifiers().getOfferId());
        Assertions.assertEquals(661, offer.getIdentifiers().getShopId());
        Assertions.assertEquals(661661, offer.getIdentifiers().getFeedId());
    }

    @Test
    void testEmptyDelete() throws IOException {
        mockUltraControllerClient();
        String content = prepareContentForDeleteTest();
        String response = performDelete(10661, content, 200, Format.JSON);
        String expectedResponse = "{\"status\":\"OK\"}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    void testBasicDelete() throws IOException {
        mockUltraControllerClient();
        hideOffersForDeleteTests();
        String content = prepareContentForDeleteTest();
        String response = performDelete(10661, content, 200, Format.JSON);
        String expectedResponse = "{\"status\":\"OK\"}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
        List<PapiFeedMarketSkuHiding> entries =
                papiMarketSkuOfferService.hiddenOffersEntries(Collections.singleton(661661L), Paging.firstN(1000));
        assertTrue(entries.isEmpty());
    }

    @Test
    void testDeleteFeedExpansionDisabled() throws IOException {
        mockUltraControllerClient();
        hideOffersForDeleteTests();
        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> logbrokerServiceEventCaptor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        doNothing().when(logbrokerService).publishEvent(logbrokerServiceEventCaptor.capture());
        String content = prepareContentForDeleteTest();
        String response = performDelete(10661, content, 200, Format.JSON);
        String expectedResponse = "{\"status\":\"OK\"}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
        List<PapiFeedMarketSkuHiding> entries =
                papiMarketSkuOfferService.hiddenOffersEntries(Collections.singleton(661661L), Paging.firstN(1000));
        assertTrue(entries.isEmpty());

        //Проверяем, что эвент был отправлен в Логброкер
        verify(logbrokerService, times(1)).publishEvent(any());
        var dataCampEvents = logbrokerServiceEventCaptor.getValue().getPayload();
        System.out.println("testRes " + dataCampEvents);
        assertEquals(2, dataCampEvents.size());
    }

    private void hideOffersForDeleteTests() {
        Instant now = Instant.now();
        papiMarketSkuOfferService.hideOffers(
                Collections.singletonMap(
                        IndexerOfferKey.offerId(661661L, "offer1"),
                        new PapiHidingEvent.Builder()
                                .setHiddenAt(now)
                                .setHidingExpiresAt(now.plus(Duration.ofDays(2)))
                                .build()),
                CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        papiMarketSkuOfferService.hideOffers(
                Collections.singletonMap(
                        IndexerOfferKey.offerId(661661L, "offer2"),
                        new PapiHidingEvent.Builder()
                                .setHiddenAt(now)
                                .setHidingExpiresAt(now.plus(Duration.ofDays(2)))
                                .build()),
                CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
    }

    private String prepareContentForDeleteTest() {
        return "{" +
                "   \"hiddenOffers\" : [" +
                "       {" +
                "           \"feedId\" : 661661," +
                "           \"marketSku\" : 1," +
                "           \"offerId\": \"offer1\"" +
                "       }," +
                "       {" +
                "           \"feedId\" : 661661," +
                "           \"marketSku\" : 2," +
                "           \"offerId\": \"offer2\"" +
                "       }" +
                "   ]" +
                "}";
    }

    @Test
    void testBasicGet() throws IOException {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(10, ChronoUnit.HOURS).minusSeconds(1);
        Map<IndexerOfferKey, PapiHidingEvent> hidings = new HashMap<>();
        hidings.put(IndexerOfferKey.offerId(661661L, "offer1"), new PapiHidingEvent.Builder()
                .setHiddenAt(now)
                .setHidingExpiresAt(expiresAt)
                .build());
        hidings.put(IndexerOfferKey.offerId(661661L, "offer2"), new PapiHidingEvent.Builder()
                .setHiddenAt(now)
                .setHidingExpiresAt(expiresAt)
                .build());
        hidings.put(IndexerOfferKey.offerId(661661L, "offer3"), new PapiHidingEvent.Builder()
                .setHiddenAt(now)
                .setHidingExpiresAt(expiresAt)
                .setComment("testComment")
                .build());
        hidings.put(IndexerOfferKey.offerId(661661L, "offer4"), new PapiHidingEvent.Builder()
                .setHiddenAt(now)
                .setHidingExpiresAt(expiresAt)
                .build());
        papiMarketSkuOfferService.hideOffers(hidings, 325076, CampaignType.SUPPLIER, DATASOURCE_ID);
        Multimap<String, Object> params = LinkedListMultimap.create();
        params.put("offset", 1);
        params.put("limit", 3);
        String response = performGet(10661, Format.JSON, 200, params);
        String expectedResponse =
                //language=json
                "{\n" +
                        "  \"status\": \"OK\",\n" +
                        "  \"result\": {\n" +
                        "    \"total\": 4,\n" +
                        "    \"paging\": {\"prevPageToken\": \"MA\", \"nextPageToken\": \"NA\"},\n" +
                        "    \"hiddenOffers\": [\n" +
                        "      {\n" +
                        "        \"offerId\": \"offer2\",\n" +
                        "        \"ttlInHours\": 9\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"offerId\": \"offer3\",\n" +
                        "        \"ttlInHours\": 9\n" +
                        ",        \"comment\": \"testComment\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"offerId\": \"offer4\",\n" +
                        "        \"ttlInHours\": 9\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }
}
