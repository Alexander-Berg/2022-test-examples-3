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
public class ShowUserEmailsSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера showUserEmails")
    public DirectResponse openShowUserEmails() {
        return execute(getRequestBuilder().get(CMD.SHOW_USER_EMAILS));
    }

    //т.к. страница не на bem, для нее не работает get_vars
    @Step("Получаем ответ контроллера showUserEmails в формате json")
    public DirectResponse openShowUserEmailsAtJsonFormat() {
        BasicDirectFormParameters params = new BasicDirectFormParameters();
        params.setFormat("json");
        return execute(getRequestBuilder().get(CMD.SHOW_USER_EMAILS, params));
    }
}
