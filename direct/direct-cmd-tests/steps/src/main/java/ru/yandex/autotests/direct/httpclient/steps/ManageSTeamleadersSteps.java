package ru.yandex.autotests.direct.httpclient.steps;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 23.09.14
 */
public class ManageSTeamleadersSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера manageSTeamleaders")
    public DirectResponse openManageSTeamleaders() {
        return execute(getRequestBuilder().get(CMD.MANAGE_S_TEAMLEADERS));
    }
}
