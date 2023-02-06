package ru.yandex.autotests.direct.httpclient.steps;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 07.10.14
 */
public class AdvertizeSteps extends DirectBackEndSteps {

    CommonSteps commonSteps = getInstance(CommonSteps.class, config);

    @Step("Получаем ответ контроллера advertize")
    public DirectResponse openAdvertize() {
        return execute(getRequestBuilder().get(CMD.ADVERTIZE));
    }

    @Step("Проверяем редирект контроллера advertize на страницу {0}")
    public void checkAdvertizeRedirect(String expectedSubstring) {
        DirectResponse directResponse = openAdvertize();
        commonSteps.checkRedirect(directResponse, expectedSubstring);
    }
}
