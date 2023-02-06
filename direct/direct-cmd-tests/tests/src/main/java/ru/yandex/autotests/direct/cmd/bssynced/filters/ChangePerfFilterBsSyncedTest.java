package ru.yandex.autotests.direct.cmd.bssynced.filters;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Condition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.TargetFunnelEnum;
import ru.yandex.autotests.direct.cmd.data.strategy.AjaxSaveAutoBudgetRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BidsPerformanceStatusbssynced;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced группы при изменении данных условия нацеливания ДМО")
@Stories(TestFeatures.Conditions.AJAX_EDIT_PERFORMANCE_FILTERS)
@Features(TestFeatures.CONDITIONS)
@Tag(ObjectTag.PERFORMANCE_FILTER)
@Tag(CampTypeTag.PERFORMANCE)
public class ChangePerfFilterBsSyncedTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT2;
    private static final String TARGET_NAME = "{adtarget_name}";
    private static final String NEW_PERF_FILTER_NAME = "New filter condition";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private Long campaignId;
    private String filterId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        filterId = bannersRule.getCurrentGroup().getPerformanceFilters().get(0).getPerfFilterId();
        prepareStatuses();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced условия нацеливания ДМО и группы при изменении условий")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9329")
    public void checkBsSyncedChangeConditionPerfFilterTest() {
        changePerfFilter(getPerformanceFilter().withConditions(singletonList(new Condition()
                .withField("url")
                .withRelation("ilike")
                .withValue(singletonList("newvalue"))))
        );

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(),
                StatusBsSynced.NO);
        BsSyncedHelper.checkBannerBsSynced(CLIENT, bannersRule.getBannerId(),
                BannersStatusbssynced.No);
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced условия нацеливания ДМО и группы при изменении targetFunel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9330")
    public void checkBsSyncedChangeTargetFunelPerfFilterTest() {
        changePerfFilter(getPerformanceFilter().withTargetFunnel(TargetFunnelEnum.PRODUCT_PAGE_VISIT.getValue()));

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(),
                StatusBsSynced.NO);
        BsSyncedHelper.checkBannerBsSynced(CLIENT, bannersRule.getBannerId(),
                BannersStatusbssynced.No);
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced условия нацеливания ДМО и группы при изменении price_cpc")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9331")
    public void checkBsSyncedChangePriceCpcPerfFilterTest() {
        changePerfFilter(getPerformanceFilter().withPriceCpc("5"));

        checkPerfFilterBsSynced();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced условия нацеливания ДМО и группы при изменении price_cpa")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9332")
    public void checkBsSyncedChangePriceCpaPerfFilterTest() {
        changePerfFilter(getPerformanceFilter().withPriceCpa("5"));

        checkPerfFilterBsSynced();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced условия нацеливания ДМО и группы при изменении приоритета")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9333")
    public void checkBsSyncedChangePriorityPerfFilterTest() {
        prepareStatistics();
        CampaignStrategy strategy = CmdStrategyBeans.getStrategyBean(Strategies.ROI_OPTIMIZATION_DMO);
        strategy.getNet().withGoalId(MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId().toString());
        AjaxSaveAutoBudgetRequest request = new AjaxSaveAutoBudgetRequest()
                .withCid(String.valueOf(campaignId))
                .withJsonStrategy(strategy)
                .withUlogin(CLIENT);
        CommonResponse commonResponse = cmdRule.cmdSteps().strategySteps().postAjaxSaveAutobudget(request);
        assumeThat("стратегия успешно установлена", commonResponse.getSuccess(), equalTo("1"));

        prepareStatuses();
        changePerfFilter(getPerformanceFilter().withAutobudgetPriority("5"));

        checkPerfFilterBsSynced();
    }

    @Test
    @Ignore("https://st.yandex-team.ru/DIRECT-55000")
    @Description("Проверяем сброс статуса bsSynced группы при изменении названия условия ДМО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9334")
    public void checkBsSyncedChangeNamePerfFilterTest() {
        Group group = getGroupWithIds();
        group.setHrefParams(TARGET_NAME);
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));

        prepareStatuses();
        changePerfFilter(getPerformanceFilter().withFilterName(NEW_PERF_FILTER_NAME));

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(),
                StatusBsSynced.NO);
    }

    private void prepareStatuses() {
        BsSyncedHelper.moderateCamp(cmdRule, campaignId);

        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
        BsSyncedHelper.setBannerBsSynced(cmdRule, bannersRule.getBannerId(), StatusBsSynced.YES);
        BsSyncedHelper.setPerfFilterBsSynced(Long.valueOf(filterId), BidsPerformanceStatusbssynced.Yes, CLIENT);
    }

    private void checkPerfFilterBsSynced() {
        assertThat("статусы bsSynced соответствуют ожиданию",
                bannersRule.getCurrentGroup().getPerformanceFilters().get(0).getStatusBsSynced(),
                equalTo(StatusBsSynced.NO.toString()));
    }

    private void changePerfFilter(PerformanceFilter filter) {
        cmdRule.cmdSteps().ajaxEditPerformanceFiltersSteps()
                .performanceFiltersChangeWithAssumption(campaignId, bannersRule.getGroupId(), filter, CLIENT);
    }

    private Group getGroupWithIds() {
        Group group = bannersRule.getGroup();
        group.setAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.getBanners().get(0).setBid(bannersRule.getBannerId());
        return group;
    }

    private PerformanceFilter getPerformanceFilter() {
        return cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, String.valueOf(campaignId))
                .getGroups().get(0).getPerformanceFilters().get(0)
                .withIsSuspended(null);
    }

    private void prepareStatistics() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campMetrikaGoalsSteps().
                addOrUpdateMetrikaGoals(campaignId,
                        MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId(), 50L, 50L);

    }

}
