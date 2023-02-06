package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.campaigns.ShowCampsResponseBean;
import ru.yandex.autotests.direct.httpclient.data.campaigns.campaignInfo.CampaignInfoCmd;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 23.09.14
 */
public class ShowCampsSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера showCamps для логина {0} ")
    public DirectResponse openShowCamps(String login) {
        BasicDirectFormParameters params = new BasicDirectFormParameters();
        params.setUlogin(login);
        return execute(getRequestBuilder().get(CMD.SHOW_CAMPS, params));
    }

    @Step("Получаем ответ контроллера showCamps")
    public DirectResponse openShowCamps() {
        return execute(getRequestBuilder().get(CMD.SHOW_CAMPS));
    }

    public ShowCampsResponseBean getShowCamps(String login) {
        DirectResponse response = openShowCamps(login);
        return JsonPathJSONPopulater.evaluateResponse(response, new ShowCampsResponseBean());
    }

    public ShowCampsResponseBean getShowCamps() {
        DirectResponse response = openShowCamps();
        return JsonPathJSONPopulater.evaluateResponse(response, new ShowCampsResponseBean());
    }

    public static CampaignInfoCmd getCampaign(ShowCampsResponseBean responseBean, Integer campaignID) {
        for (CampaignInfoCmd campaign : responseBean.getCampaigns()) {
            if (Integer.valueOf(campaign.getCampaignID()).equals(campaignID)) {
                return campaign;
            }
        }
        return null;
    }
}
