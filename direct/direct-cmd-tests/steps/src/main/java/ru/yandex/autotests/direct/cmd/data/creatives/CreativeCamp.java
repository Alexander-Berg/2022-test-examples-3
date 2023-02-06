package ru.yandex.autotests.direct.cmd.data.creatives;

import com.google.gson.annotations.SerializedName;

public class CreativeCamp {

    @SerializedName("id")
    private Long campaignId;
    @SerializedName("campaign_name")
    private String name;
    @SerializedName("status_empty")
    private String statusEmpty;
    @SerializedName("status_moderate")
    private String statusModerate;
    @SerializedName("currency")
    private String currency;
    @SerializedName("sum")
    private Double sum;

    public Long getCampaignId() {
        return campaignId;
    }

    public CreativeCamp withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public String getName() {
        return name;
    }

    public CreativeCamp withName(String name) {
        this.name = name;
        return this;
    }

    public String getStatusEmpty() {
        return statusEmpty;
    }

    public CreativeCamp withStatusEmpty(String statusEmpty) {
        this.statusEmpty = statusEmpty;
        return this;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public CreativeCamp withStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public CreativeCamp withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Double getSum() {
        return sum;
    }

    public CreativeCamp withSum(Double sum) {
        this.sum = sum;
        return this;
    }
}