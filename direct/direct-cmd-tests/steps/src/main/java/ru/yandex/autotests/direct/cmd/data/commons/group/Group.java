package ru.yandex.autotests.direct.cmd.data.commons.group;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.interest.RelevanceMatch;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterests;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;

/**
 * Отправляется в формате json в ручки:
 * saveDynamicAdGroups
 */
public class Group {

    @SerializedName("modelID")
    private String modelId;

    @SerializedName("adgroup_id")
    private String adGroupID;

    @SerializedName("adgroup_type")
    private String adGroupType;

    @SerializedName("group_name")
    private String adGroupName;

    @SerializedName("cid")
    private String campaignID;

    @SerializedName("minus_words")
    private List<String> minusWords;

    @SerializedName("banners")
    private List<Banner> banners;

    @SerializedName("phrases")
    private List<Phrase> phrases;

    @SerializedName("keywords")
    private List<Phrase> keywords;

    @SerializedName("dynamic_conditions")
    private List<DynamicCondition> dynamicConditions;

    @SerializedName("geo")
    private String geo;

    @SerializedName("retargetings")
    private List<Retargeting> retargetings = new ArrayList<>();

    @SerializedName("banners_quantity")
    private Long bannersQuantity;

    @SerializedName("statusBsSynced")
    private String statusBsSynced;

    @SerializedName("status_bs_synced")
    private String status_bs_synced;

    @SerializedName("errors")
    private GroupErrors errors;

    /**
     * входит в запрос saveTextAdGroups
     */
    @SerializedName("banners_arch_quantity")
    private Double bannersArchQuantity;

    /**3
     * входит в запрос saveTextAdGroups
     */
    @SerializedName("edit_banners_quantity")
    private Double editBannersQuantity;

    /**
     * Для динамической группы
     */
    @SerializedName("href_params")
    private String hrefParams;

    @SerializedName("main_domain")
    private String mainDomain;

    @SerializedName("domain")
    private String domain;

    @SerializedName("tags")
    private Object tags;

    @SerializedName("page")
    private Double page;

    @SerializedName("section")
    private String section;

    @SerializedName("shownBids")
    private List<Object> shownBids = new ArrayList<>();

    @SerializedName("has_feed_id")
    private String hasFeedId;

    @SerializedName("has_main_domain")
    private String hasMainDomain;

    /**
     * Для cpm группы
     */

    @SerializedName("cpm_banners_type")
    private String cpmBannersType;

    /**
     * Для перформанс группы
     */

    @SerializedName("feed_id")
    private String feedId;

    @SerializedName("performance_filters")
    private List<PerformanceFilter> performanceFilters;

    @SerializedName("used_creative_ids")
    private List<String> usedCreativeIds;

    @SerializedName("otherCreativeIds")
    private List<String> otherCreativeIds;

    /**
     * Для мобильной группы
     */

    @SerializedName("mobile_content")
    private MobileContent mobileContent;

    @SerializedName("store_content_href")
    private String storeContentHref;

    @SerializedName("device_type_targeting")
    private List<String> deviceTypeTargeting;

    @SerializedName("network_targeting")
    private List<String> networkTargeting;

    @SerializedName("min_os_version")
    private String minOsVersion;

    /**
     * входит в запрос saveTextAdGroups
     */
    @SerializedName("showNewBanners")
    private Boolean showNewBanners;

    @SerializedName("is_edited_by_moderator")
    private String isEditedByModerator;

    @SerializedName("day_budget")
    private String dayBudget;

    @SerializedName("showModEditNotice")
    private String showModEditNotice;

    @SerializedName("autobudget")
    private String autobudget;

    @SerializedName("status")
    private String status;

    @SerializedName("showsForecastSign")
    private String showsForecastSign;

    @SerializedName("statusShowsForecast")
    private String statusShowsForecast;

    @SerializedName("statusActive")
    private String statusActive;

    @SerializedName("statusAutobudgetShow")
    private String statusAutobudgetShow;

    @SerializedName("statusShow")
    private String statusShow;

    @SerializedName("showAgeLabels")
    private Boolean showAgeLabels;

