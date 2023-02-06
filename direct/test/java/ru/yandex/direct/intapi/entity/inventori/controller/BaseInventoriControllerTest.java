package ru.yandex.direct.intapi.entity.inventori.controller;

import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxImpressionsCustomPeriodStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachCustomPeriodStrategy;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.intapi.entity.inventori.model.InventoriResponse;
import ru.yandex.direct.intapi.entity.inventori.model.InventoriResult;
import ru.yandex.direct.inventori.model.request.CampaignParametersSchedule;
import ru.yandex.direct.inventori.model.request.StrategyType;
import ru.yandex.direct.inventori.model.request.TrafficTypeCorrections;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBannerWithTurbolanding;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxImpressionsCustomPeriodStrategy;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class BaseInventoriControllerTest {

    protected static final String EXPECTED_CONTROLLER_MAPPING = "/inventori/campaigns_info";
    protected static final TrafficTypeCorrections DEFAULT_TRAFFIC_TYPE_CORRECTIONS =
            new TrafficTypeCorrections(null, null, null, null, null, null);

    @Autowired
    private InventoriController controller;

    @Autowired
    protected Steps steps;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    protected TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    @Autowired
    protected TestCampaignRepository testCampaignRepository;

    private MockMvc mockMvc;
    protected ClientInfo clientInfo;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    protected void check(List<Long> campaignIds, InventoriResponse expected) throws Exception {
        Assertions.assertThat(getResponse(campaignIds).getResults())
                .as("В ответе верные данные")
                .containsExactlyInAnyOrder(expected.getResults().toArray(new InventoriResult[0]));
    }

    protected InventoriResponse getResponse(List<Long> campaignIds) throws Exception {
        String r = mockMvc
                .perform(get(EXPECTED_CONTROLLER_MAPPING).param("campaign_ids", getStringQuery(campaignIds)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonUtils.fromJson(r, InventoriResponse.class);
    }

    private String getStringQuery(List<Long> campaignsIds) {
        return StreamEx.of(campaignsIds)
                .joining(",");
    }

    private void createFullCpmBannerAdGroup(CampaignInfo campaignInfo) {
        createFullCpmBannerAdGroup(campaignInfo, null);
    }

    protected AdGroupInfo createFullCpmBannerAdGroup(CampaignInfo campaignInfo, Long creativeId) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
        steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId), adGroupInfo);
        RetargetingCondition retargetingCondition = defaultCpmRetCondition();
        steps.retargetingSteps().createRetargeting(defaultTargetInterest(), adGroupInfo,
                new RetConditionInfo()
                        .withClientInfo(campaignInfo.getClientInfo())
                        .withRetCondition(retargetingCondition));
        testCryptaSegmentRepository.addAll(
                StreamEx.of(retargetingCondition.collectGoals())
                        .filter(goal -> !goal.getType().isMetrika())
                        .toList());
        return adGroupInfo;
    }

    protected void createFullCpmYndxFrontpageAdGroup(CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmYndxFrontpageAdGroup(campaignInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(
                defaultCanvas(clientInfo.getClientId(), null), clientInfo);
        steps.bannerSteps().createActiveCpmBanner(activeCpmBanner(campaignInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), creativeInfo.getCreativeId()), adGroupInfo);
        RetargetingCondition retargetingCondition = defaultCpmRetCondition();
        steps.retargetingSteps().createRetargeting(defaultTargetInterest(), adGroupInfo,
                new RetConditionInfo()
                        .withClientInfo(campaignInfo.getClientInfo())
                        .withRetCondition(retargetingCondition));
        testCryptaSegmentRepository.addAll(
                StreamEx.of(retargetingCondition.collectGoals())
                        .filter(goal -> !goal.getType().isMetrika())
                        .toList());
    }

    protected void testCpmGeoproductTargetTags(List<String> targetTags) throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(null, null).withStrategy(strategy));

        createFullCpmGeoproductAdGroup(campaignInfo, targetTags, targetTags);

        InventoriResponse response = getResponse(singletonList(campaignInfo.getCampaignId()));

        assumeThat(response.getResults(), hasSize(1));
        InventoriResult result = response.getResults().get(0);

        assertThat(result.getError(), notNullValue());
    }

    protected void createFullCpmGeoproductAdGroup(CampaignInfo campaignInfo, List<String> pageGroupTags,
            List<String> targetTags) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmGeoproductAdGroup(campaignInfo.getCampaignId())
                        .withPageGroupTags(pageGroupTags)
                        .withTargetTags(targetTags))
                .withCampaignInfo(campaignInfo));
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(
                defaultCanvas(clientInfo.getClientId(), null), clientInfo);

        steps.bannerSteps().createActiveCpmBanner(
                activeCpmBannerWithTurbolanding(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(),
                        creativeInfo.getCreativeId(), null),
                adGroupInfo);
        RetargetingCondition retargetingCondition = defaultCpmRetCondition();
        steps.retargetingSteps().createRetargeting(defaultTargetInterest(), adGroupInfo,
                new RetConditionInfo()
                        .withClientInfo(campaignInfo.getClientInfo())
                        .withRetCondition(retargetingCondition));
        testCryptaSegmentRepository.addAll(
                StreamEx.of(retargetingCondition.collectGoals())
                        .filter(goal -> !goal.getType().isMetrika())
                        .toList());
    }

    protected CampaignParametersSchedule defaultCampaignParametersSchedule(
            AutobudgetMaxReachCustomPeriodStrategy strategy) {
        return CampaignParametersSchedule.builder()
                .withStrategyType(StrategyType.MIN_CPM_CUSTOM_PERIOD)
                .withBudget(strategy.getBudget().movePointRight(Money.MICRO_MULTIPLIER_SCALE).longValue())
                .withStartDate(strategy.getStartDate())
                .withEndDate(strategy.getFinishDate())
                .withCpm(strategy.getAvgCpm().movePointRight(Money.MICRO_MULTIPLIER_SCALE).longValue())
                .withAutoProlongation(false)
                .build();
    }

}
