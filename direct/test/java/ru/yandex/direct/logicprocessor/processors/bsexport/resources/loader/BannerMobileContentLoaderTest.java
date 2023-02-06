package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.adv.direct.banner.resources.PlatformName;
import ru.yandex.direct.core.bsexport.repository.resources.BsExportMobileContentRepository;
import ru.yandex.direct.core.bsexport.resources.model.MobileAppForBsExport;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithMobileContentAdGroupForBsExport;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.AdditionalInfo;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.TestPlatformNames;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.MobileContentInfo;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.DomainFilterService;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.HrefAndSite;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.HrefAndSiteService;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.mobilecontent.TrackerHrefHandleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BannerMobileContentLoaderTest {
    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private DomainFilterService domainFilterService;
    private HrefAndSiteService hrefAndSiteService;
    private BsExportMobileContentRepository exportMobileContentRepository;
    private TrackerHrefHandleService trackerHrefHandleService;
    private BannerMobileContentLoader loader;
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
        this.domainFilterService = mock(DomainFilterService.class);
        this.exportMobileContentRepository = mock(BsExportMobileContentRepository.class);
        this.hrefAndSiteService = mock(HrefAndSiteService.class);
        this.trackerHrefHandleService = mock(TrackerHrefHandleService.class);
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.loader = new BannerMobileContentLoader(context, domainFilterService, exportMobileContentRepository,
                trackerHrefHandleService, hrefAndSiteService);
        when(hrefAndSiteService.isValidDomain(any())).thenCallRealMethod();
        when(hrefAndSiteService.isValidHref(any())).thenCallRealMethod();
    }

    @Test
    void testGooglePlay() {
        test("play.google.com", TestPlatformNames.GOOGLE_PLAY_RU);
    }

    @Test
    void testFifaPlay() {
        test("play.fifa.com", null);
    }

    void test(String site, PlatformName expectedPlatformName) {
        mockOrderIdCalculator();
        String storeContentId = "com.cocoplay.fashion.style";
        String bannerHref = "https://app.appsflyer.com/com.cocoplay.fashion" +
                ".style?pid=yandexdirect_int&af_click_lookback=7d";
        String bannerHrefWithParams = "https://app.appsflyer.com/com.cocoplay.fashion" +
                ".style?pid=yandexdirect_int&af_click_lookback=7d&clickid={logid}&advertising_id={google_aid" +
                "}&android_id={androidid}";
        var storeContentHref = String.format(
                "https://%s/store/apps/details?id=com.cocoplay.fashion.style&hl=ru&gl=US", site);
        var osType = OsType.ANDROID;
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setPid(ADGROUP_ID)
                .setCid(CAMPAIGN_ID)
                .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                .build();
        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES)
                .withHref(bannerHref);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithMobileContentAdGroupForBsExport.class));

        var mobileContent = new MobileContent()
                .withId(124982L)
                .withStoreContentId(storeContentId)
                .withOsType(osType)
                .withPublisherDomainId(9807330L);
        var mobileApp = new MobileAppForBsExport()
                .withDomainId(9807330L)
                .withMobileAppId(38575L)
                .withMobileContentId(124982L)
                .withStoreHref(storeContentHref);

        var mockParameters = new MockParameters()
                .withCampaign(getCampaign())
                .withMobileContent(mobileContent)
                .withStoreContentHref(storeContentHref)
                .withMobileApp(mobileApp)
                .withDomainId(9807330L)
                .withDomainName("www.crazylabs.com")
                .withDomainFilter("crazylabs.com");
        mockRepository(mockParameters);

        doReturn(bannerHrefWithParams)
                .when(trackerHrefHandleService)
                .handleHref(eq(bannerHref), eq(osType));

        doReturn(bannerHref)
                .when(hrefAndSiteService)
                .prepareHref(bannerHref);

        doReturn(new HrefAndSite("https://app.appsflyer.com/com.cocoplay.fashion" +
                ".style?pid=yandexdirect_int&af_click_lookback=7d&clickid={TRACKID}&advertising_id={GOOGLE_AID_LC}" +
                "&android_id={ANDROID_ID_LC}", "app.appsflyer.com", "app.appsflyer.com"))
                .when(hrefAndSiteService)
                .extract(eq(bannerHrefWithParams), any(), any());
        var expectedMobileContentInfo = MobileContentInfo.builder()
                .withId(124982L)
                .withBundleId(storeContentId)
                .withOsType(OsType.ANDROID)
                .withHref("https://app.appsflyer.com/com.cocoplay.fashion" +
                        ".style?pid=yandexdirect_int&af_click_lookback=7d&clickid={TRACKID}&advertising_id" +
                        "={GOOGLE_AID_LC}" +
                        "&android_id={ANDROID_ID_LC}")
                .withSite(site)
                .withSiteFilter(storeContentId)
                .withDomainFilter("crazylabs.com")
                .withPlatformName(expectedPlatformName)
                .withRegion("us")
                .withLang("ru")
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedMobileContentInfo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var gotResource = loader.loadResources(SHARD, List.of(object));
        assertThat(gotResource.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
        assertThat(gotResource.getResources())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedResource);
    }

    /**
     * Тест проверяет, что если у баннера нет ссылки, то в href будет передан adgroups_mobile_content.store_content_url
     */
    @Test
    void bannerWithoutHrefTest() {
        mockOrderIdCalculator();
        String storeContentId = "com.cocoplay.fashion.style";
        var storeContentHref = "https://play.google.com/store/apps/details?id=com.cocoplay.fashion.style&hl=ru&gl=US";
        var osType = OsType.ANDROID;
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setPid(ADGROUP_ID)
                .setCid(CAMPAIGN_ID)
                .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                .build();
        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES)
                .withHref(null);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithMobileContentAdGroupForBsExport.class));

        var mobileContent = new MobileContent()
                .withId(124982L)
                .withStoreContentId(storeContentId)
                .withOsType(osType)
                .withPublisherDomainId(9807330L);
        var mobileApp = new MobileAppForBsExport()
                .withDomainId(9807330L)
                .withMobileAppId(38575L)
                .withMobileContentId(124982L)
                .withStoreHref(storeContentHref);

        var mockParameters = new MockParameters()
                .withCampaign(getCampaign())
                .withMobileContent(mobileContent)
                .withStoreContentHref(storeContentHref)
                .withMobileApp(mobileApp)
                .withDomainId(9807330L)
                .withDomainName("www.crazylabs.com")
                .withDomainFilter("crazylabs.com");
        mockRepository(mockParameters);

        var expectedMobileContentInfo = MobileContentInfo.builder()
                .withId(124982L)
                .withBundleId(storeContentId)
                .withOsType(OsType.ANDROID)
                .withHref(storeContentHref)
                .withSite("play.google.com")
                .withSiteFilter(storeContentId)
                .withDomainFilter("crazylabs.com")
                .withPlatformName(TestPlatformNames.GOOGLE_PLAY_RU)
                .withRegion("us")
                .withLang("ru")
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedMobileContentInfo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var gotResource = loader.loadResources(SHARD, List.of(object));
        verify(trackerHrefHandleService, never()).handleHref(anyString(), any());

        verify(hrefAndSiteService, never()).prepareHref(anyString());
        verify(hrefAndSiteService, never()).extract(anyString(), any(), any());

        assertThat(gotResource.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
        assertThat(gotResource.getResources())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedResource);
    }

    /**
     * Тест проверяет, что если для publisher_domain_id не нашло домена(то есть он некорректный),
     * то в DomainFiler запишется значение SiteFiler
     */
    @Test
    void bannerWithIncorrectPublisherDomainIdTest() {
        mockOrderIdCalculator();
        String storeContentId = "com.cocoplay.fashion.style";
        var storeContentHref = "https://play.google.com/store/apps/details?id=com.cocoplay.fashion.style&hl=ru&gl=US";
        var osType = OsType.ANDROID;
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setPid(ADGROUP_ID)
                .setCid(CAMPAIGN_ID)
                .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                .build();
        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES)
                // для упроещения, в этом тесте тоже примем, что ссылка у баннера не указана
                .withHref(null);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithMobileContentAdGroupForBsExport.class));

        var mobileContent = new MobileContent()
                .withId(124982L)
                .withStoreContentId(storeContentId)
                .withOsType(osType)
                .withPublisherDomainId(9807330L);
        var mobileApp = new MobileAppForBsExport()
                .withDomainId(9807330L)
                .withMobileAppId(38575L)
                .withMobileContentId(124982L)
                .withStoreHref(storeContentHref);

        var mockParameters = new MockParameters()
                .withCampaign(getCampaign())
                .withMobileContent(mobileContent)
                .withStoreContentHref(storeContentHref)
                .withMobileApp(mobileApp)
                .withDomainId(9807330L)
                // домен не найден
                .withDomainName(null);
        mockRepository(mockParameters);

        var expectedMobileContentInfo = MobileContentInfo.builder()
                .withId(124982L)
                .withBundleId(storeContentId)
                .withOsType(OsType.ANDROID)
                .withHref(storeContentHref)
                .withSite("play.google.com")
                .withSiteFilter(storeContentId)
                .withDomainFilter(storeContentId)
                .withPlatformName(TestPlatformNames.GOOGLE_PLAY_RU)
                .withRegion("us")
                .withLang("ru")
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedMobileContentInfo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var gotResource = loader.loadResources(SHARD, List.of(object));
        verify(trackerHrefHandleService, never()).handleHref(anyString(), any());

        verify(hrefAndSiteService, never()).prepareHref(anyString());
        verify(hrefAndSiteService, never()).extract(anyString(), any(), any());

        assertThat(gotResource.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
        assertThat(gotResource.getResources())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedResource);
    }

    @ParameterizedTest
    @EnumSource(value = BannerStatusModerate.class, mode = EnumSource.Mode.EXCLUDE, names = "YES")
    void notModerateBannerTest(BannerStatusModerate statusModerate) {
        mockOrderIdCalculator();
        String bannerHref = "https://app.appsflyer.com/com.cocoplay.fashion" +
                ".style?pid=yandexdirect_int&af_click_lookback=7d";
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setPid(ADGROUP_ID)
                .setCid(CAMPAIGN_ID)
                .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                .build();
        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(statusModerate)
                .withHref(bannerHref);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithMobileContentAdGroupForBsExport.class));


        verify(exportMobileContentRepository, never()).getMobileContentByAdGroupIds(anyInt(), anyCollection());
        verify(exportMobileContentRepository, never()).getStoreUrlByAdGroupIds(anyInt(), anyCollection());
        verify(exportMobileContentRepository, never()).getCampaigns(anyInt(), anyCollection());
        verify(exportMobileContentRepository, never()).getMobileAppsForCampaigns(anyInt(), anyCollection());
        verify(hrefAndSiteService, never()).extract(anyString(), any(), any());
        verify(exportMobileContentRepository, never()).getDomainsByIdsFromDict(anyCollection());
        verify(hrefAndSiteService, never()).isValidDomain(any());
        verify(hrefAndSiteService, never()).isValidHref(any());

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var gotResource = loader.loadResources(SHARD, List.of(object));
        assertThat(gotResource.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
        assertThat(gotResource.getResources()).isEmpty();

    }

    /**
     * Тест проверяет, что если для группы баннера нет записи в adgroups_mobile_content, то по баннеру ничего
     * посылаться не будет
     */
    @Test
    void noAdgroupInAdGroupsMobileContentTest() {
        mockOrderIdCalculator();
        String bannerHref = "https://app.appsflyer.com/com.cocoplay.fashion" +
                ".style?pid=yandexdirect_int&af_click_lookback=7d";
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setPid(ADGROUP_ID)
                .setCid(CAMPAIGN_ID)
                .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                .build();
        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES)
                .withHref(bannerHref);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithMobileContentAdGroupForBsExport.class));

        doReturn(List.of(getCampaign())).when(exportMobileContentRepository)
                .getCampaigns(anyInt(), argThat(campaingIds -> campaingIds.contains(CAMPAIGN_ID)));

        doReturn(Map.of())
                .when(exportMobileContentRepository)
                .getMobileContentByAdGroupIds(anyInt(), argThat(adGroupIds -> adGroupIds.contains(ADGROUP_ID)));


        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var gotResource = loader.loadResources(SHARD, List.of(object));
        verify(exportMobileContentRepository, never()).getStoreUrlByAdGroupIds(anyInt(), anyCollection());
        verify(exportMobileContentRepository, never()).getMobileAppsForCampaigns(anyInt(), anyCollection());
        verify(hrefAndSiteService, never()).extract(anyString(), any(), any());
        verify(exportMobileContentRepository, never()).getDomainsByIdsFromDict(anyCollection());
        verify(hrefAndSiteService, never()).isValidDomain(any());
        verify(hrefAndSiteService, never()).isValidHref(any());
        assertThat(gotResource.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
        assertThat(gotResource.getResources()).isEmpty();
    }

    /**
     * Тест проверяет, что если для группы баннера нет кампании, то по баннеру ничего
     * посылаться не будет
     */
    @Test
    void noCampaignForBannerTest() {
        mockOrderIdCalculator();
        String bannerHref = "https://app.appsflyer.com/com.cocoplay.fashion" +
                ".style?pid=yandexdirect_int&af_click_lookback=7d";
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setPid(ADGROUP_ID)
                .setCid(CAMPAIGN_ID)
                .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                .build();
        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES)
                .withHref(bannerHref);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithMobileContentAdGroupForBsExport.class));

        var mobileContent = new MobileContent()
                .withId(124982L)
                .withStoreContentId("com.cocoplay.fashion.style")
                .withOsType(OsType.ANDROID)
                .withPublisherDomainId(9807330L);

        doReturn(List.of()).when(exportMobileContentRepository)
                .getCampaigns(anyInt(), argThat(campaingIds -> campaingIds.contains(CAMPAIGN_ID)));

        doReturn(Map.of(ADGROUP_ID, mobileContent))
                .when(exportMobileContentRepository)
                .getMobileContentByAdGroupIds(anyInt(), argThat(adGroupIds -> adGroupIds.contains(ADGROUP_ID)));


        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var gotResource = loader.loadResources(SHARD, List.of(object));
        verify(exportMobileContentRepository, never()).getStoreUrlByAdGroupIds(anyInt(), anyCollection());
        verify(exportMobileContentRepository, never()).getMobileAppsForCampaigns(anyInt(), anyCollection());
        verify(hrefAndSiteService, never()).extract(anyString(), any(), any());
        verify(exportMobileContentRepository, never()).getDomainsByIdsFromDict(anyCollection());
        verify(hrefAndSiteService, never()).isValidDomain(any());
        verify(hrefAndSiteService, never()).isValidHref(any());
        assertThat(gotResource.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
        assertThat(gotResource.getResources()).isEmpty();
    }

    @Test
    void getPublisherDomainId_SameContentIdForAppAndContentTest() {
        var mobileAppDomainId = 789123L;
        var mobileContentPublisherDomainId = 98765L;

        var mobileContent = new MobileContent()
                .withId(1234L)
                .withPublisherDomainId(mobileContentPublisherDomainId);
        var mobileApp = new MobileAppForBsExport()
                .withMobileContentId(1234L)
                .withDomainId(mobileAppDomainId);
        var gotDomainId = loader.getPublisherDomainId(mobileContent, mobileApp);
        assertThat(gotDomainId).isEqualTo(mobileAppDomainId);
    }

    @Test
    void getPublisherDomainId_DifferentContentIdForAppAndContentTest() {
        var mobileAppDomainId = 789123L;
        var mobileContentPublisherDomainId = 98765L;
        var mobileContentId1 = 1234L;
        var mobileContentId2 = 4321L;

        var mobileContent = new MobileContent()
                .withId(mobileContentId1)
                .withPublisherDomainId(mobileContentPublisherDomainId);
        var mobileApp = new MobileAppForBsExport()
                .withMobileContentId(mobileContentId2)
                .withDomainId(mobileAppDomainId);
        var gotDomainId = loader.getPublisherDomainId(mobileContent, mobileApp);
        assertThat(gotDomainId).isEqualTo(mobileContentPublisherDomainId);
    }

    @Test
    void getPublisherDomainId_AbsentMobileAppTest() {
        var mobileContentPublisherDomainId = 98765L;

        var mobileContent = new MobileContent()
                .withId(1234L)
                .withPublisherDomainId(mobileContentPublisherDomainId);
        var gotDomainId = loader.getPublisherDomainId(mobileContent, null);
        assertThat(gotDomainId).isEqualTo(mobileContentPublisherDomainId);
    }

    /**
     * Тест проверяет, что если для publisher_domain_id не нашлось домена, то для этого баннера метод
     * getBidToPublisherDomainFilterMap не вернер результат
     */
    @Test
    void getBidToPublisherDomainFilterMap_NoPublisherDomainForDomainIdTest() {
        var domainId = 123456L;
        var resourceFromDb = getBannerWithCommonFields();
        var mobileContent = new MobileContent()
                .withId(1234L)
                .withPublisherDomainId(domainId);
        var mobileApp = new MobileAppForBsExport()
                .withMobileContentId(1234L)
                .withDomainId(domainId);
        when(exportMobileContentRepository.getDomainsByIdsFromDict(argThat(domainIds -> domainIds.contains(domainId))))
                .thenReturn(List.of());
        var result = loader.getBidToPublisherDomainFilterMap(List.of(resourceFromDb), Map.of(ADGROUP_ID, mobileContent),
                Map.of(CAMPAIGN_ID, mobileApp));
        verify(domainFilterService).getDomainsFilters(argThat(Collection::isEmpty));
        assertThat(result).isEmpty();
    }

    /**
     * Тест проверяет, что если publisher_domain_id = null, то для этого баннера метод
     * getBidToPublisherDomainFilterMap не вернер результат
     */
    @Test
    void getBidToPublisherDomainFilterMap_NullPublisherDomainIdTest() {
        var domainId = 123456L;
        var resourceFromDb = getBannerWithCommonFields();
        var mobileContent = new MobileContent()
                .withId(1234L)
                .withPublisherDomainId(null);

        when(exportMobileContentRepository.getDomainsByIdsFromDict(argThat(domainIds -> domainIds.contains(domainId))))
                .thenReturn(List.of());
        var result = loader.getBidToPublisherDomainFilterMap(List.of(resourceFromDb), Map.of(ADGROUP_ID,
                mobileContent), Map.of());
        verify(domainFilterService).getDomainsFilters(argThat(Collection::isEmpty));
        assertThat(result).isEmpty();
    }

    @Test
    void getBsExportBundleId_OsTypeIosNullBundleIdTest() {
        var storeContentId = "com.cocoplay.fashion.style";
        var mobileContent = new MobileContent()
                .withStoreContentId(storeContentId).withBundleId(null).withOsType(OsType.IOS);
        assertThat(loader.getBsExportBundleId(mobileContent)).isEqualTo("");
    }

    @Test
    void getStoreAppId_OsTypeAndroidTest() {
        var storeContentId = "com.cocoplay.fashion.style";
        var bundleId = "ru.yandex.mobile.search";
        var mobileContent = new MobileContent()
                .withStoreContentId(storeContentId).withBundleId(bundleId).withOsType(OsType.ANDROID);
        assertThat(loader.getStoreAppId(mobileContent)).isEqualTo(storeContentId);
    }

    @Test
    void getStoreAppId_OsTypeIosTest() {
        var storeContentId = "com.cocoplay.fashion.style";
        var bundleId = "ru.yandex.mobile.search";
        var mobileContent = new MobileContent()
                .withStoreContentId(storeContentId).withBundleId(bundleId).withOsType(OsType.IOS);
        assertThat(loader.getStoreAppId(mobileContent)).isEqualTo(bundleId);
    }

    @Test
    void getAdditionalBids_MobileContentTest() {
        var mobileContentId = 1234L;
        var adgroupId = 987L;
        var bids = List.of(456L, 678L);
        var additionalInfo = new AdditionalInfo(TablesEnum.MOBILE_CONTENT, mobileContentId);
        doReturn(List.of(adgroupId))
                .when(exportMobileContentRepository)
                .getAdgroupIdsForMobileContentIds(anyInt(), argThat(ids -> ids.contains(mobileContentId)));
        doReturn(bids)
                .when(exportMobileContentRepository)
                .getBannerIdsByAdGroupIds(anyInt(), argThat(ids -> ids.contains(adgroupId)));
        var additionalBids = loader.getAdditionalBids(SHARD, List.of(additionalInfo));
        assertThat(additionalBids).containsExactlyInAnyOrder(bids.toArray(Long[]::new));
    }

    @Test
    void getAdditionalBids_CampaignsMobileContentTest() {
        var campaigns = 785L;
        var bids = List.of(654L, 876L);
        var additionalInfo = new AdditionalInfo(TablesEnum.CAMPAIGNS_MOBILE_CONTENT, campaigns);
        doReturn(bids)
                .when(exportMobileContentRepository)
                .getBannerIdsByCampaignIds(anyInt(), argThat(ids -> ids.contains(campaigns)));
        var additionalBids = loader.getAdditionalBids(SHARD, List.of(additionalInfo));
        assertThat(additionalBids).containsExactlyInAnyOrder(bids.toArray(Long[]::new));
    }

    private BannerResource.Builder<MobileContentInfo> getResourceWithCommonFields() {
        return new BannerResource.Builder<MobileContentInfo>()
                .setBid(BID)
                .setPid(ADGROUP_ID)
                .setCid(CAMPAIGN_ID)
                .setOrderId(ORDER_ID)
                .setBsBannerId(BS_BANNER_ID);
    }

    private BannerWithMobileContentAdGroupForBsExport getBannerWithCommonFields() {
        return new ImageBanner()
                .withId(BID)
                .withAdGroupId(ADGROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withBsBannerId(BS_BANNER_ID)
                .withLanguage(Language.RU_);
    }

    private CommonCampaign getCampaign() {
        return new MobileContentCampaign()
                .withId(CAMPAIGN_ID);
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
        doReturn(Map.of(ADGROUP_ID, parameters.storeContentHref))
                .when(exportMobileContentRepository)
                .getStoreUrlByAdGroupIds(anyInt(), argThat(adGroupIds -> adGroupIds.contains(ADGROUP_ID)));

        if (Objects.nonNull(parameters.campaign)) {
            doReturn(List.of(parameters.campaign))
                    .when(exportMobileContentRepository)
                    .getCampaigns(anyInt(), argThat(campaignIds -> campaignIds.contains(CAMPAIGN_ID)));
        } else {
            doReturn(List.of())
                    .when(exportMobileContentRepository)
                    .getCampaigns(anyInt(), argThat(campaignIds -> campaignIds.contains(CAMPAIGN_ID)));
        }

        if (Objects.nonNull(parameters.mobileApp)) {
            doReturn(Map.of(CAMPAIGN_ID, parameters.mobileApp))
                    .when(exportMobileContentRepository).getMobileAppsForCampaigns(anyInt(),
                            argThat(campaignIds -> campaignIds.contains(CAMPAIGN_ID)));
        } else {
            doReturn(Map.of())
                    .when(exportMobileContentRepository).getMobileAppsForCampaigns(anyInt(),
                            argThat(campaignIds -> campaignIds.contains(CAMPAIGN_ID)));
        }

        if (Objects.nonNull(parameters.domainName)) {
            doReturn(List.of(new Domain().withDomain(parameters.domainName).withId(parameters.domainId)))
                    .when(exportMobileContentRepository)
                    .getDomainsByIdsFromDict(argThat(domainIds -> domainIds.contains(parameters.domainId)));
            doReturn(Map.of(parameters.domainName, parameters.domainFilter))
                    .when(domainFilterService)
                    .getDomainsFilters(argThat(domains -> domains.contains(parameters.domainName)));
        } else {
            doReturn(List.of())
                    .when(exportMobileContentRepository)
                    .getDomainsByIdsFromDict(argThat(domainIds -> domainIds.contains(parameters.domainId)));
        }
    }

    private static class MockParameters {
        private CommonCampaign campaign = null;
        private MobileContent mobileContent = null;
        private MobileAppForBsExport mobileApp = null;
        private String storeContentHref;
        private long domainId;
        private String domainName = null;
        private String domainFilter = null;

        public MockParameters withCampaign(CommonCampaign campaign) {
            this.campaign = campaign;
            return this;
        }

        public MockParameters withMobileContent(MobileContent mobileContent) {
            this.mobileContent = mobileContent;
            return this;
        }

        public MockParameters withMobileApp(MobileAppForBsExport mobileApp) {
            this.mobileApp = mobileApp;
            return this;
        }

        public MockParameters withStoreContentHref(String storeContentHref) {
            this.storeContentHref = storeContentHref;
            return this;
        }

        public MockParameters withDomainId(long domainId) {
            this.domainId = domainId;
            return this;
        }

        public MockParameters withDomainName(String domainName) {
            this.domainName = domainName;
            return this;
        }

        public MockParameters withDomainFilter(String domainFilter) {
            this.domainFilter = domainFilter;
            return this;
        }
    }
}
