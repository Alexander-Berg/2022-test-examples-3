package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.DelCampRequestBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 09.06.15
 */
public class DelCampSteps extends DirectBackEndSteps {

    @Step("Удаляем кампанию")
    public DirectResponse deleteCampaign(CSRFToken token, DelCampRequestBean params) {
        return execute(getRequestBuilder().get(CMD.DEL_CAMP, token, params));
    }

    public DirectResponse deleteCampaign(CSRFToken token, String campaignId) {
        DelCampRequestBean delCampRequestBean = new DelCampRequestBean();
        delCampRequestBean.setCampaignId(campaignId);
        return execute(getRequestBuilder().get(CMD.DEL_CAMP, token, delCampRequestBean));
    }

    public DirectResponse deleteCampaign(CSRFToken token, String campaignId, String login) {
        DelCampRequestBean delCampRequestBean = new DelCampRequestBean();
        delCampRequestBean.setCampaignId(campaignId);
        delCampRequestBean.setUlogin(login);
        return execute(getRequestBuilder().get(CMD.DEL_CAMP, token, delCampRequestBean));
    }
}
