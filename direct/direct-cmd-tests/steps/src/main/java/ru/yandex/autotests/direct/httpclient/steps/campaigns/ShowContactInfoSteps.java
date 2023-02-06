package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.showcontactinfo.ShowContactInfoParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by shmykov on 16.06.15.
 */
public class ShowContactInfoSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера ShowContactInfo")
    public DirectResponse getShowContactInfo(ShowContactInfoParameters params, CSRFToken token) {
        return execute(getRequestBuilder().get(CMD.SHOW_CONTACT_INFO, token, params));
    }
}