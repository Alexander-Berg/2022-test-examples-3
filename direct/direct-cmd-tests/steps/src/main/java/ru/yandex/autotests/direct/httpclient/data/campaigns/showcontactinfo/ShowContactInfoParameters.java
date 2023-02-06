package ru.yandex.autotests.direct.httpclient.data.campaigns.showcontactinfo;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 16.06.15.
 */
public class ShowContactInfoParameters extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(requestPath = "bid")
    private String bid;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }
}