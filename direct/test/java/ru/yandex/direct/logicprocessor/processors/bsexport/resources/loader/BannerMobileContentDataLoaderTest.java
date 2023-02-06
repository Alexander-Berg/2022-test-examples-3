package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.bsexport.repository.resources.BsExportMobileContentRepository;
import ru.yandex.direct.core.entity.banner.model.BannerWithMobileContentDataForBsExport;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.HrefAndSiteService;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.mobilecontent.TrackerHrefHandleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerMobileContentDataLoaderTest {
    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private HrefAndSiteService hrefAndSiteService;
    private BsExportMobileContentRepository exportMobileContentRepository;
    private TrackerHrefHandleService trackerHrefHandleService;
    private BannerMobileContentDataLoader loader;
    private BsOrderIdCalculator bsOrderIdCalculator;

    private static final long BID = 1L;
    private static final long ADGROUP_ID = 3L;
    private static final long CAMPAIGN_ID = 5L;
    private static final long BS_BANNER_ID = 40L;
    private static final long ORDER_ID = 30L;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        this.exportMobileContentRepository = mock(BsExportMobileContentRepository.class);
        this.hrefAndSiteService = mock(HrefAndSiteService.class);
        this.trackerHrefHandleService = mock(TrackerHrefHandleService.class);
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.loader = new BannerMobileContentDataLoader(context, exportMobileContentRepository,
                trackerHrefHandleService, hrefAndSiteService);
        when(hrefAndSiteService.isValidDomain(any())).thenCallRealMethod();
        when(hrefAndSiteService.isValidHref(any())).thenCallRealMethod();
    }

    @Test
    void test() {
        mockOrderIdCalculator();
        String storeContentId = "com.cocoplay.fashion.style";
        String bannerImpressionUrl = "https://impression.appsflyer.com/com.cocoplay.fashion" +
                ".style?pid=yandexdirect_int&af_click_lookback=7d";
        String bannerImpressionUrlWithParams = "https://impression.appsflyer.com/com.cocoplay.fashion" +
                ".style?pid=yandexdirect_int&af_click_lookback=7d&clickid={logid}&advertising_id={google_aid}" +
                "&android_id={androidid}";
        String bannerImpressionUrlWithMacrosExpanded = "https://impression.appsflyer.com/com.cocoplay.fashion" +
                ".style?pid=yandexdirect_int&af_click_lookback=7d&clickid={TRACKID}&advertising_id={GOOGLE_AID_LC}" +
                "&android_id={ANDROID_ID_LC}";
        var osType = OsType.ANDROID;
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setPid(ADGROUP_ID)
                .setCid(CAMPAIGN_ID)
                .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT_DATA)
                .build();
        var resourceFromDb = getBannerWithCommonFields()
                .withImpressionUrl(bannerImpressionUrl);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithMobileContentDataForBsExport.class));

        var mobileContent = new MobileContent()
                .withId(124982L)
                .withOsType(osType);

        var mockParameters = new MockParameters()
                .withCampaign(new MobileContentCampaign().withId(CAMPAIGN_ID))
                .withMobileContent(mobileContent);
        mockRepository(mockParameters);

        doReturn(bannerImpressionUrlWithParams)
                .when(trackerHrefHandleService)
                .handleHref(eq(bannerImpressionUrl), eq(osType));

        doReturn(bannerImpressionUrl)
                .when(hrefAndSiteService)
                .prepareHref(bannerImpressionUrl);

        doReturn(bannerImpressionUrlWithMacrosExpanded)
                .when(hrefAndSiteService)
                .expandHref(eq(bannerImpressionUrlWithParams), any(), any());

        var expectedResource = getResourceWithCommonFields()
                .setResource(bannerImpressionUrlWithMacrosExpanded)
                .build();

        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var gotResource = loader.loadResources(SHARD, List.of(object));
        assertThat(gotResource.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
        assertThat(gotResource.getResources())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedResource);
    }

    private BannerResource.Builder<String> getResourceWithCommonFields() {
        return new BannerResource.Builder<String>()
                .setBid(BID)
                .setPid(ADGROUP_ID)
                .setCid(CAMPAIGN_ID)
                .setOrderId(ORDER_ID)
                .setBsBannerId(BS_BANNER_ID);
    }

    private BannerWithMobileContentDataForBsExport getBannerWithCommonFields() {
        return new MobileAppBanner()
                .withId(BID)
                .withAdGroupId(ADGROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withBsBannerId(BS_BANNER_ID);
    }

    private void mockOrderIdCalculator() {
        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection()))
                .thenReturn(Map.of(CAMPAIGN_ID, 30L));
    }

    private void mockRepository(MockParameters parameters) {
        if (Objects.nonNull(parameters.mobileContent)) {
            doReturn(Map.of(ADGROUP_ID, parameters.mobileContent))
                    .when(exportMobileContentRepository)
                    .getMobileContentByAdGroupIds(anyInt(), argThat(adGroupIds -> adGroupIds.contains(ADGROUP_ID)));
        } else {
            doReturn(Map.of())
                    .when(exportMobileContentRepository)
                    .getMobileContentByAdGroupIds(anyInt(), argThat(adGroupIds -> adGroupIds.contains(ADGROUP_ID)));
        }

        if (Objects.nonNull(parameters.campaign)) {
            doReturn(List.of(parameters.campaign))
                    .when(exportMobileContentRepository)
                    .getCampaigns(anyInt(), argThat(campaignIds -> campaignIds.contains(CAMPAIGN_ID)));
        } else {
            doReturn(List.of())
                    .when(exportMobileContentRepository)
                    .getCampaigns(anyInt(), argThat(campaignIds -> campaignIds.contains(CAMPAIGN_ID)));
        }
    }

    private static class MockParameters {
        private CommonCampaign campaign = null;
        private MobileContent mobileContent = null;

        public MockParameters withCampaign(CommonCampaign campaign) {
            this.campaign = campaign;
            return this;
        }

        public MockParameters withMobileContent(MobileContent mobileContent) {
            this.mobileContent = mobileContent;
            return this;
        }
    }
}
