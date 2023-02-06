package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.banners.MediaGroupResponseBean;
import ru.yandex.autotests.direct.httpclient.data.banners.ShowMediaCampBannerBean;
import ru.yandex.autotests.direct.httpclient.data.campaigns.ShowCampParameters;
import ru.yandex.autotests.direct.httpclient.data.campaigns.ShowCampResponseBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 15.06.15
 */
public class ShowMediaCampSteps extends DirectBackEndSteps {

    @Step("Открываем страницу мкб кампании с номером {1} для логина {0}")
    public DirectResponse openShowMediaCamp(String login, String campaignID) {
        ShowCampParameters params = new ShowCampParameters();
        params.setUlogin(login);
        params.setCid(campaignID);
        return execute(getRequestBuilder().get(CMD.SHOW_MEDIA_CAMP, params));
    }

    public ShowCampResponseBean getShowMediaCamp(String login, String campaignID) {
        return JsonPathJSONPopulater.evaluateResponse(openShowMediaCamp(login, campaignID), new ShowCampResponseBean());
    }

    public MediaGroupResponseBean getShowMediaCampGroups(String login, String campaignID) {
        return JsonPathJSONPopulater.evaluateResponse(openShowMediaCamp(login, campaignID), new MediaGroupResponseBean());
    }
}
