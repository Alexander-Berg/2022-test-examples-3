package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.List;
import java.util.Map;

import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.banner.resources.PlatformName;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithHrefForBsExport;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.TestPlatformNames;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.HrefsInfo;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.BsDomainIdGenerationService;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.DomainFilterService;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.HrefAndSite;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.HrefAndSiteService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BannerHrefLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private BannerHrefLoader loader;
    private BsOrderIdCalculator bsOrderIdCalculator;
    private HrefAndSiteService hrefAndSiteService;
    private CampaignTypedRepository campaignTypedRepository;
    private DomainFilterService domainFilterService;
    private BsDomainIdGenerationService bsDomainIdGenerationService;
    private AdGroupRepository adGroupRepository;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.hrefAndSiteService = mock(HrefAndSiteService.class);
        var hostingsHandler = new HostingsHandler(List.of(), List.of());
        this.campaignTypedRepository = mock(CampaignTypedRepository.class);
        this.domainFilterService = mock(DomainFilterService.class);
        this.bsDomainIdGenerationService = mock(BsDomainIdGenerationService.class);
        this.adGroupRepository = mock(AdGroupRepository.class);
        this.loader = new BannerHrefLoader(context, hostingsHandler, hrefAndSiteService, campaignTypedRepository,
                bsDomainIdGenerationService, domainFilterService, adGroupRepository);
    }

    @Test
    @Description("Проверяем для случайного href, PlatformName должен быть UNKNOWN")
    void moderateBannerTest() {
        moderateBanner(
                "https://my-site.com?param=1",
                "www.мой-сайт.ком",
                "www.my-site.com",
                "my-site.com",
                null);
    }

    @Test
    @Description("Проверяем для href для которого есть PlatformName")
    void moderateBannerPlatformNameTest() {
        moderateBanner(
                "https://maps.yandex.ru/",
                "www.maps.yandex.ru",
                "www.maps.yandex.ru",
                "maps.yandex.ru",
                TestPlatformNames.YANDEX_MAPS_RU);
    }

    void moderateBanner(String href, String domain, String domainAscii, String siteFilter,
                        PlatformName platformName) {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_HREF)
                        .build();

        var bannerFromDb = getBannerWithCommonFields();
        bannerFromDb.withStatusModerate(BannerStatusModerate.YES)
                .withHref(href)
                .withDomain(domain)
                .withLanguage(Language.RU_);

        var campaign = getCampaign();
        var mockParams =
                new MockServicesParams()
                        .withBannerFromDb(bannerFromDb)
                        .withCampaign(campaign)
                        .withHref(href)
                        .withDomain(domain)
                        .withDomainAscii(domainAscii)
                        // domain filter - главное зеркало домена
                        .withDomainFilter("my-super-site.com")
                        //site filter должен получиться из domainAscii без www
                        .withSiteFilter(siteFilter)
                        .withDomainFilterId(1234L)
                        .withSiteFilterId(4321L);
        mockServices(mockParams);
        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(HrefsInfo.builder()
                        .withHref(href)
                        .withSite(domain)
                        .withDomainFilter("my-super-site.com")
                        .withSiteFilter(siteFilter)
                        .withDomainFilterId(1234L)
                        .withSiteFilterId(4321L)
                        .withPlatformName(platformName)
                        .build())
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Если у баннера тип группы mobile_content, то такой баннер не должен обрабатываться этим loader'ом
     */
    @Test
    void bannerWithMobileContentAdGroupTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_HREF)
                        .build();

        var bannerFromDb = getBannerWithCommonFields();
        var href = "https://my-site.com?param=1";
        var domain = "www.мой-сайт.ком";
        bannerFromDb.withStatusModerate(BannerStatusModerate.YES)
                .withHref(href)
                .withDomain(domain);

        // получаем баннер из бд
        doReturn(List.of(bannerFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithHrefForBsExport.class));

        // получаем тип группы из базы
        var adgroupId = bannerFromDb.getAdGroupId();
        doReturn(Map.of(adgroupId, AdGroupType.MOBILE_CONTENT))
                .when(adGroupRepository)
                .getAdGroupTypesByIds(anyInt(), argThat(ids -> ids.contains(adgroupId)));


        var res = loader.loadResources(SHARD, List.of(object));
        verify(campaignTypedRepository, never()).getSafely(any(), any(), anyCollection());
        verify(hrefAndSiteService, never()).extract(any(), any());
        verify(domainFilterService, never()).getDomainsFilters(anyCollection());
        verify(bsDomainIdGenerationService, never()).generate(anyCollection());

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Если для баннер не промодеерирован, то по баннеру не будут извелечены ссылки и домены
     */
    @Test
    @SuppressWarnings("unchecked")
    void notModerateBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_HREF)
                        .build();

        var bannerFromDb = getBannerWithCommonFields();
        var href = "https://my-site.com?param=1";
        var domain = "www.мой-сайт.ком";
        bannerFromDb.withStatusModerate(BannerStatusModerate.NO)
                .withHref(href)
                .withDomain(domain);

        var campaign = getCampaign();
        doReturn(List.of(campaign))
                .when(campaignTypedRepository)
                .getSafely(anyInt(), (List<Long>) argThat(v -> contains(campaign.getId()).matches(v)),
                        eq(CommonCampaign.class));
        verify(hrefAndSiteService, never()).extract(any(), any());
        verify(domainFilterService, never()).getDomainsFilters(anyCollection());
        verify(bsDomainIdGenerationService, never()).generate(anyCollection());
        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);

        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);

    }

    /**
     * Если для баннера кампания удалена, то по баннеру не будут извелечены ссылки и домены
     */
    @Test
    void moderateBannerWithDeletedCampaignTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_HREF)
                        .build();

        var bannerFromDb = getBannerWithCommonFields();
        var href = "https://my-site.com?param=1";
        var domain = "www.my-site.com";
        bannerFromDb.withStatusModerate(BannerStatusModerate.YES)
                .withHref(href)
                .withDomain(domain);
        doReturn(List.of(bannerFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithHrefForBsExport.class));

        doReturn(List.of()).when(campaignTypedRepository).getSafely(anyInt(), anyCollection(),
                eq(CommonCampaign.class));
        verify(hrefAndSiteService, never()).extract(any(), any());
        verify(domainFilterService, never()).getDomainsFilters(anyCollection());
        verify(bsDomainIdGenerationService, never()).generate(anyCollection());


        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);

        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @SuppressWarnings("unchecked")
    private void mockServices(MockServicesParams params) {
        // получаем баннер из бд
        doReturn(List.of(params.bannerFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithHrefForBsExport.class));
        // получаем кампанию из бд
        doReturn(List.of(params.campaign))
                .when(campaignTypedRepository)
                .getSafely(anyInt(), (List<Long>) argThat(v -> contains(params.campaign.getId()).matches(v)),
                        eq(CommonCampaign.class));

        // получаем тип группы из базы
        var adgroupId = params.bannerFromDb.getAdGroupId();
        doReturn(Map.of(adgroupId, params.adGroupType))
                .when(adGroupRepository)
                .getAdGroupTypesByIds(anyInt(), argThat(ids -> ids.contains(adgroupId)));

        // получаем ссылку у домен из баннера
        doReturn(new HrefAndSite(params.href, params.domain, params.domainAscii))
                .when(hrefAndSiteService).extract(eq(params.bannerFromDb), eq(params.campaign));

        // получаем главное зеркало домена
        doReturn(Map.of(params.domainAscii, params.domainFilter))
                .when(domainFilterService).getDomainsFilters(argThat(domains -> domains.contains(params.domainAscii)));

        // получаем id доменов в БК для domainFilter и siteFilter
        doReturn(Map.of(params.domainFilter, params.domainFilterId, params.siteFilter, params.siteFilterId))
                .when(bsDomainIdGenerationService)
                .generate(argThat(domains -> domains.containsAll(List.of(params.domainFilter, params.siteFilter))));

    }

    private BannerResource.Builder<HrefsInfo> getResourceWithCommonFields() {
        return new BannerResource.Builder<HrefsInfo>()
                .setBid(1L)
                .setPid(3L)
                .setCid(5L)
                .setBsBannerId(40L)
                .setOrderId(30L);
    }

    private BannerWithHrefForBsExport getBannerWithCommonFields() {
        return new TextBanner()
                .withId(1L)
                .withAdGroupId(3L)
                .withCampaignId(5L)
                .withBsBannerId(40L);
    }

    private CommonCampaign getCampaign() {
        return new TextCampaign()
                .withId(5L);
    }

    private void mockOrderIdCalculator() {
        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection())).thenReturn(Map.of(5L, 30L));
    }

    private static class MockServicesParams {
        private BannerWithHrefForBsExport bannerFromDb;
        private CommonCampaign campaign;
        private AdGroupType adGroupType = AdGroupType.BASE;
        private String href;
        private String domain;
        private String domainAscii;
        private String domainFilter;
        private String siteFilter;
        private Long domainFilterId;
        private Long siteFilterId;

        public MockServicesParams withBannerFromDb(BannerWithHrefForBsExport bannerFromDb) {
            this.bannerFromDb = bannerFromDb;
            return this;
        }

        public MockServicesParams withCampaign(CommonCampaign campaign) {
            this.campaign = campaign;
            return this;
        }

        public MockServicesParams withAdGroupType(AdGroupType adGroupType) {
            this.adGroupType = adGroupType;
            return this;
        }

        public MockServicesParams withHref(String href) {
            this.href = href;
            return this;
        }

        public MockServicesParams withDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public MockServicesParams withDomainAscii(String domainAscii) {
            this.domainAscii = domainAscii;
            return this;
        }

        public MockServicesParams withDomainFilter(String domainFilter) {
            this.domainFilter = domainFilter;
            return this;
        }

        public MockServicesParams withSiteFilter(String siteFilter) {
            this.siteFilter = siteFilter;
            return this;
        }

        public MockServicesParams withDomainFilterId(Long domainFilterId) {
            this.domainFilterId = domainFilterId;
            return this;
        }

        public MockServicesParams withSiteFilterId(Long siteFilterId) {
            this.siteFilterId = siteFilterId;
            return this;
        }
    }
}
