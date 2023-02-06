package ru.yandex.direct.core.entity.addition.callout.service;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.banner.type.banneradditions.BannerAdditionsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class CalloutServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CalloutService calloutService;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private BannerAdditionsRepository bannerAdditionsRepository;

    private Callout callout;
    private CampaignInfo campaign;
    private TextBannerInfo banner;

    @Before
    public void setUp() {
        campaign = steps.campaignSteps().createDefaultCampaign();
        var adGroup = steps.adGroupSteps().createDefaultAdGroup(campaign);
        banner = steps.bannerSteps().createDefaultBanner(adGroup);
        callout = steps.calloutSteps().createDefaultCallout(campaign.getClientInfo());
        steps.calloutSteps().linkCalloutToBanner(callout, banner);

        Set<Long> links = bannerAdditionsRepository.getLinkedBannersAdditions(campaign.getShard(),
                List.of(callout.getId()));
        assertThat(links).hasSize(1);
    }

    @Test
    public void detachAndDeleteCallouts_calloutsAreDetached() {
        calloutService.detachAndDeleteCallouts(List.of(callout.getId()), campaign.getClientId());
        Set<Long> links = bannerAdditionsRepository.getLinkedBannersAdditions(campaign.getShard(),
                List.of(callout.getId()));
        assertThat(links).hasSize(0);
    }

    @Test
    public void detachAndDeleteCallouts_bannerLastChangeIsChanged() throws InterruptedException {
        var before = getBanner();
        Thread.sleep(1000);
        calloutService.detachAndDeleteCallouts(List.of(callout.getId()), campaign.getClientId());
        var after = getBanner();

        assertThat(after.getLastChange()).isAfter(before.getLastChange());
    }

    private BannerWithSystemFields getBanner() {
        return (BannerWithSystemFields) bannerService.get(campaign.getClientId(), campaign.getUid(),
                List.of(banner.getBannerId())).get(0);
    }
}
