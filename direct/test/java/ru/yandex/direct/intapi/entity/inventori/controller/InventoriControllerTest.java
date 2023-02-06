package ru.yandex.direct.intapi.entity.inventori.controller;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxImpressionsCustomPeriodStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxImpressionsStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachCustomPeriodStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachStrategy;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.inventori.model.InventoriResponse;
import ru.yandex.direct.intapi.entity.inventori.model.InventoriResult;
import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.CampaignParameters;
import ru.yandex.direct.inventori.model.request.CampaignParametersRf;
import ru.yandex.direct.inventori.model.request.CampaignParametersSchedule;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.InventoriCampaignType;
import ru.yandex.direct.inventori.model.request.StrategyType;
import ru.yandex.direct.inventori.model.request.Target;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE_MOBILE;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CONTENT_CATEGORY_UPPER_BOUND;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validContentCategoriesAdGroupAdditionalTargeting;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxImpressionsCustomPeriodStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxImpressionsStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxReachCustomPeriodStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxReachStrategy;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultAdaptive;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class InventoriControllerTest extends BaseInventoriControllerTest {


    // cpm_banner

    @Test
    public void getInventoriRequests_AutobudgetMaxReachStrategy_AllFieldsAreCorrect() throws Exception {
        AutobudgetMaxReachStrategy strategy = autobudgetMaxReachStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.MAX_REACH)
                                .withBudget(strategy.getSum().movePointRight(Money.MICRO_MULTIPLIER_SCALE).longValue())
                                .withStartDate(LocalDate.now())
                                .withEndDate(LocalDate.now().plusDays(6))
                                .withCpm(strategy.getAvgCpm().movePointRight(Money.MICRO_MULTIPLIER_SCALE).longValue())
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(campaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_AutobudgetMaxReachCustomPeriodStrategy_AllFieldsAreCorrect() throws Exception {
        AutobudgetMaxReachCustomPeriodStrategy strategy = autobudgetMaxReachCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.MAX_REACH_CUSTOM_PERIOD)
                                .withBudget(strategy.getBudget()
                                        .movePointRight(Money.MICRO_MULTIPLIER_SCALE).longValue())
                                .withStartDate(strategy.getStartDate())
                                .withEndDate(strategy.getFinishDate())
                                .withCpm(strategy.getAvgCpm().movePointRight(Money.MICRO_MULTIPLIER_SCALE).longValue())
                                .withAutoProlongation(true)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(campaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_AutobudgetMaxImpressionsStrategy_AllFieldsAreCorrect()
            throws Exception {
        AutobudgetMaxImpressionsStrategy strategy = autobudgetMaxImpressionsStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.MIN_CPM)
                                .withBudget(strategy.getSum().movePointRight(Money.MICRO_MULTIPLIER_SCALE).longValue())
                                .withStartDate(LocalDate.now())
                                .withEndDate(LocalDate.now().plusDays(6))
                                .withCpm(strategy.getAvgCpm().movePointRight(Money.MICRO_MULTIPLIER_SCALE).longValue())
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(campaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_AutobudgetMaxImpressionsCustomPeriodStrategy_AllFieldsAreCorrect()
            throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        defaultCampaignParametersSchedule(strategy),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(campaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_TwoEqualCampaignIds() throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        defaultCampaignParametersSchedule(strategy),
                        new CampaignParametersRf(0, 0))
        );

        check(asList(campaign.getId(), campaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_TwoValidIdenticalCampaignIds() throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        CampaignInfo campaignInfo2 = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign2 = campaignInfo2.getCampaign();

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        defaultCampaignParametersSchedule(strategy),
                        new CampaignParametersRf(0, 0))
        );

        InventoriResult expectedResult2 = InventoriResult.success(campaignInfo2.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        defaultCampaignParametersSchedule(strategy),
                        new CampaignParametersRf(0, 0))
        );

        check(asList(campaign.getId(), campaign2.getId()),
                new InventoriResponse(asList(expectedResult, expectedResult2)));
    }

    @Test
    public void getInventoriRequests_RfIsZeroButRfResetIsNot_ResponseHasValidRfAndRfReset() throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        testCampaignRepository.updateRfAndRfReset(campaignInfo.getShard(), campaignInfo.getCampaignId(), 0L, 10L);

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        defaultCampaignParametersSchedule(strategy),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(campaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_CampaignWithCpmGeoproductTarget_TargetHasCorrectGroupType() throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));

        createFullCpmGeoproductAdGroup(campaignInfo, List.of("app-metro"), List.of("app-metro"));

        InventoriResponse response = getResponse(singletonList(campaignInfo.getCampaignId()));

        assumeThat(response.getResults(), hasSize(1));
        InventoriResult result = response.getResults().get(0);

        assertThat(result.getError(), notNullValue());
    }

    @Test
    public void getInventoriRequests_CampaignWithCpmGeoproductTarget_AppMetro_TargetHasCorrectTargetTags()
            throws Exception {
        testCpmGeoproductTargetTags(List.of("app-navi"));
    }

    @Test
    public void getInventoriRequests_CampaignWithCpmGeoproductTarget_AppNavi_TargetHasCorrectTargetTags()
            throws Exception {
        testCpmGeoproductTargetTags(List.of("app-metro"));
    }

    @Test
    public void getInventoriRequests_CampaignWithCpmGeoproductTarget_AppMetroAndNavi_TargetHasCorrectTargetTags()
            throws Exception {
        testCpmGeoproductTargetTags(List.of("app-metro", "app-navi"));
    }

    @Test
    public void getInventoriRequests_CpmBannerWithAdaptiveCreative_TargetHasCorrectGroupType() throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));

        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(
                defaultAdaptive(clientInfo.getClientId(), null), clientInfo);

        createFullCpmBannerAdGroup(campaignInfo, creativeInfo.getCreativeId());

        InventoriResponse response = getResponse(singletonList(campaignInfo.getCampaignId()));

        assumeThat(response.getResults(), hasSize(1));
        InventoriResult result = response.getResults().get(0);

        assumeThat(result.getTargets(), hasSize(1));
        Target target = result.getTargets().get(0);
        assertThat(new HashSet<>(target.getBlockSizes()), is(ALLOWED_BLOCK_SIZES));
    }

    @Test
    public void getInventoriRequests_CpmBannerWithNoAdaptiveCreative_TargetHasCorrectGroupType() throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));

        Creative creative = defaultCanvas(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        createFullCpmBannerAdGroup(campaignInfo, creativeInfo.getCreativeId());

        InventoriResponse response = getResponse(singletonList(campaignInfo.getCampaignId()));

        assumeThat(response.getResults(), hasSize(1));
        InventoriResult result = response.getResults().get(0);

        assumeThat(result.getTargets(), hasSize(1));
        Target target = result.getTargets().get(0);
        assertThat(target.getBlockSizes(),
                is(singletonList(new BlockSize(creative.getWidth().intValue(), creative.getHeight().intValue()))));
    }

    @Test
    public void getInventoriRequests_CpmBanner_TargetHasGenresAndCategories() throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy)
        );

        Creative creative = defaultCanvas(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        var adGroupInfo = createFullCpmBannerAdGroup(campaignInfo, creativeInfo.getCreativeId());
        steps.adGroupAdditionalTargetingSteps().addValidTargetingsToAdGroup(adGroupInfo,
                List.of(validContentCategoriesAdGroupAdditionalTargeting()));
        steps.cryptaGoalsSteps().addGoals((Goal) new Goal()
                .withId(CONTENT_CATEGORY_UPPER_BOUND - 1)
                .withName("name")
                .withInterestType(CryptaInterestType.all)
                .withKeyword("982")
                .withKeywordValue("4294968318"));

        InventoriResponse response = getResponse(singletonList(campaignInfo.getCampaignId()));

        assumeThat(response.getResults(), hasSize(1));
        InventoriResult result = response.getResults().get(0);

        assumeThat(result.getTargets(), hasSize(1));
        Target target = result.getTargets().get(0);
        assertThat(target.getGenresAndCategories(), hasSize(1));
    }
    //

    // cpm_yndx_frontpage

    @Test
    public void getInventoriRequests_CpmYndxFrontpageCampaignWithAllTypesOfTraffic_AllFieldsAreCorrect()
            throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                campaignInfo.getShard(), campaignInfo.getCampaignId(), ImmutableSet.of(FRONTPAGE, FRONTPAGE_MOBILE));

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        defaultCampaignParametersSchedule(strategy),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(campaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_CpmYndxFrontpageCampaignWithDesktopTraffic_AllFieldsAreCorrect()
            throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                campaignInfo.getShard(), campaignInfo.getCampaignId(), ImmutableSet.of(FRONTPAGE));

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        defaultCampaignParametersSchedule(strategy),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(campaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_CpmYndxFrontpageCampaignWithMobileTraffic_AllFieldsAreCorrect()
            throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();

        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                campaignInfo.getShard(), campaignInfo.getCampaignId(), ImmutableSet.of(FRONTPAGE_MOBILE));

        InventoriResult expectedResult = InventoriResult.success(campaignInfo.getCampaignId(), null,
                InventoriCampaignType.MEDIA_RSYA,
                emptyList(),
                false, false, false, new CampaignParameters(
                        defaultCampaignParametersSchedule(strategy),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(campaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_CpmYndxFrontpageCampaignWithTarget_TrafficTypeIsNotAddedToCampaign()
            throws Exception {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), null).withStrategy(strategy));

        createFullCpmYndxFrontpageAdGroup(campaignInfo);
        createFullCpmYndxFrontpageAdGroup(campaignInfo);
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                campaignInfo.getShard(), campaignInfo.getCampaignId(), ImmutableSet.of(FRONTPAGE_MOBILE));

        InventoriResponse response = getResponse(singletonList(campaignInfo.getCampaignId()));

        assumeThat(response.getResults(), hasSize(1));
        InventoriResult result = response.getResults().get(0);

        assumeThat(result.getTargets(), hasSize(2));

        result.getTargets().forEach(target -> {
            assertThat("Тип траффика не добавлен на уровне таргета", target.getMainPageTrafficType(),
                    nullValue());
            assertThat("Тип нацеливания добавлен на уровне таргета", target.getGroupType(),
                    is(GroupType.MAIN_PAGE_AND_NTP));
        });
    }

}
