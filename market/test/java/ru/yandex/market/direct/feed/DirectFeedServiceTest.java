package ru.yandex.market.direct.feed;

import java.util.List;

import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampOfferMeta;
import NCrawl.Feeds;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.direct.feed.DirectFeedService;
import ru.yandex.market.core.direct.feed.RefreshFeedYtDao;
import ru.yandex.market.core.direct.feed.model.FullDirectFeedInfo;
import ru.yandex.market.core.direct.feed.model.RefreshFeedRecord;
import ru.yandex.market.core.feature.FeatureCutoffService;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.core.logbroker.samovar.SamovarEvent;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.mbi.util.net.MbiUrlBuilder;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author moskovkin@yandex-team.ru
 * @since 24.12.2020
 */
@DbUnitDataSet(
        before = {
                "DirectFeedTest.csv",
                "DirectFeedTest.campaigns.before.csv"
        }
)
class DirectFeedServiceTest extends FunctionalTest {
    private static final long DIRECT_CLIENT_ID_1 = 11;
    private static final long DIRECT_FEED_1 = 12;
    private static final long DIRECT_OWNER_1 = 13;
    private static final long BUSINESS_ID_1 = 14;
    private static final long PARTNER_ID_1 = 15;
    private static final long FEED_ID_1 = 16;
    private static final String URL_1 = "http://test.me/feed";
    private static final String LOGIN_1 = "login1";
    private static final String PASSWORD_1 = "password1";

    // new feed
    private static final String URL_3 = "http://market.yandex.ru";
    private static final String LOGIN_3 = "login3";
    private static final String PASSWORD_3 = "password3";
    private static final long DIRECT_CLIENT_ID_3 = 21;
    private static final long DIRECT_FEED_3 = 22;
    private static final long DIRECT_OWNER_3 = 23;

    // existing feed
    private static final String URL_4 = "http://test.me/feed4";
    private static final long DIRECT_CLIENT_ID_4 = 41;
    private static final long DIRECT_FEED_4 = 42;
    private static final long DIRECT_OWNER_4 = 43;
    private static final long BUSINESS_ID_4 = 44;

    private static final long ACTION_ID = 1L;
    private static final String SITE_PARSE_PIPELINE = "test-feed-site-processing";

    @Autowired
    private DirectFeedService directFeedService;

    @Autowired
    @Qualifier("samovarLogbrokerService")
    private LogbrokerService samovarLogbrokerService;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @Autowired
    private PassportService passportService;

    @Autowired
    private RefreshFeedYtDao refreshFeedYtDao;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private FeatureCutoffService featureCutoffService;

    @BeforeEach
    void setup() {
        mockPassportUserParams("User 13", "user13", DIRECT_OWNER_1);
        mockPassportUserParams("User 14", "user14", DIRECT_OWNER_3);
    }

