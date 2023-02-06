package ru.yandex.autotests.direct.cmd.data.provenewagencyclients;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ProveNewAgencyClientsRequest extends BasicDirectRequest {

    @SerializeKey("data")
    private String data;

    public String getData() {
        return data;
    }

    public ProveNewAgencyClientsRequest withData(String data) {
        this.data = data;
        return this;
    }
}
