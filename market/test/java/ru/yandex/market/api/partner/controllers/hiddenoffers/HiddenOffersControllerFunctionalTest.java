package ru.yandex.market.api.partner.controllers.hiddenoffers;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.PapiHidingEvent;
import ru.yandex.market.core.offer.PapiMarketSkuOfferService;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Функциональные тесты на {@link HiddenOffersController}.
 */
class HiddenOffersControllerFunctionalTest extends AbstractHiddenOffersControllerFunctionalTest {
    private static final long CLIENT_ID = 678;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("papiMarketSkuOfferSeparatePoolService")
    private PapiMarketSkuOfferService papiMarketSkuOfferService;

    @Autowired
    @Qualifier("marketQuickLogbrokerService")
    private LogbrokerEventPublisher<SyncChangeOfferLogbrokerEvent> logbrokerService;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void setUp() {
        mockUltraControllerClient();
    }

    @Test
    @DbUnitDataSet(before = "hideOffers.before.csv", after = "hideOffers.after.csv")
    void testPostBasicXml() throws IOException {
        postBasicXmlContent(774, 10774, CampaignType.SHOP);
    }

    private void postBasicXmlContent(long shopId, long campaignId, CampaignType campaignType) throws IOException {
        addFeeds(shopId, 1069);
        setCampaignLimit(campaignId, 3);
        String content =
                "<hidden-offers>" +
                        "   <hidden-offer feed-id=\"1069\" offer-id=\"1\" ttl-in-hours=\"10\"/>" +
                        "   <hidden-offer feed-id=\"1069\" offer-id=\"3\" ttl-in-hours=\"2\"/>" +
                        "   <hidden-offer feed-id=\"1069\" offer-id=\"5\" comment=\"test\"/>" +
                        "</hidden-offers>";
        String response = performPost(campaignId, content, 200, Format.XML, "76573");
        String expectedResponse = "<response><status>OK</status></response>";
        MbiAsserts.assertXmlEquals(expectedResponse, response);
    }

    @Test
    @DbUnitDataSet(before = "hideOffers.before.csv", after = "hideOffers.after.csv")
    void testPostBasicJson() throws IOException {
        postBasicJson(774, 10774, CampaignType.SHOP, 1);
    }

    private void postBasicJson(long shopId, long campaignId, CampaignType campaignType, int times) throws IOException {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        addFeeds(shopId, 1069);
        setCampaignLimit(campaignId, 5);
        String content =
                "{\n" +
                        "  \"hiddenOffers\" : [\n" +
                        "    {\n" +
                        "      \"feedId\" : 1069,\n" +
                        "      \"offerId\" : \"1\",\n" +
                        "      \"ttlInHours\" : 10\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"feedId\" : 1069,\n" +
                        "      \"offerId\" : \"3\",\n" +
                        "      \"ttlInHours\" : 2\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"feedId\" : 1069,\n" +
                        "      \"offerId\" : \"5\",\n" +
                        "      \"comment\" : \"test\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
        String response = performPost(campaignId, content, 200, Format.JSON, "76573");
        String expectedResponse =
                "{\n" +
                        "  \"status\" : \"OK\"\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
        //Проверяем, что эвент был отправлен в Логброкер
        verify(logbrokerService, times(times)).publishEvent(Mockito.any());
    }

