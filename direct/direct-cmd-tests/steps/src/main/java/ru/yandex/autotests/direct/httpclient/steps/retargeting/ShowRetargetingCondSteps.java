package ru.yandex.autotests.direct.httpclient.steps.retargeting;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by shmykov on 29.01.15.
 * TESTIRT-4057
 */
public class ShowRetargetingCondSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера ShowRetargetingCond")
    public DirectResponse openShowRetargetingCond(String login) {
        BasicDirectFormParameters params = new BasicDirectFormParameters();
        params.setUlogin(login);
        return execute(getRequestBuilder().get(CMD.SHOW_RETARGETING_COND, params));
    }
}