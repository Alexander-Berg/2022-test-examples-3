package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SearchQueriesWithFiltersStatRequest extends ReportWizardStatRequest {
    @SerializeKey("fl_campaign_type__eq[]")
    private String filterCampaignType;

    @SerializeKey("fl_campaign__eq[]")
    private String filterCampaignEq;

    @SerializeKey("fl_campaign__ne[]")
    private String filterCampaignNe;

    @SerializeKey("fl_adgroup__starts_with")
    private String filterAdgroupStartsWith;

    @SerializeKey("fl_adgroup__not_starts_with")
    private String filterAdgroupNotStartsWith;

    @SerializeKey("fl_banner__eq")
    private String filterBannerEq;

    @SerializeKey("fl_banner__ne")
    private String filterBannerNo;

    @SerializeKey("fl_search_query__starts_with")
    private String filterSearchQueryStartsWith;

    @SerializeKey("fl_search_query__not_starts_with")
    private String filterSearchQueryNotStartsWith;

    @SerializeKey("fl_search_query_status__eq[]")
    private QueryStatusEnum filterSearchQueryStatus;

    @SerializeKey("fl_contexttype__eq[]")
    private DisplayConditionEnum filterDisplayCondition;

    @SerializeKey("fl_ext_phrase_status__eq[]")
    private PhraseStatusEnum filterExtPhraseStatus;

    @SerializeKey("fl_phrase__starts_with")
    private String filterPhraseStartsWith;

    @SerializeKey("fl_phrase__not_starts_with")
    private String filterPhraseNotStartsWith;

    @SerializeKey("fl_dynamic__eq[]")
    private String filterDynamicShowConditionEqual;

    @SerializeKey("fl_dynamic__ne[]")
    private String filterDynamicShowConditionNotEqual;

    @SerializeKey("fl_goal_id__eq")
    private Integer filterGoal;

    @SerializeKey("fl_shows__gt")
    private Integer filterShowsGreater;

    @SerializeKey("fl_shows__lt")
    private Integer filterShowsLess;

    @SerializeKey("fl_clicks__gt")
    private Integer filterClicksGreater;

    @SerializeKey("fl_clicks__lt")
    private Integer filterClicksLess;

    @SerializeKey("fl_ctr__gt")
    private Integer filterCtrGreater;

    @SerializeKey("fl_ctr__lt")
    private Integer filterCtrLess;

    @SerializeKey("fl_sum__gt")
    private Integer filterSumGreater;

    @SerializeKey("fl_sum__lt")
    private Integer filterSumLess;

    @SerializeKey("fl_av_sum__gt")
    private Integer filterAvSumGreater;

    @SerializeKey("fl_av_sum__lt")
    private Integer filterAvSumLess;

    @SerializeKey("fl_fp_shows_avg_pos__gt")
    private Integer filterAvShowsPositionGreater;

    @SerializeKey("fl_fp_shows_avg_pos__lt")
    private Integer filterAvShowsPositionLess;

    @SerializeKey("fl_fp_clicks_avg_pos__gt")
    private Integer filterAvClickPositionGreater;

    @SerializeKey("fl_fp_clicks_avg_pos__lt")
    private Integer filterAvClickPositionLess;

    @SerializeKey("fl_bounce_ratio__gt")
    private Integer filterBounceRatioGreater;

    @SerializeKey("fl_bounce_ratio__lt")
    private Integer filterBounceRatioLess;

    @SerializeKey("fl_adepth__gt")
    private Integer filterDepthGreater;

    @SerializeKey("fl_adepth__lt")
    private Integer filterDepthLess;

    @SerializeKey("fl_aconv__gt")
    private Integer filterConversionGreater;

    @SerializeKey("fl_aconv__lt")
    private Integer filterConversionLess;

    @SerializeKey("fl_agoalnum__gt")
    private Integer filterConversionsNumGreater;

    @SerializeKey("fl_agoalnum__lt")
    private Integer filterConversionsNumLess;

    @SerializeKey("fl_agoalcost__gt")
    private Integer filterGoalCostGreater;

    @SerializeKey("fl_agoalcost__lt")
    private Integer filterGoalCostLess;

    @SerializeKey("fl_agoalroi__gt")
    private Integer filterProfitabilityGreater;

    @SerializeKey("fl_agoalroi__lt")
    private Integer filterProfitabilityLess;

    @SerializeKey("fl_winrate__gt")
    private Integer filterWinrateGreater;

    @SerializeKey("fl_winrate__lt")
    private Integer filterWinrateLess;

    public String getFilterCampaignType() {
        return filterCampaignType;
    }

    public SearchQueriesWithFiltersStatRequest withFilterCampaignType(String campaignType) {
        this.filterCampaignType = campaignType;
        return this;
    }

    public String getFilterCampaignEq() {
        return filterCampaignEq;
    }

    public SearchQueriesWithFiltersStatRequest withFilterCampaignEq(String campaign) {
        this.filterCampaignEq = campaign;
        return this;
    }

    public String getFilterCampaignNe() {
        return filterCampaignNe;
    }

    public SearchQueriesWithFiltersStatRequest withFilterCampaignNe(String campaign) {
        this.filterCampaignNe = campaign;
        return this;
    }


    public String getFilterAdgroupStartsWith() {
        return filterAdgroupStartsWith;
    }

    public SearchQueriesWithFiltersStatRequest withFilterAdgroupStartsWith(String adgroup) {
        this.filterAdgroupStartsWith = adgroup;
        return this;
    }

    public String getFilterAdgroupNotStartsWith() {
        return filterAdgroupNotStartsWith;
    }

    public SearchQueriesWithFiltersStatRequest withFilterAdgroupNotStartsWith(String adgroup) {
        this.filterAdgroupNotStartsWith = adgroup;
        return this;
    }

    public String getFilterBannerEq() {
        return filterBannerEq;
    }

    public SearchQueriesWithFiltersStatRequest withFilterBannerEq(String banner) {
        this.filterBannerEq = banner;
        return this;
    }

    public String getFilterBannerNo() {
        return filterBannerNo;
    }

    public SearchQueriesWithFiltersStatRequest withFilterBannerNo(String banner) {
        this.filterBannerNo = banner;
        return this;
    }


    public String getFilterSearchQueryStartsWith() {
        return filterSearchQueryStartsWith;
    }

    public SearchQueriesWithFiltersStatRequest withFilterSearchQueryStartsWith(String query) {
        this.filterSearchQueryStartsWith = query;
        return this;
    }

    public String getFilterSearchQueryNotStartsWith() {
        return filterSearchQueryNotStartsWith;
    }

    public SearchQueriesWithFiltersStatRequest withFilterSearchQueryNotStartsWith(String query) {
        this.filterSearchQueryNotStartsWith = query;
        return this;
    }

    public QueryStatusEnum getFilterSearchQueryStatus() {
        return filterSearchQueryStatus;
    }

    public SearchQueriesWithFiltersStatRequest withFilterSearchQueryStatus(QueryStatusEnum queryStatus) {
        this.filterSearchQueryStatus = queryStatus;
        return this;
    }

    public DisplayConditionEnum getFilterDisplayCondition() {
        return filterDisplayCondition;
    }

    public SearchQueriesWithFiltersStatRequest withFilterDisplayCondition(DisplayConditionEnum condition) {
        this.filterDisplayCondition = condition;
        return this;
    }

    public PhraseStatusEnum getFilterExtPhraseStatus() {
        return filterExtPhraseStatus;
    }

    public SearchQueriesWithFiltersStatRequest withFilterExtPhraseStatus(PhraseStatusEnum phraseStatus) {
        this.filterExtPhraseStatus = phraseStatus;
        return this;
    }

    public String getFilterPhraseStartsWith() {
        return filterPhraseStartsWith;
    }

    public SearchQueriesWithFiltersStatRequest withFilterPhraseStartsWith(String phrase) {
        this.filterPhraseStartsWith = phrase;
        return this;
    }

    public String getFilterPhraseNotStartsWith() {
        return filterPhraseNotStartsWith;
    }

    public SearchQueriesWithFiltersStatRequest withFilterPhraseNotStartsWith(String phrase) {
        this.filterPhraseNotStartsWith = phrase;
        return this;
    }


    public Integer getFilterGoal() {
        return filterGoal;
    }

    public SearchQueriesWithFiltersStatRequest withFilterGoal(Integer goal) {
        this.filterGoal = goal;
        return this;
    }

    public Integer getFilterShowsGreater() {
        return filterShowsGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterShowsGreater(Integer shows) {
        this.filterShowsGreater = shows;
        return this;
    }

    public Integer getFilterShowsLess() {
        return filterShowsLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterShowsLess(Integer shows) {
        this.filterShowsLess = shows;
        return this;
    }

    public Integer getFilterClicksGreater() {
        return filterClicksGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterClicksGreater(Integer clicks) {
        this.filterClicksGreater = clicks;
        return this;
    }

    public Integer getFilterClicksLess() {
        return filterClicksLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterClicksLess(Integer clicks) {
        this.filterClicksLess = clicks;
        return this;
    }

    public Integer getFilterCtrGreater() {
        return filterCtrGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterCtrGreater(Integer ctr) {
        this.filterCtrGreater = ctr;
        return this;
    }

    public Integer getFilterCtrLess() {
        return filterCtrLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterCtrLess(Integer ctr) {
        this.filterCtrLess = ctr;
        return this;
    }

    public Integer getFilterSumGreater() {
        return filterSumGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterSumGreater(Integer sum) {
        this.filterSumGreater = sum;
        return this;
    }

    public Integer getFilterSumLess() {
        return filterSumLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterSumLess(Integer sum) {
        this.filterSumLess = sum;
        return this;
    }

    public Integer getFilterAvSumGreater() {
        return filterAvSumGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterAvSumGreater(Integer sum) {
        this.filterAvSumGreater = sum;
        return this;
    }

    public Integer getFilterAvSumLess() {
        return filterAvSumLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterAvSumLess(Integer sum) {
        this.filterAvSumLess = sum;
        return this;
    }

    public Integer getFilterAvShowsPositionGreater() {
        return filterAvShowsPositionGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterAvShowsPositionGreater(Integer position) {
        this.filterAvShowsPositionGreater = position;
        return this;
    }

    public Integer getFilterAvShowsPositionLess() {
        return filterAvShowsPositionLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterAvShowsPositionLess(Integer position) {
        this.filterAvShowsPositionLess = position;
        return this;
    }

    public Integer getFilterAvClickPositionGreater() {
        return filterAvClickPositionGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterAvClickPositionGreater(Integer position) {
        this.filterAvClickPositionGreater = position;
        return this;
    }

    public Integer getFilterAvClickPositionLess() {
        return filterAvClickPositionLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterAvClickPositionLess(Integer position) {
        this.filterAvClickPositionLess = position;
        return this;
    }

    public Integer getFilterBounceRatioGreater() {
        return filterBounceRatioGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterBounceRatioGreater(Integer ratio) {
        this.filterBounceRatioGreater = ratio;
        return this;
    }

    public Integer getFilterBounceRatioLess() {
        return filterBounceRatioLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterBounceRatioLess(Integer ratio) {
        this.filterBounceRatioLess = ratio;
        return this;
    }

    public Integer getFilterDepthGreater() {
        return filterDepthGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterDepthGreater(Integer depth) {
        this.filterDepthGreater = depth;
        return this;
    }

    public Integer getFilterDepthLess() {
        return filterDepthLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterDepthLess(Integer depth) {
        this.filterDepthLess = depth;
        return this;
    }

    public Integer getFilterConversionGreater() {
        return filterConversionGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterConversionGreater(Integer conversion) {
        this.filterConversionGreater = conversion;
        return this;
    }

    public Integer getFilterConversionLess() {
        return filterConversionLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterConversionLess(Integer conversion) {
        this.filterConversionLess = conversion;
        return this;
    }

    public Integer getFilterConversionsNumGreater() {
        return filterConversionsNumGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterConversionsNumGreater(Integer conversion) {
        this.filterConversionsNumGreater = conversion;
        return this;
    }

    public Integer getFilterConversionsNumLess() {
        return filterConversionsNumLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterConversionsNumLess(Integer conversion) {
        this.filterConversionsNumLess = conversion;
        return this;
    }

    public Integer getFilterGoalCostGreater() {
        return filterGoalCostGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterGoalCostGreater(Integer cost) {
        this.filterGoalCostGreater = cost;
        return this;
    }

    public Integer getFilterGoalCostLess() {
        return filterGoalCostLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterGoalCostLess(Integer cost) {
        this.filterGoalCostLess = cost;
        return this;
    }

    public Integer getFilterProfitabilityGreater() {
        return filterProfitabilityGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterProfitabilityGreater(Integer profit) {
        this.filterProfitabilityGreater = profit;
        return this;
    }

    public Integer getFilterProfitabilityLess() {
        return filterProfitabilityLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterProfitabilityLess(Integer profit) {
        this.filterProfitabilityLess = profit;
        return this;
    }

    public Integer getFilterWinrateGreater() {
        return filterWinrateGreater;
    }

    public SearchQueriesWithFiltersStatRequest withFilterWinrateGreater(Integer value) {
        this.filterWinrateGreater = value;
        return this;
    }

    public Integer getFilterWinrateLess() {
        return filterWinrateLess;
    }

    public SearchQueriesWithFiltersStatRequest withFilterWinrateLess(Integer value) {
        this.filterWinrateLess = value;
        return this;
    }

    public String getFilterDynamicShowConditionEqual() {
        return filterDynamicShowConditionEqual;
    }

    public SearchQueriesWithFiltersStatRequest withFilterDynamicShowConditionEqual(String value) {
        this.filterDynamicShowConditionEqual = value;
        return this;
    }

    public String getFilterDynamicShowConditionNotEqual() {
        return filterDynamicShowConditionNotEqual;
    }

    public SearchQueriesWithFiltersStatRequest withFilterDynamicShowConditionNotEqual(String value) {
        this.filterDynamicShowConditionNotEqual = value;
        return this;
    }
}
