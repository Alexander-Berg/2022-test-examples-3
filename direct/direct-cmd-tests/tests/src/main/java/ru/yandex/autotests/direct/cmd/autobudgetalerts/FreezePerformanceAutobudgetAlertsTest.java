package ru.yandex.autotests.direct.cmd.autobudgetalerts;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Aqua.Test
@Description("Заморозка уведомлений по стратегиям для смарт-кампаний")
@Features(TestFeatures.AUTOBUDGET_ALERTS)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(BusinessProcessTag.ALERT)
public class FreezePerformanceAutobudgetAlertsTest extends FreezeAutobudgetAlertsBaseTest {

    @Override
    BannersRule getBannerRule() {
        if (bannersRule == null) {
            bannersRule = new PerformanceBannersRule()
                    .withCampStrategy(CmdStrategyBeans.getStrategyBean(Strategies.CPC_OPTIMIZATION_CAMP))
                    .withUlogin(CLIENT);
        }
        return bannersRule;
    }

    //увеличиваем максимальную ставку в параметрах стратегии
    protected void incrementBid() {
        CampaignStrategy campaignStrategy = CmdStrategyBeans.getStrategyBean(Strategies.CPC_OPTIMIZATION_CAMP);
        Float bid;
        bid = Float.valueOf(campaignStrategy.getNet().getBid()) + 1;
        campaignStrategy.getNet().withBid(bid.toString());
        ajaxSaveAutoBudgetRequest(campaignStrategy);
    }

    @Test
    @Description("Заморозка причины I_1 при увеличении максимальной ставки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9011")
    public void freezeProblemI_1ByIncrementBid() {
        sendProblems(PROBLEM_I_1);
        incrementBid();
        checkProblemsFreeze(PROBLEM_I_1);
    }

    @Test
    @Description("Заморозка причины I_2 при увеличении максимальной ставки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9012")
    public void freezeProblemI_2ByIncrementBid() {
        sendProblems(PROBLEM_I_2);
        incrementBid();
        checkProblemsFreeze(PROBLEM_I_2);
    }

    @Test
    @Description("Заморозка причины III при увеличении максимальной ставки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9013")
    public void freezeProblemIIIByIncrementBid() {
        sendProblems(PROBLEM_III);
        incrementBid();
        checkProblemsFreeze(PROBLEM_III);
    }

    @Test
    @Description("Заморозка причины I_1 при добавлении фильтра")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9014")
    public void freezeProblemI_1ByAddFilter() {
        sendProblems(PROBLEM_I_1);
        addFilter();
        checkProblemsFreeze(PROBLEM_I_1);
    }

    @Test
    @Description("Заморозка причины I_2 при добавлении фильтра")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9015")
    public void freezeProblemI_2ByAddFilter() {
        sendProblems(PROBLEM_I_2);
        addFilter();
        checkProblemsFreeze(PROBLEM_I_2);
    }

    @Test
    @Description("Причина III не замораживается при добавлении фильтра")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9016")
    public void notFreezeProblemIIIByAddFilter() {
        sendProblems(PROBLEM_III);
        addFilter();
        checkProblemIIIActive();
    }
}
