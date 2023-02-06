package ru.yandex.autotests.direct.cmd.steps.campaings;


import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.strategy.AjaxSaveAutobudgetResponse;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.direct.cmd.data.strategy.AjaxSaveAutoBudgetRequest;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class StrategySteps extends DirectBackEndSteps {

    @Step("Сохранение стратегии для кампании {0}")
    public AjaxSaveAutobudgetResponse saveAutobudget(String campaignId, CampaignStrategy strategy) {
        return postAjaxSaveAutobudget(new AjaxSaveAutoBudgetRequest().
                withCid(campaignId).
                withJsonStrategy(strategy));
    }

    @Step("Сохранение стратегии для кампании {0}")
    public AjaxSaveAutobudgetResponse saveAutobudget(String campaignId, CampaignStrategy strategy,
            DayBudget dayBudget)
    {
        return postAjaxSaveAutobudget(new AjaxSaveAutoBudgetRequest().
                withDayBudget(dayBudget).
                withCid(campaignId).
                withJsonStrategy(strategy));
    }

    @Step("POST cmd = ajaxSaveAutobudget (сохранение стратегии для кампании)")
    public AjaxSaveAutobudgetResponse postAjaxSaveAutobudget(AjaxSaveAutoBudgetRequest request) {
        return post(CMD.AJAX_SAVE_AUTOBUDGET, request, AjaxSaveAutobudgetResponse.class);
    }

}
