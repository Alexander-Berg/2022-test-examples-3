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
@Description("Заморозка уведомлений для динамической кампании со стратагией - Средняя цена клика")
@Features(TestFeatures.AUTOBUDGET_ALERTS)
@Tag(CampTypeTag.DYNAMIC)
@Tag(BusinessProcessTag.ALERT)
public class FreezeDynamicAutobudgetAvgClickAlertsTest extends FreezeAutobudgetAlertsBaseTest {

    @Override
    BannersRule getBannerRule() {
        if (bannersRule == null) {
            bannersRule = new DynamicBannersRule()
                    .withCampStrategy(CmdStrategyBeans.getStrategyBean(Strategies.AVERAGE_PRICE_SHOWS_DISABLED))
                    .withUlogin(CLIENT);
        }
        return bannersRule;
    }

    //увеличиваем среднюю цену клика в параметрах стратегии
    protected void incrementAvgBid() {
        CampaignStrategy campaignStrategy = CmdStrategyBeans.getStrategyBean(Strategies.AVERAGE_PRICE_SHOWS_DISABLED);
        Float bid = Float.valueOf(campaignStrategy.getSearch().getAvgBid()) + 1;
        campaignStrategy.getSearch().withAvgBid(bid.toString());
        ajaxSaveAutoBudgetRequest(campaignStrategy);
    }

    @Test
    @Description("Заморозка причины I_1 при увеличении средней цены клика")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9005")
    public void freezeProblemI_1ByIncrementAvgBid() {
        sendProblems(PROBLEM_I_1);
        incrementAvgBid();
        checkProblemsFreeze(PROBLEM_I_1);
    }

    @Test
    @Description("Заморозка причины I_2 при увеличении средней цены клика")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9006")
    public void freezeProblemI_2ByIncrementAvgBid() {
        sendProblems(PROBLEM_I_2);
        incrementAvgBid();
        checkProblemsFreeze(PROBLEM_I_2);
    }

    @Test
    @Description("Заморозка причины III при увеличении средней цены клика")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9007")
    public void freezeProblemIIIByIncrementAvgBid() {
        sendProblems(PROBLEM_III);
        incrementAvgBid();
        checkProblemsFreeze(PROBLEM_III);
    }
}