    @Test
    void testPostJsonErrors() throws IOException {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        addFeeds(774, 1069);
        setCampaignLimit(10774, 3);
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"feedId\" : 1070," +
                        "           \"offerId\" : \"offer1\"" +
                        "       }," +
                        "       {" +
                        "           \"feedId\" : 1069," +
                        "           \"offerId\" : \"offer2\"" +
                        "       }," +
                        "       {" +
                        "           \"feedId\" : 1069," +
                        "           \"offerId\" : \"offer2\"" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performPost(10774, content, 400, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "   \"status\":\"ERROR\",\n" +
                        "   \"errors\":[\n" +
                        "      {\n" +
                        "         \"code\":\"INVALID_FEED_ID\",\n" +
                        "         \"message\":\"invalid feedId: 1070\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "         \"code\":\"DUPLICATE_OFFER\",\n" +
                        "         \"message\":\"duplicate {feedId=1069, offerId=offer2} pair at positions [1, 2]\"\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
        //проверяем, что при непрошедшей валидации эвент в Логброкер не отправляется
        Mockito.verifyZeroInteractions(logbrokerService);
    }

    @Test
    void testMissingFeedIdJsonErrors() throws IOException {
        addFeeds(774, 1069);
        setCampaignLimit(10774, 3);
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"offerId\" : \"offer1\"" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performPost(10774, content, 400, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "   \"status\":\"ERROR\",\n" +
                        "   \"errors\":[\n" +
                        "      {\n" +
                        "         \"code\":\"INVALID_FEED_ID\",\n" +
                        "         \"message\":\"empty feed id\"\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    void testMissingOfferIdJsonErrors() throws IOException {
        addFeeds(774, 1069);
        setCampaignLimit(10774, 3);
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"feedId\" : 1069" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performPost(10774, content, 400, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "   \"status\":\"ERROR\",\n" +
                        "   \"errors\":[\n" +
                        "      {\n" +
                        "         \"code\":\"INVALID_OFFER_ID\",\n" +
                        "         \"message\":\"invalid offerId at position 0\"\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    void testMarketSkuInsteadOfOfferIdJsonErrors() throws IOException {
        addFeeds(774, 1069);
        setCampaignLimit(10774, 3);
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"feedId\" : 1069," +
                        "           \"marketSku\": 1241412" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performPost(10774, content, 400, Format.JSON);
        String expectedResponse =
                "{\n" +
                        "   \"status\":\"ERROR\",\n" +
                        "   \"errors\":[\n" +
                        "      {\n" +
                        "         \"code\":\"INVALID_OFFER_ID\",\n" +
                        "         \"message\":\"invalid offerId at position 0\"\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }


    @Test
    void testEmptyDelete() throws IOException {
        addFeeds(774, 1069);
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"feedId\" : 1069," +
                        "           \"offerId\" : \"offer1\"" +
                        "       }," +
                        "       {" +
                        "           \"feedId\" : 1069," +
                        "           \"offerId\" : \"offer2\"" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performDelete(10774, content, 200, Format.JSON);
        String expectedResponse = "{\"status\":\"OK\"}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    void testEmptyBodyDelete() throws IOException {
        addFeeds(774, 1069);
        String response = performDelete(10774, "{}", 200, Format.JSON);
        String expectedResponse = "{\"status\":\"OK\"}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    @DbUnitDataSet(before = "showOffers.before.csv", after = "showOffers.after.csv")
    void testBasicDelete() throws IOException {
        addFeeds(774, 1069);
        String content =
                "{" +
                        "   \"hiddenOffers\" : [" +
                        "       {" +
                        "           \"feedId\" : 1069," +
                        "           \"offerId\" : \"offer1\"" +
                        "       }," +
                        "       {" +
                        "           \"feedId\" : 1069," +
                        "           \"offerId\" : \"offer2\"" +
                        "       }" +
                        "   ]" +
                        "}";
        String response = performDelete(10774, content, 200, Format.JSON);
        String expectedResponse = "{\"status\":\"OK\"}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Сценарий получения скрытых офферов с фильтром по feed_id")
    void testBasicGet() throws IOException {
        prepareShopAndHiddenOffers();
        Multimap<String, Object> params = LinkedListMultimap.create();
        params.put("offset", 1);
        params.put("limit", 3);
        params.put("feed_id", 1069);
        params.put("feed_id", 1070);
        String response = performGet(10774, Format.JSON, 200, params);
        String expectedResponse = //language=json
                "{\n" +
                        "  \"status\": \"OK\",\n" +
                        "  \"result\": {\n" +
                        "    \"total\": 5,\n" +
                        "    \"paging\": {\"prevPageToken\": \"MA\", \"nextPageToken\": \"NA\"},\n" +
                        "    \"hiddenOffers\": [\n" +
                        "      {\n" +
                        "        \"feedId\": 1069,\n" +
                        "        \"offerId\": \"offer2\",\n" +
                        "        \"ttlInHours\": 9\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"feedId\": 1069,\n" +
                        "        \"offerId\": \"offer3\",\n" +
                        "        \"ttlInHours\": 9\n" +
                        ",        \"comment\": \"testCommnet\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"feedId\": 1069,\n" +
                        "        \"offerId\": \"offer4\",\n" +
                        "        \"ttlInHours\": 9,\n" +
                        "        \"comment\": \"Hided\"\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Сценарий получения скрытых предложений с фильтром по feed_id и offer_id")
    void testGetByFeedIdAndOfferId() throws IOException {
        prepareShopAndHiddenOffers();
        Multimap<String, Object> params = LinkedListMultimap.create();
        params.put("offset", 0);
        params.put("limit", 3);
        params.put("feed_id", 1069);
        params.put("offer_id", "offer2");
        params.put("offer_id", "offer4");
        String response = performGet(10774, Format.JSON, 200, params);
        String expectedResponse = //language=json
                "" +
                        "{\n" +
                        "  \"status\": \"OK\",\n" +
                        "  \"result\": {\n" +
                        "    \"total\": 2,\n" +
                        "    \"paging\": {},\n" +
                        "    \"hiddenOffers\": [\n" +
                        "      {\n" +
                        "        \"feedId\": 1069,\n" +
                        "        \"offerId\": \"offer2\",\n" +
                        "        \"ttlInHours\": 9\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"feedId\": 1069,\n" +
                        "        \"offerId\": \"offer4\",\n" +
                        "        \"ttlInHours\": 9,\n" +
                        "        \"comment\": \"Hided\"\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Получение всех скрытых предложений магазина")
    void getHiddenOffersWithoutParams() throws IOException {
        prepareShopAndHiddenOffers();
        Multimap<String, Object> params = LinkedListMultimap.create();
        params.put("offset", 0);
        params.put("limit", 10);
        String response = performGet(10774, Format.JSON, 200, params);
        String expectedResponse = //language=json
                "{\n" +
                        "  \"status\": \"OK\",\n" +
                        "  \"result\": {\n" +
                        "    \"total\": 5,\n" +
                        "    \"paging\": {},\n" +
                        "    \"hiddenOffers\": [\n" +
                        "      {\n" +
                        "        \"feedId\": 1069,\n" +
                        "        \"offerId\": \"offer1\",\n" +
                        "        \"ttlInHours\": 9\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"feedId\": 1069,\n" +
                        "        \"offerId\": \"offer2\",\n" +
                        "        \"ttlInHours\": 9\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"feedId\": 1069,\n" +
                        "        \"offerId\": \"offer3\",\n" +
                        "        \"ttlInHours\": 9,\n" +
                        "        \"comment\": \"testCommnet\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"feedId\": 1069,\n" +
                        "        \"offerId\": \"offer4\",\n" +
                        "        \"ttlInHours\": 9,\n" +
                        "        \"comment\": \"Hided\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"feedId\": 1070,\n" +
                        "        \"offerId\": \"offer5\",\n" +
                        "        \"ttlInHours\": 9\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}";
        MbiAsserts.assertJsonEquals(expectedResponse, response);
    }

    private void prepareShopAndHiddenOffers() {
        addFeeds(774, 1069, 1070);
        papiMarketSkuOfferService.hideOffers(
                new ImmutableMap.Builder<IndexerOfferKey, PapiHidingEvent>()
                        .put(IndexerOfferKey.offerId(1069, "offer1"), papiHidingEvent(null))
                        .put(IndexerOfferKey.offerId(1069, "offer2"), papiHidingEvent(null))
                        .put(IndexerOfferKey.offerId(1069, "offer3"), papiHidingEvent("testCommnet"))
                        .put(IndexerOfferKey.offerId(1069, "offer4"), papiHidingEvent("Hided"))
                        .put(IndexerOfferKey.offerId(1070, "offer5"), papiHidingEvent(null))
                        .put(IndexerOfferKey.offerId(1071, "offer1"), papiHidingEvent(null))
                        .build(),
                -1L,
                CampaignType.SHOP,
                774);
    }

    private PapiHidingEvent papiHidingEvent(@Nullable String comment) {
        Instant now = Instant.now();
        PapiHidingEvent.Builder builder = new PapiHidingEvent.Builder()
                .setHiddenAt(now)
                .setHidingExpiresAt(now.plus(10, ChronoUnit.HOURS));
        Optional.ofNullable(comment).ifPresent(builder::setComment);
        return builder.build();
    }

    private void addFeeds(final long shopId, final long... feedIds) {
        for (long feedId : feedIds) {
            jdbcTemplate.update("insert into shops_web.datafeed(url, datasource_id, id, is_enabled, site_type) values" +
                    " ('trash', ?, ?, 1, 0)", shopId, feedId);
        }
    }

    private void setCampaignLimit(long campaignId, int limit) {
        jdbcTemplate.update("insert into shops_web.hidden_offer_limits(campaign_id, limit_offers) values (?, ?)",
                campaignId, limit);
    }
}
