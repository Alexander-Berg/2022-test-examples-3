package ru.yandex.autotests.direct.cmd.steps.transfer;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignFromSum;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignToSum;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferDoneRequest;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferTypeEnum;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

public class TransferDoneSteps extends DirectBackEndSteps {

    @Step("POST cmd = transfer_done (Перенос средств между кампаниями)")
    public RedirectResponse postTransferDone(TransferDoneRequest request) {
        return post(CMD.TRANSFER_DONE, request, RedirectResponse.class);
    }

    @Step("POST cmd = transfer_done (Перенос средств между кампаниями)")
    public ErrorResponse postTransferDoneErrorResponse(TransferDoneRequest request) {
        return post(CMD.TRANSFER_DONE, request, ErrorResponse.class);
    }

    @Step("Перенос средств от {0} к {1} с редирект ответом")
    public RedirectResponse transferDone(String clientFrom, String clientTo,
                                         List<CampaignFromSum> campaignsFrom,
                                         List<CampaignToSum> campaignsTo, TransferTypeEnum transferType) {
        return postTransferDone(
                new TransferDoneRequest(clientFrom, clientTo, campaignsFrom, campaignsTo, transferType));
    }

    @Step("Перенос средств от {0} к {1}  с json ответом")
    public ErrorResponse transferDoneErrorResponse(String clientFrom, String clientTo,
                                         List<CampaignFromSum> campaignsFrom,
                                         List<CampaignToSum> campaignsTo, TransferTypeEnum transferType) {
        return postTransferDoneErrorResponse(
                new TransferDoneRequest(clientFrom, clientTo, campaignsFrom, campaignsTo, transferType));
    }

    public RedirectResponse transferDone(List<CampaignFromSum> campaignsFrom,
                                         List<CampaignToSum> campaignsTo, TransferTypeEnum transferType) {
        return transferDone(null, null, campaignsFrom, campaignsTo, transferType);
    }

}
