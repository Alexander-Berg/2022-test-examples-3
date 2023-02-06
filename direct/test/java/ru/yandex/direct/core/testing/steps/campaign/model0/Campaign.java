package ru.yandex.direct.core.testing.steps.campaign.model0;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.steps.campaign.model0.context.ContextSettings;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.model.ModelWithId;

/**
 * Кампания
 * (без rf, rf_reset, DoShow)
 */
public class Campaign implements BaseCampaign, ModelWithId {

    public static final ModelProperty<Campaign, Long> ID =
            prop("id", Campaign::getId, Campaign::setId);
    public static final ModelProperty<Campaign, Long> CLIENT_ID =
            prop("clientId", Campaign::getClientId, Campaign::setClientId);
    public static final ModelProperty<Campaign, Long> WALLET_ID =
            prop("walletId", Campaign::getWalletId, Campaign::setWalletId);
    public static final ModelProperty<Campaign, Long> UID =
            prop("uid", Campaign::getUid, Campaign::setUid);
    public static final ModelProperty<Campaign, Long> AGENCY_UID =
            prop("agencyUid", Campaign::getAgencyUid, Campaign::setAgencyUid);
    public static final ModelProperty<Campaign, Long> AGENCY_ID =
            prop("agencyId", Campaign::getAgencyId, Campaign::setAgencyId);
    public static final ModelProperty<Campaign, Long> MANAGER_UID =
            prop("managerUid", Campaign::getManagerUid, Campaign::setManagerUid);
    public static final ModelProperty<Campaign, String> NAME =
            prop("name", Campaign::getName, Campaign::setName);
    public static final ModelProperty<Campaign, CampaignType> TYPE =
            prop("type", Campaign::getType, Campaign::setType);
    public static final ModelProperty<Campaign, Boolean> STATUS_EMPTY =
            prop("statusEmpty", Campaign::getStatusEmpty, Campaign::setStatusEmpty);
    public static final ModelProperty<Campaign, StatusModerate> STATUS_MODERATE =
            prop("statusModerate", Campaign::getStatusModerate, Campaign::setStatusModerate);
    public static final ModelProperty<Campaign, StatusPostModerate> STATUS_POST_MODERATE =
            prop("statusPostModerate", Campaign::getStatusPostModerate, Campaign::setStatusPostModerate);
    public static final ModelProperty<Campaign, Boolean> STATUS_SHOW =
            prop("statusShow", Campaign::getStatusShow, Campaign::setStatusShow);
    public static final ModelProperty<Campaign, Boolean> STATUS_ACTIVE =
            prop("statusActive", Campaign::getStatusActive, Campaign::setStatusActive);
    public static final ModelProperty<Campaign, List<String>> MINUS_KEYWORDS =
            prop("minusKeywords", Campaign::getMinusKeywords, Campaign::setMinusKeywords);
    public static final ModelProperty<Campaign, List<Long>> ALLOWED_PAGE_IDS =
            prop("allowedPageIds", Campaign::getAllowedPageIds, Campaign::setAllowedPageIds);
    public static final ModelProperty<Campaign, List<Long>> DISALLOWED_PAGE_IDS =
            prop("disallowedPageIds", Campaign::getDisallowedPageIds, Campaign::setDisallowedPageIds);
    public static final ModelProperty<Campaign, Boolean> ENABLE_COMPANY_INFO =
            prop("enableCompanyInfo", Campaign::getEnableCompanyInfo, Campaign::setEnableCompanyInfo);
    public static final ModelProperty<Campaign, Boolean> EXCLUDE_PAUSED_COMPETING_ADS =
            prop("excludePausedCompetingAds", Campaign::getExcludePausedCompetingAds,
                    Campaign::setExcludePausedCompetingAds);
    public static final ModelProperty<Campaign, Long> MINUS_KEYWORDS_ID =
            prop("minusKeywordsId", Campaign::getMinusKeywordsId, Campaign::setMinusKeywordsId);
    public static final ModelProperty<Campaign, Long> TIMEZONE_ID =
            prop("timezoneId", Campaign::getTimezoneId, Campaign::setTimezoneId);
    public static final ModelProperty<Campaign, TimeTarget> TIME_TARGET =
            prop("timeTarget", Campaign::getTimeTarget, Campaign::setTimeTarget);
    public static final ModelProperty<Campaign, Set<Integer>> GEO = prop("geo", Campaign::getGeo, Campaign::setGeo);
    public static final ModelProperty<Campaign, Long> ORDER_ID =
            prop("orderId", Campaign::getOrderId, Campaign::setOrderId);
    public static final ModelProperty<Campaign, StatusBsSynced> STATUS_BS_SYNCED =
            prop("statusBsSynced", Campaign::getStatusBsSynced, Campaign::setStatusBsSynced);
    public static final ModelProperty<Campaign, LocalDate> START_TIME =
            prop("startTime", Campaign::getStartTime, Campaign::setStartTime);
    public static final ModelProperty<Campaign, LocalDate> FINISH_TIME =
            prop("finishTime", Campaign::getFinishTime, Campaign::setFinishTime);
    public static final ModelProperty<Campaign, LocalDateTime> LAST_SHOW_TIME =
            prop("lastShowTime", Campaign::getLastShowTime, Campaign::setLastShowTime);
    public static final ModelProperty<Campaign, LocalDateTime> LAST_CHANGE =
            prop("lastChange", Campaign::getLastChange, Campaign::setLastChange);
    public static final ModelProperty<Campaign, List<Long>> METRIKA_COUNTERS =
            prop("metrikaCounters", Campaign::getMetrikaCounters, Campaign::setMetrikaCounters);
    public static final ModelProperty<Campaign, String> BRAND_SURVEY_ID =
            prop("brandSurveyId", Campaign::getBrandSurveyId, Campaign::setBrandSurveyId);
    public static final ModelProperty<Campaign, BroadmatchFlag> BROADMATCH_FLAG =
            prop("broadMatchFlag", Campaign::getBroadMatchFlag, Campaign::setBroadMatchFlag);
    public static final ModelProperty<Campaign, Integer> BROADMATCH_LIMIT =
            prop("broadMatchLimit", Campaign::getBroadMatchLimit, Campaign::setBroadMatchLimit);
    public static final ModelProperty<Campaign, LocalDateTime> AUTOBUDGET_FORECAST_DATE =
            prop("autobudgetForecastDate", Campaign::getAutobudgetForecastDate, Campaign::setAutobudgetForecastDate);
    public static final ModelProperty<Campaign, String> EMAIL =
            prop("email", Campaign::getEmail, Campaign::setEmail);
    public static final ModelProperty<Campaign, Boolean> ARCHIVED =
            prop("archived", Campaign::getArchived, Campaign::setArchived);
    public static final ModelProperty<Campaign, Boolean> STATUS_METRICA_CONTROL =
            prop("statusMetricaControl", Campaign::getStatusMetricaControl, Campaign::setStatusMetricaControl);
    public static final ModelProperty<Campaign, Set<String>> DISABLED_DOMAINS =
            prop("disabledDomains", Campaign::getDisabledDomains, Campaign::setDisabledDomains);
    public static final ModelProperty<Campaign, List<String>> DISABLED_SSP =
            prop("disabledSsp", Campaign::getDisabledSsp, Campaign::setDisabledSsp);
    public static final ModelProperty<Campaign, List<String>> DISABLED_VIDEO_PLACEMENTS =
            prop("disabledVideoPlacements", Campaign::getDisabledVideoPlacements, Campaign::setDisabledVideoPlacements);
    public static final ModelProperty<Campaign, ContextSettings> CONTEXT_SETTINGS =
            prop("contextSettings", Campaign::getContextSettings, Campaign::setContextSettings);
    public static final ModelProperty<Campaign, Long> CLIENT_DIALOG_ID =
            prop("clientDialogId", Campaign::getClientDialogId, Campaign::setClientDialogId);
    public static final ModelProperty<Campaign, Long> DEFAULT_PERMALINK =
            prop("defaultPermalink", Campaign::getDefaultPermalink, Campaign::setDefaultPermalink);
    public static final ModelProperty<Campaign, Boolean> HAS_ADD_METRIKA_TAG_TO_URL =
            prop("hasAddMetrikaTagToUrl", Campaign::getHasAddMetrikaTagToUrl, Campaign::setHasAddMetrikaTagToUrl);
    public static final ModelProperty<Campaign, CampaignSource> SOURCE =
            prop("source", Campaign::getSource, Campaign::setSource);
    public static final ModelProperty<Campaign, CampaignMetatype> METATYPE =
            prop("metatype", Campaign::getMetatype, Campaign::setMetatype);
    public static final ModelProperty<Campaign, Long> MASTER_CID =
            prop("masterCid", Campaign::getMasterCid, Campaign::setMasterCid);

