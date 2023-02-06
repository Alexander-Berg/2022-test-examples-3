package ru.yandex.market.api.partner.controllers.feed;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampOfferMeta;
import com.google.protobuf.Timestamp;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.api.ApiObject;
import ru.yandex.market.api.partner.apisupport.ApiNotFoundException;
import ru.yandex.market.api.partner.controllers.util.FeedHelper;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.datacamp.PushSettingsService;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestService;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feed.FeedRefreshService;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.feed.model.FeedInfo;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.feed.model.FullFeedInfo;
import ru.yandex.market.core.feed.samovar.SamovarFeedRepositoryImpl;
import ru.yandex.market.core.feed.supplier.SupplierFeedService;
import ru.yandex.market.core.feed.supplier.model.SupplierFeed;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.samovar.mapper.SamovarEventMapperImpl;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeed;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeedInfoConverter;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeedMapper;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeedService;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshFeedTest {
    private static final String SAMOVAR_SITE_PROCESSING_FEED_NAME = "test-feed-site-processing";
    private static final String SAMOVAR_SITE_PREVIEW_FEED_NAME = "test-feed-site-preview";
    /**
     * Проверяем если запрос через fp.
     */
    @Captor
    ArgumentCaptor<SamovarFeed> fpCaptor;
    private FeedControllerV2 feedControllerV2;
    @Mock
    private CampaignService campaignService;
    @Mock
    private FeedService feedService;
    @Mock
    private SamovarFeedService samovarFeedService;
    @Mock
    private SupplierFeedService supplierFeedService;
    @Mock
    private PushSettingsService pushSettingsService;
    @Mock
    private LogbrokerService logbrokerService;
    @Mock
    private HttpClient httpClient;
    @Mock
    private PartnerService partnerService;
    @Mock
    private DatasourceService datasourceService;
    @Mock
    private BusinessService businessService;
    @Mock
    private EnvironmentService environmentService;
    @Mock
    private FeatureService featureService;
    @Mock
    private PartnerPlacementProgramService partnerPlacementProgramService;
    @Mock
    private PartnerTypeAwareService partnerTypeAwareService;
    @Mock
    private DeliveryInfoService deliveryInfoService;
    @Mock
    private FeedProcessorUpdateRequestService feedProcessorUpdateRequestService;

    private static Stream<Arguments> provideUrlsForSamovar() {
        return Stream.of(
                Arguments.of(new ResourceAccessCredentials("testLogin", "testPwd"), "https://stub"),
                Arguments.of(null, "https://stub")
        );
    }

    @BeforeEach
    void setUp() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(10);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        final FeedRefreshService feedRefreshService = new FeedRefreshService(new SamovarEventMapperImpl(),
                logbrokerService, new SamovarFeedRepositoryImpl(List.of(samovarFeedService)),
                new SamovarFeedInfoConverter(businessService, partnerService, environmentService,
                        new SamovarFeedMapper(environmentService), featureService, partnerPlacementProgramService,
                        partnerTypeAwareService, deliveryInfoService, Map.of()),
                feedProcessorUpdateRequestService,
                SAMOVAR_SITE_PROCESSING_FEED_NAME, SAMOVAR_SITE_PREVIEW_FEED_NAME);
        final FeedHelper feedHelper = new FeedHelper();
        feedHelper.setFeedService(feedService);

        feedControllerV2 = new FeedControllerV2();

        feedControllerV2.setFeedRefreshService(feedRefreshService);
        feedControllerV2.setCampaignService(campaignService);
        feedControllerV2.setFeedHelper(feedHelper);
        feedControllerV2.setFeedService(feedService);
        feedControllerV2.setSupplierFeedService(supplierFeedService);
        feedControllerV2.setPushSettingsService(pushSettingsService);
        feedControllerV2.setDatasourceService(datasourceService);

        when(environmentService.getCurrentEnvironmentType()).thenReturn(EnvironmentType.DEVELOPMENT);
    }

    /**
     * Проверяем попытку триггернуть загрузку фида, неактивного магазина
     */
    @Test
    void testTryInactiveShopRefreshFeed() {
        CampaignInfo campaignInfo = new CampaignInfo(11774L, 1774L, 13L, 1774L, CampaignType.SHOP);
        when(campaignService.getMarketCampaign(eq(11774L)))
                .thenReturn(campaignInfo);
        Assertions.assertThrows(
                ApiNotFoundException.class,
                () -> feedControllerV2.triggerFeedRefresh(11774L, 6500)
        );
    }

    /**
     * Проверяем попытку триггернуть загрузку фида, неактивного магазина
     */
    @Test
    void testTryInactiveShop2RefreshFeed() {
        mockGetCampaign(11774L, CampaignType.SHOP);

        Assertions.assertThrows(
                ApiNotFoundException.class,
                () -> feedControllerV2.triggerFeedRefresh(11774L, 6500)
        );
    }

    /**
     * Проверяем попытку триггернуть загрузку фида, не принадлежащего текущей кампании
     */
    @Test
    void testTryIllegalRefreshFeed() {
        mockGetCampaign(11774L, CampaignType.SHOP);
        Assertions.assertThrows(
                ApiNotFoundException.class,
                () -> feedControllerV2.triggerFeedRefresh(11774L, 6500)
        );
    }

    @ParameterizedTest
    @MethodSource("provideUrlsForSamovar")
    void testTryRefreshFeedSamovar(ResourceAccessCredentials credentials, String expectedUrl) {
        SamovarFeed expected = buildSamovarFeed(credentials);

        mockGetCampaign(11774L, CampaignType.SUPPLIER);
        when(supplierFeedService.getSupplierFeed(eq(1774L))).thenReturn(Optional.of(new SupplierFeed.Builder()
                .setId(659515L)
                .setSupplierId(1774L)
                .setBusinessId(666L)
                .setResource(RemoteResource.of("url"))
                .setUpdatedAt(Instant.now())
                .build()));
        when(pushSettingsService.usePushScheme(1774L))
                .thenReturn(true);
        when(samovarFeedService.getFeedForOneTimeDownload(659515L,
                PartnerId.partnerId(1774L, CampaignType.SUPPLIER), FeedType.ASSORTMENT)
        )
                .thenReturn(Optional.of(expected));
        when(feedService.getFullFeeds(1774L, true)).thenReturn(Collections.singletonList(getFullFeedInfo()));

        feedControllerV2.triggerFeedRefresh(11774L, 659515);

        Mockito.verify(feedProcessorUpdateRequestService).sendRequestToUpdate(fpCaptor.capture());
        SamovarFeed actual = fpCaptor.getValue();

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .ignoringFieldsMatchingRegexes(".*forceRefreshStart.*", ".*memoizedSize.*", ".*memoizedHashCode.*")
                .isEqualTo(expected);
    }

    /**
     * Проверяем если индексатор отвечает хорошо на наш запрос
     */
    @Test
    void testTryRefreshFeedSuccessful() throws IOException {
        when(samovarFeedService.getFeedForOneTimeDownload(659515L,
                PartnerId.partnerId(11774L, CampaignType.SHOP), FeedType.ASSORTMENT)
        )
                .thenReturn(Optional.empty());
        mockGetCampaign(11774L, CampaignType.SHOP);

        when(feedService.getFullFeeds(1774L, true)).thenReturn(Collections.singletonList(getFullFeedInfo()));

        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1),
                200,
                "OK"));
        HttpEntity httpEntity = EntityBuilder.create().setText("0;ok").build();
        httpResponse.setEntity(httpEntity);
        when(httpClient.execute(any())).thenReturn(httpResponse);

        ApiObject.Ok responseV2Update = feedControllerV2.triggerFeedRefresh(11774L, 659515);
        assertEquals(ApiObject.OK, responseV2Update);
    }

    private FullFeedInfo getFullFeedInfo() {
        FullFeedInfo fullFeedInfo = new FullFeedInfo();
        FeedInfo feedInfo = new FeedInfo();
        feedInfo.setId(659515);
        fullFeedInfo.setFeedInfo(feedInfo);
        return fullFeedInfo;
    }

    private SamovarFeed buildSamovarFeed(ResourceAccessCredentials credentials) {
        SamovarContextOuterClass.FeedInfo expectedFeedInfo = SamovarContextOuterClass.FeedInfo.newBuilder()
                .setCampaignType(CampaignType.SUPPLIER.name())
                .setFeedId(659515L)
                .setUpdatedAt(
                        Timestamp.newBuilder().setSeconds(1575948458).setNanos(172803000)
                )
                .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                        .setColor(DataCampOfferMeta.MarketColor.BLUE)
                        .build())
                .setShopId(1774L)
                .build();

        SamovarContextOuterClass.SamovarContext expectedContext = SamovarContextOuterClass.SamovarContext.newBuilder()
                .addFeeds(expectedFeedInfo)
                .setEnvironment(EnvironmentType.DEVELOPMENT.getValue())
                .build();

        return SamovarFeed.builder()
                .setUrl("https://stub")
                .setCredentials(credentials)
                .setPeriodMinutes(20)
                .setTimeoutSeconds(100)
                .setContext(expectedContext)
                .setEnabled(true)
                .build();
    }

    private void mockGetCampaign(long campaignId, CampaignType campaignType) {
        long datasourceId = 1774L;
        CampaignInfo campaignInfo = new CampaignInfo(campaignId, datasourceId, 13L, 1774L, campaignType);
        when(campaignService.getMarketCampaign(eq(campaignId))).thenReturn(campaignInfo);
        when(datasourceService.isShopAlive(eq(datasourceId))).thenReturn(true);
    }
}
