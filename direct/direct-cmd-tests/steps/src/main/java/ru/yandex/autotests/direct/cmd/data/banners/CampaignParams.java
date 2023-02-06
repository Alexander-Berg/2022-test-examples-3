package ru.yandex.autotests.direct.cmd.data.banners;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.ProductInfo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;

import java.util.ArrayList;
import java.util.List;

public class CampaignParams {
    @SerializedName("groups")
    private List<Group> groups;

    @SerializedName("budget_strategy")
    private String budgetStrategy;

    @SerializedName("statusShow")
    private String statusShow;

    @SerializedName("sum_to_pay")
    private String sumToPay;

    @SerializedName("autobudget")
    private String autobudget;

    @SerializedName("autobudget_date")
    private String autobudgetDate;

    @SerializedName("sendAccNews")
    private String sendAccNews;

    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("enable_cpc_hold")
    private String enableCpcHold;

    @SerializedName("day_budget_daily_change_count")
    private String dayBudgetDailyChangeCount;

    @SerializedName("warnPlaceInterval")
    private String warnPlaceInterval;

    @SerializedName("LastChange")
    private String LastChange;

    @SerializedName("MAX_BANNER_LIMIT")
    private Double MAXBANNERLIMIT;

    @SerializedName("is_search_stop")
    private String isSearchStop;

    @SerializedName("warnplaces_str")
    private String warnplacesStr;

    @SerializedName("valid")
    private String valid;

    @SerializedName("ClientID")
    private String ClientID;

    @SerializedName("day_budget_stop_time")
    private String dayBudgetStopTime;

    @SerializedName("broad_match_limit")
    private String broadMatchLimit;

    @SerializedName("total_units")
    private String totalUnits;

    @SerializedName("ContextLimit")
    private String ContextLimit;

    @SerializedName("original_sum_spent")
    private String originalSumSpent;

    @SerializedName("statusOpenStat")
    private String statusOpenStat;

    @SerializedName("mediaplan_status")
    private String mediaplanStatus;

    @SerializedName("statusMail")
    private String statusMail;

    @SerializedName("ProductID")
    private String ProductID;

    @SerializedName("money_warning_value")
    private String moneyWarningValue;

    @SerializedName("broad_match_flag")
    private String broadMatchFlag;

    @SerializedName("uid")
    private String uid;

    @SerializedName("manager_uid")
    private Double managerUid;

    @SerializedName("optimal_groups_on_page")
    private Double optimalGroupsOnPage;

    @SerializedName("lastnews")
    private String lastnews;

    @SerializedName("sendNews")
    private String sendNews;

    @SerializedName("statusBsSynced")
    private String statusBsSynced;

    @SerializedName("offlineStatNotice")
    private String offlineStatNotice;

    @SerializedName("tags")
    private List<Object> tags = new ArrayList<>();

    @SerializedName("email")
    private String email;

    @SerializedName("dd")
    private String dd;

    @SerializedName("autoOptimization")
    private String autoOptimization;

    @SerializedName("broad_match_rate")
    private String broadMatchRate;

    @SerializedName("timetarget_coef")
    private Double timetargetCoef;

    @SerializedName("lastShowTime")
    private String lastShowTime;

    @SerializedName("FIO")
    private String FIO;

    @SerializedName("fio")
    private String fio;

    @SerializedName("cid")
    private String cid;

    @SerializedName("statusPostModerate")
    private String statusPostModerate;

    @SerializedName("common_geo_set")
    private Double commonGeoSet;

    @SerializedName("yyyy")
    private String yyyy;

    @SerializedName("copiedFrom")
    private String copiedFrom;

    @SerializedName("MIN_PHRASE_RANK_WARNING")
    private Double MINPHRASERANKWARNING;

    @SerializedName("stopTime")
    private String stopTime;

    @SerializedName("common_geo")
    private String commonGeo;

    @SerializedName("currency")
    private String currency;

    @SerializedName("auto_optimize_request")
    private String autoOptimizeRequest;

    @SerializedName("no_title_substitute")
    private Double noTitleSubstitute;

    @SerializedName("banners_count")
    private String bannersCount;

    @SerializedName("statusAutobudgetForecast")
    private String statusAutobudgetForecast;

