package ru.yandex.autotests.direct.cmd.data.commons.campaign;

import com.google.gson.annotations.SerializedName;

public class CampaignV2 extends Campaign {

    @SerializedName("day_budget")
    private DayBudget dayBudget;

    @SerializedName("device_targeting")
    private DeviceTargeting deviceTargeting;

    @SerializedName("DontShow")
    private String[] dontShow;

    @SerializedName("has_probabilistic_auction")
    private Integer hasProbalisticAuction;

    @SerializedName("attribution_model")
    private String attributionModel;


    public String getAttributionModel() {
        return attributionModel;
    }

    public CampaignV2 withAttributionModel(String attributionModel) {
        this.attributionModel = attributionModel;
        return this;
    }

    public Integer getHasProbalisticAuction() {
        return hasProbalisticAuction;
    }

    public CampaignV2 withHasProbalisticAuction(Integer hasProbalisticAuction) {
        this.hasProbalisticAuction = hasProbalisticAuction;
        return this;
    }

    public String[] getDontShow() {
        return dontShow;
    }

    public Campaign withDontShow(String[] dontShow) {
        this.dontShow = dontShow;
        return this;
    }

    public CampaignV2 withDayBudget(DayBudget dayBudget) {
        this.dayBudget = dayBudget;
        return this;
    }

    public CampaignV2 withDeviceTargeting(
            DeviceTargeting deviceTargeting)
    {
        this.deviceTargeting = deviceTargeting;
        return this;
    }

    public DeviceTargeting getDeviceTargeting() {
        return deviceTargeting;
    }

    public void setDeviceTargeting(DeviceTargeting deviceTargeting) {
        this.deviceTargeting = deviceTargeting;
    }

    public DayBudget getDayBudget() {
        return dayBudget;
    }

    public void setDayBudget(DayBudget dayBudget) {
        this.dayBudget = dayBudget;
    }
}
