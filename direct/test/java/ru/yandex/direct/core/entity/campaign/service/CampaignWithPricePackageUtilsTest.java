package ru.yandex.direct.core.entity.campaign.service;

import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithPricePackage;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.CampaignWithPricePackageUtils.isCampaignFullWithBanners;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignWithPricePackageUtilsTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    private ClientInfo client;

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void isCampaignFullWithBanners_Success() {
        CpmPriceCampaign campaign = createCampaign(List.of(ViewType.DESKTOP));
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                client);
        OldCpmBanner banner1 = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);
        OldCpmBanner banner2 = createBannerWithCreativeFormat(defaultAdGroup, 768L, 240L);

        assertThat(isCampaignFullWithCurrentBanners(campaign)).isTrue();
    }

    @Test
    public void isCampaignFullWithBanners_EmptyCampaignViewTypes() {
        CpmPriceCampaign campaign = createCampaign(emptyList());
        assertThat(isCampaignFullWithCurrentBanners(campaign)).isFalse();
    }

    @Test
    public void isCampaignFullWithBanners_NoActiveBannerForFormat() {
        CpmPriceCampaign campaign = createCampaign(List.of(ViewType.DESKTOP));
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                client);
        Creative creative = createCreativeWithFormat(1456L, 180L);
        OldCpmBanner banner = activeCpmBanner(defaultAdGroup.getCampaignId(), defaultAdGroup.getId(), creative.getId())
                .withStatusShow(false);
        steps.bannerSteps().createActiveCpmBannerRaw(client.getShard(), banner, defaultAdGroup);

        assertThat(isCampaignFullWithCurrentBanners(campaign)).isFalse();
    }

    private PricePackage createPricePackage(List<ViewType> viewTypes) {
        PricePackage pricePackage = approvedPricePackage()
                .withClients(List.of(allowedPricePackageClient(client)));
        pricePackage.getTargetingsFixed()
                .withViewTypes(viewTypes)
                .withAllowExpandedDesktopCreative(false);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        return pricePackage;
    }

    private CpmPriceCampaign createCampaign(List<ViewType> viewTypes) {
        PricePackage pricePackage = createPricePackage(viewTypes);
        return steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackage);
    }

    private OldCpmBanner createBannerWithCreativeFormat(AdGroup adGroup, Long width, Long height) {
        Creative creative = createCreativeWithFormat(width, height);
        OldCpmBanner banner = activeCpmBanner(adGroup.getCampaignId(), adGroup.getId(), creative.getId());
        steps.bannerSteps().createActiveCpmBannerRaw(client.getShard(), banner, adGroup);
        return banner;
    }

    private Creative createCreativeWithFormat(Long width, Long height) {
        Creative creative = defaultHtml5(client.getClientId(), steps.creativeSteps().getNextCreativeId())
                .withWidth(width)
                .withHeight(height)
                .withExpandedPreviewUrl(null);
        creativeRepository.add(client.getShard(), singletonList(creative));
        return creative;
    }

    private boolean isCampaignFullWithCurrentBanners(CpmPriceCampaign campaign) {
        int shard = client.getShard();
        var banners = StreamEx.of(bannerTypedRepository
                .getBannersByCampaignIds(shard, List.of(campaign.getId())))
                .map(b -> (BannerWithPricePackage) b)
                .toList();
        List<Long> bannerIds = mapList(banners, Banner::getId);
        Map<Long, Creative> creatives = creativeRepository.getCreativesByBannerIds(client.getShard(), bannerIds);
        return isCampaignFullWithBanners(campaign, banners, creatives);
    }

}