    private static <V> ModelProperty<Campaign, V> prop(String name,
                                                       Function<Campaign, V> getter, BiConsumer<Campaign, V> setter) {
        return ModelProperty.create(Campaign.class, name, getter, setter);
    }

    private Long id;
    private Long clientId;
    private Long walletId;
    private Long uid;
    private Long agencyUid;
    private Long agencyId;
    private Long managerUid;
    private String name;
    private CampaignType type;
    private Boolean statusEmpty;
    private StatusModerate statusModerate;
    private StatusPostModerate statusPostModerate;
    private Boolean statusShow;
    private Boolean statusActive;
    private List<Long> metrikaCounters;
    private String brandSurveyId;
    private Long minusKeywordsId;
    private List<String> minusKeywords;
    private Long timezoneId;
    private TimeTarget timeTarget;
    private List<Long> allowedPageIds;
    private List<Long> disallowedPageIds;
    private Boolean enableCompanyInfo;
    private Boolean excludePausedCompetingAds;
    private Set<String> disabledDomains;
    private List<String> disabledSsp;
    private List<String> disabledVideoPlacements;
    private Set<Integer> geo;
    private String email;
    private Boolean archived;
    private Boolean statusMetricaControl;
    private Long clientDialogId;
    private Long defaultPermalink;
    private Boolean hasAddMetrikaTagToUrl;
    private CampaignSource source;
    private CampaignMetatype metatype;
    private Long masterCid;

