package ru.yandex.autotests.direct.httpclient.steps.transfer;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.transfer.CampaignFromSumRequestBean;
import ru.yandex.autotests.direct.httpclient.data.transfer.CampaignToSumRequestBean;
import ru.yandex.autotests.direct.httpclient.data.transfer.TransferDoneRequestBean;
import ru.yandex.autotests.direct.httpclient.data.transfer.TransferTypeEnum;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public class TransferDoneSteps extends DirectBackEndSteps {

    @Step("Переносим средства между кампаниями")
    public DirectResponse transferDone(CSRFToken csrfToken, TransferDoneRequestBean parameters) {
        return execute(getRequestBuilder().post(CMD.TRANSFER_DONE, csrfToken, parameters));
    }

    public DirectResponse transferDone(CSRFToken csrfToken, String clientFrom, String clientTo,
                                       List<CampaignFromSumRequestBean> campaignsFrom,
                                       List<CampaignToSumRequestBean> campaignsTo, TransferTypeEnum transferType) {
        TransferDoneRequestBean parameters = new TransferDoneRequestBean();
        parameters.setClientFrom(clientFrom);
        parameters.setClientTo(clientTo);
        switch (transferType) {
            case FROM_ONE_TO_MANY:
                parameters.setTransferFrom(campaignsFrom.get(0).getCampaignId());
                parameters.setTransferFromRadio(campaignsFrom.get(0).getCampaignId());
                parameters.setCampaignToSums(campaignsTo);
                break;
            case FROM_MANY_TO_ONE:
                parameters.setTransferTo(campaignsTo.get(0).getCampaignId());
                parameters.setTransferToRadio(campaignsTo.get(0).getCampaignId());
                parameters.setCampaignFromSums(campaignsFrom);
                break;
           default:
               throw new BackEndClientException("Не указан тип переноса средств");
        }
        return transferDone(csrfToken, parameters);

    }

    public DirectResponse transferDone(CSRFToken csrfToken, List<CampaignFromSumRequestBean> campaignsFrom,
                                       List<CampaignToSumRequestBean> campaignsTo, TransferTypeEnum transferType) {
        return transferDone(csrfToken, null, null, campaignsFrom, campaignsTo, transferType);
    }
}
