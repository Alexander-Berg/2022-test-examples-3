package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.retargeting.RetargetingCmdBean;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 29.04.15.
 */
public class GroupCmdBean extends JsonStringTransformableCmdBean {

    @JsonPath(responsePath = "group_name", requestPath = "group_name")
    private String adGroupName;

    @JsonPath(responsePath = "adgroup_id", requestPath = "adgroup_id")
    private String adGroupID;

    @JsonPath(responsePath = "cid")
    private String campaignID;

    @JsonPath(requestPath = "tags", responsePath = "tags")
    private Object tags;

    @JsonPath(responsePath = "minus_words", requestPath = "minus_words")
    private List<String> minusKeywords;

    @JsonPath(responsePath = "banners", requestPath = "banners")
    private List<BannerCmdBean> banners;

    @JsonPath(responsePath = "phrases", requestPath = "phrases")
    private List<PhraseCmdBean> phrases;

    @JsonPath(requestPath = "geo", responsePath = "geo")
    private String geo;

    @JsonPath(responsePath = "banners_quantity")
    private String bannersQuantity;

    @JsonPath(requestPath = "retargetings", responsePath = "retargetings")
    private List<RetargetingCmdBean> retargetings;

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

    public List<String> getMinusKeywords() {
        return minusKeywords;
    }

    public void setMinusKeywords(List<String> minusKeywords) {
        this.minusKeywords = minusKeywords;
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

    public String getCampaignID() {
        return campaignID;
    }

    public void setCampaignID(String campaignID) {
        this.campaignID = campaignID;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public List<RetargetingCmdBean> getRetargetings() {
        return retargetings;
    }

    public void setRetargetings(List<RetargetingCmdBean> retargetings) {
        this.retargetings = retargetings;
    }

    public String getBannersQuantity() {
        return bannersQuantity;
    }

    public void setBannersQuantity(String bannersQuantity) {
        this.bannersQuantity = bannersQuantity;
    }

    public Object getTags() {
        return tags;
    }

    public void setTags(Object tags) {
        this.tags = tags;
    }
}