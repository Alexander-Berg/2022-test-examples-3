package ru.yandex.autotests.direct.cmd.steps.showdiag;

import java.util.function.Function;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.showdiag.ShowDiagRequest;
import ru.yandex.autotests.direct.cmd.data.showdiag.ShowDiagResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class ShowDiagSteps extends DirectBackEndSteps {

    @Step("GET cmd = showDiag (получение причин отклонения картиночного баннера)")
    public ShowDiagResponse getShowDiag(String bid) {
        ShowDiagRequest request = new ShowDiagRequest().withBid(bid).withFormat("json");
        return get(CMD.SHOW_DIAG, request, ShowDiagResponse.class);
    }

    public ShowDiagResponse getShowDiag(String bid, Function<ShowDiagRequest, ShowDiagRequest> requestBuilder) {
        ShowDiagRequest request = new ShowDiagRequest().withBid(bid).withFormat("json");
        request = requestBuilder.apply(request);
        return get(CMD.SHOW_DIAG, request, ShowDiagResponse.class);
    }
}
