package ru.yandex.direct.grid.core.entity.recommendation.service;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;
import ru.yandex.direct.grid.processing.model.recommendation.GdOutdoorVideoFormat;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKpiChooseAppropriatePlacementsForAdGroup;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKpiChooseAppropriatePlacementsForBanner;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKpiUploadAppropriateCreatives;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationPlacementRatioInfo;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.chooseAppropriatePlacementsForAdGroup;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.chooseAppropriatePlacementsForBanner;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.uploadAppropriateCreatives;

@GridCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridRecommendationServiceOutdoorVideoRecommendationsTest extends GridOutdoorVideoRecommendationServiceTestBase {

    @Autowired
    private GridRecommendationService serviceUnderTest;
    @Autowired
    private AdGroupSteps adGroupSteps;

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void getOutdoorVideoRecommendations_RightSizes_EmptyRecommendations() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1, SIZE_2);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_1, SIZE_2);
        Set<GdiRecommendationType> allTypes = Set.of(uploadAppropriateCreatives,
                chooseAppropriatePlacementsForAdGroup,
                chooseAppropriatePlacementsForBanner);
        List<GdiRecommendation> outdoorVideoRecommendations = serviceUnderTest.getOutdoorVideoRecommendations(shard,
                defaultClientIdLong, allTypes, null, singleton(defaultAdGroupId));
        assertThat(outdoorVideoRecommendations, hasSize(0));
    }

    @Test
    public void getOutdoorVideoRecommendations_CampaignWithDifferentAdGroupTypes_IgnoreCpmBannerType() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_2);
        Long campaignId = defaultAdGroup.getCampaignId();
        adGroupSteps.createAdGroup(activeCpmBannerAdGroup(campaignId), defaultAdGroup.getCampaignInfo());
        List<GdiRecommendation> outdoorVideoRecommendations = serviceUnderTest.getOutdoorVideoRecommendations(shard,
                defaultClientIdLong, singleton(uploadAppropriateCreatives), singleton(campaignId), null);
        assertThat(outdoorVideoRecommendations, hasSize(1));
        GdiRecommendation gdiRecommendation = outdoorVideoRecommendations.get(0);
        checkPlacementRecommendation(gdiRecommendation, SIZE_3);
    }

    @Test
    public void getOutdoorVideoRecommendations_CheckAllTypes() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_2);
        Set<GdiRecommendationType> allTypes = Set.of(uploadAppropriateCreatives,
                chooseAppropriatePlacementsForAdGroup,
                chooseAppropriatePlacementsForBanner);
        List<GdiRecommendation> outdoorVideoRecommendations = serviceUnderTest.getOutdoorVideoRecommendations(shard,
                defaultClientIdLong, allTypes, null, singleton(defaultAdGroupId));
        assertThat(outdoorVideoRecommendations, hasSize(3));
        var placementRecommendation = getByType(outdoorVideoRecommendations, uploadAppropriateCreatives);
        var adGroupRecommendation = getByType(outdoorVideoRecommendations, chooseAppropriatePlacementsForAdGroup);
        var bannerRecommendation = getByType(outdoorVideoRecommendations, chooseAppropriatePlacementsForBanner);
        checkPlacementRecommendation(placementRecommendation, SIZE_3);
        checkAdGroupRecommendation(adGroupRecommendation, DEFAULT_RATIO);
        checkBannerRecommendation(bannerRecommendation, SIZE_1);
    }

    private void checkPlacementRecommendation(GdiRecommendation placementRecommendation, Size expectedSize) {
        GdRecommendationKpiUploadAppropriateCreatives placementRecommendationKpi = getKpi(placementRecommendation);
        assertThat(placementRecommendationKpi.getIsAdGroupHasShows(), is(false));
        List<GdOutdoorVideoFormat> videoFormats = placementRecommendationKpi.getVideoFormats();
        assertThat(videoFormats, hasSize(1));
        checkVideoFormat(videoFormats.get(0), expectedSize);
    }

    private void checkAdGroupRecommendation(GdiRecommendation adGroupRecommendation, String ratio) {
        GdRecommendationKpiChooseAppropriatePlacementsForAdGroup kpi = getKpi(adGroupRecommendation);
        List<GdRecommendationPlacementRatioInfo> ratioInfoList = kpi.getRatioInfoList();
        assertThat(ratioInfoList, hasSize(1));
        GdRecommendationPlacementRatioInfo ratioInfo = ratioInfoList.get(0);
        assertThat(ratioInfo.getCreativesCount(), is(1));
        checkVideoFormat(ratioInfo.getVideoFormat(), ratio);
    }

    private void checkBannerRecommendation(GdiRecommendation bannerRecommendation, Size expectedSize) {
        GdRecommendationKpiChooseAppropriatePlacementsForBanner bannerRecommendationKpi = getKpi(bannerRecommendation);
        List<GdOutdoorVideoFormat> videoFormats = bannerRecommendationKpi.getVideoFormats();
        assertThat(videoFormats, hasSize(1));
        checkVideoFormat(videoFormats.get(0), expectedSize);
    }

    private void checkVideoFormat(GdOutdoorVideoFormat actualFormat, String ratio) {
        String[] wxh2 = ratio.split(":");
        int expectedWidth = Integer.parseInt(wxh2[0]);
        int expectedHeight = Integer.parseInt(wxh2[1]);
        checkVideoFormat(actualFormat, new Size(expectedWidth, expectedHeight));
    }

    private void checkVideoFormat(GdOutdoorVideoFormat actualFormat, Size expectedSize) {
        assertThat(actualFormat.getWidth(), is(expectedSize.getWidth()));
        assertThat(actualFormat.getHeight(), is(expectedSize.getHeight()));
        assertThat(actualFormat.getDuration(), is(ROUNDED_DEFAULT_DURATION));
    }

    private GdiRecommendation getByType(List<GdiRecommendation> outdoorVideoRecommendations,
                                        GdiRecommendationType type) {
        return outdoorVideoRecommendations.stream().filter(x -> x.getType().equals(type)).findAny().orElseThrow();
    }
}
