package ru.yandex.direct.grid.processing.service.banner;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldImageCreativeBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldMcBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldMobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAdsMassAction;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageCreativeBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMcBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMobileAppBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class BannersMassActionsServiceBaseTest {

    static final String TEST_TITLE = "Тестовый баннер";
    static final String TEST_HREF = "http://banner.ru";
    static final String BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    totalCount,\n"
            + "    successCount,\n"
            + "    processedAdIds,\n"
            + "    skippedAdIds,\n"
            + "  }\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    GridGraphQLProcessor processor;

    User operator;
    ClientInfo clientInfo;

    @Autowired
    Steps steps;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
    }

    GdAdsMassAction createRequest(OldBanner... banners) {
        return new GdAdsMassAction()
                .withAdIds(mapList(Arrays.asList(banners), OldBanner::getId));
    }

    GdAdsMassAction createRequest(List<Long> bannersIds) {
        return new GdAdsMassAction()
                .withAdIds(bannersIds);
    }

    OldBanner createTextBanner(AdGroupInfo adGroupInfo,
                               boolean isShowing,
                               boolean isArchived,
                               boolean isSentToBS,
                               OldBannerStatusModerate statusModerate) {
        return createTextBanner(adGroupInfo, isShowing, isArchived, isSentToBS, statusModerate, TEST_HREF);
    }

    OldBanner createTextBanner(AdGroupInfo adGroupInfo,
                               boolean isShowing,
                               boolean isArchived,
                               boolean isSentToBS,
                               OldBannerStatusModerate statusModerate,
                               String href) {
        OldTextBanner banner = activeTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withTitle(TEST_TITLE)
                .withHref(href)
                .withBsBannerId(isSentToBS ? RandomUtils.nextLong(0, Long.MAX_VALUE) : 0L)
                .withStatusActive(isSentToBS)
                .withStatusShow(isShowing)
                .withStatusModerate(statusModerate)
                .withStatusArchived(isArchived);
        return steps.bannerSteps().createBanner(banner, adGroupInfo).getBanner();
    }

    OldBanner createSmartBanner(AdGroupInfo adGroupInfo,
                                boolean isShowing,
                                boolean isArchived,
                                boolean isSentToBS,
                                OldBannerStatusModerate statusModerate) {
        Creative creative = defaultPerformanceCreative(clientInfo.getClientId(), null)
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        OldPerformanceBanner banner = activePerformanceBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(),
                creativeInfo.getCreativeId())
                .withHref(TEST_HREF)
                .withBsBannerId(isSentToBS ? RandomUtils.nextLong(0, Long.MAX_VALUE) : 0L)
                .withStatusActive(isSentToBS)
                .withStatusShow(isShowing)
                .withStatusModerate(statusModerate)
                .withStatusArchived(isArchived);
        return steps.bannerSteps().createBanner(banner, adGroupInfo).getBanner();
    }

    OldBanner createMcBanner(AdGroupInfo adGroupInfo,
                             boolean isShowing,
                             boolean isArchived,
                             boolean isSentToBS,
                             OldBannerStatusModerate statusModerate) {
        return createMcBanner(adGroupInfo, isShowing, isArchived, isSentToBS, statusModerate, TEST_HREF);
    }

    OldBanner createMcBanner(AdGroupInfo adGroupInfo,
                             boolean isShowing,
                             boolean isArchived,
                             boolean isSentToBS,
                             OldBannerStatusModerate statusModerate,
                             String href) {
        OldMcBanner banner = activeMcBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withHref(href)
                .withBsBannerId(isSentToBS ? RandomUtils.nextLong(0, Long.MAX_VALUE) : 0L)
                .withStatusActive(isSentToBS)
                .withStatusShow(isShowing)
                .withStatusModerate(statusModerate)
                .withStatusArchived(isArchived);
        return steps.bannerSteps().createBanner(banner, adGroupInfo).getBanner();
    }

    OldBanner createImageHashBanner(AdGroupInfo adGroupInfo, String href) {
        OldImageHashBanner banner = activeImageHashBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withHref(href)
                .withBsBannerId(0L)
                .withStatusActive(false)
                .withStatusShow(true)
                .withStatusModerate(OldBannerStatusModerate.SENDING)
                .withStatusArchived(false);
        return steps.bannerSteps().createBanner(banner, adGroupInfo).getBanner();
    }

    OldBanner createImageCreativeBanner(AdGroupInfo adGroupInfo, Long creativeId, String href) {
        Long campaignId = adGroupInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        OldImageCreativeBanner banner = activeImageCreativeBanner(campaignId, adGroupId, creativeId)
                .withHref(href)
                .withBsBannerId(0L)
                .withStatusActive(false)
                .withStatusShow(true)
                .withStatusModerate(OldBannerStatusModerate.SENDING)
                .withStatusArchived(false);
        return steps.bannerSteps().createBanner(banner, adGroupInfo).getBanner();
    }

    OldBanner createCpmBanner(AdGroupInfo adGroupInfo, Long creativeId, String href) {
        Long campaignId = adGroupInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, creativeId)
                .withHref(href)
                .withBsBannerId(0L)
                .withStatusActive(false)
                .withStatusShow(true)
                .withStatusModerate(OldBannerStatusModerate.SENDING)
                .withStatusArchived(false);
        return steps.bannerSteps().createBanner(banner, adGroupInfo).getBanner();
    }

    OldBanner createCpmVideoBanner(AdGroupInfo adGroupInfo, Long creativeId, String href) {
        Long campaignId = adGroupInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        OldCpmBanner banner = activeCpmVideoBanner(campaignId, adGroupId, creativeId)
                .withHref(href)
                .withBsBannerId(0L)
                .withStatusActive(false)
                .withStatusShow(true)
                .withStatusModerate(OldBannerStatusModerate.SENDING)
                .withStatusArchived(false);
        return steps.bannerSteps().createBanner(banner, adGroupInfo).getBanner();
    }

    OldBanner createMobileAppBanner(AdGroupInfo adGroupInfo, String href) {
        Long campaignId = adGroupInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        OldMobileAppBanner banner = activeMobileAppBanner(campaignId, adGroupId)
                .withHref(href)
                .withBsBannerId(0L)
                .withStatusActive(false)
                .withStatusShow(true)
                .withStatusModerate(OldBannerStatusModerate.SENDING)
                .withStatusArchived(false);
        return steps.bannerSteps().createBanner(banner, adGroupInfo).getBanner();
    }

    OldBanner createDynamicBanner(AdGroupInfo adGroupInfo, String href) {
        Long campaignId = adGroupInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        OldDynamicBanner banner = activeDynamicBanner(campaignId, adGroupId)
                .withHref(href)
                .withBsBannerId(0L)
                .withStatusActive(false)
                .withStatusShow(true)
                .withStatusModerate(OldBannerStatusModerate.SENDING)
                .withStatusArchived(false);
        return steps.bannerSteps().createBanner(banner, adGroupInfo).getBanner();
    }

    NewBannerInfo createInternalBanner(AdGroupInfo adGroupInfo) {
        return steps.internalBannerSteps().createModeratedInternalBanner(adGroupInfo,
                BannerStatusModerate.NEW);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }
}
