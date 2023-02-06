package ru.yandex.autotests.direct.httpclient.data.payment;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

public class Client {
    @JsonPath(responsePath = "NDS")
    private Double nds;

    public Double getNds() {
        return nds;
    }

    public void setNds(Double nds) {
        this.nds = nds;
    }

    public Client withNds(Double nds) {
        this.nds = nds;
        return this;
    }
}
