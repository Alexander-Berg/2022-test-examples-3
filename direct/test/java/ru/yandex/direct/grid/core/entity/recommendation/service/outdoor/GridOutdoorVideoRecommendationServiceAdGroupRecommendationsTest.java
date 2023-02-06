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

import ru.yandex.direct.core.testing.data.TestFfmpegResolution;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridOutdoorVideoRecommendationServiceTestBase;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;
import ru.yandex.direct.grid.processing.model.recommendation.GdOutdoorVideoFormat;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKpiChooseAppropriatePlacementsForAdGroup;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationPlacementRatioInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestFfmpegResolution.R_31_576p;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.chooseAppropriatePlacementsForAdGroup;

@GridCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridOutdoorVideoRecommendationServiceAdGroupRecommendationsTest extends GridOutdoorVideoRecommendationServiceTestBase {

    @Autowired
    private GridOutdoorVideoRecommendationForAdGroupService serviceUnderTest;

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void getAdGroupRecommendations_OneFormatFit_EmptyRecommendations() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1, SIZE_2);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_2, SIZE_3);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).isEmpty();
    }

    @Test
    public void getAdGroupRecommendations_OneGoodAndOneWrongPlacement_EmptyRecommendations() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DURATION_2, SIZE_1);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).isEmpty();
    }

    @Test
    public void getAdGroupRecommendations_SameRoundedDuration_EmptyRecommendations() {
        addBannerToAdGroup(defaultAdGroup, DURATION_2, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DURATION_3, SIZE_1);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).isEmpty();
    }

    @Test
    public void getAdGroupRecommendations_SameRatio_OneRecommendation() {
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_2, SIZE_3);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).hasSize(1);
        GdiRecommendation gdiRecommendation = result.get(0);

        assertRecommendationByDefaultAdGroup(gdiRecommendation);

        GdRecommendationKpiChooseAppropriatePlacementsForAdGroup recommendationKpi = getKpi(gdiRecommendation);
        List<GdRecommendationPlacementRatioInfo> ratioInfoList = recommendationKpi.getRatioInfoList();
        assertThat(ratioInfoList).hasSize(1);
        GdRecommendationPlacementRatioInfo ratioInfo = ratioInfoList.get(0);

        GdOutdoorVideoFormat videoFormat = ratioInfo.getVideoFormat();
        Integer creativesCount = ratioInfo.getCreativesCount();
        assertThat(creativesCount).isEqualTo(1);

        checkVideoFormat(videoFormat, DEFAULT_RATIO);

    }


    /**
     * 4 баннера:
     * 1) Не подходит под щит. Соотношение 2к1
     * 2) Не подходит под щит. Соотношение 2к1
     * 3) Подходит под щит.
     * 4) Не подходит под щит. Соотношение 3к1
     */
    @Test
    public void getAdGroupRecommendations_DifferentRatio_TwoRecommendations() {
        TestFfmpegResolution resolution3to1Ratio = R_31_576p;
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_1);
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_2);
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, SIZE_3);
        addBannerToAdGroup(defaultAdGroup, DEFAULT_DURATION, new Size(resolution3to1Ratio));
        addPlacementsToAdGroup(defaultAdGroupId, DEFAULT_DURATION, SIZE_3);

        Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
        adGroupIdToCampaignId.put(defaultAdGroupId, defaultAdGroup.getCampaignId());

        var result = serviceUnderTest.getRecommendations(shard, defaultClientIdLong, adGroupIdToCampaignId);
        assertThat(result).hasSize(1);
        GdiRecommendation gdiRecommendation = result.get(0);

        assertRecommendationByDefaultAdGroup(gdiRecommendation);

        GdRecommendationKpiChooseAppropriatePlacementsForAdGroup recommendationKpi = getKpi(gdiRecommendation);
        List<GdRecommendationPlacementRatioInfo> ratioInfoList = recommendationKpi.getRatioInfoList();

        ratioInfoList.sort(Comparator.comparing(x -> x.getVideoFormat().getWidth()));

        GdOutdoorVideoFormat videoFormat1 = ratioInfoList.get(0).getVideoFormat();
        GdOutdoorVideoFormat videoFormat2 = ratioInfoList.get(1).getVideoFormat();
        Integer creativesCount1 = ratioInfoList.get(0).getCreativesCount();
        Integer creativesCount2 = ratioInfoList.get(1).getCreativesCount();

        assertThat(creativesCount1).isEqualTo(2);
        assertThat(creativesCount2).isEqualTo(1);

        checkVideoFormat(videoFormat1, DEFAULT_RATIO);
        checkVideoFormat(videoFormat2, resolution3to1Ratio.getRatio());
    }

    private void checkVideoFormat(GdOutdoorVideoFormat actualFormat, String ratio) {
        String[] wxh2 = ratio.split(":");
        int expectedWidth = Integer.parseInt(wxh2[0]);
        int expectedHeight = Integer.parseInt(wxh2[1]);
        checkVideoFormat(actualFormat, new Size(expectedWidth, expectedHeight));
    }

    private void checkVideoFormat(GdOutdoorVideoFormat actualFormat, Size expectedSize) {
        assertThat(actualFormat.getWidth()).isEqualTo(expectedSize.getWidth());
        assertThat(actualFormat.getHeight()).isEqualTo(expectedSize.getHeight());
        assertThat(actualFormat.getDuration()).isEqualTo(ROUNDED_DEFAULT_DURATION);
    }

    private void assertRecommendationByDefaultAdGroup(GdiRecommendation gdiRecommendation) {
        Long cid = gdiRecommendation.getCid();
        Long clientId = gdiRecommendation.getClientId();
        Long bid = gdiRecommendation.getBid();
        Long pid = gdiRecommendation.getPid();

        assertThat(pid).isEqualTo(defaultAdGroupId);
        assertThat(cid).isEqualTo(defaultAdGroup.getCampaignId());
        assertThat(bid).isEqualTo(0);
        assertThat(clientId).isEqualTo(defaultClientIdLong);

        GdiRecommendationType type = gdiRecommendation.getType();
        assertThat(type).isEqualTo(chooseAppropriatePlacementsForAdGroup);
    }

}
