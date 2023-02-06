package ru.yandex.autotests.direct.cmd.steps.representative;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class RepresentativeSteps extends DirectBackEndSteps {

    @Step("POST cmd = deleteClRep (удаляем представителя)")
    public ErrorResponse postDeleteClRepErrorResponse(BasicDirectRequest request) {
        return post(CMD.DELETE_CL_REP, request, ErrorResponse.class);
    }

    @Step("удаляем представителя {0}")
    public ErrorResponse postDeleteClRepErrorResponse(String login) {
        return postDeleteClRepErrorResponse((BasicDirectRequest) new BasicDirectRequest().withUlogin(login));
    }
}
