package ru.yandex.autotests.direct.httpclient.steps;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.clients.ModifyUserBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater.evaluateResponse;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 23.09.14
 */
public class ModifyUserSteps extends DirectBackEndSteps {

    @Step("получаем ответ контроллера modifyUser для логина {0} с crf-токеном {1}")
    public DirectResponse openModifyUser(String login, CSRFToken token) {
        BasicDirectFormParameters params = new BasicDirectFormParameters();
        params.setUlogin(login);
        return execute(getRequestBuilder().get(CMD.MODIFY_USER, token, params));
    }

    @Step("Сохраняем настройки пользователя")
    public DirectResponse modifyUser(ModifyUserBean params, CSRFToken token) {
        return execute(getRequestBuilder().post(CMD.MODIFY_USER, token, params));
    }

    public ModifyUserBean getModifyUserResponse(String login, CSRFToken token) {
        DirectResponse response = openModifyUser(login, token);
        return evaluateResponse(response, new ModifyUserBean());
    }
}
