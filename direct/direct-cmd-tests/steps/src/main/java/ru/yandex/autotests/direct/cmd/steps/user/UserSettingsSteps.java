package ru.yandex.autotests.direct.cmd.steps.user;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.clients.SettingsModel;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class UserSettingsSteps extends DirectBackEndSteps {

    @Step("POST cmd = saveSettings (сохранение настроек пользователя)")
    public RedirectResponse postSaveSettings(SettingsModel request) {
        return post(CMD.SAVE_SETTINGS, request, RedirectResponse.class);
    }

    @Step("GET cmd = userSettings (получение настроек пользователя)")
    public SettingsModel getUserSettings(BasicDirectRequest request) {
        return get(CMD.USER_SETTINGS, request, SettingsModel.class);
    }

    @Step("Получение настроек пользователя {0}")
    public SettingsModel getUserSettings(String login) {
        return getUserSettings((BasicDirectRequest) new BasicDirectRequest().withUlogin(login));
    }
}
