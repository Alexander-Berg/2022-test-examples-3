package ru.yandex.direct.grid.core.entity.recommendation.service.outdoor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridOutdoorVideoRecommendationServiceTestBase;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;
import ru.yandex.direct.grid.processing.model.recommendation.GdOutdoorVideoFormat;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKpiChooseAppropriatePlacementsForBanner;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.chooseAppropriatePlacementsForBanner;

@GridCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridOutdoorVideoRecommendationServiceBannersRecommendationsTest extends GridOutdoorVideoRecommendationServiceTestBase {

    @Autowired
    private GridOutdoorVideoRecommendationForBannersService serviceUnderTest;

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void getBannersRecommendations_TwoCreativeFormats_EmptyRecommendations() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1, SIZE_2);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_1);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).isEmpty();
    }

    @Test
    public void getBannersRecommendations_OneGoodAndOneWrongPlacement_EmptyRecommendations() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DURATION_2, SIZE_1);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).isEmpty();
    }

    @Test
    public void getBannersRecommendations_SameRoundedDuration_EmptyRecommendations() {
        addBannerToAdGroup(defaultAdGroup, DURATION_2, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DURATION_3, SIZE_1);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).isEmpty();
    }

    @Test
    public void getBannersRecommendations_WrongDuration_OneRecommendation() {
        long bannerId = addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DURATION_2, SIZE_1);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).hasSize(1);
        GdiRecommendation gdiRecommendation = result.get(0);
        assertRecommendationByDefaultAdGroup(gdiRecommendation, bannerId);

        GdRecommendationKpiChooseAppropriatePlacementsForBanner recommendationKpi = getKpi(gdiRecommendation);
        GdOutdoorVideoFormat videoFormat = getVideoFormats(recommendationKpi);

        checkVideoFormat(videoFormat, SIZE_1);
    }

    @Test
    public void getBannersRecommendations_CreativeWithTwoSizes_WrongSizeTwoRecommendations() {
        long bannerId = addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1, SIZE_2);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_3);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).hasSize(1);
        GdiRecommendation gdiRecommendation = result.get(0);
        assertRecommendationByDefaultAdGroup(gdiRecommendation, bannerId);

        GdRecommendationKpiChooseAppropriatePlacementsForBanner recommendationKpi = getKpi(gdiRecommendation);
        List<GdOutdoorVideoFormat> videoFormats = recommendationKpi.getVideoFormats();
        assertThat(videoFormats).hasSize(2);

        videoFormats.sort(Comparator.comparing(GdOutdoorVideoFormat::getHeight));
        GdOutdoorVideoFormat videoFormat1 = videoFormats.get(0);
        GdOutdoorVideoFormat videoFormat2 = videoFormats.get(1);

        checkVideoFormat(videoFormat1, SIZE_1);
        checkVideoFormat(videoFormat2, SIZE_2);
    }

    @Test
    public void getBannersRecommendations_FourBanners_ThreeRecommendations() {
        long bannerId1 = addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        long bannerId2 = addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        long bannerId3 = addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_2);
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1, SIZE_3);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_3);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);

        assertThat(result).hasSize(3);
        result.sort(Comparator.comparingLong(GdiRecommendation::getBid));
        GdiRecommendation gdiRecommendation1 = result.get(0);
        GdiRecommendation gdiRecommendation2 = result.get(1);
        GdiRecommendation gdiRecommendation3 = result.get(2);

        assertRecommendationByDefaultAdGroup(gdiRecommendation1, bannerId1);
        assertRecommendationByDefaultAdGroup(gdiRecommendation2, bannerId2);
        assertRecommendationByDefaultAdGroup(gdiRecommendation3, bannerId3);

        GdRecommendationKpiChooseAppropriatePlacementsForBanner recommendationKpi1 = getKpi(gdiRecommendation1);
        GdRecommendationKpiChooseAppropriatePlacementsForBanner recommendationKpi2 = getKpi(gdiRecommendation2);
        GdRecommendationKpiChooseAppropriatePlacementsForBanner recommendationKpi3 = getKpi(gdiRecommendation3);

        GdOutdoorVideoFormat videoFormat1 = getVideoFormats(recommendationKpi1);
        GdOutdoorVideoFormat videoFormat2 = getVideoFormats(recommendationKpi2);
        GdOutdoorVideoFormat videoFormat3 = getVideoFormats(recommendationKpi3);

        checkVideoFormat(videoFormat1, SIZE_1);
        checkVideoFormat(videoFormat2, SIZE_1);
        checkVideoFormat(videoFormat3, SIZE_2);
    }

    private void checkVideoFormat(GdOutdoorVideoFormat actualFormat, Size expectedSize) {
        assertThat(actualFormat.getHeight()).isEqualTo(expectedSize.getHeight());
        assertThat(actualFormat.getWidth()).isEqualTo(expectedSize.getWidth());
        assertThat(actualFormat.getDuration()).isEqualTo(ROUNDED_DEFAULT_DURATION);
    }

    private GdOutdoorVideoFormat getVideoFormats(GdRecommendationKpiChooseAppropriatePlacementsForBanner recommendationKpi) {
        List<GdOutdoorVideoFormat> videoFormats = recommendationKpi.getVideoFormats();
        assertThat(videoFormats).hasSize(1);
        return videoFormats.get(0);
    }

    private void assertRecommendationByDefaultAdGroup(GdiRecommendation gdiRecommendation, long bannerId) {
        Long cid = gdiRecommendation.getCid();
        Long clientId = gdiRecommendation.getClientId();
        Long bid = gdiRecommendation.getBid();
        Long pid = gdiRecommendation.getPid();

        assertThat(pid).isEqualTo(defaultAdGroupId);
        assertThat(cid).isEqualTo(defaultAdGroup.getCampaignId());
        assertThat(bid).isEqualTo(bannerId);
        assertThat(clientId).isEqualTo(defaultClientIdLong);

        GdiRecommendationType type = gdiRecommendation.getType();
        assertThat(type).isEqualTo(chooseAppropriatePlacementsForBanner);
    }

}