    @Test
    void testRefreshExistingFeedSendSamovarEvent() throws InvalidProtocolBufferException {
        directFeedService.refreshDirectFeed(DIRECT_CLIENT_ID_1, DIRECT_OWNER_1, DIRECT_FEED_1, URL_1, LOGIN_1, PASSWORD_1, true, ACTION_ID);

        var argumentCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1)).publishEvent(argumentCaptor.capture());

        FeedUpdateTaskOuterClass.FeedUpdateTask payload = argumentCaptor.getValue().getPayload();
        ProtoTestUtil.assertThat(payload)
                .ignoringFieldsMatchingRegexes(".*requestedAt.*")
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setShopId(PARTNER_ID_1)
                                .setCampaignType("DIRECT")
                                .setFeedId(FEED_ID_1)
                                .setUrl(URL_1)
                                .setBusinessId(BUSINESS_ID_1)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .setLogin(LOGIN_1)
                                .setPassword(PASSWORD_1)
                                .build())
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.DIRECT)
                                .setIsUpload(false)
                                .setDirectFeedId(12)
                                .setClientId(11)
                                .setDirectStandby(false)
                                .setDirectGoodsAds(false)
                                .setDirectSearchSnippetGallery(false)
                                .setVerticalShare(false)
                                .build())
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                        .setIsRegularParsing(true)
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .build()
                );
        assertThat(payload.getRequestedAt().isInitialized()).isTrue();
    }

    @Test
    void testRefreshNewBusinessFeedSendSamoverEvent() throws InvalidProtocolBufferException {
        directFeedService.refreshDirectFeed(DIRECT_CLIENT_ID_3, DIRECT_OWNER_3, DIRECT_FEED_3, URL_3, LOGIN_3, PASSWORD_3, true, ACTION_ID);

        var argumentCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1)).publishEvent(argumentCaptor.capture());

        FeedUpdateTaskOuterClass.FeedUpdateTask payload = argumentCaptor.getValue().getPayload();
        ProtoTestUtil.assertThat(payload)
                .ignoringFieldsMatchingRegexes(".*requestedAt.*", ".*shopId.*", ".*businessId.*", ".*feedId.*")
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setCampaignType("DIRECT")
                                .setUrl(URL_3)
                                .setLogin(LOGIN_3)
                                .setPassword(PASSWORD_3)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .build())
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.DIRECT)
                                .setDirectFeedId(22)
                                .setClientId(21)
                                .setIsUpload(false)
                                .setDirectStandby(true)
                                .setDirectGoodsAds(false)
                                .setDirectSearchSnippetGallery(false)
                                .setVerticalShare(false)
                                .build())
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                        .setIsRegularParsing(true)
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .build()
                );
        assertThat(payload.getRequestedAt().isInitialized()).isTrue();
        assertThat(payload.getFeed().getBusinessId()).isNotEqualTo(0);
        assertThat(payload.getFeed().getShopId()).isNotEqualTo(0);
        assertThat(payload.getFeed().getFeedId()).isNotEqualTo(0);
    }

    @Test
    void testRefreshNewFeedDoNotOpenCutoff() {
        FullDirectFeedInfo feedInfo = directFeedService.refreshDirectFeed(
                DIRECT_CLIENT_ID_3, DIRECT_OWNER_3, DIRECT_FEED_3, URL_3, LOGIN_3, PASSWORD_3, true, ACTION_ID
        );

        assertThat(featureService.getFeatureInfo(feedInfo.getPartnerId(), FeatureType.DIRECT_STATUS).getStatus())
                .isEqualTo(ParamCheckStatus.SUCCESS);
        assertThat(featureService.getFeatureInfo(feedInfo.getPartnerId(), FeatureType.DIRECT_STANDBY).getStatus())
                .isEqualTo(ParamCheckStatus.SUCCESS);
    }

    @Test
    void testRefreshNewFeedOpenCutoff() {
        FullDirectFeedInfo feedInfo = directFeedService.refreshDirectFeed(
                DIRECT_CLIENT_ID_3, DIRECT_OWNER_3, DIRECT_FEED_3, URL_3, LOGIN_3, PASSWORD_3, false, ACTION_ID
        );

        assertThat(featureService.getFeatureInfo(feedInfo.getPartnerId(), FeatureType.DIRECT_STATUS).getStatus())
                .isEqualTo(ParamCheckStatus.DONT_WANT);
    }

    @Test
    void testRefreshExistingFeedCloseCutoff() {
        assertThat(featureService.getFeatureInfo(PARTNER_ID_1, FeatureType.DIRECT_STATUS).getStatus())
                .isEqualTo(ParamCheckStatus.DONT_WANT);

        directFeedService.refreshDirectFeed(
                DIRECT_CLIENT_ID_1, DIRECT_OWNER_1, DIRECT_FEED_1, URL_1, LOGIN_1, PASSWORD_1, true, ACTION_ID
        );

        assertThat(featureService.getFeatureInfo(PARTNER_ID_1, FeatureType.DIRECT_STATUS).getStatus())
                .isEqualTo(ParamCheckStatus.SUCCESS);
    }

    @Test
    void testRefreshExistingFeedOpenCutoff() {
        featureCutoffService.closeCutoff(1, PARTNER_ID_1, FeatureType.DIRECT_STATUS, FeatureCutoffType.PARTNER);
        assertThat(featureService.getFeatureInfo(PARTNER_ID_1, FeatureType.DIRECT_STATUS).getStatus())
                .isEqualTo(ParamCheckStatus.SUCCESS);

        directFeedService.refreshDirectFeed(
                DIRECT_CLIENT_ID_1, DIRECT_OWNER_1, DIRECT_FEED_1, URL_1, LOGIN_1, PASSWORD_1, false, ACTION_ID
        );

        assertThat(featureService.getFeatureInfo(PARTNER_ID_1, FeatureType.DIRECT_STATUS).getStatus())
                .isEqualTo(ParamCheckStatus.DONT_WANT);
    }

    @Test
    void testRegisterDatacampUrlFeedDontSendSamovarEvent() throws InvalidProtocolBufferException {
        directFeedService.registerDatacampUrlFeed(
                URL_3, LOGIN_3, PASSWORD_3, null, DIRECT_OWNER_3, List.of(), null, List.of(), RegionConstants.RUSSIA, ACTION_ID
        );
        // нет шопдаты, событие не отправляется
        verify(feedProcessorUpdateLogbrokerEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testRegisterExistingDatacampUrlFeedSendSamovarEvent() throws InvalidProtocolBufferException {
        directFeedService.registerDatacampUrlFeed(
                URL_1, LOGIN_1, PASSWORD_1, BUSINESS_ID_1, DIRECT_OWNER_1, List.of(), null, List.of(), RegionConstants.RUSSIA, ACTION_ID
        );

        var argumentCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1)).publishEvent(argumentCaptor.capture());

        FeedUpdateTaskOuterClass.FeedUpdateTask payload = argumentCaptor.getValue().getPayload();
        ProtoTestUtil.assertThat(payload)
                .ignoringFieldsMatchingRegexes(".*requestedAt.*")
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setShopId(PARTNER_ID_1)
                                .setCampaignType("DIRECT")
                                .setFeedId(FEED_ID_1)
                                .setUrl(URL_1)
                                .setLogin(LOGIN_1)
                                .setPassword(PASSWORD_1)
                                .setBusinessId(BUSINESS_ID_1)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .build())
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.DIRECT)
                                .setDirectFeedId(12)
                                .setClientId(11)
                                .setDirectStandby(false)
                                .setDirectGoodsAds(false)
                                .setDirectSearchSnippetGallery(false)
                                .setVerticalShare(false)
                                .setIsUpload(false)
                                .build())
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                        .setIsRegularParsing(true)
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .build()
                );
        assertThat(payload.getRequestedAt().isInitialized()).isTrue();
    }


    @Test
    void testRefreshNewSiteSendSamoverEvent() throws InvalidProtocolBufferException {
        directFeedService.refreshDirectSite(DIRECT_CLIENT_ID_3, DIRECT_OWNER_3, DIRECT_FEED_3, URL_3, true, ACTION_ID);

        ArgumentCaptor<SamovarEvent> argumentCaptor = ArgumentCaptor.forClass(SamovarEvent.class);
        verify(samovarLogbrokerService, times(1)).publishEvent(argumentCaptor.capture());

        Feeds.TFeedExt payload = argumentCaptor.getValue().getPayload();
        assertThat(payload)
                .isEqualToComparingOnlyGivenFields(
                        Feeds.TFeedExt.newBuilder()
                                .setFeedName(SITE_PARSE_PIPELINE)
                                .setUrl(URL_3)
                                .build(),
                        "url", "feedName"
                );

        SamovarContextOuterClass.SamovarContext samovarContext = SamovarContextOuterClass.SamovarContext.parseFrom(
                payload.getFeedContext().getBytesValue()
        );
        ProtoTestUtil.assertThat(samovarContext)
                .ignoringFields(
                        "requestId_",
                        "forceRefreshStart_",
                        "environment_",
                        "memoizedHashCode",
                        "feeds_.updatedAt_",
                        "feeds_.memoizedIsInitialized",
                        "feeds_.memoizedHashCode",

                        // We do not know exact values of this fields.
                        // Just check what this fields is not empty later.
                        "feeds_.businessId_",
                        "feeds_.feedId_",
                        "feeds_.shopId_",
                        "feeds_.directStandBy_"
                )
                .isEqualTo(SamovarContextOuterClass.SamovarContext.newBuilder()
                        .addFeeds(
                                SamovarContextOuterClass.FeedInfo.newBuilder()
                                        .setCampaignType("DIRECT")
                                        .setUrl(URL_3)
                                        .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                                .setColor(DataCampOfferMeta.MarketColor.DIRECT)
                                                .setDirectFeedId(22)
                                                .setClientId(21)
                                                .setIsUpload(false)
                                                .setDirectStandby(true)
                                                .setDirectGoodsAds(false)
                                                .setDirectSearchSnippetGallery(false)
                                                .setVerticalShare(false)
                                                .build())
                                        .build()
                        )
                        .build()
                );
        assertThat(samovarContext.getForceRefreshStart().isInitialized()).isTrue();

        assertThat(samovarContext.getFeedsList()).hasSize(1);
        assertThat(samovarContext.getFeeds(0).getBusinessId()).isNotEqualTo(0);
        assertThat(samovarContext.getFeeds(0).getShopId()).isNotEqualTo(0);
        assertThat(samovarContext.getFeeds(0).getFeedId()).isNotEqualTo(0);
    }

    @Test
    void testRegisterNewDatacampSiteFeedSendSamoverEvent() throws InvalidProtocolBufferException {
        directFeedService.registerDatacampSiteFeed(URL_3,
                null,
                DIRECT_OWNER_3,
                List.of(new DirectFeedService.FeatureState(FeatureType.VERTICAL_SHARE, true)),
                null,
                List.of(),
                RegionConstants.RUSSIA,
                ACTION_ID);

        ArgumentCaptor<SamovarEvent> argumentCaptor = ArgumentCaptor.forClass(SamovarEvent.class);
        verify(samovarLogbrokerService, times(1)).publishEvent(argumentCaptor.capture());

        Feeds.TFeedExt payload = argumentCaptor.getValue().getPayload();
        assertThat(payload)
                .isEqualToComparingOnlyGivenFields(
                        Feeds.TFeedExt.newBuilder()
                                .setFeedName(SITE_PARSE_PIPELINE)
                                .setUrl(URL_3)
                                .build(),
                        "url", "feedName"
                );

        SamovarContextOuterClass.SamovarContext samovarContext = SamovarContextOuterClass.SamovarContext.parseFrom(
                payload.getFeedContext().getBytesValue()
        );
        assertThat(samovarContext)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .ignoringFields(
                        "requestId_",
                        "forceRefreshStart_",
                        "environment_",
                        "memoizedHashCode",
                        "feeds_.updatedAt_",
                        "feeds_.memoizedIsInitialized",
                        "feeds_.memoizedHashCode",
                        "feeds_.shopsDatParameters_.memoizedIsInitialized",
                        "feeds_.shopsDatParameters_.memoizedHashCode",

                        // We do not know exact values of this fields.
                        // Just check what this fields is not empty later.
                        "feeds_.businessId_",
                        "feeds_.feedId_",
                        "feeds_.shopId_"
                )
                .isEqualTo(SamovarContextOuterClass.SamovarContext.newBuilder()
                        .addFeeds(
                                SamovarContextOuterClass.FeedInfo.newBuilder()
                                        .setCampaignType("DIRECT")
                                        .setUrl(URL_3)
                                        .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                        .setVerticalShare(true)
                                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                                .setColor(DataCampOfferMeta.MarketColor.DIRECT)
                                                .setIsUpload(false)
                                                .setDirectStandby(false)
                                                .setDirectGoodsAds(false)
                                                .setDirectSearchSnippetGallery(false)
                                                .build())
                                        .build()
                        )
                        .build()
                );
        assertThat(samovarContext.getForceRefreshStart().isInitialized()).isTrue();

        assertThat(samovarContext.getFeedsList()).hasSize(1);
        assertThat(samovarContext.getFeeds(0).getBusinessId()).isNotEqualTo(0);
        assertThat(samovarContext.getFeeds(0).getShopId()).isNotEqualTo(0);
        assertThat(samovarContext.getFeeds(0).getFeedId()).isNotEqualTo(0);
    }

    @Test
    void testRefreshNewFileRegisterRefreshFeed() {
        FullDirectFeedInfo fullDirectFeedInfo = directFeedService
                .refreshDirectFile(DIRECT_CLIENT_ID_3, DIRECT_OWNER_3, DIRECT_FEED_3, URL_3, true, true, ACTION_ID);

        ArgumentCaptor<RefreshFeedRecord> argumentCaptor =
                ArgumentCaptor.forClass(RefreshFeedRecord.class);
        verify(refreshFeedYtDao, times(1)).saveRefreshRecord(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue()).isEqualTo(
                RefreshFeedRecord.builder()
                        .setFeedId(fullDirectFeedInfo.getFeedId())
                        .setEtag("SOME_VALUE_FOR_YT")
                        .build()
        );
    }

    @Test
    @DbUnitDataSet(
            after = "DirectFeedTest.testRefreshNewFileRefreshNewFeed.after.csv"
    )
    void testRefreshNewFileRefreshNewFeed() {
        directFeedService.refreshDirectFeed(DIRECT_CLIENT_ID_1, DIRECT_OWNER_1, DIRECT_FEED_1, URL_1, null, null, true, ACTION_ID);
        verify(refreshFeedYtDao).deleteRefreshRecord(any());
        directFeedService.refreshDirectFile(DIRECT_CLIENT_ID_1, DIRECT_OWNER_1, DIRECT_FEED_1, URL_1, true, true, ACTION_ID);
        verify(refreshFeedYtDao).saveRefreshRecord(any());
    }

    @Test
    void testRefreshExistsMarketFeed() {
        FullDirectFeedInfo fullDirectFeedInfo = directFeedService
                .refreshDirectFile(DIRECT_CLIENT_ID_1, DIRECT_OWNER_1, DIRECT_FEED_1, URL_1, true, true, ACTION_ID);

        var argumentCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1)).publishEvent(argumentCaptor.capture());

        FeedUpdateTaskOuterClass.FeedUpdateTask payload = argumentCaptor.getValue().getPayload();
        ProtoTestUtil.assertThat(payload)
                .ignoringFieldsMatchingRegexes(".*requestedAt.*")
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setUrl(URL_1)
                                .setBusinessId(14)
                                .setCampaignType("DIRECT")
                                .setUpload(true)
                                .setShopId(15)
                                .setFeedId(16)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .build())
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                        .setIsRegularParsing(true)
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.DIRECT)
                                .setDirectFeedId(DIRECT_FEED_1)
                                .setClientId(DIRECT_CLIENT_ID_1)
                                .setDirectStandby(false)
                                .setIsUpload(true)
                                .build()
                        )
                        .build());
    }

    @Test
    void testRefreshNewFileSendDatacampEvent() {
        directFeedService.refreshDirectFile(DIRECT_CLIENT_ID_3, DIRECT_OWNER_3, DIRECT_FEED_3, URL_3, false, true, ACTION_ID);
        var argumentCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1)).publishEvent(argumentCaptor.capture());

        FeedUpdateTaskOuterClass.FeedUpdateTask payload = argumentCaptor.getValue().getPayload();
        ProtoTestUtil.assertThat(payload)
                .ignoringFieldsMatchingRegexes(".*businessId.*", ".*feedId.*", ".*shopId.*", ".*requestedAt.*")
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setUrl(URL_3)
                                .setCampaignType("DIRECT")
                                .setUpload(true)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .build())
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.DIRECT)
                                .setDirectFeedId(DIRECT_FEED_3)
                                .setClientId(DIRECT_CLIENT_ID_3)
                                .setDirectStandby(true)
                                .setIsUpload(true)
                                .build()
                        )
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                        .setIsRegularParsing(false)
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .build());
        assertThat(payload.getFeed().getBusinessId()).isNotEqualTo(0);
        assertThat(payload.getFeed().getShopId()).isNotEqualTo(0);
        assertThat(payload.getFeed().getFeedId()).isNotEqualTo(0);
    }

    @Test
    @DbUnitDataSet(
            after = "DirectFeedTest.csv"
    )
    void testRefreshExistingFeedDoNotChangeDbState() {
        directFeedService.refreshDirectFeed(DIRECT_CLIENT_ID_1, DIRECT_OWNER_1, DIRECT_FEED_1, URL_1, LOGIN_1, PASSWORD_1, false, ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            after = "DirectFeedTest.csv"
    )
    void testRefreshExistingSiteDoNotChangeDbState() {
        directFeedService.refreshDirectSite(DIRECT_CLIENT_ID_4, DIRECT_OWNER_4, DIRECT_FEED_4, URL_4, false, ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            after = {"DirectFeedTest.csv", "DirectFeedTest.testRefreshNewFeedCreateRequiredObjects.csv"}
    )
    void testRefreshNewFeedCreateRequiredObjects() {
        directFeedService.refreshDirectFeed(DIRECT_CLIENT_ID_3, DIRECT_OWNER_3, DIRECT_FEED_3, URL_3, LOGIN_3, PASSWORD_3, true, ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            after = {"DirectFeedTest.csv", "DirectFeedTest.testRegisterSiteFeedCreateRequiredObjects.csv"}
    )
    void testRegisterSiteFeedCreateRequiredObjects() {
        directFeedService.registerDatacampSiteFeed(
                URL_3, null, DIRECT_OWNER_3,
                List.of(new DirectFeedService.FeatureState(FeatureType.VERTICAL_SHARE, true)), null, List.of(), RegionConstants.RUSSIA, ACTION_ID
        );
    }

    @Test
    @DbUnitDataSet(
            after = "DirectFeedTest.csv"
    )
    void testRegisterExistingSiteFeedDoNotChangeDbState() {
        directFeedService.registerDatacampSiteFeed(URL_4, BUSINESS_ID_4, null, List.of(), null, List.of(), RegionConstants.RUSSIA, ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            after = "DirectFeedTest.csv"
    )
    void testRegisterExistingUrlFeedDoNotChangeDbState() {
        directFeedService.registerDatacampUrlFeed(
                URL_1, null, null, BUSINESS_ID_1, null, List.of(), null, List.of(), RegionConstants.RUSSIA, ACTION_ID
        );
    }

    @Test
    void testRegisterExistingUrlFeedCheckFeedType() {
        assertThatThrownBy(() ->
                directFeedService.registerDatacampUrlFeed(
                        URL_4, null, null, BUSINESS_ID_4, null, List.of(), null, List.of(), RegionConstants.RUSSIA, ACTION_ID
                )
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRegisterExistingSiteFeedCheckFeedType() {
        assertThatThrownBy(() ->
                directFeedService.registerDatacampSiteFeed(
                        URL_1, BUSINESS_ID_1, null, List.of(), null, List.of(), RegionConstants.RUSSIA, ACTION_ID
                )
        )
                .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    @DbUnitDataSet(
            after = {"DirectFeedTest.csv", "DirectFeedTest.testRegisterUrlFeedCreateRequiredObjects.csv"}
    )
    void testRegisterUrlFeedCreateRequiredObjects() {
        directFeedService.registerDatacampUrlFeed(
                URL_3, LOGIN_3, PASSWORD_3, null, DIRECT_OWNER_3,
                List.of(new DirectFeedService.FeatureState(FeatureType.VERTICAL_SHARE, true)), null, List.of(), RegionConstants.RUSSIA, ACTION_ID
        );
    }

    @Test
    @DbUnitDataSet(
            after = {"DirectFeedTest.csv", "DirectFeedTest.testRefreshNewFileCreateRequiredObjects.csv"}
    )
    void testRefreshNewFileCreateRequiredObjects() {
        directFeedService.refreshDirectFile(DIRECT_CLIENT_ID_3, DIRECT_OWNER_3, DIRECT_FEED_3, URL_3, false, true, ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            after = {"DirectFeedTest.csv", "DirectFeedTest.testRefreshNewSiteCreateRequiredObjects.csv"}
    )
    void testRefreshNewSiteCreateRequiredObjects() {
        directFeedService.refreshDirectSite(DIRECT_CLIENT_ID_3, DIRECT_OWNER_3, DIRECT_FEED_3, URL_3, true, ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            after = {"DirectFeedTest.csv", "DirectFeedTest.testSendSiteForPreview.csv"}
    )
    void testSitePreview() throws InvalidProtocolBufferException {
        directFeedService.sendSiteForPreview(MbiUrlBuilder.fromString(URL_3).toUri(), 8);
        ArgumentCaptor<SamovarEvent> argumentCaptor = ArgumentCaptor.forClass(SamovarEvent.class);
        verify(samovarLogbrokerService, times(1)).publishEvent(argumentCaptor.capture());
        var captured = argumentCaptor.getValue().getPayload();
        assertThat(captured.getFeedContext().getUint64Value()).isEqualTo(8);
        assertThat(captured.getUrl()).isEqualTo(URL_3);
        var samovarContext = SamovarContextOuterClass.SamovarContext.parseFrom(
                captured.getFeedContext().getBytesValue());
        assertThat(samovarContext.getFeedsCount()).isEqualTo(1);
        var feed = samovarContext.getFeeds(0);
        assertThat(feed.getUrl()).isEqualTo(URL_3);
        assertThat(feed.getCampaignType()).isEqualTo(CampaignType.SITE_PREVIEW.getId());
    }

    @Test
    @DbUnitDataSet(
            after = {"DirectFeedTest.testSetFeatures.csv"}
    )
    void testSetFeatures() {
        directFeedService.setShopFeatures(List.of(
                ShopFeature.of(15L, FeatureType.DIRECT_GOODS_ADS, ParamCheckStatus.SUCCESS),
                ShopFeature.of(15L, FeatureType.DIRECT_STANDBY, ParamCheckStatus.SUCCESS),
                ShopFeature.of(15L, FeatureType.VERTICAL_SHARE, ParamCheckStatus.SUCCESS),
                ShopFeature.of(15L, FeatureType.DIRECT_SEARCH_SNIPPET_GALLERY, ParamCheckStatus.DONT_WANT)
        ), 1L);
    }

    @Test
    @DbUnitDataSet(
            after = {"DirectFeedTest.testSetFeaturesByToggle.csv"}
    )
    void testSetFeatureStates() {
        directFeedService.setShopFeatures(15L, List.of(
                new DirectFeedService.FeatureState(FeatureType.DIRECT_GOODS_ADS, true),
                new DirectFeedService.FeatureState(FeatureType.DIRECT_STANDBY, true),
                new DirectFeedService.FeatureState(FeatureType.VERTICAL_SHARE, true),
                new DirectFeedService.FeatureState(FeatureType.DIRECT_SEARCH_SNIPPET_GALLERY, false)
        ), 1L);
    }

    @Test
    @DbUnitDataSet(
            after = {"DirectFeedTest.csv"}
    )
    void testSetFeaturesNoFeedInfo() {
        assertThatThrownBy(() -> directFeedService.setShopFeatures(List.of(
                ShopFeature.of(144L, FeatureType.DIRECT_GOODS_ADS, ParamCheckStatus.SUCCESS)), 0
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void mockPassportUserParams(String fio, String login, long uid) {
        when(passportService.getUserInfo(uid))
                .thenReturn(new UserInfo(uid, fio, null, login));
    }

    @Test
    void testIdsInSitePreview() throws InvalidProtocolBufferException {
        directFeedService.sendSiteForPreview(MbiUrlBuilder.fromString(URL_1).toUri(), 8);
        ArgumentCaptor<SamovarEvent> argumentCaptor = ArgumentCaptor.forClass(SamovarEvent.class);
        verify(samovarLogbrokerService).publishEvent(argumentCaptor.capture());
        var captured = argumentCaptor.getValue().getPayload();
        var samovarContext = SamovarContextOuterClass.SamovarContext.parseFrom(captured.getFeedContext().getBytesValue());
        var feed = samovarContext.getFeeds(0);
        assertThat(feed.getShopId()).isEqualTo(122);
        assertThat(feed.getBusinessId()).isEqualTo(12);
        assertThat(feed.getFeedId()).isEqualTo(1222);
    }
}
