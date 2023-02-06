package ru.yandex.autotests.direct.cmd.strategy.savenewcamp;


import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.direct.cmd.util.CommonUtils.convertEmptyToNull;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;


@RunWith(Parameterized.class)
public abstract class SaveNewCampStrategyTestBase {
    protected final static String CLIENT = "at-direct-backend-c";
    @Rule
    public DirectCmdRule cmdRule;
    public Strategies strategy;
    protected CampaignStrategy expectedCampaignStrategy;
    private Long campaignId;

    private CampaignRule campaignRule;

    public SaveNewCampStrategyTestBase(Strategies strategy) {
        this.strategy = strategy;
        campaignRule = getCampaignRule();
        campaignRule.overrideCampTemplate(getSaveCampRequest());
        cmdRule = DirectCmdRule.defaultRule().as(getLogin()).withRules(campaignRule);
    }

    protected SaveCampRequest getSaveCampRequest() {
        SaveCampRequest saveCampRequest = new SaveCampRequest();
        expectedCampaignStrategy = CmdStrategyBeans.getStrategyBean(strategy);
        if (expectedCampaignStrategy.getIsSearchStop().equals("1")) {
            saveCampRequest.setBroad_match_flag("0");
        }
        saveCampRequest.setJsonStrategy(expectedCampaignStrategy);
        return saveCampRequest;
    }

    protected abstract CampaignRule getCampaignRule();

    protected String getLogin() {
        return CLIENT;
    }

    @Before
    public void before() {
        campaignId = campaignRule.getCampaignId();
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

}
