package ru.yandex.direct.intapi.entity.campaigns.impressionrate;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithImpressionRate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaignWithStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxImpressionsCustomPeriodStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignImpressionRateControllerTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CampaignImpressionRateController controller;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private TestCampaignRepository campaignRepository;

    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;

    @Before
    public void before() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().setCurrentClient(clientInfo.getClientId());
        campaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);
    }

    @Test
    public void setImpressionRatePositiveTest() {
        var response = controller.setCampaignImpressionRate(
                campaignInfo.getClientId().asLong(), campaignInfo.getCampaignId(), 10, 20);

        var campaigns =
                campaignTypedRepository.getSafely(
                        campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId()),
                        CampaignWithImpressionRate.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isTrue();
            softly.assertThat(campaigns).hasSize(1);
        });

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(campaigns.get(0).getImpressionRateCount()).isEqualTo(10);
            softly.assertThat(campaigns.get(0).getImpressionRateIntervalDays()).isEqualTo(20);
        });
    }

    @Test
    public void setImpressionRatePositiveWithGetTest() {
        var response = controller.setCampaignImpressionRate(
                campaignInfo.getClientId().asLong(), campaignInfo.getCampaignId(), 10, 20);

        var getResponse =
                controller.getCampaignImpressionRate(campaignInfo.getClientId().asLong(), campaignInfo.getCampaignId());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isTrue();
            softly.assertThat(getResponse.getRateCount()).isEqualTo(10);
            softly.assertThat(getResponse.getRateIntervalDays()).isEqualTo(20);
        });
    }

    @Test
    public void setImpressionRateCpmBannerPositiveTest() {
        campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(autobudgetMaxImpressionsCustomPeriodStrategy()), clientInfo);

        var response = controller.setCampaignImpressionRate(
                campaignInfo.getClientId().asLong(), campaignInfo.getCampaignId(), 10, 20);

        var campaigns =
                campaignTypedRepository.getSafely(
                        campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId()),
                        CampaignWithImpressionRate.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isTrue();
            softly.assertThat(campaigns).hasSize(1);
        });

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(campaigns.get(0).getImpressionRateCount()).isEqualTo(10);
            softly.assertThat(campaigns.get(0).getImpressionRateIntervalDays()).isEqualTo(20);
        });
    }

    @Test
    public void resetCampaignImpressionRatePositiveTest() {
        campaignRepository.setImpressionRate(
                campaignInfo.getShard(), campaignInfo.getCampaignId(), 10L, 20L);
        var response = controller.setCampaignImpressionRate(
                campaignInfo.getClientId().asLong(), campaignInfo.getCampaignId(), null, null);

        var campaigns =
                campaignTypedRepository.getSafely(
                        campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId()),
                        CampaignWithImpressionRate.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isTrue();
            softly.assertThat(campaigns).hasSize(1);
        });

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(campaigns.get(0).getImpressionRateCount()).isEqualTo(null);
            softly.assertThat(campaigns.get(0).getImpressionRateIntervalDays()).isEqualTo(null);
        });
    }

    @Test
    public void setCampaignImpressionRateNegativeTest() {
        var response = controller.setCampaignImpressionRate(
                campaignInfo.getClientId().asLong(), campaignInfo.getCampaignId(), null, 10);

        var campaigns =
                campaignTypedRepository.getSafely(
                        campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId()),
                        CampaignWithImpressionRate.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isFalse();
            softly.assertThat(response.validationResult().getErrors().size()).isEqualTo(1);
            softly.assertThat(campaigns).hasSize(1);
        });

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(campaigns.get(0).getImpressionRateCount()).isEqualTo(null);
            softly.assertThat(campaigns.get(0).getImpressionRateIntervalDays()).isEqualTo(null);
        });
    }

    @Test
    public void setCampaignImpressionRateBadCampaignIdNegativeTest() {
        var response = controller.setCampaignImpressionRate(
                campaignInfo.getClientId().asLong(), campaignInfo.getCampaignId() + 1, 10, 20);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isFalse();
            softly.assertThat(response.validationResult().getErrors().size()).isEqualTo(1);
        });
    }

    @Test
    public void setCampaignImpressionRateBadClientIdNegativeTest() {
        var response = controller.setCampaignImpressionRate(
                campaignInfo.getClientId().asLong() + 1, campaignInfo.getCampaignId(), 10, 20);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isFalse();
            softly.assertThat(response.validationResult().getErrors().size()).isEqualTo(1);
        });
    }

    @Test
    public void setCampaignImpressionRateBadTypeNegativeTest() {
        campaignInfo = steps.campaignSteps().createCampaign(
                activePerformanceCampaignWithStrategy(clientInfo.getClientId(), clientInfo.getUid()), clientInfo);

        var response = controller.setCampaignImpressionRate(
                campaignInfo.getClientId().asLong(), campaignInfo.getCampaignId(), 10, 20);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isFalse();
            softly.assertThat(response.validationResult().getErrors().size()).isEqualTo(1);
        });
    }

    @Test
    public void setCampaignImpressionRateBadTypeNegativeGetTest() {
        campaignInfo = steps.campaignSteps().createCampaign(
                activePerformanceCampaignWithStrategy(clientInfo.getClientId(), clientInfo.getUid()), clientInfo);

        var response = controller.getCampaignImpressionRate(
                campaignInfo.getClientId().asLong(), campaignInfo.getCampaignId());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isFalse();
            softly.assertThat(response.validationResult().getErrors().size()).isEqualTo(1);
            softly.assertThat(response.getRateCount()).isNull();
            softly.assertThat(response.getRateIntervalDays()).isNull();
        });
    }
}
