package ru.yandex.autotests.direct.httpclient.data.banners.managevcards;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

public class ManageVCardsRequestParams extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String cid;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

}