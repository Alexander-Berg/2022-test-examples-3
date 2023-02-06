package ru.yandex.autotests.direct.cmd.autobudgetalerts;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Aqua.Test
@Description("Заморозка уведомлений для текстовой кампании")
@Features(TestFeatures.AUTOBUDGET_ALERTS)
@Tag(CampTypeTag.TEXT)
@Tag(BusinessProcessTag.ALERT)
public class FreezeTextAlertsTest extends FreezeAutobudgetAlertsBaseTest {

    @Override
    BannersRule getBannerRule() {
        if (bannersRule == null) {
            bannersRule = new TextBannersRule()
                    .withCampStrategy(CmdStrategyBeans.getStrategyBean(Strategies.WEEKLY_BUDGET_MAX_CLICKS_DEFAULT))
                    .withUlogin(CLIENT);
        }
        return bannersRule;
    }

    //увеличиваем максимальную ставку в параметрах стратегии
    private void incrementBid() {
        CampaignStrategy campaignStrategy = CmdStrategyBeans.getStrategyBean(Strategies.WEEKLY_BUDGET_MAX_CLICKS_DEFAULT);
        float bid = Float.parseFloat(campaignStrategy.getSearch().getBid()) + 1;
        campaignStrategy.getSearch().withBid(Float.toString(bid));
        campaignStrategy.getNet().withName("default");
        ajaxSaveAutoBudgetRequest(campaignStrategy);
    }

    @Test
    @Description("Заморозка причины I_1 при увеличении максимальной ставки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9018")
    public void freezeProblemI_1ByIncrementAvgBid() {
        sendProblems(PROBLEM_I_1);
        incrementBid();
        checkProblemsFreeze(PROBLEM_I_1);
    }

    @Test
    @Description("Заморозка причины I_2 при увеличении максимальной ставки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9019")
    public void freezeProblemI_2ByIncrementAvgBid() {
        sendProblems(PROBLEM_I_2);
        incrementBid();
        checkProblemsFreeze(PROBLEM_I_2);
    }

    @Test
    @Description("Заморозка причины III при увеличении максимальной ставки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9020")
    public void freezeProblemIIIByIncrementAvgBid() {
        sendProblems(PROBLEM_III);
        incrementBid();
        checkProblemsFreeze(PROBLEM_III);
    }

    @Test
    @Description("Заморозка причины I_1 при добавлении фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9017")
    public void freezeProblemI_1ByAddPhrase() {
        sendProblems(PROBLEM_I_1);
        addPhrase();
        checkProblemsFreeze(PROBLEM_I_1);
    }

    @Test
    @Description("Заморозка причины I_2 при добавлении фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9021")
    public void freezeProblemI_2ByAddPhrase() {
        sendProblems(PROBLEM_I_2);
        addPhrase();
        checkProblemsFreeze(PROBLEM_I_2);
    }

    @Test
    @Description("Заморозка причины II при добавлении фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9022")
    public void freezeProblemIIByAddPhrase() {
        sendProblems(PROBLEM_II);
        addPhrase();
        checkProblemsFreeze(PROBLEM_II);
    }

    @Test
    @Description("Причина III не замораживается при добавлении фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9023")
    public void notFreezeProblemIIIByAddPhrase() {
        sendProblems(PROBLEM_III);
        addPhrase();
        checkProblemIIIActive();
    }
}
