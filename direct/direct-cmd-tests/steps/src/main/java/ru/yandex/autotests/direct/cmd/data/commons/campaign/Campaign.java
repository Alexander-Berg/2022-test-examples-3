package ru.yandex.autotests.direct.cmd.data.commons.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.campaigns.OptimizedCamp;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ImageAd;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo.ExtendedGeoItem;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;

@SuppressWarnings("unused")
public class Campaign {

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

//    @SerializedName("strategy_min_price")
//    private StrategyMinPrice strategyMinPrice;

    @SerializedName("sum_spent_units")
    private String sumSpentUnits;

    @SerializedName("warnplaces")
    private Double warnplaces;

    //TODO Надо думать, что с этим делать editDynamicAdGroups возвращает строку, а в перфоманс объект
    /*@SerializedName("sms_flags")
    private SmsFlags smsFlags;*/

    @SerializedName("agency_uid")
    private Double agencyUid;

    @SerializedName("sum_spent")
    private String sumSpent;

    @SerializedName("balance_tid")
    private String balanceTid;

    @SerializedName("strategy")
    private CampaignStrategy strategy;

    @SerializedName("AgencyID")
    private String AgencyID;

    @SerializedName("rfReset")
    private String rfReset;

    @SerializedName("timeTarget")
    private String timeTarget;

    @SerializedName("json_minus_words")
    private List<String> jsonMinusWords;

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

    @SerializedName("json_campaign_minus_words")
    private List<String> jsonCampaignMinusWords;

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

    @SerializedName("pictures")
    private List<ImageAd> imageAds;

    @SerializedName("optimize_camp")
    private OptimizedCamp optimizeCamp;

    @SerializedName("geo_multipliers_enabled")
    private Integer geoMultipliersEnabled;

    @SerializedName("pid_to_group_name")
    private Map<Long, String> groupIdToNameMap;

    @SerializedName("extended_geo")
    private Map<String, ExtendedGeoItem> extendedGeoItemsMap;

    @SerializedName("ab_sections_statistic")
    private List<Long> abSectionsStatistic;

    @SerializedName("ab_segments_retargeting")
    private List<Long> abSegmentsRetargeting;

    public OptimizedCamp getOptimizeCamp() {
        return optimizeCamp;
    }

    public Campaign withOptimizeCamp(OptimizedCamp optimizedCamp) {
        this.optimizeCamp = optimizedCamp;
        return this;
    }

    public String getBroadMatchGoalId() {
        return broadMatchGoalId;
    }

    public void setBroadMatchGoalId(String broadMatchGoalId) {
        this.broadMatchGoalId = broadMatchGoalId;
    }

    public String getBudgetStrategy() {
        return budgetStrategy;
    }

