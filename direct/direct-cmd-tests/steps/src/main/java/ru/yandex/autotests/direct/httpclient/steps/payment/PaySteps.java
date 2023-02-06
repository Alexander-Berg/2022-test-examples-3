package ru.yandex.autotests.direct.httpclient.steps.payment;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.payment.PayRequestBean;
import ru.yandex.autotests.direct.httpclient.data.payment.PayResponseBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public class PaySteps extends DirectBackEndSteps {

    @Step("Открываем страницу оплаты кампании с номером {1} для логина {0}")
    public DirectResponse openPay(String login, String campaignID) {
        PayRequestBean params = new PayRequestBean();
        params.setUlogin(login);
        params.setCampaignId(campaignID);
        return execute(getRequestBuilder().get(CMD.PAY, params));
    }

    public PayResponseBean getPay(String login, String campaignID) {
        return JsonPathJSONPopulater.evaluateResponse(openPay(login, campaignID), new PayResponseBean());
    }
}
