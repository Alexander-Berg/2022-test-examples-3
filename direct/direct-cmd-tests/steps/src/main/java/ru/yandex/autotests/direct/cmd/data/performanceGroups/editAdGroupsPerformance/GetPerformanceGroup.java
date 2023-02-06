package ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;

import java.util.List;

public class GetPerformanceGroup {

    @SerializedName("group_name")
    private String adGroupName;

    @SerializedName("adgroup_id")
    private String adGroupID;

    @SerializedName("cid")
    private Long campaignID;

    @SerializedName("adgroup_type")
    private String adGroupType;

    @SerializedName("banners_quantity")
    private String bannersQuantity;

    @SerializedName("banners")
    private List<Banner> banners;

    @SerializedName("feed_id")
    private String feedId;

    @SerializedName("href_params")
    private String hrefParams;

    @SerializedName("performance_filters")
    private List<PerformanceFilter> performanceFilters;

    @SerializedName("minus_words")
    private List<String> minusKeywords;

    @SerializedName("geo")
    private String geo;

    @SerializedName("isCopyGroup")
    private String isCopyGroup;

    @SerializedName("isSingleGroup")
    private String isSingleGroup;

    @SerializedName("isNewGroup")
    private String isNewGroup;

    @SerializedName("is_group_copy_action")
    private String isGroupCopyAction;

    @SerializedName("autobudget")
    private String autobudget;

    @SerializedName("tags")
    private Object tags;

    @SerializedName("used_creative_ids")
    private List<String> usedCreativeIds;

    @SerializedName("otherCreativeIds")
    private List<String> otherCreativeIds;

    @SerializedName("hierarchical_multipliers")
    private HierarchicalMultipliers hierarchicalMultipliers;

    @SerializedName("status_bs_synced")
    private String status_bs_synced;

    @SerializedName("status_moderate")
    private String status_moderate;

    @SerializedName("status_post_moderate")
    private String status_post_moderate;

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

    public Long getCampaignID() {
        return campaignID;
    }

    public void setCampaignID(Long campaignID) {
        this.campaignID = campaignID;
    }

    public String getAdGroupType() {
        return adGroupType;
    }

    public void setAdGroupType(String adGroupType) {
        this.adGroupType = adGroupType;
    }

    public String getBannersQuantity() {
        return bannersQuantity;
    }

    public void setBannersQuantity(String bannersQuantity) {
        this.bannersQuantity = bannersQuantity;
    }

    public List<Banner> getBanners() {
        return banners;
    }

    public void setBanners(List<Banner> banners) {
        this.banners = banners;
    }

    public String getHrefParams() {
        return hrefParams;
    }

    public void setHrefParams(String hrefParams) {
        this.hrefParams = hrefParams;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public List<PerformanceFilter> getPerformanceFilters() {
        return performanceFilters;
    }

    public void setPerformanceFilters(List<PerformanceFilter> performanceFilters) {
        this.performanceFilters = performanceFilters;
    }

    public List<String> getMinusKeywords() {
        return minusKeywords;
    }

    public void setMinusKeywords(List<String> minusKeywords) {
        this.minusKeywords = minusKeywords;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getIsCopyGroup() {
        return isCopyGroup;
    }

    public void setIsCopyGroup(String isCopyGroup) {
        this.isCopyGroup = isCopyGroup;
    }

    public String getIsSingleGroup() {
        return isSingleGroup;
    }

    public void setIsSingleGroup(String isSingleGroup) {
        this.isSingleGroup = isSingleGroup;
    }

    public String getIsNewGroup() {
        return isNewGroup;
    }

    public void setIsNewGroup(String isNewGroup) {
        this.isNewGroup = isNewGroup;
    }

    public String getIsGroupCopyAction() {
        return isGroupCopyAction;
    }

    public void setIsGroupCopyAction(String isGroupCopyAction) {
        this.isGroupCopyAction = isGroupCopyAction;
    }

    public String getAutobudget() {
        return autobudget;
    }

    public void setAutobudget(String autobudget) {
        this.autobudget = autobudget;
    }

    public Object getTags() {
        return tags;
    }

    public void setTags(Object tags) {
        this.tags = tags;
    }

    public List<String> getUsedCreativeIds() {
        return usedCreativeIds;
    }

    public void setUsedCreativeIds(List<String> usedCreativeIds) {
        this.usedCreativeIds = usedCreativeIds;
    }

    public List<String> getOtherCreativeIds() {
        return otherCreativeIds;
    }

    public void setOtherCreativeIds(List<String> otherCreativeIds) {
        this.otherCreativeIds = otherCreativeIds;
    }

    public HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    public void setHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
    }

    public String getStatus_bs_synced() {
        return status_bs_synced;
    }

    public void setStatus_bs_synced(String status_bs_synced) {
        this.status_bs_synced = status_bs_synced;
    }

    public String getStatus_moderate() {
        return status_moderate;
    }

    public GetPerformanceGroup withStatus_moderate(String status_moderate) {
        this.status_moderate = status_moderate;
        return this;
    }

    public String getStatus_post_moderate() {
        return status_post_moderate;
    }

    public GetPerformanceGroup withStatus_post_moderate(String status_post_moderate) {
        this.status_post_moderate = status_post_moderate;
        return this;
    }
}
