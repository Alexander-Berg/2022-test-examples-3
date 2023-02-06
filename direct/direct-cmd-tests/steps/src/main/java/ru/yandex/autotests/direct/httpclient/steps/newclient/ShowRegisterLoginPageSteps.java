package ru.yandex.autotests.direct.httpclient.steps.newclient;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.11.14
 */
public class ShowRegisterLoginPageSteps extends DirectBackEndSteps {

    @Step("Открываем страницу регистрации клиента агентства showRegisterLoginPage")
    public DirectResponse openShowRegisterLoginPage() {
        return execute(getRequestBuilder().get(CMD.SHOW_REGISTER_LOGIN_PAGE));
    }
}
