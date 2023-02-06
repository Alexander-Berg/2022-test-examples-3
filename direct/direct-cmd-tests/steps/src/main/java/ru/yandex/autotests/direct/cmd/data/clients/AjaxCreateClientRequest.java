package ru.yandex.autotests.direct.cmd.data.clients;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

public class AjaxCreateClientRequest extends BasicDirectRequest {

    @SerializeKey("json_client_data")
    @SerializeBy(ValueToJsonSerializer.class)
    private ClientData clientData;

    public ClientData getClientData() {
        return clientData;
    }

    public void setClientData(ClientData clientData) {
        this.clientData = clientData;
    }

    public AjaxCreateClientRequest withClientData(ClientData clientData) {
        this.clientData = clientData;
        return this;
    }
}