    @SerializedName("statusModerate")
    private String statusModerate;

    @SerializedName("sendWarn")
    private String sendWarn;

    @SerializedName("has_groups")
    private Double hasGroups;

    @SerializedName("statusYacobotDeleted")
    private String statusYacobotDeleted;

    @SerializedName("name")
    private String name;

    @SerializedName("sms_time")
    private String smsTime;

    @SerializedName("total")
    private Double total;

    @SerializedName("geo")
    private String geo;

    @SerializedName("fairAuction")
    private String fairAuction;

    @SerializedName("start_date")
    private String startDate;

    @SerializedName("product_type")
    private String productType;

    @SerializedName("statusEmpty")
    private String statusEmpty;

    @SerializedName("untagged_banners_num")
    private String untaggedBannersNum;

    @SerializedName("statusActive")
    private String statusActive;

    @SerializedName("compaign_domains_count")
    private String compaignDomainsCount;

    @SerializedName("original_sum")
    private String originalSum;

    @SerializedName("disableBehavior")
    private String disableBehavior;

    @SerializedName("statusBehavior")
    private String statusBehavior;

    @SerializedName("dontShowYacontext")
    private String dontShowYacontext;

    @SerializedName("money_type")
    private String moneyType;

    @SerializedName("common_vcard_set")
    private Double commonVcardSet;

    @SerializedName("all_banners_num")
    private Double allBannersNum;

    @SerializedName("dontShowCatalog")
    private String dontShowCatalog;

    @SerializedName("sum_units")
    private String sumUnits;

    @SerializedName("statusBsArchived")
    private String statusBsArchived;

    @SerializedName("day_budget_show_mode")
    private String dayBudgetShowMode;

    @SerializedName("metrika_counters")
    private String metrikaCounters;

    @SerializedName("product_info")
    private ProductInfo productInfo;

    @SerializedName("banners_per_page")
    private String bannersPerPage;

    @SerializedName("is_related_keywords_enabled")
    private String isRelatedKeywordsEnabled;

    @SerializedName("statusMetricaControl")
    private String statusMetricaControl;

    @SerializedName("contactinfo")
    private String contactinfo;

    @SerializedName("vcard")
    private ContactInfo vcard;

    @SerializedName("OrderID")
    private String OrderID;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("rf")
    private String rf;

    @SerializedName("statusNoPay")
    private String statusNoPay;

    @SerializedName("competitors_domains")
    private String competitorsDomains;

    @SerializedName("timezone_id")
    private String timezoneId;

    @SerializedName("strategy_min_price")
    private String strategyMinPrice;

    @SerializedName("sum_spent_units")
    private String sumSpentUnits;

    @SerializedName("warnplaces")
    private Double warnplaces;

    @SerializedName("agency_uid")
    private Double agencyUid;

    @SerializedName("sum_spent")
    private String sumSpent;

    @SerializedName("balance_tid")
    private String balanceTid;

    @SerializedName("strategy")
    private String strategy;

    @SerializedName("AgencyID")
    private String AgencyID;

    @SerializedName("rfReset")
    private String rfReset;

    @SerializedName("timeTarget")
    private String timeTarget;

    @SerializedName("minus_words")
    private List<String> minusWords;

    @SerializedName("images")
    private List<Object> images = new ArrayList<>();

    @SerializedName("archived")
    private String archived;

    @SerializedName("sum_last")
    private String sumLast;

    @SerializedName("hierarchical_multipliers")
    private HierarchicalMultipliers hierarchicalMultipliers;

    @SerializedName("currencyConverted")
    private String currencyConverted;

    @SerializedName("is_favorite")
    private String isFavorite;

    @SerializedName("paid_by_certificate")
    private String paidByCertificate;

    @SerializedName("is_camp_locked")
    private Double isCampLocked;

    @SerializedName("mm")
    private String mm;

    @SerializedName("sum")
    private String sum;

    @SerializedName("finish_time")
    private String finishTime;

    @SerializedName("tabclass_search_count")
    private Double tabclassSearchCount;

    @SerializedName("status_click_track")
    private String statusClickTrack;

    @SerializedName("MAX_PHRASE_RANK_WARNING")
    private Double MAXPHRASERANKWARNING;

