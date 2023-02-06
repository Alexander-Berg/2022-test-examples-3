package ru.yandex.direct.web.entity.campaign.service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.BrandSurveyStopReason;
import ru.yandex.direct.core.entity.campaign.model.CampaignBudgetReachDaily;
import ru.yandex.direct.core.entity.campaign.model.SurveyStatus;
import ru.yandex.direct.core.entity.campaign.repository.CampaignBudgetReachDailyRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.campaign.model.BrandSurveyStatusWeb;
import ru.yandex.direct.web.entity.campaign.model.BrandSurveyStopReasonWeb;
import ru.yandex.direct.web.entity.campaign.model.CampaignBrandSurveyWebResponse;
import ru.yandex.direct.web.entity.campaign.model.CampaignsBrandSurveyResponse;
import ru.yandex.direct.web.entity.campaign.model.SurveyStatusWeb;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxImpressionsCustomPeriodStrategy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@DirectWebTest
@RunWith(SpringRunner.class)
public class WebCampaignServiceDailyTest {

    @Autowired
    private Steps steps;

    @Autowired
    private WebCampaignService webCampaignService;

    @Autowired
    private CampaignBudgetReachDailyRepository campaignBudgetReachRepository;

    private ClientInfo clientInfo;
    private UserInfo userInfo;
    private CampaignInfo campaign0;
    private CampaignInfo campaign1;
    private CampaignInfo campaign2;
    private String brandSurveyId0;
    private String brandSurveyId1;

    @Before
    public void before() {
        brandSurveyId0 = "brandSurveyId0";
        brandSurveyId1 = "brandSurveyId1";

        clientInfo = steps.clientSteps().createDefaultClient();
        userInfo = clientInfo.getChiefUserInfo();
        var startDate = LocalDate.of(2020, 2, 22);
        campaign0 = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(clientInfo, brandSurveyId0, startDate, startDate.plusDays(1L));
        campaign1 = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(clientInfo, brandSurveyId1, startDate, startDate.plusDays(1L));
        campaign2 = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(autobudgetMaxImpressionsCustomPeriodStrategy()), clientInfo);

        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);
    }

    @Test
    public void getBrandLiftTest() {
        int shard = clientInfo.getShard();
        Long defaultValue = 10L;
        Long largeValue = 30L;
        BigDecimal defaultBigDecimalValue = new BigDecimal(defaultValue);
        BigDecimal largeBigDecimalValue = new BigDecimal(largeValue);
        LocalDate largeDate = LocalDate.of(2018, 6, 6);

        CampaignBudgetReachDaily campaignBudgetReach0 = new CampaignBudgetReachDaily()
                .withCampaignId(campaign0.getCampaignId())
                .withBudgetThreshold(defaultBigDecimalValue)
                .withBudgetSpent(defaultBigDecimalValue)
                .withBudgetEstimated(defaultBigDecimalValue)
                .withTargetForecast(defaultValue)
                .withTargetThreshold(defaultValue)
                .withDate(largeDate)
                .withTrafficLightColour(1L)
                .withBrandSurveyStopReasons(null);

        CampaignBudgetReachDaily campaignBudgetReach1 = new CampaignBudgetReachDaily()
                .withCampaignId(campaign1.getCampaignId())
                .withBudgetThreshold(largeBigDecimalValue)
                .withBudgetSpent(defaultBigDecimalValue)
                .withBudgetEstimated(defaultBigDecimalValue)
                .withTargetForecast(defaultValue)
                .withTargetThreshold(largeValue)
                .withDate(largeDate)
                .withTrafficLightColour(1L)
                .withBrandSurveyStopReasons(EnumSet.of(BrandSurveyStopReason.LOW_TOTAL_BUDGET));

        campaignBudgetReachRepository.addCampaignBudgetReaches(shard,
                asList(campaignBudgetReach0, campaignBudgetReach1));

        CampaignsBrandSurveyResponse campaignsBrandSurveyResponse =
                webCampaignService.getBrandLift(asList(
                        campaign0.getCampaignId(),
                        campaign1.getCampaignId(),
                        campaign2.getCampaignId()),
                        userInfo.getUid(),
                        clientInfo.getClientId());

        List<CampaignBrandSurveyWebResponse> campaignsBrandSurveyResponseResult =
                campaignsBrandSurveyResponse.getResult();

        BrandSurveyStatusWeb expectedBrandSurveyStatus = new BrandSurveyStatusWeb()
                .withSurveyStatusDaily(SurveyStatusWeb.DRAFT)
                .withBrandSurveyStopReasonsDaily(EnumSet.noneOf(BrandSurveyStopReasonWeb.class))
                .withReasonIds(Collections.emptyList())
                .withSumSpentByTotalPeriod(new BigDecimal("20.00"))
                .withSumSpentByDay(new BigDecimal("10.00"));

        assertThat(campaignsBrandSurveyResponseResult, hasSize(3));
        assertThat(campaignsBrandSurveyResponseResult.get(0).getBrandSurveyStatusWeb(),
                beanDiffer(expectedBrandSurveyStatus));
        assertThat(campaignsBrandSurveyResponseResult.get(0).getBrandSurveyId(), equalTo(brandSurveyId0));

        assertThat(campaignsBrandSurveyResponseResult.get(1).getBrandSurveyStatusWeb(),
                beanDiffer(new BrandSurveyStatusWeb()
                        .withReasonIds(Collections.emptyList())
                        .withBrandSurveyStopReasonsDaily(EnumSet.noneOf(BrandSurveyStopReasonWeb.class))
                        .withSurveyStatusDaily(SurveyStatusWeb.DRAFT)
                        .withSumSpentByTotalPeriod(new BigDecimal("20.00"))
                        .withSumSpentByDay(new BigDecimal("10.00"))));
        assertThat(campaignsBrandSurveyResponseResult.get(1).getBrandSurveyId(), equalTo(brandSurveyId1));

        assertThat(campaignsBrandSurveyResponseResult.get(2).getBrandSurveyStatusWeb(), nullValue());
        assertThat(campaignsBrandSurveyResponseResult.get(2).getBrandSurveyId(), nullValue());
    }

    @Test
    public void mapAllTypesOfSurveyStatusToSurveyStatusWebTest() {

        List<String> surveyStatuses = mapList(asList(SurveyStatus.values()),
                surveyStatus -> surveyStatus.name());
        List<String> valuesOfSurveyStatusWeb = mapList(asList(SurveyStatusWeb.values()),
                surveyStatus -> surveyStatus.name());

        assertThat(surveyStatuses, notNullValue());
        assertThat(valuesOfSurveyStatusWeb, notNullValue());

        assertThat(surveyStatuses.containsAll(valuesOfSurveyStatusWeb), equalTo(true));
        assertThat(surveyStatuses, hasSize(valuesOfSurveyStatusWeb.size()));

    }

}
