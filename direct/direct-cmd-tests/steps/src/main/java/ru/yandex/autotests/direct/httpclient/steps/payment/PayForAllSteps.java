package ru.yandex.autotests.direct.httpclient.steps.payment;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.payment.payForAll.CampaignSumRequestBean;
import ru.yandex.autotests.direct.httpclient.data.payment.payForAll.PayForAllRequestBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public class PayForAllSteps extends DirectBackEndSteps {

    @Step("Открываем данные для выставления счета")
    public DirectResponse payForAll(CSRFToken csrfToken, PayForAllRequestBean payForAllRequestBean) {
        return execute(getRequestBuilder().get(CMD.PAY_FOR_ALL, csrfToken, payForAllRequestBean));
    }

    public DirectResponse payForAll(CSRFToken csrfToken, String login, String campaignID, String sum, String sumsWithNds) {
        PayForAllRequestBean payForAllRequestBean = new PayForAllRequestBean();
        payForAllRequestBean.setCampaignId(campaignID);
        payForAllRequestBean.setUlogin(login);
        CampaignSumRequestBean campaignSumRequestBean = new CampaignSumRequestBean();
        campaignSumRequestBean.setCampaignId(campaignID);
        campaignSumRequestBean.setSum(sum);
        payForAllRequestBean.addCampaignSum(campaignSumRequestBean);
        payForAllRequestBean.setSumsWithNds(sumsWithNds);
        return payForAll(csrfToken, payForAllRequestBean);
    }
}
