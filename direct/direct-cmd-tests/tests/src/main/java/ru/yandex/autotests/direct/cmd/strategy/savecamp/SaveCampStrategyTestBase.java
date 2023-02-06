package ru.yandex.autotests.direct.cmd.strategy.savecamp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.direct.cmd.util.CommonUtils.convertEmptyToNull;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@RunWith(Parameterized.class)
public abstract class SaveCampStrategyTestBase {
    protected final static String CLIENT = "at-direct-backend-c";
    @Parameterized.Parameter
    public Strategies strategy;
    protected Long campaignId;
    protected CampaignStrategy expectedCampaignStrategy;

    protected SaveCampRequest saveCampRequest;

    private CampaignRule campaignRule = getCampaignRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(getLogin()).withRules(campaignRule);

    protected abstract CampaignRule getCampaignRule();

    protected String getLogin() {
        return CLIENT;
    }

    @Before
    public void before() {
        campaignId = campaignRule.getCampaignId();
        expectedCampaignStrategy = CmdStrategyBeans.getStrategyBean(strategy);
        saveCampRequest = getSaveCampRequest().withMobileAppId(null);
        prepareStatistics();

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
    }

    @Description("Проверяем сохранение стратегии")
    public void checkCampaignStrategyBlock() {
        EditCampResponse editCampResponse =
                cmdRule.cmdSteps().campaignSteps().getEditCamp(campaignId, CLIENT);

        checkSearchStrategy(editCampResponse);
        checkNetStrategy(editCampResponse);
    }

    protected void checkSearchStrategy(EditCampResponse editCampResponse) {
        if ("".equals(expectedCampaignStrategy.getSearch().getGoalId())) {
            expectedCampaignStrategy.getSearch().setGoalId(null);
        }
        assertThat("Параметры стратегии совпадают с ожидаемыми",
                editCampResponse.getCampaign().getStrategy().getSearch(),
                beanDiffer(convertEmptyToNull(expectedCampaignStrategy.getSearch()))
                        .useCompareStrategy(onlyExpectedFields()));
    }

    protected void checkNetStrategy(EditCampResponse editCampResponse) {
        if ("".equals(expectedCampaignStrategy.getNet().getGoalId())) {
            expectedCampaignStrategy.getNet().setGoalId(null);
        }
        assertThat("Параметры контекстной стратегии совпадают с ожидаемыми",
                editCampResponse.getCampaign().getStrategy().getNet(),
                beanDiffer(convertEmptyToNull(expectedCampaignStrategy.getNet()))
                        .useCompareStrategy(onlyExpectedFields()));
    }

    private SaveCampRequest getSaveCampRequest() {
        SaveCampRequest saveCampRequest = campaignRule.getSaveCampRequest();
        saveCampRequest.setCid(campaignId.toString());
        if (expectedCampaignStrategy.getIsSearchStop().equals("1")) {
            saveCampRequest.setBroad_match_flag("0");
        }
        saveCampRequest.setJsonStrategy(expectedCampaignStrategy);
        return saveCampRequest;
    }

    protected void prepareStatistics() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campMetrikaGoalsSteps()
                .addOrUpdateMetrikaGoals(campaignId, MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId(), 50L, 50L);
    }
}
