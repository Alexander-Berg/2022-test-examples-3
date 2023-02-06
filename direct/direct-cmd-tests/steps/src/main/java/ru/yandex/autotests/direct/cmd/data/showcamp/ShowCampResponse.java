package ru.yandex.autotests.direct.cmd.data.showcamp;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.campaigns.OptimizedCamp;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.Wallet;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.interest.InterestCategory;

public class ShowCampResponse {

    @SerializedName("can_export_in_excel")
    private String canExportInExcel;

    @SerializedName("banners")
    private List<Banner> groups;

    @SerializedName("cid")
    private String campaignID;

    @SerializedName("name")
    private String name;

    @SerializedName("FIO")
    private String FIO;

    @SerializedName("start_date")
    private String startDate;

    @SerializedName("finish_date")
    private String finishDate;

    @SerializedName("email")
    private String email;

    @SerializedName("strategy")
    private CampaignStrategy strategy;

    @SerializedName("sum")
    private Float sum;

    @SerializedName("clicks")
    private Integer clicks;

    @SerializedName("currency")
    private String campaignCurrency;

    @SerializedName("metrika_counters")
    private String additionalMetrikaCounters;

    @SerializedName("sms_time")
    private String smsTime;

    @SerializedName("time_target_holiday_from")
    private Integer holidayShowFrom;
    @SerializedName("time_target_holiday_to")
    private Integer holidayShowTo;
    @SerializedName("timezone_id")
    private String timeZone;

    @SerializedName("statusContextStop")
    private String statusContextStop;

    @SerializedName("ContextLimit")
    private Integer contextLimitSum;

    @SerializedName("ContextPriceCoef")
    private Integer contextPricePercent;

    @SerializedName("autoOptimization")
    private String autoOptimization;

    @SerializedName("statusMetricaControl")
    private String statusMetricaControl;

    @SerializedName("DontShow")
    private String disabledDomains;

    @SerializedName("disabledIps")
    private String disabledIps;

    @SerializedName("statusOpenStat")
    private String statusOpenStat;

    @SerializedName("statusShow")
    private String statusShow;

    @SerializedName("archived")
    private String statusArchive;

    @SerializedName("statusModerate")
    private String statusModerate;

    @SerializedName("statusActive")
    private String isActive;

    @SerializedName("minus_words")
    private List<String> minusKeywords;

//    @SerializedName("day_budget")
//    private DayBudget dayBudget;

    @SerializedName("geo")
    private String geo;

    @SerializedName("vcard")
    private ContactInfo contactInfo;

    @SerializedName("broad_match_flag")
    private String broadMatchFlag;

    @SerializedName("broad_match_limit")
    private String broadMatchLimit;

    @SerializedName("broad_match_rate")
    private String broadMatchRate;

    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("is_related_keywords_enabled")
    private String isRelatedKeywordsEnabled;

    @SerializedName("sms_flags/active_orders_money_out_sms")
    private String activeOrdersMoneyOutSms;

    @SerializedName("sms_flags/active_orders_money_warning_sms")
    private String activeOrdersMoneyWarningSms;

    @SerializedName("sms_flags/moderate_result_sms")
    private String moderateResultSms;

    @SerializedName("sms_flags/notify_order_money_in_sms")
    private String notifyOrderMoneyInSms;

    @SerializedName("banners_per_page")
    private String bannersPerPage;

//    @SerializedName("device_targeting")
//    private DeviceTargeting deviceTargeting;

    @SerializedName("competitors_domains")
    private String competitorsDomains;

    @SerializedName("easy_direct")
    private String easyDirect;

    @SerializedName("broad_match_goal_id")
    private String broadMatchGoalId;

    @SerializedName("optimize_camp")
    private OptimizedCamp optimizeCamp;

    @SerializedName("statusBsSynced")
    private String statusBsSynced;

    @SerializedName("show_adgroups")
    private Map<Long, Integer> showGroups;

    @SerializedName("wallet")
    private Wallet wallet;

