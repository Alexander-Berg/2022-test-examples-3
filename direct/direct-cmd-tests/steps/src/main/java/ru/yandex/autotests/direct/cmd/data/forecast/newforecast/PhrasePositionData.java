package ru.yandex.autotests.direct.cmd.data.forecast.newforecast;

import com.google.gson.annotations.SerializedName;

public class PhrasePositionData {

    @SerializedName("bid")
    private Float bid;

    @SerializedName("clicks")
    private Integer clicks;

    @SerializedName("budget")
    private Double budget;

    @SerializedName("ctr")
    private Double ctr;

    @SerializedName("shows")
    private Integer shows;

    public Float getBid() {
        return bid;
    }

    public PhrasePositionData withBid(Float bid) {
        this.bid = bid;
        return this;
    }

    public Integer getClicks() {
        return clicks;
    }

    public PhrasePositionData withClicks(Integer clicks) {
        this.clicks = clicks;
        return this;
    }

    public Double getBudget() {
        return budget;
    }

    public PhrasePositionData withBudget(Double budget) {
        this.budget = budget;
        return this;
    }

    public Double getCtr() {
        return ctr;
    }

    public PhrasePositionData withCtr(Double ctr) {
        this.ctr = ctr;
        return this;
    }

    public Integer getShows() {
        return shows;
    }

    public PhrasePositionData withShows(Integer shows) {
        this.shows = shows;
        return this;
    }
}
