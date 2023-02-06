package ru.yandex.autotests.direct.httpclient.steps.stepzero;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.stepzero.StepZeroProcessParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by shmykov on 06.04.15.
 * TESTIRT-5051
 */
public class StepZeroProcessSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера StepZeroProcess")
    public DirectResponse getStepZeroProcess(StepZeroProcessParameters params) {
        return execute(getRequestBuilder().get(CMD.STEP_ZERO_PROCESS, params));
    }
}