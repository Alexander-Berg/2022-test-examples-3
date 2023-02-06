package ru.yandex.autotests.direct.httpclient.steps.client;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 30.11.14
 */
public class UserSettingsSteps extends DirectBackEndSteps {

    @Step("Получаем настройки клиента {0} контроллером userSettings")
    public DirectResponse openUserSettings(String login) {
        BasicDirectFormParameters parameters = new BasicDirectFormParameters();
        parameters.setUlogin(login);
        return execute(getRequestBuilder().get(CMD.USER_SETTINGS, parameters));
    }

}
