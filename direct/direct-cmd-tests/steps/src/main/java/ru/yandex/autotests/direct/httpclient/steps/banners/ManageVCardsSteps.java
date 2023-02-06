package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.banners.managevcards.ManageVCardsRequestParams;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by shmykov on 12.06.15.
 */
public class ManageVCardsSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера ManageVCards")
    public DirectResponse openManageVCards(ManageVCardsRequestParams params, CSRFToken token) {
        return execute(getRequestBuilder().get(CMD.MANAGE_VCARDS, token, params));
    }
}