    @SerializedName("statusContextStop")
    private String statusContextStop;

    @SerializedName("campaign_minus_words")
    private List<String> campaignMinusWords;

    @SerializedName("type")
    private String type;

    @SerializedName("ContextPriceCoef")
    private String ContextPriceCoef;

    @SerializedName("groups_on_page")
    private Double groupsOnPage;

    @SerializedName("pages_num")
    private Double pagesNum;

    @SerializedName("finish_date")
    private String finishDate;

    @SerializedName("content_lang")
    private String contentLang;

    @SerializedName("broad_match_goal_id")
    private String broadMatchGoalId;

    @SerializedName("no_extended_geotargeting")
    private Integer noExtendedGeotargeting;

    public List<Group> getGroups() {
        return groups;
    }

    public CampaignParams withGroups(List<Group> groups) {
        this.groups = groups;
        return this;
    }

    public String getBudgetStrategy() {
        return budgetStrategy;
    }

    public CampaignParams withBudgetStrategy(String budgetStrategy) {
        this.budgetStrategy = budgetStrategy;
        return this;
    }

    public String getStatusShow() {
        return statusShow;
    }

    public CampaignParams withStatusShow(String statusShow) {
        this.statusShow = statusShow;
        return this;
    }

    public String getSumToPay() {
        return sumToPay;
    }

    public CampaignParams withSumToPay(String sumToPay) {
        this.sumToPay = sumToPay;
        return this;
    }

    public String getAutobudget() {
        return autobudget;
    }

    public CampaignParams withAutobudget(String autobudget) {
        this.autobudget = autobudget;
        return this;
    }

    public String getAutobudgetDate() {
        return autobudgetDate;
    }

    public CampaignParams withAutobudgetDate(String autobudgetDate) {
        this.autobudgetDate = autobudgetDate;
        return this;
    }

    public String getSendAccNews() {
        return sendAccNews;
    }

    public CampaignParams withSendAccNews(String sendAccNews) {
        this.sendAccNews = sendAccNews;
        return this;
    }

    public String getMediaType() {
        return mediaType;
    }

    public CampaignParams withMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public String getEnableCpcHold() {
        return enableCpcHold;
    }

    public CampaignParams withEnableCpcHold(String enableCpcHold) {
        this.enableCpcHold = enableCpcHold;
        return this;
    }

    public String getDayBudgetDailyChangeCount() {
        return dayBudgetDailyChangeCount;
    }

    public CampaignParams withDayBudgetDailyChangeCount(String dayBudgetDailyChangeCount) {
        this.dayBudgetDailyChangeCount = dayBudgetDailyChangeCount;
        return this;
    }

    public String getWarnPlaceInterval() {
        return warnPlaceInterval;
    }

    public CampaignParams withWarnPlaceInterval(String warnPlaceInterval) {
        this.warnPlaceInterval = warnPlaceInterval;
        return this;
    }

    public String getLastChange() {
        return LastChange;
    }

    public CampaignParams withLastChange(String lastChange) {
        LastChange = lastChange;
        return this;
    }

    public Double getMAXBANNERLIMIT() {
        return MAXBANNERLIMIT;
    }

    public CampaignParams withMAXBANNERLIMIT(Double MAXBANNERLIMIT) {
        this.MAXBANNERLIMIT = MAXBANNERLIMIT;
        return this;
    }

    public String getIsSearchStop() {
        return isSearchStop;
    }

    public CampaignParams withIsSearchStop(String isSearchStop) {
        this.isSearchStop = isSearchStop;
        return this;
    }

    public String getWarnplacesStr() {
        return warnplacesStr;
    }

    public CampaignParams withWarnplacesStr(String warnplacesStr) {
        this.warnplacesStr = warnplacesStr;
        return this;
    }

    public String getValid() {
        return valid;
    }

    public CampaignParams withValid(String valid) {
        this.valid = valid;
        return this;
    }

    public String getClientID() {
        return ClientID;
    }

    public CampaignParams withClientID(String clientID) {
        ClientID = clientID;
        return this;
    }

    public String getDayBudgetStopTime() {
        return dayBudgetStopTime;
    }

