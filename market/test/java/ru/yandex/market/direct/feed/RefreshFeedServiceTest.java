package ru.yandex.market.direct.feed;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Clock;
import java.util.List;

import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampOfferMeta;
import com.sun.net.httpserver.HttpServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.direct.feed.RefreshFeedService;
import ru.yandex.market.core.direct.feed.RefreshFeedYtDao;
import ru.yandex.market.core.direct.feed.model.RefreshFeedRecord;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.feed.model.FeedInfo;
import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(
        before = "RefreshFeedTest.csv"
)
public class RefreshFeedServiceTest extends FunctionalTest {
    private static final int FEED_ID_1 = 16;
    private static final int BUSINESS_ID_1 = 14;
    private static final int PARTNER_ID_1 = 15;
    private static final int FEED_ID_4 = 46;
    private static final String FEED_1_PATH = "/feed_16";
    private static final String FEED_1_ETAG = "feed_16_etag";
    private static final String FEED_1_BODY = "feed_16_body";
    private static final String FEED_2_PATH = "/feed_26";
    private static final int BUSINESS_ID_2 = 24;
    private static final int PARTNER_ID_2 = 25;
    private static final int FEED_ID_2 = 26;
    private static final String FEED_2_ETAG = "feed_26_etag";
    private static final String FEED_4_PATH = "/feed_46";
    private static final int BUSINESS_ID_4 = 44;
    private static final int PARTNER_ID_4 = 45;

    private static final List<RefreshFeedRecord> TEST_REFRESH_FEEDS = List.of(
            RefreshFeedRecord.builder()
                    .setFeedId(FEED_ID_1)
                    .build(),
            RefreshFeedRecord.builder()
                    .setFeedId(26)
                    .setEtag("feed_26_etag")
                    .build(),
            RefreshFeedRecord.builder()
                    .setFeedId(FEED_ID_4)
                    .setHash("ZmVlZF80Nl9ib2R5")
                    .build()
    );

    private int serverPort;
    private HttpServer server;

    @Autowired
    private Clock clock;


    private FeedService feedService;


    private BusinessService businessService;

    @Autowired
    private RefreshFeedYtDao refreshFeedYtDao;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;


    private RefreshFeedService refreshFeedService;

    @BeforeEach
    void setup() {
        feedService = Mockito.mock(FeedService.class);
        businessService = Mockito.mock(BusinessService.class);
        Mockito.when(refreshFeedYtDao.getAllRefreshRecords()).thenReturn(TEST_REFRESH_FEEDS);

        refreshFeedService = new RefreshFeedService(
                clock,
                refreshFeedYtDao,
                feedService,
                applicationEventPublisher,
                businessService
        );

        serverPort = setupServerOnFreePort();
    }

