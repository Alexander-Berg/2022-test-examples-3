package ru.yandex.autotests.direct.cmd.autobudgetalerts;

import java.util.Collections;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.data.strategy.AjaxSaveAutoBudgetRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutobudgetAlertsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutobudgetAlertsRecord;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.OrdersNotExceededBudgetParams;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.direct.db.utils.JooqRecordDifferMatcher.recordDiffer;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

public abstract class FreezeAutobudgetAlertsBaseTest {
    protected static final String CLIENT = "at-direct-smart-alert-2";
    @ClassRule
    public static final DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    static final short PROBLEM_I_1 = 2;
    static final short PROBLEM_I_2 = 4;
    static final short PROBLEM_II = 8;
    static final short PROBLEM_III = 16;
    private static final int OVERDRAFT = -20;
    protected DarkSideSteps darkSideSteps;
    protected Integer cid;
    private Integer orderID;
    protected BannersRule bannersRule = getBannerRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    abstract BannersRule getBannerRule();

    @Before
    public void before() {
        darkSideSteps = cmdRule.apiSteps().getDarkSideSteps();
        Long campaignId = bannersRule.getCampaignId();
        cid = campaignId.intValue();
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignActive(cid);
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerActive(bannersRule.getBannerId());
        orderID = cmdRule.apiSteps().campaignFakeSteps().setRandomOrderID(cid);
    }

    void sendProblems(int problems) {
        darkSideSteps.getAutobudgetAlertsSteps().ordersNotExceededBudget(
                new OrdersNotExceededBudgetParams()
                        .withAlert(orderID, problems, OVERDRAFT)
        );
    }

    void checkProblemIIIActive() {
        checkProblemsStatus(PROBLEM_III, AutobudgetAlertsStatus.active);
    }

    void checkProblemsFreeze(short expProblems) {
        checkProblemsStatus(expProblems, AutobudgetAlertsStatus.frozen);
    }

    private void checkProblemsStatus(short expProblems, AutobudgetAlertsStatus alertStatus) {
        cmdRule.apiSteps().getDirectJooqDbSteps().useShardForLogin(CLIENT);
        AutobudgetAlertsRecord testRecord = cmdRule.apiSteps().getDirectJooqDbSteps().autoBudgetAlertsSteps()
                .getAutobudgetAlertsRecord((long) cid);
        AutobudgetAlertsRecord expected = new AutobudgetAlertsRecord();
        expected.setCid((long) cid);
        expected.setOverdraft((long) OVERDRAFT);
        expected.setProblems(expProblems);
        expected.setStatus(alertStatus);
        assumeThat("Алерт соответствует ожиданиям",
                testRecord,
                recordDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    void ajaxSaveAutoBudgetRequest(CampaignStrategy campaignStrategy) {
        AjaxSaveAutoBudgetRequest ajaxSaveAutoBudgetRequest = new AjaxSaveAutoBudgetRequest()
                .withCid(bannersRule.getCampaignId().toString())
                .withuLogin(CLIENT)
                .withJsonStrategy(campaignStrategy);
        cmdRule.cmdSteps().strategySteps().postAjaxSaveAutobudget(ajaxSaveAutoBudgetRequest);
    }

    void addFilter() {
        Group group = bannersRule.getGroup();
        group.setAdGroupID(bannersRule.getGroupId().toString());
        PerformanceFilter newFilter = BeanLoadHelper
                .loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class);
        group.getBanners().get(0).setBid(bannersRule.getBannerId());
        group.getPerformanceFilters().add(newFilter);
        cmdRule.cmdSteps().groupsSteps().postSavePerformanceAdGroups(
                CLIENT,
                bannersRule.getCampaignId().toString(),
                singletonList(group));
    }

    void addPhrase() {
        Group group = bannersRule.getCurrentGroup();
        Phrase newPhrase = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_PHRASE_DEFAULT2, Phrase.class);
        newPhrase.withPhrase("ретро автомобили");
        group.getPhrases().add(newPhrase);
        group.setRetargetings(Collections.emptyList());
        group.setTags(Collections.emptyMap());
        group.setAdGroupID(bannersRule.getGroupId().toString());

        GroupsParameters groupsParameters = GroupsParameters.
                forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);
    }
}
