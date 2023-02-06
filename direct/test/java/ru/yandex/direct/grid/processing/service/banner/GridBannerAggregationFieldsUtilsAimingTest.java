package ru.yandex.direct.grid.processing.service.banner;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.grid.core.entity.banner.model.GdiBanner;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusBsSynced;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusModerate;
import ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatusDesc;
import ru.yandex.direct.grid.model.campaign.GdCampaignStatus;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroupTruncated;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.banner.GridBannerAggregationFieldsUtils.isBannerAimingAllowed;

public class GridBannerAggregationFieldsUtilsAimingTest {

    public GdiBanner banner;
    public GdTextAdGroupTruncated group;

    @Before
    public void init() {
        banner = new GdiBanner()
                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                .withStatusModerate(GdiBannerStatusModerate.YES)
                .withStatusActive(true)
                .withStatusShow(true);

        group = new GdTextAdGroup().withCampaign(new GdTextCampaign()
                .withStatus(new GdCampaignStatus()
                        .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE)));
    }

    @Test
    public void testIsBannerAimingAllowed_success() {
        checkIsBannerAimingAllowed(true);
    }

    @Test
    public void testIsBannerAimingAllowed_bannerNotModerated() {
        banner.withStatusModerate(GdiBannerStatusModerate.READY);
        checkIsBannerAimingAllowed(false);
    }

    @Test
    public void testIsBannerAimingAllowed_bannerNotShow() {
        banner.withStatusShow(false);
        checkIsBannerAimingAllowed(false);
    }

    @Test
    public void testIsBannerAimingAllowed_bannerNotActive() {
        banner.withStatusActive(false);
        checkIsBannerAimingAllowed(false);
    }

    @Test
    public void testIsBannerAimingAllowed_bannerNotSynced() {
        banner.withStatusBsSynced(GdiBannerStatusBsSynced.SENDING);
        checkIsBannerAimingAllowed(false);
    }

    @Test
    public void testIsBannerAimingAllowed_campaignPausedByTimeTargeting() {
        group.getCampaign().getStatus().setPrimaryStatus(GdCampaignPrimaryStatus.TEMPORARILY_PAUSED);
        group.getCampaign().getStatus().setPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_PAUSED_BY_TIMETARGETING);
        checkIsBannerAimingAllowed(true);
    }

    @Test
    public void testIsBannerAimingAllowed_campaignNotActive() {
        group.getCampaign().getStatus().setPrimaryStatus(GdCampaignPrimaryStatus.TEMPORARILY_PAUSED);
        checkIsBannerAimingAllowed(false);
    }

    private void checkIsBannerAimingAllowed(boolean expected) {
        boolean isBannerAimingAllowed = isBannerAimingAllowed(banner, group);
        assertThat(isBannerAimingAllowed).isEqualTo(expected);
    }
}
