package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.ShowCampParameters;
import ru.yandex.autotests.direct.httpclient.data.campaigns.ShowCampResponseBean;
import ru.yandex.autotests.direct.httpclient.data.mediaplan.SendOptimizeParameters;
import ru.yandex.autotests.direct.httpclient.steps.CommonSteps;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class CampaignsSteps extends DirectBackEndSteps {

    CommonSteps commonSteps = getInstance(CommonSteps.class, config);

    @Step("Открываем страницу кампании с номером {1} для логина {0}")
    public DirectResponse openShowCamp(String login, String campaignID) {
        ShowCampParameters params = new ShowCampParameters();
        params.setUlogin(login);
        params.setCid(campaignID);
        return execute(getRequestBuilder().get(CMD.SHOW_CAMP, params));
    }

    public ShowCampResponseBean getShowCamp(String login, String campaignID) {
        return JsonPathJSONPopulater.evaluateResponse(openShowCamp(login, campaignID), new ShowCampResponseBean());
    }

    @Step("Получаем из ответа id запроса на оптимизацию")
    public String getOptimizeRequestID(DirectResponse resp) {
        final String REQUEST_ID_PATH = "$.optimize_camp.request_id";
        return commonSteps.readResponseJsonProperty(resp, REQUEST_ID_PATH);
    }

    @Step("Отправляем баннеры на оптимизацию")
    public DirectResponse sendOptimize(SendOptimizeParameters params, CSRFToken csrfToken) {
        return execute(getRequestBuilder().post(CMD.SEND_OPTIMIZE, csrfToken, params));
    }


}
