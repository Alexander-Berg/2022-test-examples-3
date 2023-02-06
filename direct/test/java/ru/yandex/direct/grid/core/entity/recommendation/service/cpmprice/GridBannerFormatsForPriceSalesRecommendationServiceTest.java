package ru.yandex.direct.grid.core.entity.recommendation.service.cpmprice;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.processing.model.recommendation.GdPricePackageRecommendationFormat;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKpiAddBannerFormatsForPriceSalesCorrectness;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationWithKpi;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_EXPANDED_PREVIEW_URL;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.frontpageVideoPackage;
import static ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService.GDI_RECOMMENDATION_GD_RECOMMENDATION_FUNCTION;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridBannerFormatsForPriceSalesRecommendationServiceTest {

    @Autowired
    private GridBannerFormatsForPriceSalesRecommendationService serviceUnderTest;
    @Autowired
    private CreativeRepository creativeRepository;
    @Autowired
    private Steps steps;

    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
    }

    @Test
    public void getRecommendationsForCampaigns_AdGroupWithoutBanners_NotEmptyRecommendations() {
        CpmPriceCampaign campaign = createCampaign(List.of(ViewType.DESKTOP), false);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);

        assertRecommendationsForCampaign(campaign,
                new GdPricePackageRecommendationFormat()
                        .withWidth(1456L)
                        .withHeight(180L)
                        .withHasExpand(false)
        );
    }

    @Test
    public void getRecommendationsForCampaigns_AllRequiredSizesExist_EmptyRecommendations() {
        CpmPriceCampaign campaign = createCampaign(List.of(ViewType.DESKTOP), false);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        addCreative(adGroup, 1456L, 180L, false);

        List<GdiRecommendation> recommendations = getRecommendationsForCampaigns(campaign);
        assertThat(recommendations).isEmpty();
    }

    @Test
    public void getRecommendationsForCampaigns_CampaignWithoutDefaultGroup_NotEmptyRecommendations() {
        CpmPriceCampaign campaign = createCampaign(List.of(ViewType.DESKTOP), false);

        assertRecommendationsForCampaign(campaign,
                new GdPricePackageRecommendationFormat()
                        .withWidth(1456L)
                        .withHeight(180L)
                        .withHasExpand(false)
        );
    }

    @Test
    public void getRecommendationsForCampaigns_DesktopAndNewTabWithExpand_NotEmptyRecommendations() {
        CpmPriceCampaign campaign = createCampaign(List.of(ViewType.DESKTOP, ViewType.NEW_TAB), true);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        addCreative(adGroup, 1456L, 180L, true);

        assertRecommendationsForCampaign(campaign,
                new GdPricePackageRecommendationFormat()
                        .withWidth(1456L)
                        .withHeight(180L)
                        .withHasExpand(false)
        );
    }

    @Test
    public void getRecommendationsForAdGroups_CampaignWithoutGroups_EmptyRecommendations() {
        CpmPriceCampaign campaign = createCampaign(List.of(ViewType.DESKTOP), false);

        List<GdiRecommendation> recommendations = getRecommendationsForAdGroups(campaign);
        assertThat(recommendations).isEmpty();
    }

    @Test
    public void getRecommendationsForAdGroups_CampaignWithOnlySpecificGroup_NotEmptyRecommendations() {
        CpmPriceCampaign campaign = createCampaign(List.of(ViewType.DESKTOP), false);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);
        addCreative(adGroup, 1456L, 180L, false);

        assertRecommendationsForAdGroups(campaign, List.of(adGroup),
                new GdPricePackageRecommendationFormat()
                        .withWidth(1456L)
                        .withHeight(180L)
                        .withHasExpand(false)
        );
    }

    @Test
    public void getRecommendationsForAdGroups_CampaignWithDefaultAndSpecificGroups_EmptyRecommendations() {
        CpmPriceCampaign campaign = createCampaign(List.of(ViewType.DESKTOP), false);
        CpmYndxFrontpageAdGroup defaultAdGroup =
                steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        CpmYndxFrontpageAdGroup specificAdGroup =
                steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);
        addCreative(defaultAdGroup, 1456L, 180L, false);
        addCreative(specificAdGroup, 728L, 90L, false);

        List<GdiRecommendation> recommendations = getRecommendationsForAdGroups(campaign);
        assertThat(recommendations).isEmpty();
    }

    @Test
    public void videoFrontPage_EmptyRecommendations() {
        PricePackage pricePackage = frontpageVideoPackage(clientInfo);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, clientInfo);

        List<GdiRecommendation> recommendations = getRecommendationsForCampaigns(campaign);
        assertThat(recommendations).isEmpty();
    }

    private void addCreative(AdGroup adGroup, Long width, Long height, Boolean withExpand) {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultHtml5(clientId, creativeId)
                .withWidth(width)
                .withHeight(height)
                .withExpandedPreviewUrl(withExpand ? DEFAULT_EXPANDED_PREVIEW_URL : null);
        creativeRepository.add(shard, singletonList(creative));
        OldCpmBanner cpmBanner = activeCpmBanner(adGroup.getCampaignId(), adGroup.getId(), creativeId);
        steps.bannerSteps().createActiveCpmBannerRaw(shard, cpmBanner, adGroup);
    }

    private PricePackage createPricePackage(List<ViewType> viewTypes, Boolean allowExpandedDesktopCreative) {
        PricePackage pricePackage = approvedPricePackage()
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed()
                .withViewTypes(viewTypes)
                .withAllowExpandedDesktopCreative(allowExpandedDesktopCreative);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        return pricePackage;
    }

    private CpmPriceCampaign createCampaign(List<ViewType> viewTypes, Boolean allowExpandedDesktopCreative) {
        PricePackage pricePackage = createPricePackage(viewTypes, allowExpandedDesktopCreative);
        return steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
    }

    private void assertRecommendationsForCampaign(CpmPriceCampaign campaign,
                                                  GdPricePackageRecommendationFormat... expectedFormats) {
        List<GdiRecommendation> recommendations = getRecommendationsForCampaigns(campaign);
        assertThat(recommendations).hasSize(1);

        GdiRecommendation recommendation = recommendations.get(0);
        assertThat(recommendation.getCid()).isEqualTo(campaign.getId());

        List<GdPricePackageRecommendationFormat> pricePackageFormats =
                getRecommendationPricePackageFormats(recommendation);
        assertThat(pricePackageFormats).containsExactlyInAnyOrder(expectedFormats);
    }

    private void assertRecommendationsForAdGroups(CpmPriceCampaign campaign,
                                                  List<CpmYndxFrontpageAdGroup> adGroups,
                                                  GdPricePackageRecommendationFormat... expectedFormats) {
        List<GdiRecommendation> recommendations = getRecommendationsForAdGroups(campaign);
        assertThat(recommendations).hasSize(adGroups.size());

        assertThat(recommendations).extracting("cid").containsOnly(campaign.getId());
        assertThat(recommendations).extracting("pid")
                .containsExactlyInAnyOrder(mapList(adGroups, CpmYndxFrontpageAdGroup::getId).toArray());

        assertSoftly(softly ->
                recommendations.forEach(recommendation -> {
                    List<GdPricePackageRecommendationFormat> pricePackageFormats =
                            getRecommendationPricePackageFormats(recommendation);
                    softly.assertThat(pricePackageFormats).containsExactlyInAnyOrder(expectedFormats);
                })
        );
    }

    private List<GdiRecommendation> getRecommendationsForCampaigns(CpmPriceCampaign campaign) {
        return serviceUnderTest.getRecommendationsForCampaigns(shard, clientId, singleton(campaign.getId()));
    }

    private List<GdiRecommendation> getRecommendationsForAdGroups(CpmPriceCampaign campaign) {
        return serviceUnderTest.getRecommendationsForAdGroups(shard, clientId, singleton(campaign.getId()));
    }

    private List<GdPricePackageRecommendationFormat> getRecommendationPricePackageFormats(
            GdiRecommendation gdiRecommendation) {
        GdRecommendationWithKpi gdRecommendation =
                (GdRecommendationWithKpi) GDI_RECOMMENDATION_GD_RECOMMENDATION_FUNCTION.apply(gdiRecommendation);
        return ((GdRecommendationKpiAddBannerFormatsForPriceSalesCorrectness) gdRecommendation.getKpi())
                .getPricePackageFormats();
    }
}