    public CampaignParams withDayBudgetStopTime(String dayBudgetStopTime) {
        this.dayBudgetStopTime = dayBudgetStopTime;
        return this;
    }

    public String getBroadMatchLimit() {
        return broadMatchLimit;
    }

    public CampaignParams withBroadMatchLimit(String broadMatchLimit) {
        this.broadMatchLimit = broadMatchLimit;
        return this;
    }

    public String getTotalUnits() {
        return totalUnits;
    }

    public CampaignParams withTotalUnits(String totalUnits) {
        this.totalUnits = totalUnits;
        return this;
    }

    public String getContextLimit() {
        return ContextLimit;
    }

    public CampaignParams withContextLimit(String contextLimit) {
        ContextLimit = contextLimit;
        return this;
    }

    public String getOriginalSumSpent() {
        return originalSumSpent;
    }

    public CampaignParams withOriginalSumSpent(String originalSumSpent) {
        this.originalSumSpent = originalSumSpent;
        return this;
    }

    public String getStatusOpenStat() {
        return statusOpenStat;
    }

    public CampaignParams withStatusOpenStat(String statusOpenStat) {
        this.statusOpenStat = statusOpenStat;
        return this;
    }

    public String getMediaplanStatus() {
        return mediaplanStatus;
    }

    public CampaignParams withMediaplanStatus(String mediaplanStatus) {
        this.mediaplanStatus = mediaplanStatus;
        return this;
    }

    public String getStatusMail() {
        return statusMail;
    }

    public CampaignParams withStatusMail(String statusMail) {
        this.statusMail = statusMail;
        return this;
    }

    public String getProductID() {
        return ProductID;
    }

    public CampaignParams withProductID(String productID) {
        ProductID = productID;
        return this;
    }

    public String getMoneyWarningValue() {
        return moneyWarningValue;
    }

    public CampaignParams withMoneyWarningValue(String moneyWarningValue) {
        this.moneyWarningValue = moneyWarningValue;
        return this;
    }

    public String getBroadMatchFlag() {
        return broadMatchFlag;
    }

    public CampaignParams withBroadMatchFlag(String broadMatchFlag) {
        this.broadMatchFlag = broadMatchFlag;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public CampaignParams withUid(String uid) {
        this.uid = uid;
        return this;
    }

    public Double getManagerUid() {
        return managerUid;
    }

    public CampaignParams withManagerUid(Double managerUid) {
        this.managerUid = managerUid;
        return this;
    }

    public Double getOptimalGroupsOnPage() {
        return optimalGroupsOnPage;
    }

    public CampaignParams withOptimalGroupsOnPage(Double optimalGroupsOnPage) {
        this.optimalGroupsOnPage = optimalGroupsOnPage;
        return this;
    }

    public String getLastnews() {
        return lastnews;
    }

    public CampaignParams withLastnews(String lastnews) {
        this.lastnews = lastnews;
        return this;
    }

    public String getSendNews() {
        return sendNews;
    }

    public CampaignParams withSendNews(String sendNews) {
        this.sendNews = sendNews;
        return this;
    }

    public String getStatusBsSynced() {
        return statusBsSynced;
    }

    public CampaignParams withStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
        return this;
    }

    public String getOfflineStatNotice() {
        return offlineStatNotice;
    }

    public CampaignParams withOfflineStatNotice(String offlineStatNotice) {
        this.offlineStatNotice = offlineStatNotice;
        return this;
    }

    public List<Object> getTags() {
        return tags;
    }

