package ru.yandex.autotests.direct.cmd.steps.user;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.ajaxuseroptions.AjaxUserOptionsRequest;
import ru.yandex.autotests.direct.cmd.data.ajaxuseroptions.AjaxUserOptionsResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class AjaxUserOptionsSteps extends DirectBackEndSteps {
    @Step("POST cmd = saveSettings (сохранение настроек пользователя)")
    public AjaxUserOptionsResponse postAjaxUserOptions(AjaxUserOptionsRequest request) {
        return post(CMD.AJAX_USER_OPTIONS, request, AjaxUserOptionsResponse.class);
    }

    @Step("сохранение настройки скрытия тизера {1} пользователя {0}")
    public AjaxUserOptionsResponse postAjaxUserOptions(String login, String hide) {
        return post(
                CMD.AJAX_USER_OPTIONS, new AjaxUserOptionsRequest().withHideRecommendationEmailTeaser(hide)
                .withUlogin(login),
                AjaxUserOptionsResponse.class)
                ;
    }

}
