package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.lang.reflect.Field;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class PhraseSaveParameters extends BasicDirectRequestParameters {
    public PhraseSaveParameters() {
        objectId = "1";
    }

    private String bid;

    @JsonPath(requestPath = "arr")
    private String arr;
    @JsonPath(requestPath = "broker")
    private String broker;
    @JsonPath(requestPath = "context_coverage")
    private String contextCoverage;
    @JsonPath(requestPath = "context_stop_flag")
    private String contextStopFlag;
    @JsonPath(requestPath = "id")
    private String id;
    @JsonPath(requestPath = "larr")
    private String lArr;
    @JsonPath(requestPath = "parr")
    private String pArr;
    @JsonPath(requestPath = "phrase")
    private String phrase;
    @JsonPath(requestPath = "rank")
    private String rank;
    @JsonPath(requestPath = "showsForecast")
    private String showsForecast;
    @JsonPath(requestPath = "val")
    private String val;

    @Override
    protected String getFormFieldName(Field field) {
        String name = field.getAnnotation(JsonPath.class).requestPath();
        return name + "-" + getBid() + "_" + objectId;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getArr() {
        return arr;
    }

    public void setArr(String arr) {
        this.arr = arr;
    }

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public String getContextCoverage() {
        return contextCoverage;
    }

    public void setContextCoverage(String contextCoverage) {
        this.contextCoverage = contextCoverage;
    }

    public String getContextStopFlag() {
        return contextStopFlag;
    }

    public void setContextStopFlag(String contextStopFlag) {
        this.contextStopFlag = contextStopFlag;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getlArr() {
        return lArr;
    }

    public void setlArr(String lArr) {
        this.lArr = lArr;
    }

    public String getpArr() {
        return pArr;
    }

    public void setpArr(String pArr) {
        this.pArr = pArr;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getShowsForecast() {
        return showsForecast;
    }

    public void setShowsForecast(String showsForecast) {
        this.showsForecast = showsForecast;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}
