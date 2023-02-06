package ru.yandex.autotests.direct.httpclient.data.clients;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.05.15
 */
public class ProveNewAgencyClientsRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "data")
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
