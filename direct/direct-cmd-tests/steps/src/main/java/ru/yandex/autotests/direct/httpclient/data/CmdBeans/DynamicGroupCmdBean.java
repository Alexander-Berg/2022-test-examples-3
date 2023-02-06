package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.dynamicconditions.DynamicConditionsCmdBean;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by f1nal on 01.07.15
 * TESTIRT-6117.
 */
public class DynamicGroupCmdBean extends JsonStringTransformableCmdBean {

    @JsonPath(responsePath = "group_name", requestPath = "group_name")
    private String adGroupName;

    @JsonPath(responsePath = "adgroup_id", requestPath = "adgroup_id")
    private String adGroupID;

    @JsonPath(responsePath = "cid")
    private String campaignID;

    @JsonPath(responsePath = "minus_words", requestPath = "minus_words")
    private List<String> minusKeywords;

    @JsonPath(responsePath = "banners", requestPath = "banners")
    private List<DynamicBannerCmdBean> banners;

    @JsonPath(responsePath = "dynamic_conditions", requestPath = "dynamic_conditions")
    private List<DynamicConditionsCmdBean> dynamicConditions;

    @JsonPath(requestPath = "geo", responsePath = "geo")
    private String geo;

    @JsonPath(requestPath = "banners_quantity", responsePath = "banners_quantity")
    private String bannersQuantity;

    @JsonPath(responsePath = "href_params", requestPath = "href_params")
    private String hrefParams;

    @JsonPath(responsePath = "main_domain", requestPath = "main_domain")
    private String mainDomain;

    @JsonPath(responsePath = "tags", requestPath = "tags")
    private Object tags;

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

    public List<DynamicBannerCmdBean> getBanners() {
        return banners;
    }

    public void setBanners(List<DynamicBannerCmdBean> banners) {
        this.banners = banners;
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

    public String getBannersQuantity() {
        return bannersQuantity;
    }

    public void setBannersQuantity(String bannersQuantity) {
        this.bannersQuantity = bannersQuantity;
    }

    public List<DynamicConditionsCmdBean> getDynamicConditions() {
        return dynamicConditions;
    }

    public void setDynamicConditions(List<DynamicConditionsCmdBean> dynamicConditions) {
        this.dynamicConditions = dynamicConditions;
    }

    public String getMainDomain() {
        return mainDomain;
    }

    public void setMainDomain(String mainDomain) {
        this.mainDomain = mainDomain;
    }

    public String getHrefParams() {
        return hrefParams;
    }

    public void setHrefParams(String hrefParams) {
        this.hrefParams = hrefParams;
    }

    public Object getTags() {
        return tags;
    }

    public void setTags(Object tags) {
        this.tags = tags;
    }

}