    @SerializedName("pstatusPostModerate")
    private String pstatusPostModerate;

    @SerializedName("pstatusModerate")
    private String pstatusModerate;

    @SerializedName("statusModerate")
    private String statusModerate;

    @SerializedName("statusPostModerate")
    private String statusPostModerate;

    @SerializedName("status_moderate")
    private String status_moderate;

    @SerializedName("status_post_moderate")
    private String status_post_moderate;

    @SerializedName("hierarchical_multipliers")
    private HierarchicalMultipliers hierarchicalMultipliers;

    @SerializedName("target_interests")
    private List<TargetInterests> targetInterests;

    @SerializedName("relevance_match")
    private List<RelevanceMatch> relevanceMatch;

    @SerializedName("is_bs_rarely_loaded")
    private Integer isBsRarelyLoaded;

    @SerializedName("disabled_geo")
    private String disabledGeo;

    @SerializedName("effective_geo")
    private String effectiveGeo;

    @SerializedName("auto_price")
    private AutoPrice autoPrice;

    public Integer getIsBsRarelyLoaded() {
        return isBsRarelyLoaded;
    }

    public Group withIsBsRarelyLoaded(Integer isBsRarelyLoaded) {
        this.isBsRarelyLoaded = isBsRarelyLoaded;
        return this;
    }


    public List<TargetInterests> getTargetInterests() {
        return targetInterests;
    }

    public void setTargetInterests(List<TargetInterests> targetInterests) {
        this.targetInterests = targetInterests;
    }

    public Group withTargetInterests(List<TargetInterests> targetInterests) {
        this.targetInterests = targetInterests;
        return this;
    }

    public List<RelevanceMatch> getRelevanceMatch() {
        return relevanceMatch;
    }

    public void setRelevanceMatch(List<RelevanceMatch> relevanceMatch) {
        this.relevanceMatch = relevanceMatch;
    }

    public Group withRelevanceMatch(List<RelevanceMatch> relevanceMatch) {
        this.relevanceMatch = relevanceMatch;
        return this;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getAdGroupID() {
        return adGroupID;
    }

    public void setAdGroupID(String adGroupID) {
        this.adGroupID = adGroupID;
    }

    public String getAdGroupType() {
        return adGroupType;
    }

    public void setAdGroupType(String adGroupType) {
        this.adGroupType = adGroupType;
    }

    public String getAdGroupName() {
        return adGroupName;
    }

    public void setAdGroupName(String adGroupName) {
        this.adGroupName = adGroupName;
    }

    public String getCampaignID() {
        return campaignID;
    }

    public void setCampaignID(String campaignID) {
        this.campaignID = campaignID;
    }

    public List<String> getMinusWords() {
        return minusWords;
    }

    public void setMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
    }

    public List<Banner> getBanners() {
        return banners;
    }

    public void setBanners(List<Banner> banners) {
        this.banners = banners;
    }

    public List<Phrase> getPhrases() {
        return phrases;
    }

    public void setPhrases(List<Phrase> phrases) {
        this.phrases = phrases;
    }

