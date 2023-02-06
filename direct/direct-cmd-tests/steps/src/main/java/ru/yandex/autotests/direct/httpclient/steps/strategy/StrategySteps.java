package ru.yandex.autotests.direct.httpclient.steps.strategy;

import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.data.strategy.AjaxSaveAutoBudgetParameters;
import ru.yandex.autotests.direct.httpclient.data.strategy.DayBudget;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by alexey-n on 17.08.14.
 */
public class StrategySteps extends DirectBackEndSteps {

    @Step("Сохранение стратегии в кампании {1}")
    public void saveAndCheckResponseStrategy(String campaignId, CampaignStrategy strategy, CSRFToken csrfToken) {
        DirectResponse response = saveStrategyAndDayBudget(campaignId, strategy, null, csrfToken);
        assertThat("успешное сохранение стратегии", response,
                hasJsonProperty(Responses.SUCCESS.getPath(), equalTo(1)));
    }

    @Step("Проверка ответа при сохранении дневного бюджета и стратегии в кампанию {0}")
    public void saveAndCheckResponseDayBudgetAndStrategy(String campaignId, DayBudget dayBudget, CampaignStrategy strategy,
                                              CSRFToken csrfToken) {
        DirectResponse response = saveStrategyAndDayBudget(campaignId, strategy, dayBudget, csrfToken);
        assertThat("успешное сохранение дневного бюджета и стратегии", response,
                hasJsonProperty(Responses.SUCCESS.getPath(), equalTo(1)));
    }

    @Step("Проверка ответа при сохранении дневного бюджета в кампанию {0}")
    public void saveAndCheckResponseDayBudget(String campaignId, DayBudget dayBudget,
                                                         CSRFToken csrfToken) {
        DirectResponse response = saveDayBudget(campaignId, dayBudget, csrfToken);
        assertThat("успешное сохранение дневного бюджета", response,
                hasJsonProperty(Responses.OK.getPath(), equalTo(1)));
    }

    @Step("Проверка ошибки при сохранении стратегии в кампании {0}")
    public void saveAndCheckErrorInResponseStrategy(String campaignId, CampaignStrategy strategy,
                                                    Matcher matcher, CSRFToken csrfToken) {
        DirectResponse response = saveStrategyAndDayBudget(campaignId, strategy, null, csrfToken);
        assertThat("ответ контроллера содержит ошибку, соответствующую условию", response,
                hasJsonProperty(Responses.ERROR.getPath(), matcher));
    }

    @Step("Проверка ошибки при сохранении дневного бюджета через ajaxSaveAutobudget в кампании {0}")
    public void saveAndCheckErrorInResponseDayBudgetUsingAutobudgetCMD(String campaignId, DayBudget dayBudget,
                                                     Matcher matcher, CSRFToken csrfToken) {
        DirectResponse response = saveStrategyAndDayBudget(campaignId, null, dayBudget,csrfToken);
        assertThat("ответ контроллера содержит ошибку, соответствующую условию", response,
                hasJsonProperty(Responses.ERROR.getPath(), matcher));
    }

    @Step("Проверка ошибки при сохранении дневного бюджета через ajaxSaveDayBudget в кампании {0}")
    public void saveAndCheckErrorInResponseDayBudgetUsingDayBudgetCMD(String campaignId, DayBudget dayBudget,
                                                     Matcher matcher, CSRFToken csrfToken) {
        DirectResponse response = saveDayBudget(campaignId, dayBudget, csrfToken);
        assertThat("ответ контроллера содержит ошибку, соответствующую условию", response,
                hasJsonProperty(Responses.ERROR.getPath(), matcher));
    }

    @Step("Сохранение стратегии и дневного бюджета в кампании {0}")
    public DirectResponse saveStrategyAndDayBudget(String campaignId, CampaignStrategy strategy,
                                                    DayBudget dayBudget, CSRFToken token) {
        AjaxSaveAutoBudgetParameters parameters = new AjaxSaveAutoBudgetParameters();
        parameters.setCid(campaignId);
        String json = null;
        if(dayBudget != null) {
            json = dayBudget.toJson();
        }
        parameters.setJsonDayBudget(json);
        json = null;
        if(strategy != null) {
            json = new ValueToJsonSerializer().serialize(strategy);
        }
        parameters.setJsonStrategy(json);

        DirectResponse response = execute(getRequestBuilder().post(CMD.AJAX_SAVE_AUTOBUDGET, token, parameters));
        return response;
    }

    @Step("Сохранение дневного бюджета в кампании {0}")
    public DirectResponse saveDayBudget(String campaignId,  DayBudget dayBudget, CSRFToken token) {
        AjaxSaveAutoBudgetParameters parameters = new AjaxSaveAutoBudgetParameters();
        parameters.setCid(campaignId);
        String json = null;
        if(dayBudget != null) {
            json = dayBudget.toJson();
        }
        parameters.setJsonDayBudget(json);

        DirectResponse response = execute(getRequestBuilder().post(CMD.AJAX_SAVE_DAY_BUDGET, token, parameters));
        return response;
    }
}
