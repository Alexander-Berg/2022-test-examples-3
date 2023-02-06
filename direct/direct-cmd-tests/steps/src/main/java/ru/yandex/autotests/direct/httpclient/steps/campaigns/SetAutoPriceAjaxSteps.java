package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.setautopriceajax.SetAutoPriceAjaxRequestParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by shmykov on 11.06.15.
 */
public class SetAutoPriceAjaxSteps extends DirectBackEndSteps {

    @Step("Сохраняем цены на всю кампанию")
    public DirectResponse setAutoPriceAjax(CSRFToken token, SetAutoPriceAjaxRequestParameters params) {
        return execute(getRequestBuilder().post(CMD.SET_AUTO_PRICE_AJAX, token, params));
    }
}