    public Campaign withBudgetStrategy(String budgetStrategy) {
        this.budgetStrategy = budgetStrategy;
        return this;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Campaign withGroups(List<Group> groups) {
        this.groups = groups;
        return this;
    }

    public String getStatusShow() {
        return statusShow;
    }

    public Campaign withStatusShow(String statusShow) {
        this.statusShow = statusShow;
        return this;
    }

    public String getSumToPay() {
        return sumToPay;
    }

    public Campaign withSumToPay(String sumToPay) {
        this.sumToPay = sumToPay;
        return this;
    }

    public String getAutobudget() {
        return autobudget;
    }

    public Campaign withAutobudget(String autobudget) {
        this.autobudget = autobudget;
        return this;
    }

    public String getAutobudgetDate() {
        return autobudgetDate;
    }

    public Campaign withAutobudgetDate(String autobudgetDate) {
        this.autobudgetDate = autobudgetDate;
        return this;
    }

    public String getSendAccNews() {
        return sendAccNews;
    }

    public Campaign withSendAccNews(String sendAccNews) {
        this.sendAccNews = sendAccNews;
        return this;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Campaign withMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public String getEnableCpcHold() {
        return enableCpcHold;
    }

    public Campaign withEnableCpcHold(String enableCpcHold) {
        this.enableCpcHold = enableCpcHold;
        return this;
    }

    public String getDayBudgetDailyChangeCount() {
        return dayBudgetDailyChangeCount;
    }

    public Campaign withDayBudgetDailyChangeCount(String dayBudgetDailyChangeCount) {
        this.dayBudgetDailyChangeCount = dayBudgetDailyChangeCount;
        return this;
    }

    public String getWarnPlaceInterval() {
        return warnPlaceInterval;
    }

    public Campaign withWarnPlaceInterval(String warnPlaceInterval) {
        this.warnPlaceInterval = warnPlaceInterval;
        return this;
    }

    public String getLastChange() {
        return LastChange;
    }

    public Campaign withLastChange(String lastChange) {
        LastChange = lastChange;
        return this;
    }

    public Double getMAXBANNERLIMIT() {
        return MAXBANNERLIMIT;
    }

    public Campaign withMAXBANNERLIMIT(Double MAXBANNERLIMIT) {
        this.MAXBANNERLIMIT = MAXBANNERLIMIT;
        return this;
    }

    public String getIsSearchStop() {
        return isSearchStop;
    }

    public Campaign withIsSearchStop(String isSearchStop) {
        this.isSearchStop = isSearchStop;
        return this;
    }

    public String getWarnplacesStr() {
        return warnplacesStr;
    }

    public Campaign withWarnplacesStr(String warnplacesStr) {
        this.warnplacesStr = warnplacesStr;
        return this;
    }

    public String getValid() {
        return valid;
    }

    public Campaign withValid(String valid) {
        this.valid = valid;
        return this;
    }

    public String getClientID() {
        return ClientID;
    }

    public Campaign withClientID(String clientID) {
        ClientID = clientID;
        return this;
    }

    public String getDayBudgetStopTime() {
        return dayBudgetStopTime;
    }

    public Campaign withDayBudgetStopTime(String dayBudgetStopTime) {
        this.dayBudgetStopTime = dayBudgetStopTime;
        return this;
    }

    public String getBroadMatchLimit() {
        return broadMatchLimit;
    }

    public Campaign withBroadMatchLimit(String broadMatchLimit) {
        this.broadMatchLimit = broadMatchLimit;
        return this;
    }

    public String getTotalUnits() {
        return totalUnits;
    }

    public Campaign withTotalUnits(String totalUnits) {
        this.totalUnits = totalUnits;
        return this;
    }

    public String getContextLimit() {
        return ContextLimit;
    }

    public Campaign withContextLimit(String contextLimit) {
        ContextLimit = contextLimit;
        return this;
    }

    public String getOriginalSumSpent() {
        return originalSumSpent;
    }

    public Campaign withOriginalSumSpent(String originalSumSpent) {
        this.originalSumSpent = originalSumSpent;
        return this;
    }

    public String getStatusOpenStat() {
        return statusOpenStat;
    }

    public Campaign withStatusOpenStat(String statusOpenStat) {
        this.statusOpenStat = statusOpenStat;
        return this;
    }

    public String getMediaplanStatus() {
        return mediaplanStatus;
    }

    public Campaign withMediaplanStatus(String mediaplanStatus) {
        this.mediaplanStatus = mediaplanStatus;
        return this;
    }

    public String getStatusMail() {
        return statusMail;
    }

    public Campaign withStatusMail(String statusMail) {
        this.statusMail = statusMail;
        return this;
    }

    public String getProductID() {
        return ProductID;
    }

    public Campaign withProductID(String productID) {
        ProductID = productID;
        return this;
    }

    public String getMoneyWarningValue() {
        return moneyWarningValue;
    }

    public Campaign withMoneyWarningValue(String moneyWarningValue) {
        this.moneyWarningValue = moneyWarningValue;
        return this;
    }

    public String getBroadMatchFlag() {
        return broadMatchFlag;
    }

    public Campaign withBroadMatchFlag(String broadMatchFlag) {
        this.broadMatchFlag = broadMatchFlag;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public Campaign withUid(String uid) {
        this.uid = uid;
        return this;
    }

    public Double getManagerUid() {
        return managerUid;
    }

    public Campaign withManagerUid(Double managerUid) {
        this.managerUid = managerUid;
        return this;
    }

    public Double getOptimalGroupsOnPage() {
        return optimalGroupsOnPage;
    }

    public Campaign withOptimalGroupsOnPage(Double optimalGroupsOnPage) {
        this.optimalGroupsOnPage = optimalGroupsOnPage;
        return this;
    }

    public String getLastnews() {
        return lastnews;
    }

    public Campaign withLastnews(String lastnews) {
        this.lastnews = lastnews;
        return this;
    }

    public String getSendNews() {
        return sendNews;
    }

    public Campaign withSendNews(String sendNews) {
        this.sendNews = sendNews;
        return this;
    }

    public String getStatusBsSynced() {
        return statusBsSynced;
    }

    public Campaign withStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
        return this;
    }

    public String getOfflineStatNotice() {
        return offlineStatNotice;
    }

    public Campaign withOfflineStatNotice(String offlineStatNotice) {
        this.offlineStatNotice = offlineStatNotice;
        return this;
    }

    public List<Object> getTags() {
        return tags;
    }

    public Campaign withTags(List<Object> tags) {
        this.tags = tags;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Campaign withEmail(String email) {
        this.email = email;
        return this;
    }

    public String getDd() {
        return dd;
    }

    public Campaign withDd(String dd) {
        this.dd = dd;
        return this;
    }

    public String getAutoOptimization() {
        return autoOptimization;
    }

    public Campaign withAutoOptimization(String autoOptimization) {
        this.autoOptimization = autoOptimization;
        return this;
    }

    public String getBroadMatchRate() {
        return broadMatchRate;
    }

    public Campaign withBroadMatchRate(String broadMatchRate) {
        this.broadMatchRate = broadMatchRate;
        return this;
    }

    public Double getTimetargetCoef() {
        return timetargetCoef;
    }

    public Campaign withTimetargetCoef(Double timetargetCoef) {
        this.timetargetCoef = timetargetCoef;
        return this;
    }

    public String getLastShowTime() {
        return lastShowTime;
    }

    public Campaign withLastShowTime(String lastShowTime) {
        this.lastShowTime = lastShowTime;
        return this;
    }

    public String getFIO() {
        return FIO;
    }

    public Campaign withFIO(String FIO) {
        this.FIO = FIO;
        return this;
    }

    public String getFio() {
        return fio;
    }

    public Campaign withFio(String fio) {
        this.fio = fio;
        return this;
    }

    public String getCid() {
        return cid;
    }

    public Campaign withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public String getStatusPostModerate() {
        return statusPostModerate;
    }

    public Campaign withStatusPostModerate(String statusPostModerate) {
        this.statusPostModerate = statusPostModerate;
        return this;
    }

    public Double getCommonGeoSet() {
        return commonGeoSet;
    }

    public Campaign withCommonGeoSet(Double commonGeoSet) {
        this.commonGeoSet = commonGeoSet;
        return this;
    }

    public String getYyyy() {
        return yyyy;
    }

    public Campaign withYyyy(String yyyy) {
        this.yyyy = yyyy;
        return this;
    }

    public String getCopiedFrom() {
        return copiedFrom;
    }

    public Campaign withCopiedFrom(String copiedFrom) {
        this.copiedFrom = copiedFrom;
        return this;
    }

    public Double getMINPHRASERANKWARNING() {
        return MINPHRASERANKWARNING;
    }

    public Campaign withMINPHRASERANKWARNING(Double MINPHRASERANKWARNING) {
        this.MINPHRASERANKWARNING = MINPHRASERANKWARNING;
        return this;
    }

    public String getStopTime() {
        return stopTime;
    }

    public Campaign withStopTime(String stopTime) {
        this.stopTime = stopTime;
        return this;
    }

    public String getCommonGeo() {
        return commonGeo;
    }

    public Campaign withCommonGeo(String commonGeo) {
        this.commonGeo = commonGeo;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public Campaign withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getAutoOptimizeRequest() {
        return autoOptimizeRequest;
    }

    public Campaign withAutoOptimizeRequest(String autoOptimizeRequest) {
        this.autoOptimizeRequest = autoOptimizeRequest;
        return this;
    }

    public Double getNoTitleSubstitute() {
        return noTitleSubstitute;
    }

    public Campaign withNoTitleSubstitute(Double noTitleSubstitute) {
        this.noTitleSubstitute = noTitleSubstitute;
        return this;
    }

    public String getBannersCount() {
        return bannersCount;
    }

    public Campaign withBannersCount(String bannersCount) {
        this.bannersCount = bannersCount;
        return this;
    }

    public String getStatusAutobudgetForecast() {
        return statusAutobudgetForecast;
    }

    public Campaign withStatusAutobudgetForecast(String statusAutobudgetForecast) {
        this.statusAutobudgetForecast = statusAutobudgetForecast;
        return this;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public Campaign withStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public String getSendWarn() {
        return sendWarn;
    }

    public Campaign withSendWarn(String sendWarn) {
        this.sendWarn = sendWarn;
        return this;
    }

    public Double getHasGroups() {
        return hasGroups;
    }

    public Campaign withHasGroups(Double hasGroups) {
        this.hasGroups = hasGroups;
        return this;
    }

    public String getStatusYacobotDeleted() {
        return statusYacobotDeleted;
    }

    public Campaign withStatusYacobotDeleted(String statusYacobotDeleted) {
        this.statusYacobotDeleted = statusYacobotDeleted;
        return this;
    }

    public String getName() {
        return name;
    }

    public Campaign withName(String name) {
        this.name = name;
        return this;
    }

    public String getSmsTime() {
        return smsTime;
    }

    public Campaign withSmsTime(String smsTime) {
        this.smsTime = smsTime;
        return this;
    }

    public Double getTotal() {
        return total;
    }

    public Campaign withTotal(Double total) {
        this.total = total;
        return this;
    }

    public String getGeo() {
        return geo;
    }

    public Campaign withGeo(String geo) {
        this.geo = geo;
        return this;
    }

    public String getFairAuction() {
        return fairAuction;
    }

    public Campaign withFairAuction(String fairAuction) {
        this.fairAuction = fairAuction;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public Campaign withStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getProductType() {
        return productType;
    }

    public Campaign withProductType(String productType) {
        this.productType = productType;
        return this;
    }

    public String getStatusEmpty() {
        return statusEmpty;
    }

    public Campaign withStatusEmpty(String statusEmpty) {
        this.statusEmpty = statusEmpty;
        return this;
    }

    public String getUntaggedBannersNum() {
        return untaggedBannersNum;
    }

    public Campaign withUntaggedBannersNum(String untaggedBannersNum) {
        this.untaggedBannersNum = untaggedBannersNum;
        return this;
    }

    public String getStatusActive() {
        return statusActive;
    }

    public Campaign withStatusActive(String statusActive) {
        this.statusActive = statusActive;
        return this;
    }

    public String getCompaignDomainsCount() {
        return compaignDomainsCount;
    }

    public Campaign withCompaignDomainsCount(String compaignDomainsCount) {
        this.compaignDomainsCount = compaignDomainsCount;
        return this;
    }

    public String getOriginalSum() {
        return originalSum;
    }

    public Campaign withOriginalSum(String originalSum) {
        this.originalSum = originalSum;
        return this;
    }

    public String getDisableBehavior() {
        return disableBehavior;
    }

    public Campaign withDisableBehavior(String disableBehavior) {
        this.disableBehavior = disableBehavior;
        return this;
    }

    public String getStatusBehavior() {
        return statusBehavior;
    }

    public Campaign withStatusBehavior(String statusBehavior) {
        this.statusBehavior = statusBehavior;
        return this;
    }

    public String getDontShowYacontext() {
        return dontShowYacontext;
    }

    public Campaign withDontShowYacontext(String dontShowYacontext) {
        this.dontShowYacontext = dontShowYacontext;
        return this;
    }

    public String getMoneyType() {
        return moneyType;
    }

    public Campaign withMoneyType(String moneyType) {
        this.moneyType = moneyType;
        return this;
    }

    public Double getCommonVcardSet() {
        return commonVcardSet;
    }

    public Campaign withCommonVcardSet(Double commonVcardSet) {
        this.commonVcardSet = commonVcardSet;
        return this;
    }

    public Double getAllBannersNum() {
        return allBannersNum;
    }

    public Campaign withAllBannersNum(Double allBannersNum) {
        this.allBannersNum = allBannersNum;
        return this;
    }

    public String getDontShowCatalog() {
        return dontShowCatalog;
    }

    public Campaign withDontShowCatalog(String dontShowCatalog) {
        this.dontShowCatalog = dontShowCatalog;
        return this;
    }

    public String getSumUnits() {
        return sumUnits;
    }

    public Campaign withSumUnits(String sumUnits) {
        this.sumUnits = sumUnits;
        return this;
    }

    public String getStatusBsArchived() {
        return statusBsArchived;
    }

    public Campaign withStatusBsArchived(String statusBsArchived) {
        this.statusBsArchived = statusBsArchived;
        return this;
    }

    public String getDayBudgetShowMode() {
        return dayBudgetShowMode;
    }

    public Campaign withDayBudgetShowMode(String dayBudgetShowMode) {
        this.dayBudgetShowMode = dayBudgetShowMode;
        return this;
    }

    public String getMetrikaCounters() {
        return metrikaCounters;
    }

    public Campaign withMetrikaCounters(String metrikaCounters) {
        this.metrikaCounters = metrikaCounters;
        return this;
    }

    public ProductInfo getProductInfo() {
        return productInfo;
    }

    public Campaign withProductInfo(ProductInfo productInfo) {
        this.productInfo = productInfo;
        return this;
    }

    public String getBannersPerPage() {
        return bannersPerPage;
    }

    public Campaign withBannersPerPage(String bannersPerPage) {
        this.bannersPerPage = bannersPerPage;
        return this;
    }

    public String getIsRelatedKeywordsEnabled() {
        return isRelatedKeywordsEnabled;
    }

    public Campaign withIsRelatedKeywordsEnabled(String isRelatedKeywordsEnabled) {
        this.isRelatedKeywordsEnabled = isRelatedKeywordsEnabled;
        return this;
    }

    public String getStatusMetricaControl() {
        return statusMetricaControl;
    }

    public Campaign withStatusMetricaControl(String statusMetricaControl) {
        this.statusMetricaControl = statusMetricaControl;
        return this;
    }

    public String getContactinfo() {
        return contactinfo;
    }

    public Campaign withContactinfo(String contactinfo) {
        this.contactinfo = contactinfo;
        return this;
    }

    public ContactInfo getVcard() {
        return vcard;
    }

    public Campaign withVcard(ContactInfo vcard) {
        this.vcard = vcard;
        return this;
    }

    public String getOrderID() {
        return OrderID;
    }

    public Campaign withOrderID(String orderID) {
        OrderID = orderID;
        return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public Campaign withStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public String getRf() {
        return rf;
    }

    public Campaign withRf(String rf) {
        this.rf = rf;
        return this;
    }

    public String getStatusNoPay() {
        return statusNoPay;
    }

    public Campaign withStatusNoPay(String statusNoPay) {
        this.statusNoPay = statusNoPay;
        return this;
    }

    public String getCompetitorsDomains() {
        return competitorsDomains;
    }

    public Campaign withCompetitorsDomains(String competitorsDomains) {
        this.competitorsDomains = competitorsDomains;
        return this;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public Campaign withTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
        return this;
    }

//    public StrategyMinPrice getStrategyMinPrice() {
//        return strategyMinPrice;
//    }
//
//    public Campaign withStrategyMinPrice(StrategyMinPrice strategyMinPrice) {
//        this.strategyMinPrice = strategyMinPrice;
//        return this;
//    }

    public String getSumSpentUnits() {
        return sumSpentUnits;
    }

    public Campaign withSumSpentUnits(String sumSpentUnits) {
        this.sumSpentUnits = sumSpentUnits;
        return this;
    }

    public Double getWarnplaces() {
        return warnplaces;
    }

    public Campaign withWarnplaces(Double warnplaces) {
        this.warnplaces = warnplaces;
        return this;
    }

//    public SmsFlags getSmsFlags() {
//        return smsFlags;
//    }
//
//    public Campaign withSmsFlags(SmsFlags smsFlags) {
//        this.smsFlags = smsFlags;
//        return this;
//    }

    public Double getAgencyUid() {
        return agencyUid;
    }

    public Campaign withAgencyUid(Double agencyUid) {
        this.agencyUid = agencyUid;
        return this;
    }

    public String getSumSpent() {
        return sumSpent;
    }

    public Campaign withSumSpent(String sumSpent) {
        this.sumSpent = sumSpent;
        return this;
    }

    public String getBalanceTid() {
        return balanceTid;
    }

    public Campaign withBalanceTid(String balanceTid) {
        this.balanceTid = balanceTid;
        return this;
    }

    public CampaignStrategy getStrategy() {
        return strategy;
    }

    public Campaign withStrategy(CampaignStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public String getAgencyID() {
        return AgencyID;
    }

    public Campaign withAgencyID(String agencyID) {
        AgencyID = agencyID;
        return this;
    }

    public String getRfReset() {
        return rfReset;
    }

    public Campaign withRfReset(String rfReset) {
        this.rfReset = rfReset;
        return this;
    }

    public String getTimeTarget() {
        return timeTarget;
    }

    public Campaign withTimeTarget(String timeTarget) {
        this.timeTarget = timeTarget;
        return this;
    }

    public List<String> getJsonMinusWords() {
        return jsonMinusWords;
    }

    public Campaign withJsonMinusWords(List<String> jsonMinusWords) {
        this.jsonMinusWords = jsonMinusWords;
        return this;
    }

    public List<String> getMinusWords() {
        return minusWords;
    }

    public void setMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
    }

    public Campaign withMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
        return this;
    }

    public List<Object> getImages() {
        return images;
    }

    public Campaign withImages(List<Object> images) {
        this.images = images;
        return this;
    }

    public String getArchived() {
        return archived;
    }

    public Campaign withArchived(String archived) {
        this.archived = archived;
        return this;
    }

    public String getSumLast() {
        return sumLast;
    }

    public Campaign withSumLast(String sumLast) {
        this.sumLast = sumLast;
        return this;
    }

    public HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    public Campaign withHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
        return this;
    }

    public String getCurrencyConverted() {
        return currencyConverted;
    }

    public Campaign withCurrencyConverted(String currencyConverted) {
        this.currencyConverted = currencyConverted;
        return this;
    }

    public String getIsFavorite() {
        return isFavorite;
    }

    public Campaign withIsFavorite(String isFavorite) {
        this.isFavorite = isFavorite;
        return this;
    }

    public String getPaidByCertificate() {
        return paidByCertificate;
    }

    public Campaign withPaidByCertificate(String paidByCertificate) {
        this.paidByCertificate = paidByCertificate;
        return this;
    }

    public Double getIsCampLocked() {
        return isCampLocked;
    }

    public Campaign withIsCampLocked(Double isCampLocked) {
        this.isCampLocked = isCampLocked;
        return this;
    }

    public String getMm() {
        return mm;
    }

    public Campaign withMm(String mm) {
        this.mm = mm;
        return this;
    }

    public String getSum() {
        return sum;
    }

    public Campaign withSum(String sum) {
        this.sum = sum;
        return this;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public Campaign withFinishTime(String finishTime) {
        this.finishTime = finishTime;
        return this;
    }

    public Double getTabclassSearchCount() {
        return tabclassSearchCount;
    }

    public Campaign withTabclassSearchCount(Double tabclassSearchCount) {
        this.tabclassSearchCount = tabclassSearchCount;
        return this;
    }

    public String getStatusClickTrack() {
        return statusClickTrack;
    }

    public Campaign withStatusClickTrack(String statusClickTrack) {
        this.statusClickTrack = statusClickTrack;
        return this;
    }

    public Double getMAXPHRASERANKWARNING() {
        return MAXPHRASERANKWARNING;
    }

    public Campaign withMAXPHRASERANKWARNING(Double MAXPHRASERANKWARNING) {
        this.MAXPHRASERANKWARNING = MAXPHRASERANKWARNING;
        return this;
    }

    public String getStatusContextStop() {
        return statusContextStop;
    }

    public Campaign withStatusContextStop(String statusContextStop) {
        this.statusContextStop = statusContextStop;
        return this;
    }

    public List<String> getJsonCampaignMinusWords() {
        return jsonCampaignMinusWords;
    }

    public Campaign withJsonCampaignMinusWords(List<String> jsonCampaignMinusWords) {
        this.jsonCampaignMinusWords = jsonCampaignMinusWords;
        return this;
    }

    public String getType() {
        return type;
    }

    public Campaign withType(String type) {
        this.type = type;
        return this;
    }

    public String getContextPriceCoef() {
        return ContextPriceCoef;
    }

    public Campaign withContextPriceCoef(String contextPriceCoef) {
        ContextPriceCoef = contextPriceCoef;
        return this;
    }

    public Double getGroupsOnPage() {
        return groupsOnPage;
    }

    public Campaign withGroupsOnPage(Double groupsOnPage) {
        this.groupsOnPage = groupsOnPage;
        return this;
    }

    public Double getPagesNum() {
        return pagesNum;
    }

    public Campaign withPagesNum(Double pagesNum) {
        this.pagesNum = pagesNum;
        return this;
    }

    public String getFinishDate() {
        return finishDate;
    }

    public Campaign withFinishDate(String finishDate) {
        this.finishDate = finishDate;
        return this;
    }

    public String getContentLang() {
        return contentLang;
    }

    public Campaign withContentLang(String contentLang) {
        this.contentLang = contentLang;
        return this;
    }

    public Integer getNoExtendedGeotargeting() {
        return noExtendedGeotargeting;
    }

    public void setNoExtendedGeotargeting(Integer noExtendedGeotargeting) {
        this.noExtendedGeotargeting = noExtendedGeotargeting;
    }

    public List<ImageAd> getImageAds() {
        return imageAds;
    }

    public Campaign withPictures(List<ImageAd> imageAds) {
        this.imageAds = imageAds;
        return this;
    }

    public Map<String, ExtendedGeoItem> getExtendedGeoItemsMap() {
        return extendedGeoItemsMap;
    }

    public Campaign withExtendedGeoItemsMap(
            Map<String, ExtendedGeoItem> extendedGeoItemsMap)
    {
        this.extendedGeoItemsMap = extendedGeoItemsMap;
        return this;
    }

    public List<Long> getAbSectionsStatistic() {
        return abSectionsStatistic;
    }

    public void setAbSectionsStatistic(List<Long> abSectionsStatistic) {
        this.abSectionsStatistic = abSectionsStatistic;
    }

    public Campaign withAbSectionsStatistic(List<Long> abSectionsStatistic) {
        this.abSectionsStatistic = abSectionsStatistic;
        return this;
    }

    public List<Long> getAbSegmentsRetargeting() {
        return abSegmentsRetargeting;
    }

    public void setAbSegmentsRetargeting(List<Long> abSegmentsRetargeting) {
        this.abSegmentsRetargeting = abSegmentsRetargeting;
    }

    public Campaign withAbSegmentsRetargeting(List<Long> abSegmentsRetargeting) {
        this.abSegmentsRetargeting = abSegmentsRetargeting;
        return this;
    }
}
