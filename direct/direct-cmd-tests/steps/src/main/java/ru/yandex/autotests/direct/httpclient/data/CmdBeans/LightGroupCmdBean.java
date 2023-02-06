package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 13.04.15.
 */
public class LightGroupCmdBean extends JsonStringTransformableCmdBean {

    @JsonPath(responsePath = "group_name", requestPath = "group_name")
    private String adGroupName;

    @JsonPath(responsePath = "adgroup_id", requestPath = "adgroup_id")
    private String adGroupID;

    @JsonPath(responsePath = "cid")
    private String campaignID;

    @JsonPath(responsePath = "first_available_bid", requestPath = "bid")
    private String firstAvailableBid;

    @JsonPath(responsePath = "banners", requestPath = "banners")
    private List<BannerCmdBean> banners;

    @JsonPath(responsePath = "phrases", requestPath = "phrases")
    private List<PhraseCmdBean> phrases;

    @JsonPath(responsePath = "strategy/manual_autobudget_sum", requestPath = "strategy/manual_autobudget_sum")
    private String manualAutobudgetSum;

    @JsonPath(responsePath = "strategy/manual_autobudget_sum", requestPath = "strategy/budget_strategy")
    private String budgetStrategy;

    @JsonPath(requestPath = "geo", responsePath = "geo")
    private String geo;

    public String getAdGroupName() {
        return adGroupName;
    }

    public void setAdGroupName(String adGroupName) {
        this.adGroupName = adGroupName;
    }

    public String getAdGroupID() {
        return adGroupID;
    }

    public void setAdGroupID(String adGroupID) {
        this.adGroupID = adGroupID;
    }

    public List<BannerCmdBean> getBanners() {
        return banners;
    }

    public void setBanners(List<BannerCmdBean> banners) {
        this.banners = banners;
    }

    public List<PhraseCmdBean> getPhrases() {
        return phrases;
    }

    public void setPhrases(List<PhraseCmdBean> phrases) {
        this.phrases = phrases;
    }

    public String getFirstAvailableBid() {
        return firstAvailableBid;
    }

    public void setFirstAvailableBid(String firstAvailableBid) {
        this.firstAvailableBid = firstAvailableBid;
    }

    public String getCampaignID() {
        return campaignID;
    }

    public void setCampaignID(String campaignID) {
        this.campaignID = campaignID;
    }

    public String getManualAutobudgetSum() {
        return manualAutobudgetSum;
    }

    public void setManualAutobudgetSum(String manualAutobudgetSum) {
        this.manualAutobudgetSum = manualAutobudgetSum;
    }

    public String getBudgetStrategy() {
        return budgetStrategy;
    }

    public void setBudgetStrategy(String budgetStrategy) {
        this.budgetStrategy = budgetStrategy;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }
}