    @Test
    void testNewFeed200() {
        String url = "http://localhost:" + serverPort + FEED_1_PATH;
        setupTestFeedInfo(BUSINESS_ID_1, PARTNER_ID_1, FEED_ID_1, url, null, null, FeedSiteType.MARKET);
        setupTestServerResponse(FEED_1_PATH, FEED_1_ETAG, FEED_1_BODY, 200);
        refreshFeedService.refreshFeeds(1);

        ArgumentCaptor<FeedProcessorUpdateRequestEvent> eventCaptor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1)).publishEvent(eventCaptor.capture());

        FeedUpdateTaskOuterClass.FeedUpdateTask payload = eventCaptor.getValue().getPayload();
        ProtoTestUtil.assertThat(payload)
                .ignoringExpectedNullFields()
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setBusinessId(BUSINESS_ID_1)
                                .setFeedId(FEED_ID_1)
                                .setShopId(PARTNER_ID_1)
                                .setUpload(true)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .setCampaignType("DIRECT")
                                .setUrl(url)
                                .build())
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                        .setIsRegularParsing(true)
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .build()
        );

        ArgumentCaptor<RefreshFeedRecord> refreshCaptor =
                ArgumentCaptor.forClass(RefreshFeedRecord.class);
        verify(refreshFeedYtDao, times(1)).saveRefreshRecord(refreshCaptor.capture());
        Assertions.assertThat(refreshCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(RefreshFeedRecord.builder()
                        .setFeedId(FEED_ID_1)
                        .setEtag(FEED_1_ETAG)
                        .setHttpCode(200)
                        .build()
                );
    }

    @Test
    void testNewFeed400() {
        String url = "http://localhost:" + serverPort + FEED_1_PATH;
        setupTestFeedInfo(BUSINESS_ID_1, PARTNER_ID_1, FEED_ID_1, url, null, null, FeedSiteType.MARKET);
        setupTestServerResponse(FEED_1_PATH, FEED_1_ETAG, FEED_1_BODY, 400);
        refreshFeedService.refreshFeeds(1);

        verify(feedProcessorUpdateLogbrokerEventPublisher, never()).publishEvent(any());

        ArgumentCaptor<RefreshFeedRecord> refreshCaptor =
                ArgumentCaptor.forClass(RefreshFeedRecord.class);
        verify(refreshFeedYtDao, times(1)).saveRefreshRecord(refreshCaptor.capture());

        Assertions.assertThat(refreshCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(RefreshFeedRecord.builder()
                        .setFeedId(FEED_ID_1)
                        .setHttpCode(400)
                        .setErrorMessage("Unsupported http code: 400")
                        .build()
                );
    }

    @Test
    void testExistingFeedWithEtag304() {
        String url = "http://localhost:" + serverPort + FEED_2_PATH;
        setupTestFeedInfo(BUSINESS_ID_2, PARTNER_ID_2, FEED_ID_2, url, null, null, FeedSiteType.MARKET);
        setupTestServerResponse(FEED_2_PATH, FEED_2_ETAG, "feed_26_body", 304);

        refreshFeedService.refreshFeeds(1);

        verify(feedProcessorUpdateLogbrokerEventPublisher, never()).publishEvent(any());

        ArgumentCaptor<RefreshFeedRecord> refreshCaptor =
                ArgumentCaptor.forClass(RefreshFeedRecord.class);
        verify(refreshFeedYtDao, times(1)).saveRefreshRecord(refreshCaptor.capture());

        Assertions.assertThat(refreshCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(RefreshFeedRecord.builder()
                        .setFeedId(FEED_ID_2)
                        .setEtag("feed_26_etag")
                        .setHttpCode(304)
                        .build()
                );
    }

    @Test
    void testExistingFeedWithEtag200() {
        String url = "http://localhost:" + serverPort + FEED_2_PATH;
        setupTestFeedInfo(BUSINESS_ID_2, PARTNER_ID_2, FEED_ID_2, url, null, null, FeedSiteType.MARKET);
        setupTestServerResponse(FEED_2_PATH, "feed_26_etag", "feed_26_body", 200);

        refreshFeedService.refreshFeeds(1);

        ArgumentCaptor<FeedProcessorUpdateRequestEvent> eventCaptor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1)).publishEvent(eventCaptor.capture());

        FeedUpdateTaskOuterClass.FeedUpdateTask payload = eventCaptor.getValue().getPayload();
        ProtoTestUtil.assertThat(payload)
                .ignoringExpectedNullFields()
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setBusinessId(BUSINESS_ID_2)
                                .setFeedId(FEED_ID_2)
                                .setShopId(PARTNER_ID_2)
                                .setUpload(true)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .setCampaignType("DIRECT")
                                .setUrl(url)
                                .build())
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                        .setIsRegularParsing(true)
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .build()
                );

        ArgumentCaptor<RefreshFeedRecord> refreshCaptor =
                ArgumentCaptor.forClass(RefreshFeedRecord.class);
        verify(refreshFeedYtDao, times(1)).saveRefreshRecord(refreshCaptor.capture());

        Assertions.assertThat(refreshCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(RefreshFeedRecord.builder()
                        .setFeedId(FEED_ID_2)
                        .setEtag("feed_26_etag")
                        .setHttpCode(200)
                        .build()
                );
    }

    @Test
    void testExistingFeedWithSameHash200() {
        String url = "http://localhost:" + serverPort + FEED_4_PATH;
        setupTestFeedInfo(BUSINESS_ID_4, PARTNER_ID_4, FEED_ID_4, url, null, null, FeedSiteType.MARKET);
        setupTestServerResponse(FEED_4_PATH, null, "feed_46_body", 200);

        refreshFeedService.refreshFeeds(1);
        verify(feedProcessorUpdateLogbrokerEventPublisher, never()).publishEvent(any());

        ArgumentCaptor<RefreshFeedRecord> refreshCaptor =
                ArgumentCaptor.forClass(RefreshFeedRecord.class);
        verify(refreshFeedYtDao, times(1)).saveRefreshRecord(refreshCaptor.capture());

        Assertions.assertThat(refreshCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(RefreshFeedRecord.builder()
                        .setFeedId(FEED_ID_4)
                        .setHash("ZmVlZF80Nl9ib2R5")
                        .setHttpCode(200)
                        .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "RefreshFeedTest.directFeed.before.csv")
    void testExistingFeedWithOtherHash200() {
        String url = "http://localhost:" + serverPort + FEED_4_PATH;
        setupTestFeedInfo(BUSINESS_ID_4, PARTNER_ID_4, FEED_ID_4, url, null, null, FeedSiteType.EXTERNAL_MDS_FILE);
        setupTestServerResponse(FEED_4_PATH, null, "feed_46_body_2", 200);

        refreshFeedService.refreshFeeds(1);
        ArgumentCaptor<FeedProcessorUpdateRequestEvent> eventCaptor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1)).publishEvent(eventCaptor.capture());

        FeedUpdateTaskOuterClass.FeedUpdateTask payload = eventCaptor.getValue().getPayload();
        ProtoTestUtil.assertThat(payload)
                .ignoringExpectedNullFields()
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setBusinessId(BUSINESS_ID_4)
                                .setFeedId(FEED_ID_4)
                                .setShopId(PARTNER_ID_4)
                                .setUpload(true)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .setCampaignType("DIRECT")
                                .setUrl(url)
                                .build())
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setIsUpload(true)
                                .setClientId(123)
                                .setColor(DataCampOfferMeta.MarketColor.DIRECT)
                                .setDirectFeedId(26)
                                .build())
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                        .setIsRegularParsing(true)
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .build()
                );

        ArgumentCaptor<RefreshFeedRecord> refreshCaptor =
                ArgumentCaptor.forClass(RefreshFeedRecord.class);
        verify(refreshFeedYtDao, times(1)).saveRefreshRecord(refreshCaptor.capture());

        Assertions.assertThat(refreshCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(RefreshFeedRecord.builder()
                        .setFeedId(FEED_ID_4)
                        .setHash("ZmVlZF80Nl9ib2R5XzI=")
                        .setHttpCode(200)
                        .build()
                );
    }

    private int setupServerOnFreePort() {
        int port = getFreePort();
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return port;
    }

    private int getFreePort() {
        ServerSocket socket;
        try {
            socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupTestServerResponse(String path,  String etag, String body, int code) {
        server.createContext(path, httpExchange -> {
            if (etag != null) {
                httpExchange.getResponseHeaders().add("Etag", etag);
            }
            httpExchange.sendResponseHeaders(code, body.length());
            httpExchange.getResponseBody().write(body.getBytes());
        });
    }

    private void setupTestFeedInfo(
            long businessId, long partnerId, long feedId,
            String url, String login, String password,
            FeedSiteType siteType
    ) {
        FeedInfo feedInfo = new FeedInfo();
        feedInfo.setId(feedId);
        feedInfo.setDatasourceId(partnerId);
        feedInfo.setUrl(url.replace("{port}", String.valueOf(serverPort)));
        feedInfo.setLogin(login);
        feedInfo.setPassword(password);
        feedInfo.setSiteType(siteType);
        Mockito.when(feedService.getFeed(feedId))
                .thenReturn(feedInfo);
        Mockito.when(businessService.getBusinessIdByPartner(partnerId))
                .thenReturn(businessId);
    }
}
