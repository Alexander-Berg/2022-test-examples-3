package ru.yandex.autotests.direct.httpclient.steps;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 23.09.14
 */
public class ShowManagerMyClientsSteps extends DirectBackEndSteps {

    @Step("получаем ответ контроллера showManagerMyClients для логина {0}")
    public DirectResponse openShowManagerMyClients(String login) {
        BasicDirectFormParameters params = new BasicDirectFormParameters();
        params.setUlogin(login);
        return execute(getRequestBuilder().get(CMD.SHOW_MANAGER_MY_CLIENTS, params));

    }
}
