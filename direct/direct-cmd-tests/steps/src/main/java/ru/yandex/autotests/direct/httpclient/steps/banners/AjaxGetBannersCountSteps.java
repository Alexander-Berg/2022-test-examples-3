package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.banners.AjaxGetBannersCountRequestBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 09.06.15
 */
public class AjaxGetBannersCountSteps extends DirectBackEndSteps {

    @Step("Получаем количество групп контроллером ajaxGetBannersCount")
    public DirectResponse getBannersCount(AjaxGetBannersCountRequestBean parameters) {
        return execute(getRequestBuilder().get(CMD.AJAX_GET_BANNERS_COUNT, parameters));
    }

    public DirectResponse getBannersCountByCid(String campaignId) {
        AjaxGetBannersCountRequestBean parameters = new AjaxGetBannersCountRequestBean();
        parameters.setCampaignId(campaignId);
        return getBannersCount(parameters);
    }

    public DirectResponse getBannersCount(String campaignId, String ulogin) {
        AjaxGetBannersCountRequestBean parameters = new AjaxGetBannersCountRequestBean();
        parameters.setCampaignId(campaignId);
        parameters.setUlogin(ulogin);
        return getBannersCount(parameters);
    }
}
