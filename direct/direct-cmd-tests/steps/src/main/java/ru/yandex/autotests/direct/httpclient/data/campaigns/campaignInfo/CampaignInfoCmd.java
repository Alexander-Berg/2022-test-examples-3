package ru.yandex.autotests.direct.httpclient.data.campaigns.campaignInfo;

import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.ContactInfoCmdBean;
import ru.yandex.autotests.direct.httpclient.data.adjustment.rates.HierarchicalMultipliers;
import ru.yandex.autotests.direct.httpclient.data.campaigns.CampaignGroupBean;
import ru.yandex.autotests.direct.httpclient.data.strategy.DayBudget;
import ru.yandex.autotests.direct.httpclient.data.timetargeting.TimeTargetInfoCmd;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 01.04.15
 */
public class CampaignInfoCmd {

    @JsonPath(responsePath = "cid")
    private String campaignID;

    @JsonPath(responsePath = "name")
    private String name;

    @JsonPath(responsePath = "fio")
    private String FIO;

    @JsonPath(responsePath = "start_date")
    private String startDate;

    @JsonPath(responsePath = "finish_date")
    private String finishDate;

    @JsonPath(responsePath = "email")
    private String email;

    @JsonPath(responsePath = "strategy")
    private CampaignStrategy strategy;

    @JsonPath(responsePath = "sum")
    private Float sum;

    @JsonPath(responsePath = "shows")
    private Integer shows;

    @JsonPath(responsePath = "clicks")
    private String clicks;

    @JsonPath(responsePath = "currency")
    private String campaignCurrency;

    @JsonPath(responsePath = "metrika_counters")
    private String additionalMetrikaCounters;

    @JsonPath(responsePath = "mobile_multiplier_pct")
    private Integer mobileBidAdjustment;

    @JsonPath(responsePath = "hierarchical_multipliers")
    private HierarchicalMultipliers hierarchicalMultipliers;

    @JsonPath(responsePath = "groups")
    private CampaignGroupBean group;

    @JsonPath(responsePath = "sms_time")
    private String smsTime;

    @JsonPath(responsePath = "timetargeting")
    private TimeTargetInfoCmd timeTarget;

    @JsonPath(responsePath = "statusContextStop")
    private String statusContextStop;

    @JsonPath(responsePath = "ContextLimit")
    private Integer contextLimitSum;

    @JsonPath(responsePath = "ContextPriceCoef")
    private Integer contextPricePercent;

    @JsonPath(responsePath = "autoOptimization")
    private String autoOptimization;

    @JsonPath(responsePath = "statusMetricaControl")
    private String statusMetricaControl;

    @JsonPath(responsePath = "DontShow")
    private String disabledDomains;

    @JsonPath(responsePath = "disabledIps")
    private String disabledIps;

    @JsonPath(responsePath = "statusOpenStat")
    private String statusOpenStat;

    @JsonPath(responsePath = "statusShow")
    private String statusShow;

    @JsonPath(responsePath = "archived")
    private String statusArchive;

    @JsonPath(responsePath = "statusModerate")
    private String statusModerate;

    @JsonPath(responsePath = "statusActive")
    private String isActive;

    @JsonPath(responsePath = "minus_words")
    private List<String> minusKeywords;

    @JsonPath(responsePath = "day_budget")
    private DayBudget dayBudget;

    @JsonPath(responsePath = "geo")
    private String geo;

    @JsonPath(responsePath = "vcard")
    private ContactInfoCmdBean contactInfo;

    @JsonPath(responsePath = "broad_match_flag")
    private String broadMatchFlag;

    @JsonPath(responsePath = "broad_match_limit")
    private String broadMatchLimit;

    @JsonPath(responsePath = "broad_match_rate")
    private String broadMatchRate;

    @JsonPath(responsePath = "mediaType")
    private String mediaType;

    @JsonPath(responsePath = "is_related_keywords_enabled")
    private String isRelatedKeywordsEnabled;

    @JsonPath(responsePath = "sms_flags/active_orders_money_out_sms")
    private String activeOrdersMoneyOutSms;

    @JsonPath(responsePath = "sms_flags/active_orders_money_warning_sms")
    private String activeOrdersMoneyWarningSms;

    @JsonPath(responsePath = "sms_flags/moderate_result_sms")
    private String moderateResultSms;

