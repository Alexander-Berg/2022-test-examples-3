package ru.yandex.autotests.direct.cmd.autobudgetalerts;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Aqua.Test
@Description("Заморозка уведомлений для динамической кампании со стратагией - Недельный бюджет")
@Features(TestFeatures.AUTOBUDGET_ALERTS)
@Tag(CampTypeTag.DYNAMIC)
@Tag(BusinessProcessTag.ALERT)
public class FreezeDynamicWeeklyBudgetAlertsTest extends FreezeAutobudgetAlertsBaseTest {
    @Override
    BannersRule getBannerRule() {
        if (bannersRule == null) {
            bannersRule = new DynamicBannersRule()
                    .withCampStrategy(CmdStrategyBeans.getStrategyBean(Strategies.WEEKLY_BUDGET_MAX_CLICKS_SHOWS_DISABLED))
                    .withUlogin(CLIENT);
        }
        return bannersRule;
    }

    //увеличиваем максимальную ставку в параметрах стратегии
    private void incrementBid() {
        CampaignStrategy campaignStrategy = CmdStrategyBeans.getStrategyBean(Strategies.WEEKLY_BUDGET_MAX_CLICKS_SHOWS_DISABLED);
        Float bid;
        bid = Float.valueOf(campaignStrategy.getSearch().getBid()) + 1;
        campaignStrategy.getSearch().withBid(bid.toString());
        ajaxSaveAutoBudgetRequest(campaignStrategy);
    }

    @Test
    @Description("Заморозка причины I_1 при увеличении максимальной ставки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9008")
    public void freezeProblemI_1ByIncrementBid() {
        sendProblems(PROBLEM_I_1);
        incrementBid();
        checkProblemsFreeze(PROBLEM_I_1);
    }

    @Test
    @Description("Заморозка причины I_2 при увеличении максимальной ставки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9009")
    public void freezeProblemI_2ByIncrementBid() {
        sendProblems(PROBLEM_I_2);
        incrementBid();
        checkProblemsFreeze(PROBLEM_I_2);
    }

    @Test
    @Description("Заморозка причины III при увеличении максимальной ставки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9010")
    public void freezeProblemIIIByIncrementBid() {
        sendProblems(PROBLEM_III);
        incrementBid();
        checkProblemsFreeze(PROBLEM_III);
    }
}