    /**
     * Идентификатор заказа из БК
     */
    private Long orderId;

    /**
     * Статус синхронизации кампании с БК
     */
    private StatusBsSynced statusBsSynced;


    private BroadmatchFlag broadMatchFlag;

    private Integer broadMatchLimit;

    /**
     * Дата старта кампании
     */
    private LocalDate startTime;

    /**
     * Дата окончания кампания. Значение '0000-00-00' означает, что дата не указана и показы будут идти
     * до окончания денежных средств на кампании. Имеет смысл только для текстовых кампаний.
     */
    private LocalDate finishTime;

    /**
     * Приблизительное время последнего показа
     */
    private LocalDateTime lastShowTime;

    /**
     * Время последнего изменения
     */
    private LocalDateTime lastChange;

    private BalanceInfo balanceInfo;
    private Strategy strategy;
    private ContextSettings contextSettings;

    private LocalDateTime autobudgetForecastDate;

    public Long getId() {
        return id;
    }

    public void setId(Long campaignId) {
        this.id = campaignId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Long getAgencyUid() {
        return agencyUid;
    }

    public void setAgencyUid(Long agencyUid) {
        this.agencyUid = agencyUid;
    }

    public Long getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(Long agencyId) {
        this.agencyId = agencyId;
    }

    public Long getManagerUid() {
        return managerUid;
    }

    public void setManagerUid(Long managerUid) {
        this.managerUid = managerUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CampaignType getType() {
        return type;
    }

    public void setType(CampaignType type) {
        this.type = type;
    }

    public Boolean getStatusEmpty() {
        return statusEmpty;
    }

    public void setStatusEmpty(Boolean statusEmpty) {
        this.statusEmpty = statusEmpty;
    }

    public StatusModerate getStatusModerate() {
        return statusModerate;
    }

    public void setStatusModerate(StatusModerate statusModerate) {
        this.statusModerate = statusModerate;
    }

    public StatusPostModerate getStatusPostModerate() {
        return statusPostModerate;
    }

    public void setStatusPostModerate(StatusPostModerate statusPostModerate) {
        this.statusPostModerate = statusPostModerate;
    }

    public Boolean getStatusShow() {
        return statusShow;
    }

    public void setStatusShow(Boolean statusShow) {
        this.statusShow = statusShow;
    }

    public Boolean getStatusActive() {
        return statusActive;
    }

    public void setStatusActive(Boolean statusActive) {
        this.statusActive = statusActive;
    }

    public List<String> getMinusKeywords() {
        return minusKeywords;
    }

    public void setMinusKeywords(List<String> minusKeywords) {
        this.minusKeywords = minusKeywords;
    }

    public List<Long> getAllowedPageIds() {
        return allowedPageIds;
    }

    public void setAllowedPageIds(List<Long> allowedPageIds) {
        this.allowedPageIds = allowedPageIds;
    }

    public List<Long> getDisallowedPageIds() {
        return disallowedPageIds;
    }

    public void setDisallowedPageIds(List<Long> disallowedPageIds) {
        this.disallowedPageIds = disallowedPageIds;
    }

    public Boolean getEnableCompanyInfo() {
        return enableCompanyInfo;
    }

    public void setEnableCompanyInfo(Boolean enableCompanyInfo) {
        this.enableCompanyInfo = enableCompanyInfo;
    }

    public Boolean getExcludePausedCompetingAds() {
        return excludePausedCompetingAds;
    }

    public void setExcludePausedCompetingAds(Boolean excludePausedCompetingAds) {
        this.excludePausedCompetingAds = excludePausedCompetingAds;
    }

    public Long getMinusKeywordsId() {
        return minusKeywordsId;
    }

    public void setMinusKeywordsId(Long minusKeywordsId) {
        this.minusKeywordsId = minusKeywordsId;
    }

    public Long getTimezoneId() {
        return timezoneId;
    }

    public void setTimezoneId(Long timezoneId) {
        this.timezoneId = timezoneId;
    }

    public TimeTarget getTimeTarget() {
        return timeTarget;
    }

    public void setTimeTarget(TimeTarget timeTarget) {
        this.timeTarget = timeTarget;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public StatusBsSynced getStatusBsSynced() {
        return statusBsSynced;
    }

    public void setStatusBsSynced(StatusBsSynced statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
    }

    public BroadmatchFlag getBroadMatchFlag() {
        return broadMatchFlag;
    }

    public void setBroadMatchFlag(BroadmatchFlag flag) {
        this.broadMatchFlag = flag;
    }

    public Integer getBroadMatchLimit() {
        return broadMatchLimit;
    }

    public void setBroadMatchLimit(Integer broadMatchLimit) {
        this.broadMatchLimit = broadMatchLimit;
    }

    public LocalDate getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDate startTime) {
        this.startTime = startTime;
    }

    public LocalDate getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDate finishTime) {
        this.finishTime = finishTime;
    }

    public LocalDateTime getLastShowTime() {
        return lastShowTime;
    }

    public void setLastShowTime(LocalDateTime lastShowTime) {
        this.lastShowTime = lastShowTime;
    }

    public LocalDateTime getLastChange() {
        return lastChange;
    }

    public void setLastChange(LocalDateTime lastChange) {
        this.lastChange = lastChange;
    }

    public BalanceInfo getBalanceInfo() {
        return balanceInfo;
    }

    public void setBalanceInfo(BalanceInfo balanceInfo) {
        this.balanceInfo = balanceInfo;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public ContextSettings getContextSettings() {
        return contextSettings;
    }

    public void setContextSettings(ContextSettings contextSettings) {
        this.contextSettings = contextSettings;
    }

    public List<Long> getMetrikaCounters() {
        return metrikaCounters;
    }

    public void setMetrikaCounters(List<Long> metrikaCounters) {
        this.metrikaCounters = metrikaCounters;
    }

    public String getBrandSurveyId() {
        return brandSurveyId;
    }

    public void setBrandSurveyId(String brandSurveyId) {
        this.brandSurveyId = brandSurveyId;
    }

    public Campaign withBrandSurveyId(String brandSurveyId) {
        setBrandSurveyId(brandSurveyId);
        return this;
    }

    public LocalDateTime getAutobudgetForecastDate() {
        return autobudgetForecastDate;
    }

    public void setAutobudgetForecastDate(LocalDateTime autobudgetForecastDate) {
        this.autobudgetForecastDate = autobudgetForecastDate;
    }

    public Long getClientDialogId() {
        return clientDialogId;
    }

    public void setClientDialogId(Long clientDialogId) {
        this.clientDialogId = clientDialogId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Campaign withId(Long id) {
        this.id = id;
        return this;
    }

    public Campaign withClientId(Long clientId) {
        this.clientId = clientId;
        return this;
    }

    public Campaign withWalletId(Long walletId) {
        this.walletId = walletId;
        return this;
    }

    public Campaign withUid(Long uid) {
        this.uid = uid;
        return this;
    }

    public Campaign withAgencyUid(Long agencyUid) {
        this.agencyUid = agencyUid;
        return this;
    }

    public Campaign withAgencyId(Long agencyId) {
        this.agencyId = agencyId;
        return this;
    }

    public Campaign withManagerUid(Long managerUid) {
        this.managerUid = managerUid;
        return this;
    }

    public Campaign withName(String name) {
        this.name = name;
        return this;
    }

    public Campaign withType(CampaignType type) {
        this.type = type;
        return this;
    }

    public Campaign withStatusEmpty(Boolean statusEmpty) {
        this.statusEmpty = statusEmpty;
        return this;
    }

    public Campaign withStatusModerate(StatusModerate statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public Campaign withStatusPostModerate(StatusPostModerate statusPostModerate) {
        this.statusPostModerate = statusPostModerate;
        return this;
    }

    public Campaign withStatusShow(Boolean statusShow) {
        this.statusShow = statusShow;
        return this;
    }

    public Campaign withStatusActive(Boolean statusActive) {
        this.statusActive = statusActive;
        return this;
    }

    public Campaign withMinusKeywords(List<String> minusKeywords) {
        setMinusKeywords(minusKeywords);
        return this;
    }

    public Campaign withAllowedPageIds(List<Long> allowedPageIds) {
        setAllowedPageIds(allowedPageIds);
        return this;
    }

    public Campaign withDisAllowedPageIds(List<Long> disallowedPageIds) {
        setDisallowedPageIds(disallowedPageIds);
        return this;
    }

    public Campaign withEnableCompanyInfo(Boolean enableCompanyInfo) {
        setEnableCompanyInfo(enableCompanyInfo);
        return this;
    }

    public Campaign withExcludePausedCompetingAds(Boolean excludePausedCompetingAds) {
        setExcludePausedCompetingAds(excludePausedCompetingAds);
        return this;
    }

    public Campaign withMinusKeywordsId(Long minusKeywordsId) {
        setMinusKeywordsId(minusKeywordsId);
        return this;
    }

    public Campaign withTimezoneId(Long timezoneId) {
        setTimezoneId(timezoneId);
        return this;
    }

    public Campaign withTimeTarget(TimeTarget timeTarget) {
        setTimeTarget(timeTarget);
        return this;
    }

    public Campaign withOrderId(Long orderId) {
        this.orderId = orderId;
        return this;
    }

    public Campaign withStatusBsSynced(StatusBsSynced statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
        return this;
    }

    public Campaign withBroadmatchFlag(BroadmatchFlag flag) {
        this.broadMatchFlag = flag;
        return this;
    }

    public Campaign withBroadMatchLimit(Integer broadMatchLimit) {
        this.broadMatchLimit = broadMatchLimit;
        return this;
    }

    public Campaign withStartTime(LocalDate startTime) {
        this.startTime = startTime;
        return this;
    }

    public Campaign withFinishTime(LocalDate finishTime) {
        this.finishTime = finishTime;
        return this;
    }

    public Campaign withLastShowTime(LocalDateTime lastShowTime) {
        this.lastShowTime = lastShowTime;
        return this;
    }

    public Campaign withLastChange(LocalDateTime lastChange) {
        this.lastChange = lastChange;
        return this;
    }

    public Campaign withBalanceInfo(BalanceInfo balanceInfo) {
        this.balanceInfo = balanceInfo;
        return this;
    }

    public Campaign withStrategy(Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public Campaign withContextSettings(
            ContextSettings contextSettings) {
        this.contextSettings = contextSettings;
        return this;
    }

    public Campaign withMetrikaCounters(List<Long> metrikaCounters) {
        this.metrikaCounters = metrikaCounters;
        return this;
    }

    public Campaign withAutobudgetForecastDate(LocalDateTime autobudgetForecastDate) {
        this.autobudgetForecastDate = autobudgetForecastDate;
        return this;
    }

    public Campaign withEmail(String email) {
        this.email = email;
        return this;
    }

    public Campaign withClientDialogId(Long clientDialogId) {
        this.clientDialogId = clientDialogId;
        return this;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Campaign withArchived(Boolean archived) {
        this.archived = archived;
        return this;
    }

    public Boolean getStatusMetricaControl() {
        return statusMetricaControl;
    }

    public void setStatusMetricaControl(Boolean statusMetricaControl) {
        this.statusMetricaControl = statusMetricaControl;
    }

    public Campaign withStatusMetricaControl(Boolean statusMetricaControl) {
        this.statusMetricaControl = statusMetricaControl;
        return this;
    }

    public Set<String> getDisabledDomains() {
        return disabledDomains;
    }

    public void setDisabledDomains(Set<String> disabledDomains) {
        this.disabledDomains = disabledDomains;
    }

    public Campaign withDisabledDomains(Set<String> disabledDomains) {
        setDisabledDomains(disabledDomains);
        return this;
    }

    public List<String> getDisabledSsp() {
        return disabledSsp;
    }

    public void setDisabledSsp(List<String> disabledSsp) {
        this.disabledSsp = disabledSsp;
    }

    public Campaign withDisabledSsp(List<String> disabledSsp) {
        setDisabledSsp(disabledSsp);
        return this;
    }

    public List<String> getDisabledVideoPlacements() {
        return disabledVideoPlacements;
    }

    public void setDisabledVideoPlacements(List<String> disabledVideoPlacements) {
        this.disabledVideoPlacements = disabledVideoPlacements;
    }

    public Campaign withDisabledVideoPlacements(List<String> disabledVideoPlacements) {
        setDisabledVideoPlacements(disabledVideoPlacements);
        return this;
    }

    public Set<Integer> getGeo() {
        return geo;
    }

    public void setGeo(Set<Integer> geo) {
        this.geo = geo;
    }

    public Campaign withGeo(Set<Integer> geo) {
        setGeo(geo);
        return this;
    }

    public Long getDefaultPermalink() {
        return defaultPermalink;
    }

    public void setDefaultPermalink(Long defaultPermalink) {
        this.defaultPermalink = defaultPermalink;
    }

    public Campaign withDefaultPermalink(Long defaultPermalink) {
        setDefaultPermalink(defaultPermalink);
        return this;
    }

    public Boolean getHasAddMetrikaTagToUrl() {
        return hasAddMetrikaTagToUrl;
    }

    public void setHasAddMetrikaTagToUrl(Boolean hasAddMetrikaTagToUrl) {
        this.hasAddMetrikaTagToUrl = hasAddMetrikaTagToUrl;
    }

    public Campaign withHasAddMetrikaTagToUrl(Boolean hasAddMetrikaTagToUrl) {
        setHasAddMetrikaTagToUrl(hasAddMetrikaTagToUrl);
        return this;
    }

    public CampaignSource getSource() {
        return source;
    }

    public void setSource(CampaignSource source) {
        this.source = source;
    }

    public Campaign withSource(CampaignSource source) {
        setSource(source);
        return this;
    }

    public CampaignMetatype getMetatype() {
        return metatype;
    }

    public void setMetatype(CampaignMetatype metatype) {
        this.metatype = metatype;
    }

    public Campaign withMetatype(CampaignMetatype metatype) {
        setMetatype(metatype);
        return this;
    }

    public Long getMasterCid() {
        return masterCid;
    }

    public void setMasterCid(Long masterCid) {
        this.masterCid = masterCid;
    }

    public Campaign withMasterCid(Long masterCid) {
        setMasterCid(masterCid);
        return this;
    }
}