    @JsonPath(responsePath = "sms_flags/notify_order_money_in_sms")
    private String notifyOrderMoneyInSms;

    @JsonPath(responsePath = "banners_per_page")
    private String bannersPerPage;

    @JsonPath(responsePath = "device_targeting")
    private DeviceTargeting deviceTargeting;

    @JsonPath(responsePath = "competitors_domains")
    private String competitorsDomains;

    @JsonPath(responsePath = "allow_edit_camp")
    private String allowEditCamp;

    @JsonPath(responsePath = "disableBehavior")
    private String disableBehavior;

    @JsonPath(responsePath = "device_type_targeting")
    private String deviceTypeTargeting;

    @JsonPath(responsePath = "network_targeting")
    private String networkTargeting;

    @JsonPath(responsePath = "no_extended_geotargeting")
    private Integer noExtendedGeotargeting;

    public String getCampaignID() {
        return campaignID;
    }

    public void setCampaignID(String campaignID) {
        this.campaignID = campaignID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFIO() {
        return FIO;
    }

    public void setFIO(String FIO) {
        this.FIO = FIO;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(String finishDate) {
        this.finishDate = finishDate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CampaignStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(CampaignStrategy strategy) {
        this.strategy = strategy;
    }

    public Float getSum() {
        return sum;
    }

    public void setSum(Float sum) {
        this.sum = sum;
    }

    public Integer getShows() {
        return shows;
    }

    public void setShows(Integer shows) {
        this.shows = shows;
    }

    public String getClicks() {
        return clicks;
    }

    public void setClicks(String clicks) {
        this.clicks = clicks;
    }

    public String getCampaignCurrency() {
        return campaignCurrency;
    }

    public void setCampaignCurrency(String campaignCurrency) {
        this.campaignCurrency = campaignCurrency;
    }

    public String getAdditionalMetrikaCounters() {
        return additionalMetrikaCounters;
    }

    public void setAdditionalMetrikaCounters(String additionalMetrikaCounters) {
        this.additionalMetrikaCounters = additionalMetrikaCounters;
    }

    public Integer getMobileBidAdjustment() {
        return mobileBidAdjustment;
    }

    public void setMobileBidAdjustment(Integer mobileBidAdjustment) {
        this.mobileBidAdjustment = mobileBidAdjustment;
    }

    public HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    public void setHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
    }

    public CampaignGroupBean getGroup() {
        return group;
    }

    public void setGroup(CampaignGroupBean group) {
        this.group = group;
    }

    public String getSmsTime() {
        return smsTime;
    }

    public void setSmsTime(String smsTime) {
        this.smsTime = smsTime;
    }

    public TimeTargetInfoCmd getTimeTarget() {
        return timeTarget;
    }

    public void setTimeTarget(TimeTargetInfoCmd timeTarget) {
        this.timeTarget = timeTarget;
    }

    public String getStatusContextStop() {
        return statusContextStop;
    }

    public void setStatusContextStop(String statusContextStop) {
        this.statusContextStop = statusContextStop;
    }

    public Integer getContextLimitSum() {
        return contextLimitSum;
    }

    public void setContextLimitSum(Integer contextLimitSum) {
        this.contextLimitSum = contextLimitSum;
    }

    public Integer getContextPricePercent() {
        return contextPricePercent;
    }

    public void setContextPricePercent(Integer contextPricePercent) {
        this.contextPricePercent = contextPricePercent;
    }

    public String getAutoOptimization() {
        return autoOptimization;
    }

    public void setAutoOptimization(String autoOptimization) {
        this.autoOptimization = autoOptimization;
    }

    public String getStatusMetricaControl() {
        return statusMetricaControl;
    }

    public void setStatusMetricaControl(String statusMetricaControl) {
        this.statusMetricaControl = statusMetricaControl;
    }

    public String getDisabledDomains() {
        return disabledDomains;
    }

    public void setDisabledDomains(String disabledDomains) {
        this.disabledDomains = disabledDomains;
    }

    public String getDisabledIps() {
        return disabledIps;
    }

    public void setDisabledIps(String disabledIps) {
        this.disabledIps = disabledIps;
    }

    public String getStatusOpenStat() {
        return statusOpenStat;
    }

    public void setStatusOpenStat(String statusOpenStat) {
        this.statusOpenStat = statusOpenStat;
    }

    public String getStatusShow() {
        return statusShow;
    }

    public void setStatusShow(String statusShow) {
        this.statusShow = statusShow;
    }

    public String getStatusArchive() {
        return statusArchive;
    }

    public void setStatusArchive(String statusArchive) {
        this.statusArchive = statusArchive;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public void setStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public List<String> getMinusKeywords() {
        return minusKeywords;
    }

    public void setMinusKeywords(List<String> minusKeywords) {
        this.minusKeywords = minusKeywords;
    }

    public DayBudget getDayBudget() {
        return dayBudget;
    }

    public void setDayBudget(DayBudget dayBudget) {
        this.dayBudget = dayBudget;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public ContactInfoCmdBean getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(ContactInfoCmdBean contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getBroadMatchFlag() {
        return broadMatchFlag;
    }

    public void setBroadMatchFlag(String broadMatchFlag) {
        this.broadMatchFlag = broadMatchFlag;
    }

    public String getBroadMatchLimit() {
        return broadMatchLimit;
    }

    public void setBroadMatchLimit(String broadMatchLimit) {
        this.broadMatchLimit = broadMatchLimit;
    }

    public String getBroadMatchRate() {
        return broadMatchRate;
    }

    public void setBroadMatchRate(String broadMatchRate) {
        this.broadMatchRate = broadMatchRate;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getIsRelatedKeywordsEnabled() {
        return isRelatedKeywordsEnabled;
    }

    public void setIsRelatedKeywordsEnabled(String isRelatedKeywordsEnabled) {
        this.isRelatedKeywordsEnabled = isRelatedKeywordsEnabled;
    }

    public String getActiveOrdersMoneyOutSms() {
        return activeOrdersMoneyOutSms;
    }

    public void setActiveOrdersMoneyOutSms(String activeOrdersMoneyOutSms) {
        this.activeOrdersMoneyOutSms = activeOrdersMoneyOutSms;
    }

    public String getActiveOrdersMoneyWarningSms() {
        return activeOrdersMoneyWarningSms;
    }

    public void setActiveOrdersMoneyWarningSms(String activeOrdersMoneyWarningSms) {
        this.activeOrdersMoneyWarningSms = activeOrdersMoneyWarningSms;
    }

    public String getModerateResultSms() {
        return moderateResultSms;
    }

    public void setModerateResultSms(String moderateResultSms) {
        this.moderateResultSms = moderateResultSms;
    }

    public String getNotifyOrderMoneyInSms() {
        return notifyOrderMoneyInSms;
    }

    public void setNotifyOrderMoneyInSms(String notifyOrderMoneyInSms) {
        this.notifyOrderMoneyInSms = notifyOrderMoneyInSms;
    }

    public String getBannersPerPage() {
        return bannersPerPage;
    }

    public void setBannersPerPage(String bannersPerPage) {
        this.bannersPerPage = bannersPerPage;
    }

    public DeviceTargeting getDeviceTargeting() {
        return deviceTargeting;
    }

    public void setDeviceTargeting(DeviceTargeting deviceTargeting) {
        this.deviceTargeting = deviceTargeting;
    }

    public String getCompetitorsDomains() {
        return competitorsDomains;
    }

    public void setCompetitorsDomains(String competitorsDomains) {
        this.competitorsDomains = competitorsDomains;
    }

    public String getAllowEditCamp() {
        return allowEditCamp;
    }

    public void setAllowEditCamp(String allowEditCamp) {
        this.allowEditCamp = allowEditCamp;
    }

    public String getDisableBehavior() {
        return disableBehavior;
    }

    public void setDisableBehavior(String disableBehavior) {
        this.disableBehavior = disableBehavior;
    }

    public String getDeviceTypeTargeting() {
        return deviceTypeTargeting;
    }

    public void setDeviceTypeTargeting(String deviceTypeTargeting) {
        this.deviceTypeTargeting = deviceTypeTargeting;
    }

    public String getNetworkTargeting() {
        return networkTargeting;
    }

    public void setNetworkTargeting(String networkTargeting) {
        this.networkTargeting = networkTargeting;
    }

    public Integer getNoExtendedGeotargeting() {
        return noExtendedGeotargeting;
    }

    public void setNoExtendedGeotargeting(Integer noExtendedGeotargeting) {
        this.noExtendedGeotargeting = noExtendedGeotargeting;
    }
}
