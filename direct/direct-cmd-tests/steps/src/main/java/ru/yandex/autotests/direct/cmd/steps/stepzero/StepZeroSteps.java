package ru.yandex.autotests.direct.cmd.steps.stepzero;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroRequest;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class StepZeroSteps extends DirectBackEndSteps {

    @Step("GET cmd = stepZero (нулевой шаг)")
    public StepZeroResponse getStepZero(StepZeroRequest request) {
        return get(CMD.STEP_ZERO, request, StepZeroResponse.class);
    }

    @Step("Получаем ответ контроллера StepZero")
    public StepZeroResponse getStepZero() {
        return getStepZero(new StepZeroRequest());
    }

}
