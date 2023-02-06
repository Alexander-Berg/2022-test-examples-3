package ru.yandex.autotests.direct.cmd.data.transfer;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class TransferRequest extends BasicDirectRequest {

    @SerializeKey("client_from")
    private String clientFrom;

    @SerializeKey("client_to")
    private String clientTo;

    public String getClientFrom() {
        return clientFrom;
    }

    public TransferRequest withClientFrom(String clientFrom) {
        this.clientFrom = clientFrom;
        return this;
    }

    public String getClientTo() {
        return clientTo;
    }

    public TransferRequest withClientTo(String clientTo) {
        this.clientTo = clientTo;
        return this;
    }
}
