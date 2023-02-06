package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.IdModFilter;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaCountersResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaCountersServiceTest {

    private static final Long INVALID_DIVISOR = 0L;
    private static final Long INVALID_REMINDER = -1L;

    @Autowired
    private MetrikaCountersService metrikaCountersService;

    @Autowired
    private Steps steps;

    @Test
    public void getCampCounters() {
        Long counterId1 = 1L;
        Long counterId2 = 2L;
        IdModFilter filter = new IdModFilter(2L, 1L);
        Campaign campaign = TestCampaigns.newTextCampaign(null, null);
        campaign.setMetrikaCounters(Arrays.asList(counterId1, counterId2));

        CampaignInfo campaignInfo = new CampaignInfo().withCampaign(campaign);

        campaignInfo = steps.campaignSteps().createCampaign(campaignInfo);

        steps.bsFakeSteps().setOrderId(campaignInfo);

        OldBannerTurboLanding defaultBannerTurboLanding =
                steps.turboLandingSteps().createDefaultBannerTurboLanding(campaignInfo.getClientId());

        OldTextBanner textBanner = TestBanners.defaultTextBanner(campaign.getId(), 1111L);
        textBanner.setId(3333L);
        textBanner.setTurboLandingStatusModerate(defaultBannerTurboLanding.getStatusModerate());
        textBanner.setTurboLandingId(defaultBannerTurboLanding.getId());
        steps.bannerSteps().createBanner(textBanner);
        steps.turboLandingSteps()
                .addBannerToBannerTurbolandingsTableOrUpdate(campaign.getId(), singletonList(textBanner));
        steps.bannerSteps().addTurbolandingMetricaCounters(campaignInfo.getShard(), textBanner, asList(22L, 33L));

        List<MetrikaCountersResult> resultList = metrikaCountersService
                .getCounters(filter.getDivisor(), filter.getRemainder());

        assertTrue(resultList.size() > 0);
    }

    @Test(expected = IntApiException.class)
    public void throwsExceptionWhenValidationFails() {
        metrikaCountersService.getCounters(INVALID_DIVISOR, INVALID_REMINDER);
    }
}
