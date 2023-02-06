package ru.yandex.direct.jobs.takeout;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.regions.Region;

@JobsTest
@ExtendWith(SpringExtension.class)
class TakeoutJobServiceTest extends TakeoutUploadJobTestBase {
    @Autowired
    private Steps steps;

    @Autowired
    private CampaignService campaignService;

    TextBannerInfo bannerInfo;

    private TakeoutJobService takeoutJobService;

    @BeforeEach
    void before() {
        takeoutJobService = initJobService();

        bannerInfo = steps.bannerSteps().createActiveTextBanner();
    }

    @Test
    void exportCampaigns() {
        Long regionId = Region.RUSSIA_REGION_ID;
        Campaign campaign = campaignService
                .getCampaigns(bannerInfo.getClientId(), Collections.singletonList(bannerInfo.getCampaignId())).get(0);
        Map<String, Object> campaignData = takeoutJobService.getCampaignData(campaign, regionId);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(campaignData.keySet()).contains(Campaign.ID.name());
        sa.assertThat(campaignData.keySet()).contains(Campaign.TYPE.name());
        sa.assertThat(campaignData.keySet()).contains(Campaign.NAME.name());
        sa.assertAll();
    }

    @Test
    void exportGroups() {
        Campaign campaign = campaignService
                .getCampaigns(bannerInfo.getClientId(), Collections.singletonList(bannerInfo.getCampaignId())).get(0);
        Map<String, Object> groupsData = takeoutJobService.getGroupsData(bannerInfo.getShard(), campaign,
                bannerInfo.getClientInfo().getClient().getCountryRegionId());
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(groupsData.keySet()).hasSize(1);
        sa.assertThat(groupsData.keySet()).containsExactly(bannerInfo.getAdGroupId().toString());
        sa.assertAll();
    }

    @Test
    void exportBanners() {
        Campaign campaign = campaignService
                .getCampaigns(bannerInfo.getClientId(), Collections.singletonList(bannerInfo.getCampaignId())).get(0);
        Map<String, Object> bannersData = takeoutJobService.getBannersData(bannerInfo.getShard(), campaign);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(bannersData.keySet()).hasSize(1);
        sa.assertThat(bannersData.keySet()).containsExactly(bannerInfo.getBannerId().toString());
        sa.assertAll();
    }
}
