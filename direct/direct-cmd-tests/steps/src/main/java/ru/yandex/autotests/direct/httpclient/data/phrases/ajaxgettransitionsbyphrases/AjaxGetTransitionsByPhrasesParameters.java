package ru.yandex.autotests.direct.httpclient.data.phrases.ajaxgettransitionsbyphrases;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

public class AjaxGetTransitionsByPhrasesParameters extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String cid;
    @JsonPath(requestPath = "bid")
    private String bid;
    @JsonPath(requestPath = "phrases")
    private String phrases;
    @JsonPath(requestPath = "currency")
    private String currency;
    @JsonPath(requestPath = "geo")
    private String geo;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getPhrases() {
        return phrases;
    }

    public void setPhrases(String phrases) {
        this.phrases = phrases;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }
}