    public CampaignParams withTags(List<Object> tags) {
        this.tags = tags;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public CampaignParams withEmail(String email) {
        this.email = email;
        return this;
    }

    public String getDd() {
        return dd;
    }

    public CampaignParams withDd(String dd) {
        this.dd = dd;
        return this;
    }

    public String getAutoOptimization() {
        return autoOptimization;
    }

    public CampaignParams withAutoOptimization(String autoOptimization) {
        this.autoOptimization = autoOptimization;
        return this;
    }

    public String getBroadMatchRate() {
        return broadMatchRate;
    }

    public CampaignParams withBroadMatchRate(String broadMatchRate) {
        this.broadMatchRate = broadMatchRate;
        return this;
    }

    public Double getTimetargetCoef() {
        return timetargetCoef;
    }

    public CampaignParams withTimetargetCoef(Double timetargetCoef) {
        this.timetargetCoef = timetargetCoef;
        return this;
    }

    public String getLastShowTime() {
        return lastShowTime;
    }

    public CampaignParams withLastShowTime(String lastShowTime) {
        this.lastShowTime = lastShowTime;
        return this;
    }

    public String getFIO() {
        return FIO;
    }

    public CampaignParams withFIO(String FIO) {
        this.FIO = FIO;
        return this;
    }

    public String getFio() {
        return fio;
    }

    public CampaignParams withFio(String fio) {
        this.fio = fio;
        return this;
    }

    public String getCid() {
        return cid;
    }

    public CampaignParams withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public String getStatusPostModerate() {
        return statusPostModerate;
    }

    public CampaignParams withStatusPostModerate(String statusPostModerate) {
        this.statusPostModerate = statusPostModerate;
        return this;
    }

    public Double getCommonGeoSet() {
        return commonGeoSet;
    }

    public CampaignParams withCommonGeoSet(Double commonGeoSet) {
        this.commonGeoSet = commonGeoSet;
        return this;
    }

    public String getYyyy() {
        return yyyy;
    }

    public CampaignParams withYyyy(String yyyy) {
        this.yyyy = yyyy;
        return this;
    }

    public String getCopiedFrom() {
        return copiedFrom;
    }

    public CampaignParams withCopiedFrom(String copiedFrom) {
        this.copiedFrom = copiedFrom;
        return this;
    }

    public Double getMINPHRASERANKWARNING() {
        return MINPHRASERANKWARNING;
    }

    public CampaignParams withMINPHRASERANKWARNING(Double MINPHRASERANKWARNING) {
        this.MINPHRASERANKWARNING = MINPHRASERANKWARNING;
        return this;
    }

    public String getStopTime() {
        return stopTime;
    }

    public CampaignParams withStopTime(String stopTime) {
        this.stopTime = stopTime;
        return this;
    }

    public String getCommonGeo() {
        return commonGeo;
    }

    public CampaignParams withCommonGeo(String commonGeo) {
        this.commonGeo = commonGeo;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public CampaignParams withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getAutoOptimizeRequest() {
        return autoOptimizeRequest;
    }

    public CampaignParams withAutoOptimizeRequest(String autoOptimizeRequest) {
        this.autoOptimizeRequest = autoOptimizeRequest;
        return this;
    }

    public Double getNoTitleSubstitute() {
        return noTitleSubstitute;
    }

    public CampaignParams withNoTitleSubstitute(Double noTitleSubstitute) {
        this.noTitleSubstitute = noTitleSubstitute;
        return this;
    }

    public String getBannersCount() {
        return bannersCount;
    }

    public CampaignParams withBannersCount(String bannersCount) {
        this.bannersCount = bannersCount;
        return this;
    }

    public String getStatusAutobudgetForecast() {
        return statusAutobudgetForecast;
    }

    public CampaignParams withStatusAutobudgetForecast(String statusAutobudgetForecast) {
        this.statusAutobudgetForecast = statusAutobudgetForecast;
        return this;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public CampaignParams withStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public String getSendWarn() {
        return sendWarn;
    }

    public CampaignParams withSendWarn(String sendWarn) {
        this.sendWarn = sendWarn;
        return this;
    }

    public Double getHasGroups() {
        return hasGroups;
    }

    public CampaignParams withHasGroups(Double hasGroups) {
        this.hasGroups = hasGroups;
        return this;
    }

    public String getStatusYacobotDeleted() {
        return statusYacobotDeleted;
    }

    public CampaignParams withStatusYacobotDeleted(String statusYacobotDeleted) {
        this.statusYacobotDeleted = statusYacobotDeleted;
        return this;
    }

    public String getName() {
        return name;
    }

    public CampaignParams withName(String name) {
        this.name = name;
        return this;
    }

    public String getSmsTime() {
        return smsTime;
    }

    public CampaignParams withSmsTime(String smsTime) {
        this.smsTime = smsTime;
        return this;
    }

    public Double getTotal() {
        return total;
    }

    public CampaignParams withTotal(Double total) {
        this.total = total;
        return this;
    }

    public String getGeo() {
        return geo;
    }

    public CampaignParams withGeo(String geo) {
        this.geo = geo;
        return this;
    }

    public String getFairAuction() {
        return fairAuction;
    }

    public CampaignParams withFairAuction(String fairAuction) {
        this.fairAuction = fairAuction;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public CampaignParams withStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getProductType() {
        return productType;
    }

    public CampaignParams withProductType(String productType) {
        this.productType = productType;
        return this;
    }

    public String getStatusEmpty() {
        return statusEmpty;
    }

    public CampaignParams withStatusEmpty(String statusEmpty) {
        this.statusEmpty = statusEmpty;
        return this;
    }

    public String getUntaggedBannersNum() {
        return untaggedBannersNum;
    }

    public CampaignParams withUntaggedBannersNum(String untaggedBannersNum) {
        this.untaggedBannersNum = untaggedBannersNum;
        return this;
    }

    public String getStatusActive() {
        return statusActive;
    }

    public CampaignParams withStatusActive(String statusActive) {
        this.statusActive = statusActive;
        return this;
    }

    public String getCompaignDomainsCount() {
        return compaignDomainsCount;
    }

    public CampaignParams withCompaignDomainsCount(String compaignDomainsCount) {
        this.compaignDomainsCount = compaignDomainsCount;
        return this;
    }

    public String getOriginalSum() {
        return originalSum;
    }

    public CampaignParams withOriginalSum(String originalSum) {
        this.originalSum = originalSum;
        return this;
    }

    public String getDisableBehavior() {
        return disableBehavior;
    }

    public CampaignParams withDisableBehavior(String disableBehavior) {
        this.disableBehavior = disableBehavior;
        return this;
    }

    public String getStatusBehavior() {
        return statusBehavior;
    }

    public CampaignParams withStatusBehavior(String statusBehavior) {
        this.statusBehavior = statusBehavior;
        return this;
    }

    public String getDontShowYacontext() {
        return dontShowYacontext;
    }

    public CampaignParams withDontShowYacontext(String dontShowYacontext) {
        this.dontShowYacontext = dontShowYacontext;
        return this;
    }

    public String getMoneyType() {
        return moneyType;
    }

    public CampaignParams withMoneyType(String moneyType) {
        this.moneyType = moneyType;
        return this;
    }

    public Double getCommonVcardSet() {
        return commonVcardSet;
    }

    public CampaignParams withCommonVcardSet(Double commonVcardSet) {
        this.commonVcardSet = commonVcardSet;
        return this;
    }

    public Double getAllBannersNum() {
        return allBannersNum;
    }

    public CampaignParams withAllBannersNum(Double allBannersNum) {
        this.allBannersNum = allBannersNum;
        return this;
    }

    public String getDontShowCatalog() {
        return dontShowCatalog;
    }

    public CampaignParams withDontShowCatalog(String dontShowCatalog) {
        this.dontShowCatalog = dontShowCatalog;
        return this;
    }

    public String getSumUnits() {
        return sumUnits;
    }

    public CampaignParams withSumUnits(String sumUnits) {
        this.sumUnits = sumUnits;
        return this;
    }

    public String getStatusBsArchived() {
        return statusBsArchived;
    }

    public CampaignParams withStatusBsArchived(String statusBsArchived) {
        this.statusBsArchived = statusBsArchived;
        return this;
    }

    public String getDayBudgetShowMode() {
        return dayBudgetShowMode;
    }

    public CampaignParams withDayBudgetShowMode(String dayBudgetShowMode) {
        this.dayBudgetShowMode = dayBudgetShowMode;
        return this;
    }

    public String getMetrikaCounters() {
        return metrikaCounters;
    }

    public CampaignParams withMetrikaCounters(String metrikaCounters) {
        this.metrikaCounters = metrikaCounters;
        return this;
    }

    public ProductInfo getProductInfo() {
        return productInfo;
    }

    public CampaignParams withProductInfo(ProductInfo productInfo) {
        this.productInfo = productInfo;
        return this;
    }

    public String getBannersPerPage() {
        return bannersPerPage;
    }

    public CampaignParams withBannersPerPage(String bannersPerPage) {
        this.bannersPerPage = bannersPerPage;
        return this;
    }

    public String getIsRelatedKeywordsEnabled() {
        return isRelatedKeywordsEnabled;
    }

    public CampaignParams withIsRelatedKeywordsEnabled(String isRelatedKeywordsEnabled) {
        this.isRelatedKeywordsEnabled = isRelatedKeywordsEnabled;
        return this;
    }

    public String getStatusMetricaControl() {
        return statusMetricaControl;
    }

    public CampaignParams withStatusMetricaControl(String statusMetricaControl) {
        this.statusMetricaControl = statusMetricaControl;
        return this;
    }

    public String getContactinfo() {
        return contactinfo;
    }

    public CampaignParams withContactinfo(String contactinfo) {
        this.contactinfo = contactinfo;
        return this;
    }

    public ContactInfo getVcard() {
        return vcard;
    }

    public CampaignParams withVcard(ContactInfo vcard) {
        this.vcard = vcard;
        return this;
    }

    public String getOrderID() {
        return OrderID;
    }

    public CampaignParams withOrderID(String orderID) {
        OrderID = orderID;
        return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public CampaignParams withStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public String getRf() {
        return rf;
    }

    public CampaignParams withRf(String rf) {
        this.rf = rf;
        return this;
    }

    public String getStatusNoPay() {
        return statusNoPay;
    }

    public CampaignParams withStatusNoPay(String statusNoPay) {
        this.statusNoPay = statusNoPay;
        return this;
    }

    public String getCompetitorsDomains() {
        return competitorsDomains;
    }

    public CampaignParams withCompetitorsDomains(String competitorsDomains) {
        this.competitorsDomains = competitorsDomains;
        return this;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public CampaignParams withTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
        return this;
    }

    public String getStrategyMinPrice() {
        return strategyMinPrice;
    }

    public CampaignParams withStrategyMinPrice(String strategyMinPrice) {
        this.strategyMinPrice = strategyMinPrice;
        return this;
    }

    public String getSumSpentUnits() {
        return sumSpentUnits;
    }

    public CampaignParams withSumSpentUnits(String sumSpentUnits) {
        this.sumSpentUnits = sumSpentUnits;
        return this;
    }

    public Double getWarnplaces() {
        return warnplaces;
    }

    public CampaignParams withWarnplaces(Double warnplaces) {
        this.warnplaces = warnplaces;
        return this;
    }

    public Double getAgencyUid() {
        return agencyUid;
    }

    public CampaignParams withAgencyUid(Double agencyUid) {
        this.agencyUid = agencyUid;
        return this;
    }

    public String getSumSpent() {
        return sumSpent;
    }

    public CampaignParams withSumSpent(String sumSpent) {
        this.sumSpent = sumSpent;
        return this;
    }

    public String getBalanceTid() {
        return balanceTid;
    }

    public CampaignParams withBalanceTid(String balanceTid) {
        this.balanceTid = balanceTid;
        return this;
    }

    public String getStrategy() {
        return strategy;
    }

    public CampaignParams withStrategy(String strategy) {
        this.strategy = strategy;
        return this;
    }

    public String getAgencyID() {
        return AgencyID;
    }

    public CampaignParams withAgencyID(String agencyID) {
        AgencyID = agencyID;
        return this;
    }

    public String getRfReset() {
        return rfReset;
    }

    public CampaignParams withRfReset(String rfReset) {
        this.rfReset = rfReset;
        return this;
    }

    public String getTimeTarget() {
        return timeTarget;
    }

    public CampaignParams withTimeTarget(String timeTarget) {
        this.timeTarget = timeTarget;
        return this;
    }

    public List<String> getMinusWords() {
        return minusWords;
    }

    public CampaignParams withMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
        return this;
    }

    public List<Object> getImages() {
        return images;
    }

    public CampaignParams withImages(List<Object> images) {
        this.images = images;
        return this;
    }

    public String getArchived() {
        return archived;
    }

    public CampaignParams withArchived(String archived) {
        this.archived = archived;
        return this;
    }

    public String getSumLast() {
        return sumLast;
    }

    public CampaignParams withSumLast(String sumLast) {
        this.sumLast = sumLast;
        return this;
    }

    public HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    public CampaignParams withHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
        return this;
    }

    public String getCurrencyConverted() {
        return currencyConverted;
    }

    public CampaignParams withCurrencyConverted(String currencyConverted) {
        this.currencyConverted = currencyConverted;
        return this;
    }

    public String getIsFavorite() {
        return isFavorite;
    }

    public CampaignParams withIsFavorite(String isFavorite) {
        this.isFavorite = isFavorite;
        return this;
    }

    public String getPaidByCertificate() {
        return paidByCertificate;
    }

    public CampaignParams withPaidByCertificate(String paidByCertificate) {
        this.paidByCertificate = paidByCertificate;
        return this;
    }

    public Double getIsCampLocked() {
        return isCampLocked;
    }

    public CampaignParams withIsCampLocked(Double isCampLocked) {
        this.isCampLocked = isCampLocked;
        return this;
    }

    public String getMm() {
        return mm;
    }

    public CampaignParams withMm(String mm) {
        this.mm = mm;
        return this;
    }

    public String getSum() {
        return sum;
    }

    public CampaignParams withSum(String sum) {
        this.sum = sum;
        return this;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public CampaignParams withFinishTime(String finishTime) {
        this.finishTime = finishTime;
        return this;
    }

    public Double getTabclassSearchCount() {
        return tabclassSearchCount;
    }

    public CampaignParams withTabclassSearchCount(Double tabclassSearchCount) {
        this.tabclassSearchCount = tabclassSearchCount;
        return this;
    }

    public String getStatusClickTrack() {
        return statusClickTrack;
    }

    public CampaignParams withStatusClickTrack(String statusClickTrack) {
        this.statusClickTrack = statusClickTrack;
        return this;
    }

    public Double getMAXPHRASERANKWARNING() {
        return MAXPHRASERANKWARNING;
    }

    public CampaignParams withMAXPHRASERANKWARNING(Double MAXPHRASERANKWARNING) {
        this.MAXPHRASERANKWARNING = MAXPHRASERANKWARNING;
        return this;
    }

    public String getStatusContextStop() {
        return statusContextStop;
    }

    public CampaignParams withStatusContextStop(String statusContextStop) {
        this.statusContextStop = statusContextStop;
        return this;
    }

    public List<String> getCampaignMinusWords() {
        return campaignMinusWords;
    }

    public CampaignParams withCampaignMinusWords(List<String> campaignMinusWords) {
        this.campaignMinusWords = campaignMinusWords;
        return this;
    }

    public String getType() {
        return type;
    }

    public CampaignParams withType(String type) {
        this.type = type;
        return this;
    }

    public String getContextPriceCoef() {
        return ContextPriceCoef;
    }

    public CampaignParams withContextPriceCoef(String contextPriceCoef) {
        ContextPriceCoef = contextPriceCoef;
        return this;
    }

    public Double getGroupsOnPage() {
        return groupsOnPage;
    }

    public CampaignParams withGroupsOnPage(Double groupsOnPage) {
        this.groupsOnPage = groupsOnPage;
        return this;
    }

    public Double getPagesNum() {
        return pagesNum;
    }

    public CampaignParams withPagesNum(Double pagesNum) {
        this.pagesNum = pagesNum;
        return this;
    }

    public String getFinishDate() {
        return finishDate;
    }

    public CampaignParams withFinishDate(String finishDate) {
        this.finishDate = finishDate;
        return this;
    }

    public String getContentLang() {
        return contentLang;
    }

    public CampaignParams withContentLang(String contentLang) {
        this.contentLang = contentLang;
        return this;
    }

    public String getBroadMatchGoalId() {
        return broadMatchGoalId;
    }

    public CampaignParams withBroadMatchGoalId(String broadMatchGoalId) {
        this.broadMatchGoalId = broadMatchGoalId;
        return this;
    }

    public Integer getNoExtendedGeotargeting() {
        return noExtendedGeotargeting;
    }

    public CampaignParams withNoExtendedGeotargeting(Integer noExtendedGeotargeting) {
        this.noExtendedGeotargeting = noExtendedGeotargeting;
        return this;
    }
}
