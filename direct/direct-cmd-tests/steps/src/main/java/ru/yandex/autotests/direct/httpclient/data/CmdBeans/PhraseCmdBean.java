package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 13.04.15.
 */
public class PhraseCmdBean {

    @JsonPath(responsePath = "phrase", requestPath = "phrase")
    private String phrase;

    @JsonPath(responsePath = "id", requestPath = "id")
    private String phraseID;

    @JsonPath(responsePath = "price", requestPath = "price")
    private String price;

    @JsonPath(responsePath = "autobudgetPriority", requestPath = "autobudgetPriority")
    private String autobudgetPriority;

    @JsonPath(responsePath = "Clicks")
    private String clicks;

    @JsonPath(responsePath = "Shows")
    private String shows;

    @JsonPath(responsePath = "Ctr")
    private String ctr;

    @JsonPath(responsePath = "is_suspended")
    private String isSuspended;

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getPhraseID() {
        return phraseID;
    }

    public void setPhraseID(String id) {
        this.phraseID = id;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getAutobudgetPriority() {
        return autobudgetPriority;
    }

    public void setAutobudgetPriority(String autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
    }

    public String getClicks() {
        return clicks;
    }

    public void setClicks(String clicks) {
        this.clicks = clicks;
    }

    public String getShows() {
        return shows;
    }

    public void setShows(String shows) {
        this.shows = shows;
    }

    public String getCtr() {
        return ctr;
    }

    public void setCtr(String ctr) {
        this.ctr = ctr;
    }

    public String getIsSuspended() {
        return isSuspended;
    }

    public void setIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
    }
}
