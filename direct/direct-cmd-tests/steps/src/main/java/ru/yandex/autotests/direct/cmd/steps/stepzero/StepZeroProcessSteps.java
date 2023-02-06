package ru.yandex.autotests.direct.cmd.steps.stepzero;

import org.jsoup.nodes.Document;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessErrorResponse;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessRequest;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class StepZeroProcessSteps extends DirectBackEndSteps {

    @Step("GET cmd = stepZeroProcess (нулевой шаг заведение клиента менеджером/агенством...)")
    public RedirectResponse getStepZeroProcess(StepZeroProcessRequest request) {
        return get(CMD.STEP_ZERO_PROCESS, request, RedirectResponse.class);
    }

    @Step("GET cmd = stepZeroProcess (нулевой шаг заведение клиента менеджером/агенством...)")
    public Document getStepZeroProcessDocument(StepZeroProcessRequest request) {
        return get(CMD.STEP_ZERO_PROCESS, request, Document.class);
    }

    @Step("GET cmd = stepZeroProcess (нулевой шаг заведение клиента менеджером/агенством...)")
    public StepZeroProcessErrorResponse getStepZeroProcessErrorResponse(StepZeroProcessRequest request) {
        return get(CMD.STEP_ZERO_PROCESS, request, StepZeroProcessErrorResponse.class);
    }
}
