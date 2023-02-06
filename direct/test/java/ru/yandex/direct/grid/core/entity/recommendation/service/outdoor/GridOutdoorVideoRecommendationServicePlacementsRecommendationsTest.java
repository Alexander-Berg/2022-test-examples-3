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
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKpiUploadAppropriateCreatives;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestFfmpegResolution.R_31_288p;
import static ru.yandex.direct.grid.core.entity.recommendation.service.outdoor.GridOutdoorVideoRecommendationForPlacementsService.increaseSidesToExactRatio;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.uploadAppropriateCreatives;

@GridCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridOutdoorVideoRecommendationServicePlacementsRecommendationsTest extends GridOutdoorVideoRecommendationServiceTestBase {

    @Autowired
    private GridOutdoorVideoRecommendationForPlacementsService serviceUnderTest;

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void getPlacementsRecommendations_TwoCreativeFormats_EmptyRecommendations() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1, SIZE_2);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_1);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).isEmpty();
    }

    @Test
    public void getPlacementsRecommendations_NonexistentSize_EmptyRecommendations() {
        Size nonexistentPageBlockSize = new Size(0, 0);
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, nonexistentPageBlockSize);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).isEmpty();
    }

    @Test
    public void getPlacementsRecommendations_TwoPlacementsWithSameSize_WrongDurationForOnePlacement() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DURATION_2, SIZE_1);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).hasSize(1);
        GdiRecommendation gdiRecommendation = result.get(0);

        assertRecommendationByDefaultAdGroup(gdiRecommendation);

        GdRecommendationKpiUploadAppropriateCreatives recommendationKpi = getKpi(gdiRecommendation);
        GdOutdoorVideoFormat videoFormat = getVideoFormats(recommendationKpi);

        assertThat(recommendationKpi.getIsAdGroupHasShows()).isTrue();
        checkVideoFormat(videoFormat, SIZE_3, ROUNDED_DURATION_2);
    }

    @Test
    public void getPlacementsRecommendations_TwoPlacementsWithSameSizeAndWrongDuration_TwoFormatsRecommendation() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DURATION_2, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DURATION_4, SIZE_1);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).hasSize(1);
        GdiRecommendation gdiRecommendation = result.get(0);

        assertRecommendationByDefaultAdGroup(gdiRecommendation);

        GdRecommendationKpiUploadAppropriateCreatives recommendationKpi = getKpi(gdiRecommendation);
        assertThat(recommendationKpi.getIsAdGroupHasShows()).isFalse();

        List<GdOutdoorVideoFormat> videoFormats = recommendationKpi.getVideoFormats();
        assertThat(videoFormats).hasSize(2);
        videoFormats.sort(Comparator.comparingDouble(GdOutdoorVideoFormat::getDuration));
        GdOutdoorVideoFormat videoFormat1 = videoFormats.get(0);
        GdOutdoorVideoFormat videoFormat2 = videoFormats.get(1);

        checkVideoFormat(videoFormat1, SIZE_3, ROUNDED_DURATION_2);
        checkVideoFormat(videoFormat2, SIZE_3, ROUNDED_DURATION_4);
    }

    @Test
    public void getPlacementsRecommendations_ThreePlacementsWithSameRatio_RecommendMaxRatioResolution() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_1, SIZE_2, SIZE_3);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).hasSize(1);
        GdiRecommendation gdiRecommendation = result.get(0);

        assertRecommendationByDefaultAdGroup(gdiRecommendation);

        GdRecommendationKpiUploadAppropriateCreatives recommendationKpi = getKpi(gdiRecommendation);
        GdOutdoorVideoFormat videoFormat = getVideoFormats(recommendationKpi);

        assertThat(recommendationKpi.getIsAdGroupHasShows()).isTrue();
        checkVideoFormat(videoFormat, SIZE_3);
    }

    @Test
    public void getPlacementsRecommendations_MaxFfmpegResolutionIsNotStandard_RecommendFittedMax() {
        Size expected3to1Ratio = new Size(1920, 640);
        Size size3to1Ratio = new Size(R_31_288p);
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, size3to1Ratio);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).hasSize(1);
        GdiRecommendation gdiRecommendation = result.get(0);

        assertRecommendationByDefaultAdGroup(gdiRecommendation);

        GdRecommendationKpiUploadAppropriateCreatives recommendationKpi = getKpi(gdiRecommendation);
        GdOutdoorVideoFormat videoFormat = getVideoFormats(recommendationKpi);

        assertThat(recommendationKpi.getIsAdGroupHasShows()).isFalse();
        checkVideoFormat(videoFormat, expected3to1Ratio);
    }

    @Test
    public void increaseSidesToExactRatio_NothingChanged() {
        int sourceWidth = 10;
        int sourceHeight = 5;
        int ratioWidth = 2;
        int ratioHeight = 1;
        double duration = 1;
        var result = increaseSidesToExactRatio(sourceWidth, sourceHeight, ratioWidth, ratioHeight, duration);
        checkIncreasedSize(duration, result, sourceWidth, sourceHeight);
    }

    @Test
    public void increaseSidesToExactRatio_MultipliersEquals_TwoSidesChangedByRemainder() {
        int sourceWidth = 7;
        int sourceHeight = 5;
        int ratioWidth = 3;
        int ratioHeight = 2;
        double duration = 1;
        var result = increaseSidesToExactRatio(sourceWidth, sourceHeight, ratioWidth, ratioHeight, duration);

        int expectedWidth = 9;
        int expectedHeight = 6;
        checkIncreasedSize(duration, result, expectedWidth, expectedHeight);
    }

    @Test
    public void increaseSidesToExactRatio_MultipliersNotEquals_TwoSidesChangedByRemainderAndMultiplier() {
        int sourceWidth = 10;
        int sourceHeight = 5;
        int ratioWidth = 3;
        int ratioHeight = 2;
        double duration = 1;
        var result = increaseSidesToExactRatio(sourceWidth, sourceHeight, ratioWidth, ratioHeight, duration);

        int expectedWidth = 12;
        int expectedHeight = 8;
        checkIncreasedSize(duration, result, expectedWidth, expectedHeight);
    }

    @Test
    public void increaseSidesToExactRatio_RemainderIsZero_OneSideChangedByMultiplier() {
        int sourceWidth = 12;
        int sourceHeight = 6;
        int ratioWidth = 3;
        int ratioHeight = 2;
        double duration = 1;
        var result = increaseSidesToExactRatio(sourceWidth, sourceHeight, ratioWidth, ratioHeight, duration);

        int expectedWidth = 12;
        int expectedHeight = 8;
        checkIncreasedSize(duration, result, expectedWidth, expectedHeight);
    }

    @Test
    public void increaseSidesToExactRatio_MultipliersEquals_OneSideChangedByRemainder() {
        int sourceWidth = 3;
        int sourceHeight = 2;
        int ratioWidth = 2;
        int ratioHeight = 1;
        double duration = 1;
        var result = increaseSidesToExactRatio(sourceWidth, sourceHeight, ratioWidth, ratioHeight, duration);

        int expectedWidth = 4;
        int expectedHeight = 2;
        checkIncreasedSize(duration, result, expectedWidth, expectedHeight);
    }

    private void checkIncreasedSize(double duration, OutdoorVideoFormat result, int expectedWidth, int expectedHeight) {
        assertThat(result.getWidth()).isEqualTo(expectedWidth);
        assertThat(result.getHeight()).isEqualTo(expectedHeight);
        assertThat(result.getDuration()).isEqualTo(duration);
    }

    private GdOutdoorVideoFormat getVideoFormats(GdRecommendationKpiUploadAppropriateCreatives recommendationKpi) {
        List<GdOutdoorVideoFormat> videoFormats = recommendationKpi.getVideoFormats();
        assertThat(videoFormats).hasSize(1);
        return videoFormats.get(0);
    }

    private void checkVideoFormat(GdOutdoorVideoFormat actualFormat, Size expectedSize) {
        checkVideoFormat(actualFormat, expectedSize, ROUNDED_DEFAULT_DURATION);
    }

    private void checkVideoFormat(GdOutdoorVideoFormat actualFormat, Size expectedSize, double expectedDuration) {
        assertThat(actualFormat.getHeight()).isEqualTo(expectedSize.getHeight());
        assertThat(actualFormat.getWidth()).isEqualTo(expectedSize.getWidth());
        assertThat(actualFormat.getDuration()).isEqualTo(expectedDuration);
    }

    private void assertRecommendationByDefaultAdGroup(GdiRecommendation gdiRecommendation) {
        Long cid = gdiRecommendation.getCid();
        Long clientId = gdiRecommendation.getClientId();
        Long bid = gdiRecommendation.getBid();
        Long pid = gdiRecommendation.getPid();

        assertThat(pid).isEqualTo(defaultAdGroupId);
        assertThat(cid).isEqualTo(defaultAdGroup.getCampaignId());
        assertThat(bid).isEqualTo(0L);
        assertThat(clientId).isEqualTo(defaultClientIdLong);

        GdiRecommendationType type = gdiRecommendation.getType();
        assertThat(type).isEqualTo(uploadAppropriateCreatives);
    }

}
