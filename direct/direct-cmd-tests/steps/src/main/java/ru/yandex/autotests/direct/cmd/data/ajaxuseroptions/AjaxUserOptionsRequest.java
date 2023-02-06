package ru.yandex.autotests.direct.cmd.data.ajaxuseroptions;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxUserOptionsRequest extends BasicDirectRequest {
    @SerializeKey("custom_filters")
    private String customFilters;

    @SerializeKey("budget_teaser")
    private String budgetTeaser;

    @SerializeKey("working_holiday_teaser")
    private String workingHolidayTeaser;

    @SerializeKey("phones_teaser")
    private String phonesTeaser;

    @SerializeKey("manager_teaser")
    private String managerTeaser;

    @SerializeKey("optimize_camp")
    private String optimziseCamp;

    @SerializeKey("stat_periods")
    private String statPeriods;

    @SerializeKey("word_suggestions")
    private String wordSuggestions;

    @SerializeKey("show_favorite_campaigns_only")
    private String showFavoriteCampaigsOnly;

    @SerializeKey("show_wide_money_transfer")
    private String showWideMoneyTransfer;

    @SerializeKey("show_extended_client_info")
    private String showExtendedClientInfo;

    @SerializeKey("show_my_campaigns_only")
    private String showMyCampaignsOnly;

    @SerializeKey("toggle")
    private String toggle;

    @SerializeKey("new_camp_agency")
    private String newCampAgency;

    @SerializeKey("forecast_switcher_pos")
    private String forecastSwitcherPos;

    @SerializeKey("android_teaser")
    private String androidTeaser;

    @SerializeKey("multicurrency_teaser")
    private String multicurrencyTeaser;

    @SerializeKey("show_quality_score")
    private String showQualityScore;

    @SerializeKey("hide_recommendations_email_teaser")
    private String hideRecommendationEmailTeaser;

    public String getCustomFilters() {
        return customFilters;
    }

    public AjaxUserOptionsRequest withCustomFilters(String customFilters) {
        this.customFilters = customFilters;
        return this;
    }

    public String getBudgetTeaser() {
        return budgetTeaser;
    }

    public AjaxUserOptionsRequest withBudgetTeaser(String budgetTeaser) {
        this.budgetTeaser = budgetTeaser;
        return this;
    }

    public String getWorkingHolidayTeaser() {
        return workingHolidayTeaser;
    }

    public AjaxUserOptionsRequest withWorkingHolidayTeaser(String workingHolidayTeaser) {
        this.workingHolidayTeaser = workingHolidayTeaser;
        return this;
    }

    public String getPhonesTeaser() {
        return phonesTeaser;
    }

    public AjaxUserOptionsRequest withPhonesTeaser(String phonesTeaser) {
        this.phonesTeaser = phonesTeaser;
        return this;
    }

    public String getManagerTeaser() {
        return managerTeaser;
    }

    public AjaxUserOptionsRequest withManagerTeaser(String managerTeaser) {
        this.managerTeaser = managerTeaser;
        return this;
    }

    public String getOptimziseCamp() {
        return optimziseCamp;
    }

    public AjaxUserOptionsRequest withOptimziseCamp(String optimziseCamp) {
        this.optimziseCamp = optimziseCamp;
        return this;
    }

    public String getStatPeriods() {
        return statPeriods;
    }

    public AjaxUserOptionsRequest withStatPeriods(String statPeriods) {
        this.statPeriods = statPeriods;
        return this;
    }

    public String getWordSuggestions() {
        return wordSuggestions;
    }

    public AjaxUserOptionsRequest withWordSuggestions(String wordSuggestions) {
        this.wordSuggestions = wordSuggestions;
        return this;
    }

    public String getShowFavoriteCampaigsOnly() {
        return showFavoriteCampaigsOnly;
    }

    public AjaxUserOptionsRequest withShowFavoriteCampaigsOnly(String showFavoriteCampaigsOnly) {
        this.showFavoriteCampaigsOnly = showFavoriteCampaigsOnly;
        return this;
    }

    public String getShowWideMoneyTransfer() {
        return showWideMoneyTransfer;
    }

    public AjaxUserOptionsRequest withShowWideMoneyTransfer(String showWideMoneyTransfer) {
        this.showWideMoneyTransfer = showWideMoneyTransfer;
        return this;
    }

    public String getShowExtendedClientInfo() {
        return showExtendedClientInfo;
    }

    public AjaxUserOptionsRequest withShowExtendedClientInfo(String showExtendedClientInfo) {
        this.showExtendedClientInfo = showExtendedClientInfo;
        return this;
    }

    public String getShowMyCampaignsOnly() {
        return showMyCampaignsOnly;
    }

    public AjaxUserOptionsRequest withShiwMyCampaignsOnly(String shiwMyCampaignsOnly) {
        this.showMyCampaignsOnly = shiwMyCampaignsOnly;
        return this;
    }

    public String getToggle() {
        return toggle;
    }

    public AjaxUserOptionsRequest withToggle(String toggle) {
        this.toggle = toggle;
        return this;
    }

    public String getNewCampAgency() {
        return newCampAgency;
    }

    public AjaxUserOptionsRequest withNewCampAgency(String newCampAgency) {
        this.newCampAgency = newCampAgency;
        return this;
    }

    public String getForecastSwitcherPos() {
        return forecastSwitcherPos;
    }

    public AjaxUserOptionsRequest withForecastSwitcherPos(String forecastSwitcherPos) {
        this.forecastSwitcherPos = forecastSwitcherPos;
        return this;
    }

    public String getAndroidTeaser() {
        return androidTeaser;
    }

    public AjaxUserOptionsRequest withAndroidTeaser(String androidTeaser) {
        this.androidTeaser = androidTeaser;
        return this;
    }

    public String getMulticurrencyTeaser() {
        return multicurrencyTeaser;
    }

    public AjaxUserOptionsRequest withMulticurrencyTeaser(String multicurrencyTeaser) {
        this.multicurrencyTeaser = multicurrencyTeaser;
        return this;
    }

    public String getShowQualityScore() {
        return showQualityScore;
    }

    public AjaxUserOptionsRequest withShowQualityScore(String showQualityScore) {
        this.showQualityScore = showQualityScore;
        return this;
    }

    public String getHideRecommendationEmailTeaser() {
        return hideRecommendationEmailTeaser;
    }

    public AjaxUserOptionsRequest withHideRecommendationEmailTeaser(String hideRecommendationEmailTeaser) {
        this.hideRecommendationEmailTeaser = hideRecommendationEmailTeaser;
        return this;
    }
}
