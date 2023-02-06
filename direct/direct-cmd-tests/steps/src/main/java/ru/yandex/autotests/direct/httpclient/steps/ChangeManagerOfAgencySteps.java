package ru.yandex.autotests.direct.httpclient.steps;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 23.09.14
 */
public class ChangeManagerOfAgencySteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера changeManagerOfAgency с csrf-токеном {0}")
    public DirectResponse openChangeManagerOfAgency(CSRFToken token) {
        return execute(getRequestBuilder().get(CMD.CHANGE_MANAGER_OF_AGENCY, token));
    }
}
