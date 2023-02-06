package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.retargeting.RetargetingCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.tags.TagsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.adjustment.rates.HierarchicalMultipliers;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by aleran on 15.09.2015.
 */
public class MobileGroupCmdBean extends JsonStringTransformableCmdBean {

    @JsonPath(responsePath = "group_name", requestPath = "group_name")
    private String groupName;

    @JsonPath(responsePath = "adgroup_id", requestPath = "adgroup_id")
    private String adGroupID;

    @JsonPath(responsePath = "cid")
    private String campaignID;

    @JsonPath(responsePath = "mobile_content", requestPath = "mobile_content")
    private MobileContentCmdBean mobileContent;

    @JsonPath(requestPath = "store_content_href", responsePath = "store_content_href")
    private String storeContentHref;

    @JsonPath(responsePath = "adgroup_type", requestPath = "adgroup_type")
    private String adgroupType;

    @JsonPath(responsePath = "device_type_targeting", requestPath = "device_type_targeting")
    private List<String> deviceTypeTargeting;

    @JsonPath(responsePath = "network_targeting", requestPath = "network_targeting")
    private List<String> networkTargeting;

    @JsonPath(responsePath = "minus_words", requestPath = "minus_words")
    private List<String> minusKeywords;

    @JsonPath(responsePath = "banners", requestPath = "banners")
    private List<MobileAppBannerCmdBean> banners;

    @JsonPath(responsePath = "phrases", requestPath = "phrases")
    private List<PhraseCmdBean> phrases;

    @JsonPath(requestPath = "geo", responsePath = "geo")
    private String geo;

    @JsonPath(responsePath = "banners_quantity")
    private String bannersQuantity;

    @JsonPath(requestPath = "retargetings", responsePath = "retargetings")
    private List<RetargetingCmdBean> retargetings;

    @JsonPath(responsePath = "hierarchical_multipliers", requestPath = "hierarchical_multipliers")
    private HierarchicalMultipliers hierarchicalMultipliers;

    @JsonPath(responsePath = "min_os_version", requestPath = "min_os_version")
    private String minOsVersion;

    @JsonPath(responsePath = "tags", requestPath = "tags")
    private TagsCmdBean tags;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getAdGroupID() {
        return adGroupID;
    }

    public void setAdGroupID(String adGroupID) {
        this.adGroupID = adGroupID;
    }

    public String getCampaignID() {
        return campaignID;
    }

    public void setCampaignID(String campaignID) {
        this.campaignID = campaignID;
    }

    public MobileContentCmdBean getMobileContent() {
        return mobileContent;
    }

    public void setMobileContent(MobileContentCmdBean mobileContent) {
        this.mobileContent = mobileContent;
    }

    public String getStoreContentHref() {
        return storeContentHref;
    }

    public void setStoreContentHref(String storeContentHref) {
        this.storeContentHref = storeContentHref;
    }

    public String getAdgroupType() {
        return adgroupType;
    }

    public void setAdgroupType(String adgroupType) {
        this.adgroupType = adgroupType;
    }

    public List<String> getDeviceTypeTargeting() {
        return deviceTypeTargeting;
    }

    public void setDeviceTypeTargeting(List<String> deviceTypeTargeting) {
        this.deviceTypeTargeting = deviceTypeTargeting;
    }

    public List<String> getNetworkTargeting() {
        return networkTargeting;
    }

    public void setNetworkTargeting(List<String> networkTargeting) {
        this.networkTargeting = networkTargeting;
    }

    public List<String> getMinusKeywords() {
        return minusKeywords;
    }

    public void setMinusKeywords(List<String> minusKeywords) {
        this.minusKeywords = minusKeywords;
    }

    public List<MobileAppBannerCmdBean> getBanners() {
        return banners;
    }

    public void setBanners(List<MobileAppBannerCmdBean> banners) {
        this.banners = banners;
    }

    public List<PhraseCmdBean> getPhrases() {
        return phrases;
    }

    public void setPhrases(List<PhraseCmdBean> phrases) {
        this.phrases = phrases;
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

    public List<RetargetingCmdBean> getRetargetings() {
        return retargetings;
    }

    public void setRetargetings(List<RetargetingCmdBean> retargetings) {
        this.retargetings = retargetings;
    }

    public HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    public void setHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
    }
}
