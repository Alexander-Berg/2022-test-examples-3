package ru.yandex.autotests.direct.cmd.steps.transfer;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferRequest;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class TransferSteps extends DirectBackEndSteps {

    @Step("GET cmd = transfer (просмотр кампаний для переноса средств)")
    public TransferResponse getTransfer(TransferRequest request) {
        return get(CMD.TRANSFER, request, TransferResponse.class);
    }

    @Step("GET cmd = transfer (просмотр кампаний для переноса средств)")
    public ErrorResponse getTransferErrorResponse(TransferRequest request) {
        return get(CMD.TRANSFER, request, ErrorResponse.class);
    }

    @Step("Просмотр кампаний при переносе средств от {0} к {1}")
    public ErrorResponse getTransferErrorResponse(String clientFrom, String clientTo) {
        return getTransferErrorResponse(new TransferRequest()
                .withClientFrom(clientFrom)
                .withClientTo(clientTo));
    }

    @Step("Получение списка кампаний для переноса")
    public TransferResponse getTransfer() {
        return getTransfer(new TransferRequest());
    }
}
