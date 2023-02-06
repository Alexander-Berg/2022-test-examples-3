package ru.yandex.autotests.direct.httpclient.data.transfer;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 17.06.15
 */
public class TransferRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "client_from")
    private String clientFrom;

    @JsonPath(requestPath = "client_to")
    private String clientTo;

    public String getClientFrom() {
        return clientFrom;
    }

    public void setClientFrom(String clientFrom) {
        this.clientFrom = clientFrom;
    }

    public String getClientTo() {
        return clientTo;
    }

    public void setClientTo(String clientTo) {
        this.clientTo = clientTo;
    }
}
