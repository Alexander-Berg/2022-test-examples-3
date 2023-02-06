package ru.yandex.autotests.direct.httpclient.steps.client;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.clients.SaveSettingsParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 30.11.14
 */
public class SaveSettingsSteps extends DirectBackEndSteps {

    @Step("Сохраняем настройки пользователя контроллера saveSettings")
    public DirectResponse saveUserSettings(CSRFToken csrfToken, SaveSettingsParameters saveSettingsParameters) {
        return execute(getRequestBuilder().get(CMD.SAVE_SETTINGS, csrfToken, saveSettingsParameters));
    }
}