    @SerializedName("metrika_has_ecommerce")
    private String metrikaHasEcommerce;

    @SerializedName("experiments")
    private String experiments;

    @SerializedName("interest_categories")
    private List<InterestCategory> interestCategories;

    @SerializedName("camp_has_disabled_geo")
    private String campHasDisabledGeo;

    public String getExperiments() {
        return experiments;
    }

    public ShowCampResponse withExperiments(String experiments) {
        this.experiments = experiments;
        return this;
    }

    public Map<Long, Integer> getShowGroups() {
        return showGroups;
    }

    public void setShowGroups(Map<Long, Integer> showGroups) {
        this.showGroups = showGroups;
    }

    public String getStatusBsSynced() {
        return statusBsSynced;
    }

    public void setStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
    }

    public OptimizedCamp getOptimizeCamp() {
        return optimizeCamp;
    }

    public void setOptimizeCamp(OptimizedCamp optimizeCamp) {
        this.optimizeCamp = optimizeCamp;
    }

    public String getBroadMatchGoalId() {
        return broadMatchGoalId;
    }

    public void setBroadMatchGoalId(String broadMatchGoalId) {
        this.broadMatchGoalId = broadMatchGoalId;
    }


    public String getCanExportInExcel() {
        return canExportInExcel;
    }

    public void setCanExportInExcel(String canExportInExcel) {
        this.canExportInExcel = canExportInExcel;
    }

    public List<Banner> getGroups() {
        return groups;
    }

    public void setGroups(List<Banner> groups) {
        this.groups = groups;
    }

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

    public Integer getClicks() {
        return clicks;
    }

    public void setClicks(Integer clicks) {
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

    public String getSmsTime() {
        return smsTime;
    }

    public void setSmsTime(String smsTime) {
        this.smsTime = smsTime;
    }

    public Integer getHolidayShowFrom() {
        return holidayShowFrom;
    }

    public void setHolidayShowFrom(Integer holidayShowFrom) {
        this.holidayShowFrom = holidayShowFrom;
    }

    public Integer getHolidayShowTo() {
        return holidayShowTo;
    }

    public void setHolidayShowTo(Integer holidayShowTo) {
        this.holidayShowTo = holidayShowTo;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
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

//    public DayBudget getDayBudget() {
//        return dayBudget;
//    }

//    public void setDayBudget(DayBudget dayBudget) {
//        this.dayBudget = dayBudget;
//    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(ContactInfo contactInfo) {
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

//    public DeviceTargeting getDeviceTargeting() {
//        return deviceTargeting;
//    }

//    public void setDeviceTargeting(DeviceTargeting deviceTargeting) {
//        this.deviceTargeting = deviceTargeting;
//    }

    public String getCompetitorsDomains() {
        return competitorsDomains;
    }

    public void setCompetitorsDomains(String competitorsDomains) {
        this.competitorsDomains = competitorsDomains;
    }

    public String getEasyDirect() {
        return easyDirect;
    }

    public void setEasyDirect(String easyDirect) {
        this.easyDirect = easyDirect;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public ShowCampResponse withWallet(Wallet wallet) {
        this.wallet = wallet;
        return this;
    }

    public String getMetrikaHasEcommerce() {
        return metrikaHasEcommerce;
    }

    public void setMetrikaHasEcommerce(String metrikaHasEcommerce) {
        this.metrikaHasEcommerce = metrikaHasEcommerce;
    }

    public String toString() {
        return new Gson().toJson(this);
    }

    public List<InterestCategory> getInterestCategories() {
        return interestCategories;
    }

    public ShowCampResponse withInterestCategories(List<InterestCategory> interestCategories) {
        this.interestCategories = interestCategories;
        return this;
    }

    public String getCampHasDisabledGeo() {
        return campHasDisabledGeo;
    }

    public ShowCampResponse withCampHasDisabledGeo(String campHasDisabledGeo) {
        this.campHasDisabledGeo = campHasDisabledGeo;
        return this;
    }
}