    public List<Phrase> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Phrase> keywords) {
        this.keywords = keywords;
    }

    public List<DynamicCondition> getDynamicConditions() {
        return dynamicConditions;
    }

    public void setDynamicConditions(List<DynamicCondition> dynamicConditions) {
        this.dynamicConditions = dynamicConditions;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getStatusBsSynced() {
        return statusBsSynced;
    }

    public void setStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
    }

    public Group withStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
        return this;
    }

    public String getStatus_bs_synced() {
        return status_bs_synced;
    }

    public void setStatus_bs_synced(String status_bs_synced) {
        this.status_bs_synced = status_bs_synced;
    }

    public Group withStatus_bs_synced(String status_bs_synced) {
        this.status_bs_synced = status_bs_synced;
        return this;
    }

    public GroupErrors getErrors() {
        return errors;
    }

    public List<Retargeting> getRetargetings() {
        return retargetings;
    }

    public void setRetargetings(List<Retargeting> retargetings) {
        this.retargetings = retargetings;
    }

    public Long getBannersQuantity() {
        return bannersQuantity;
    }

    public void setBannersQuantity(Long bannersQuantity) {
        this.bannersQuantity = bannersQuantity;
    }

    public Double getBannersArchQuantity() {
        return bannersArchQuantity;
    }

    public void setBannersArchQuantity(Double bannersArchQuantity) {
        this.bannersArchQuantity = bannersArchQuantity;
    }

    public Double getEditBannersQuantity() {
        return editBannersQuantity;
    }

    public void setEditBannersQuantity(Double editBannersQuantity) {
        this.editBannersQuantity = editBannersQuantity;
    }

    public String getHrefParams() {
        return hrefParams;
    }

    public void setHrefParams(String hrefParams) {
        this.hrefParams = hrefParams;
    }

    public String getMainDomain() {
        return mainDomain;
    }

    public void setMainDomain(String mainDomain) {
        this.mainDomain = mainDomain;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Object getTags() {
        return tags;
    }

    public void setTags(Object tags) {
        this.tags = tags;
    }

    public Double getPage() {
        return page;
    }

    public void setPage(Double page) {
        this.page = page;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public List<Object> getShownBids() {
        return shownBids;
    }

    public void setShownBids(List<Object> shownBids) {
        this.shownBids = shownBids;
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

    public Boolean getShowNewBanners() {
        return showNewBanners;
    }

    public void setShowNewBanners(Boolean showNewBanners) {
        this.showNewBanners = showNewBanners;
    }

    public String getIsEditedByModerator() {
        return isEditedByModerator;
    }

    public void setIsEditedByModerator(String isEditedByModerator) {
        this.isEditedByModerator = isEditedByModerator;
    }

    public String getDayBudget() {
        return dayBudget;
    }

    public void setDayBudget(String dayBudget) {
        this.dayBudget = dayBudget;
    }

    public String getShowModEditNotice() {
        return showModEditNotice;
    }

    public void setShowModEditNotice(String showModEditNotice) {
        this.showModEditNotice = showModEditNotice;
    }

    public String getAutobudget() {
        return autobudget;
    }

    public void setAutobudget(String autobudget) {
        this.autobudget = autobudget;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShowsForecastSign() {
        return showsForecastSign;
    }

    public void setShowsForecastSign(String showsForecastSign) {
        this.showsForecastSign = showsForecastSign;
    }

    public String getStatusShowsForecast() {
        return statusShowsForecast;
    }

    public void setStatusShowsForecast(String statusShowsForecast) {
        this.statusShowsForecast = statusShowsForecast;
    }

    public String getStatusActive() {
        return statusActive;
    }

    public void setStatusActive(String statusActive) {
        this.statusActive = statusActive;
    }

    public String getStatusAutobudgetShow() {
        return statusAutobudgetShow;
    }

    public void setStatusAutobudgetShow(String statusAutobudgetShow) {
        this.statusAutobudgetShow = statusAutobudgetShow;
    }

    public String getStatusShow() {
        return statusShow;
    }

    public void setStatusShow(String statusShow) {
        this.statusShow = statusShow;
    }

    public Boolean getShowAgeLabels() {
        return showAgeLabels;
    }

    public void setShowAgeLabels(Boolean showAgeLabels) {
        this.showAgeLabels = showAgeLabels;
    }

    public String getPstatusPostModerate() {
        return pstatusPostModerate;
    }

    public void setPstatusPostModerate(String pstatusPostModerate) {
        this.pstatusPostModerate = pstatusPostModerate;
    }

    public String getPstatusModerate() {
        return pstatusModerate;
    }

    public void setPstatusModerate(String pstatusModerate) {
        this.pstatusModerate = pstatusModerate;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public void setStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
    }

    public String getStatusPostModerate() {
        return statusPostModerate;
    }

    public Group withStatusPostModerate(String statusPostModerate) {
        this.statusPostModerate = statusPostModerate;
        return this;
    }

    public String getStatus_moderate() {
        return status_moderate;
    }

    public Group withStatus_moderate(String status_moderate) {
        this.status_moderate = status_moderate;
        return this;
    }

    public String getStatus_post_moderate() {
        return status_post_moderate;
    }

    public Group withStatus_post_moderate(String status_post_moderate) {
        this.status_post_moderate = status_post_moderate;
        return this;
    }

    public void setStatus_moderate(String status_moderate) {
        this.status_moderate = status_moderate;
    }

    public void setStatus_post_moderate(String status_post_moderate) {
        this.status_post_moderate = status_post_moderate;
    }

    public void setStatusPostModerate(String statusPostModerate) {
        this.statusPostModerate = statusPostModerate;
    }

    public HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    public void setHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
    }

    public MobileContent getMobileContent() {
        return mobileContent;
    }

    public void setMobileContent(MobileContent mobileContent) {
        this.mobileContent = mobileContent;
    }

    public String getStoreContentHref() {
        return storeContentHref;
    }

    public void setStoreContentHref(String storeContentHref) {
        this.storeContentHref = storeContentHref;
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

    public String getMinOsVersion() {
        return minOsVersion;
    }

    public void setMinOsVersion(String minOsVersion) {
        this.minOsVersion = minOsVersion;
    }

    public Group withModelId(String modelId) {
        this.modelId = modelId;
        return this;
    }

    public Group withAdGroupID(String adGroupID) {
        this.adGroupID = adGroupID;
        return this;
    }

    public Group withAdGroupType(String adGroupType) {
        this.adGroupType = adGroupType;
        return this;
    }

    public Group withAdGroupName(String adGroupName) {
        this.adGroupName = adGroupName;
        return this;
    }

    public Group withCampaignID(String campaignID) {
        this.campaignID = campaignID;
        return this;
    }

    public Group withMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
        return this;
    }

    public Group withBanners(List<Banner> banners) {
        this.banners = banners;
        return this;
    }

    public Group withPhrases(List<Phrase> phrases) {
        this.phrases = phrases;
        return this;
    }


    public Group withKeywords(List<Phrase> keywords) {
        this.keywords = keywords;
        return this;
    }

    public Group withDynamicConditions(List<DynamicCondition> dynamicConditions) {
        this.dynamicConditions = dynamicConditions;
        return this;
    }

    public Group withGeo(String geo) {
        this.geo = geo;
        return this;
    }

    public Group withRetargetings(List<Retargeting> retargetings) {
        this.retargetings = retargetings;
        return this;
    }

    public Group withBannersQuantity(Long bannersQuantity) {
        this.bannersQuantity = bannersQuantity;
        return this;
    }

    public Group withBannersArchQuantity(Double bannersArchQuantity) {
        this.bannersArchQuantity = bannersArchQuantity;
        return this;
    }

    public Group withEditBannersQuantity(Double editBannersQuantity) {
        this.editBannersQuantity = editBannersQuantity;
        return this;
    }

    public Group withHrefParams(String hrefParams) {
        this.hrefParams = hrefParams;
        return this;
    }

    public Group withMainDomain(String mainDomain) {
        this.mainDomain = mainDomain;
        return this;
    }

    public Group withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public Group withTags(Object tags) {
        this.tags = tags;
        return this;
    }

    public Group withPage(Double page) {
        this.page = page;
        return this;
    }

    public Group withSection(String section) {
        this.section = section;
        return this;
    }

    public Group withShownBids(List<Object> shownBids) {
        this.shownBids = shownBids;
        return this;
    }

    public Group withFeedId(String feedId) {
        this.feedId = feedId;
        return this;
    }

    public Group withPerformanceFilters(List<PerformanceFilter> performanceFilters) {
        this.performanceFilters = performanceFilters;
        return this;
    }

    public Group withUsedCreativeIds(List<String> usedCreativeIds) {
        this.usedCreativeIds = usedCreativeIds;
        return this;
    }

    public Group withOtherCreativeIds(List<String> otherCreativeIds) {
        this.otherCreativeIds = otherCreativeIds;
        return this;
    }

    public Group withMobileContent(MobileContent mobileContent) {
        this.mobileContent = mobileContent;
        return this;
    }

    public Group withStoreContentHref(String storeContentHref) {
        this.storeContentHref = storeContentHref;
        return this;
    }

    public Group withDeviceTypeTargeting(List<String> deviceTypeTargeting) {
        this.deviceTypeTargeting = deviceTypeTargeting;
        return this;
    }

    public Group withNetworkTargeting(List<String> networkTargeting) {
        this.networkTargeting = networkTargeting;
        return this;
    }

    public Group withMinOsVersion(String minOsVersion) {
        this.minOsVersion = minOsVersion;
        return this;
    }

    public Group withShowNewBanners(Boolean showNewBanners) {
        this.showNewBanners = showNewBanners;
        return this;
    }

    public Group withIsEditedByModerator(String isEditedByModerator) {
        this.isEditedByModerator = isEditedByModerator;
        return this;
    }

    public Group withDayBudget(String dayBudget) {
        this.dayBudget = dayBudget;
        return this;
    }

    public Group withShowModEditNotice(String showModEditNotice) {
        this.showModEditNotice = showModEditNotice;
        return this;
    }

    public Group withAutobudget(String autobudget) {
        this.autobudget = autobudget;
        return this;
    }

    public Group withStatus(String status) {
        this.status = status;
        return this;
    }

    public Group withShowsForecastSign(String showsForecastSign) {
        this.showsForecastSign = showsForecastSign;
        return this;
    }

    public Group withStatusShowsForecast(String statusShowsForecast) {
        this.statusShowsForecast = statusShowsForecast;
        return this;
    }

    public Group withStatusActive(String statusActive) {
        this.statusActive = statusActive;
        return this;
    }

    public Group withStatusAutobudgetShow(String statusAutobudgetShow) {
        this.statusAutobudgetShow = statusAutobudgetShow;
        return this;
    }

    public Group withStatusShow(String statusShow) {
        this.statusShow = statusShow;
        return this;
    }

    public Group withShowAgeLabels(Boolean showAgeLabels) {
        this.showAgeLabels = showAgeLabels;
        return this;
    }

    public Group withPstatusPostModerate(String pstatusPostModerate) {
        this.pstatusPostModerate = pstatusPostModerate;
        return this;
    }

    public Group withPstatusModerate(String pstatusModerate) {
        this.pstatusModerate = pstatusModerate;
        return this;
    }

    public Group withStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public Group withHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
        return this;
    }

    public String getHasFeedId() {
        return hasFeedId;
    }

    public void setHasFeedId(String hasFeedId) {
        this.hasFeedId = hasFeedId;
    }

    public Group withHasFeedId(String hasFeedId) {
        this.hasFeedId = hasFeedId;
        return this;
    }

    public String getHasMainDomain() {
        return hasMainDomain;
    }

    public Group withHasMainDomain(String hasMainDomain) {
        this.hasMainDomain = hasMainDomain;
        return this;
    }

    public void setHasMainDomain(String hasMainDomain) {
        this.hasMainDomain = hasMainDomain;
    }

    public String getCpmBannersType() {
        return cpmBannersType;
    }

    public Group withCpmBannersType(String cpmBannersType) {
        this.cpmBannersType = cpmBannersType;
        return this;
    }

    public void setCpmBannersType(String cpmBannersType) {
        this.cpmBannersType = cpmBannersType;
    }

    public String getDisabledGeo() {
        return disabledGeo;
    }

    public void setDisabledGeo(String disabledGeo) {
        this.disabledGeo = disabledGeo;
    }

    public Group withDisabledGeo(String disabledGeo) {
        this.disabledGeo = disabledGeo;
        return this;
    }

    public String getEffectiveGeo() {
        return effectiveGeo;
    }

    public void setEffectiveGeo(String effectiveGeo) {
        this.effectiveGeo = effectiveGeo;
    }

    public Group withEffectiveGeo(String effectiveGeo) {
        this.effectiveGeo = effectiveGeo;
        return this;
    }

    public AutoPrice getAutoPrice() {
        return autoPrice;
    }

    public void setAutoPrice(AutoPrice autoPrice) {
        this.autoPrice = autoPrice;
    }
}
