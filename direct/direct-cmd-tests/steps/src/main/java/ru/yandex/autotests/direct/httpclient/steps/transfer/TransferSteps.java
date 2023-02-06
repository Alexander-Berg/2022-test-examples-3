package ru.yandex.autotests.direct.httpclient.steps.transfer;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.transfer.TransferRequestBean;
import ru.yandex.autotests.direct.httpclient.data.transfer.TransferResponseBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public class TransferSteps extends DirectBackEndSteps {

    @Step("Открываем страницу переноса средств")
    public DirectResponse openTransfer(TransferRequestBean parameters) {
        return execute(getRequestBuilder().get(CMD.TRANSFER, parameters));
    }

    public DirectResponse openTransfer() {
        return execute(getRequestBuilder().get(CMD.TRANSFER, new TransferRequestBean()));
    }

    public DirectResponse openTransfer(String clientFrom, String clientTo) {
        TransferRequestBean parameters = new TransferRequestBean();
        parameters.setClientFrom(clientFrom);
        parameters.setClientTo(clientTo);
        return execute(getRequestBuilder().get(CMD.TRANSFER, parameters));
    }

    public TransferResponseBean getTransfer() {
        return JsonPathJSONPopulater.evaluateResponse(openTransfer(), new TransferResponseBean());
    }

    public TransferResponseBean getTransfer(String clientFrom, String clientTo) {
        return JsonPathJSONPopulater.evaluateResponse(openTransfer(clientFrom, clientTo), new TransferResponseBean());
    }
}
