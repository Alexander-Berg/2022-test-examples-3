package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyAgeEnum;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ReportWizardWithFiltersStatRequest extends ReportWizardStatRequest {
    @SerializeKey("fl_campaign_type__eq[]")
    private String filterCampaignType;

    @SerializeKey("fl_campaign__eq[]")
    private String filterCampaignEq;

    @SerializeKey("fl_campaign__ne[]")
    private String filterCampaignNe;

    @SerializeKey("fl_agoalincome__lt")
    private Integer filterGoalIncomeLower;

    @SerializeKey("fl_agoalincome__gt")
    private Integer filterGoalIncomeGreater;

    @SerializeKey("fl_adgroup_id__eq")
    private String filterAdgroupIdEq;

    @SerializeKey("fl_adgroup_id__ne")
    private String filterAdgroupIdNotEq;

    @SerializeKey("fl_adgroup__starts_with")
    private String filterAdgroupStartsWith;

    @SerializeKey("fl_adgroup__not_starts_with")
    private String filterAdgroupNotStartsWith;

    @SerializeKey("fl_banner__eq")
    private String filterBannerEq;

    @SerializeKey("fl_banner__ne")
    private String filterBannerNo;

    @SerializeKey("fl_banner_type__eq")
    private BannerType filterBannerTypeEq;

    @SerializeKey("fl_contexttype__eq[]")
    private DisplayConditionEnum filterDisplayCondition;

    @SerializeKey("fl_phrase__starts_with")
    private String filterPhraseStartsWith;

    @SerializeKey("fl_phrase__not_starts_with")
    private String filterPhraseNotStartsWith;

    @SerializeKey("fl_tags__eq[]")
    private String filterTagsEq;

    @SerializeKey("fl_tags__ne[]")
    private String filterTagsNe;

    @SerializeKey("fl_region__eq")
    private Integer filterTargetingRegion;

    @SerializeKey("fl_physical_region__eq")
    private Integer filterPhysicalRegion;

    @SerializeKey("fl_targettype__eq")
    private TargetTypeEnum filterTargetType;

    @SerializeKey("fl_page__eq")
    private String filterTargetNameEq;

    @SerializeKey("fl_page__ne")
    private String filterTargetNameNe;

    @SerializeKey("fl_ssp__eq")
    private SspEnum filterSspEq;

    @SerializeKey("fl_position__eq[]")
    private PositionEnum filterPosition;

    @SerializeKey("fl_click_place__eq[]")
    private ClickPlaceEnum filterClickPlace;

    @SerializeKey("fl_banner_image_type__eq")
    private ReportWizardImageEnum filterImage;

    @SerializeKey("fl_image_size__eq[]")
    private ImageSizeEnum filterImageSizeEq;

    @SerializeKey("fl_image_size__ne[]")
    private ImageSizeEnum filterImageSizeNe;

    @SerializeKey("fl_device_type__eq[]")
    private DeviceTypeEnum filterDeviceType;

    @SerializeKey("fl_detailed_device_type__eq[]")
    private OperatingSystemEnum filterOperatingSystem;

    @SerializeKey("fl_connection_type__eq[]")
    private ConnectionTypeEnum filterConnectionType;

    @SerializeKey("fl_gender__eq[]")
    private GenderEnum filterGender;

    @SerializeKey("fl_age__eq[]")
    private String filterAge;

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

    @SerializeKey("fl_retargeting__eq[]")
    private String filterRetargetingEqual;

    @SerializeKey("fl_retargeting__ne[]")
    private String filterRetargetingNotEqual;

    @SerializeKey("fl_performance__eq[]")
    private String filterFiltersEqual;

    @SerializeKey("fl_performance__ne[]")
    private String filterFiltersNotEqual;

    @SerializeKey("fl_retargeting_coef__eq[]")
    private String filterRetargetingCoefEqual;

    @SerializeKey("fl_retargeting_coef__ne[]")
    private String filterRetargetingCoefNotEqual;

    @SerializeKey("fl_dynamic__eq[]")
    private String filterDynamicShowConditionEqual;

    @SerializeKey("fl_dynamic__ne[]")
    private String filterDynamicShowConditionNotEqual;

    @SerializeKey("goals")
    private String goals;

    public String getFilterCampaignType() {
        return filterCampaignType;
    }

    public ReportWizardWithFiltersStatRequest withFilterCampaignType(String campaignType) {
        this.filterCampaignType = campaignType;
        return this;
    }

    public String getFilterCampaignEq() {
        return filterCampaignEq;
    }

    public ReportWizardWithFiltersStatRequest withFilterCampaignEq(String campaign) {
        this.filterCampaignEq = campaign;
        return this;
    }

    public String getFilterCampaignNe() {
        return filterCampaignNe;
    }

    public ReportWizardWithFiltersStatRequest withFilterCampaignNe(String campaign) {
        this.filterCampaignNe = campaign;
        return this;
    }

    public Integer getFilterGoalIncomeLower() {
        return filterGoalIncomeLower;
    }

    public ReportWizardWithFiltersStatRequest withFilterGoalIncomeLower(Integer value) {
        this.filterGoalIncomeLower = value;
        return this;
    }

    public Integer getFilterGoalIncomeGreater() {
        return filterGoalIncomeGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterGoalIncomeGreater(Integer value) {
        this.filterGoalIncomeGreater = value;
        return this;
    }

    public String getFilterAdgroupStartsWith() {
        return filterAdgroupStartsWith;
    }

    public ReportWizardWithFiltersStatRequest withFilterAdgroupStartsWith(String adgroup) {
        this.filterAdgroupStartsWith = adgroup;
        return this;
    }

    public String getFilterAdgroupNotStartsWith() {
        return filterAdgroupNotStartsWith;
    }

    public ReportWizardWithFiltersStatRequest withFilterAdgroupNotStartsWith(String adgroup) {
        this.filterAdgroupNotStartsWith = adgroup;
        return this;
    }

    public String getFilterBannerEq() {
        return filterBannerEq;
    }

    public ReportWizardWithFiltersStatRequest withFilterBannerEq(String banner) {
        this.filterBannerEq = banner;
        return this;
    }

    public String getFilterBannerNo() {
        return filterBannerNo;
    }

    public ReportWizardWithFiltersStatRequest withFilterBannerNo(String banner) {
        this.filterBannerNo = banner;
        return this;
    }

    public BannerType getFilterBannerTypeEq() {
        return filterBannerTypeEq;
    }

    public ReportWizardWithFiltersStatRequest withFilterBannerTypeEq(BannerType bannerType) {
        this.filterBannerTypeEq = bannerType;
        return this;
    }

    public DisplayConditionEnum getFilterDisplayCondition() {
        return filterDisplayCondition;
    }

    public ReportWizardWithFiltersStatRequest withFilterDisplayCondition(DisplayConditionEnum condition) {
        this.filterDisplayCondition = condition;
        return this;
    }

    public String getFilterPhraseStartsWith() {
        return filterPhraseStartsWith;
    }

    public ReportWizardWithFiltersStatRequest withFilterPhraseStartsWith(String phrase) {
        this.filterPhraseStartsWith = phrase;
        return this;
    }

    public String getFilterPhraseNotStartsWith() {
        return filterPhraseNotStartsWith;
    }

    public ReportWizardWithFiltersStatRequest withFilterPhraseNotStartsWith(String phrase) {
        this.filterPhraseNotStartsWith = phrase;
        return this;
    }

    public String getFilterTagsEq() {
        return filterTagsEq;
    }

    public ReportWizardWithFiltersStatRequest withFilterTagsEq(String tag) {
        this.filterTagsEq = tag;
        return this;
    }

    public String getFilterTagsNe() {
        return filterTagsNe;
    }

    public ReportWizardWithFiltersStatRequest withFilterTagsNe(String tag) {
        this.filterTagsNe = tag;
        return this;
    }

    public Integer getFilterTargetingRegion() {
        return filterTargetingRegion;
    }

    public ReportWizardWithFiltersStatRequest withFilterTargetingRegion(Integer region) {
        this.filterTargetingRegion = region;
        return this;
    }

    public Integer getFilterPhysicalRegion() {
        return filterPhysicalRegion;
    }

    public ReportWizardWithFiltersStatRequest withFilterPhysicalRegion(Integer region) {
        this.filterPhysicalRegion = region;
        return this;
    }

    public TargetTypeEnum getFilterTargetType() {
        return filterTargetType;
    }

    public ReportWizardWithFiltersStatRequest withFilterTargetType(TargetTypeEnum type) {
        this.filterTargetType = type;
        return this;
    }

    public String getFilterTargetNameEq() {
        return filterTargetNameEq;
    }

    public ReportWizardWithFiltersStatRequest withFilterTargetNameEq(String name) {
        this.filterTargetNameEq = name;
        return this;
    }

    public String getFilterTargetNameNe() {
        return filterTargetNameNe;
    }

    public ReportWizardWithFiltersStatRequest withFilterTargetNameNe(String name) {
        this.filterTargetNameNe = name;
        return this;
    }

    public SspEnum getFilterSspEq() {
        return filterSspEq;
    }

    public ReportWizardWithFiltersStatRequest withFilterSspEq(SspEnum ssp) {
        this.filterSspEq = ssp;
        return this;
    }

    public PositionEnum getFilterPosition() {
        return filterPosition;
    }

    public ReportWizardWithFiltersStatRequest withFilterPosition(PositionEnum position) {
        this.filterPosition = position;
        return this;
    }

    public ClickPlaceEnum getFilterClickPlace() {
        return filterClickPlace;
    }

    public ReportWizardWithFiltersStatRequest withFilterClickPlace(ClickPlaceEnum position) {
        this.filterClickPlace = position;
        return this;
    }

    public ReportWizardImageEnum getFilterImage() {
        return filterImage;
    }

    public ReportWizardWithFiltersStatRequest withFilterImage(ReportWizardImageEnum image) {
        this.filterImage = image;
        return this;
    }

    public ImageSizeEnum getFilterImageSizeEq() {
        return filterImageSizeEq;
    }

    public ReportWizardWithFiltersStatRequest withFilterImageSizeEq(ImageSizeEnum size) {
        this.filterImageSizeEq = size;
        return this;
    }

    public ImageSizeEnum getFilterImageSizeNe() {
        return filterImageSizeNe;
    }

    public ReportWizardWithFiltersStatRequest withFilterImageSizeNe(ImageSizeEnum size) {
        this.filterImageSizeNe = size;
        return this;
    }

    public DeviceTypeEnum getFilterDeviceType() {
        return filterDeviceType;
    }

    public ReportWizardWithFiltersStatRequest withFilterDeviceType(DeviceTypeEnum device) {
        this.filterDeviceType = device;
        return this;
    }

    public OperatingSystemEnum getFilterOperatingSystem() {
        return filterOperatingSystem;
    }

    public ReportWizardWithFiltersStatRequest withFilterOperatingSystem(OperatingSystemEnum system) {
        this.filterOperatingSystem = system;
        return this;
    }

    public ConnectionTypeEnum getFilterConnectionType() {
        return filterConnectionType;
    }

    public ReportWizardWithFiltersStatRequest withFilterConnectionType(ConnectionTypeEnum connection) {
        this.filterConnectionType = connection;
        return this;
    }

    public GenderEnum getFilterGender() {
        return filterGender;
    }

    public ReportWizardWithFiltersStatRequest withFilterGender(GenderEnum gender) {
        this.filterGender = gender;
        return this;
    }

    public String getFilterAge() {
        return filterAge;
    }

    public ReportWizardWithFiltersStatRequest withFilterAge(DemographyAgeEnum age) {
        this.filterAge = age.getKey();
        return this;
    }

    public Integer getFilterGoal() {
        return filterGoal;
    }

    public ReportWizardWithFiltersStatRequest withFilterGoal(Integer goal) {
        this.filterGoal = goal;
        return this;
    }

    public Integer getFilterShowsGreater() {
        return filterShowsGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterShowsGreater(Integer shows) {
        this.filterShowsGreater = shows;
        return this;
    }

    public Integer getFilterShowsLess() {
        return filterShowsLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterShowsLess(Integer shows) {
        this.filterShowsLess = shows;
        return this;
    }

    public Integer getFilterClicksGreater() {
        return filterClicksGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterClicksGreater(Integer clicks) {
        this.filterClicksGreater = clicks;
        return this;
    }

    public Integer getFilterClicksLess() {
        return filterClicksLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterClicksLess(Integer clicks) {
        this.filterClicksLess = clicks;
        return this;
    }

    public Integer getFilterCtrGreater() {
        return filterCtrGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterCtrGreater(Integer ctr) {
        this.filterCtrGreater = ctr;
        return this;
    }

    public Integer getFilterCtrLess() {
        return filterCtrLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterCtrLess(Integer ctr) {
        this.filterCtrLess = ctr;
        return this;
    }

    public Integer getFilterSumGreater() {
        return filterSumGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterSumGreater(Integer sum) {
        this.filterSumGreater = sum;
        return this;
    }

    public Integer getFilterSumLess() {
        return filterSumLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterSumLess(Integer sum) {
        this.filterSumLess = sum;
        return this;
    }

    public Integer getFilterAvSumGreater() {
        return filterAvSumGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterAvSumGreater(Integer sum) {
        this.filterAvSumGreater = sum;
        return this;
    }

    public Integer getFilterAvSumLess() {
        return filterAvSumLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterAvSumLess(Integer sum) {
        this.filterAvSumLess = sum;
        return this;
    }

    public Integer getFilterAvShowsPositionGreater() {
        return filterAvShowsPositionGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterAvShowsPositionGreater(Integer position) {
        this.filterAvShowsPositionGreater = position;
        return this;
    }

    public Integer getFilterAvShowsPositionLess() {
        return filterAvShowsPositionLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterAvShowsPositionLess(Integer position) {
        this.filterAvShowsPositionLess = position;
        return this;
    }

    public Integer getFilterAvClickPositionGreater() {
        return filterAvClickPositionGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterAvClickPositionGreater(Integer position) {
        this.filterAvClickPositionGreater = position;
        return this;
    }

    public Integer getFilterAvClickPositionLess() {
        return filterAvClickPositionLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterAvClickPositionLess(Integer position) {
        this.filterAvClickPositionLess = position;
        return this;
    }

    public Integer getFilterBounceRatioGreater() {
        return filterBounceRatioGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterBounceRatioGreater(Integer ratio) {
        this.filterBounceRatioGreater = ratio;
        return this;
    }

    public Integer getFilterBounceRatioLess() {
        return filterBounceRatioLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterBounceRatioLess(Integer ratio) {
        this.filterBounceRatioLess = ratio;
        return this;
    }

    public Integer getFilterDepthGreater() {
        return filterDepthGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterDepthGreater(Integer depth) {
        this.filterDepthGreater = depth;
        return this;
    }

    public Integer getFilterDepthLess() {
        return filterDepthLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterDepthLess(Integer depth) {
        this.filterDepthLess = depth;
        return this;
    }

    public Integer getFilterConversionGreater() {
        return filterConversionGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterConversionGreater(Integer conversion) {
        this.filterConversionGreater = conversion;
        return this;
    }

    public Integer getFilterConversionLess() {
        return filterConversionLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterConversionLess(Integer conversion) {
        this.filterConversionLess = conversion;
        return this;
    }

    public Integer getFilterConversionsNumGreater() {
        return filterConversionsNumGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterConversionsNumGreater(Integer conversion) {
        this.filterConversionsNumGreater = conversion;
        return this;
    }

    public Integer getFilterConversionsNumLess() {
        return filterConversionsNumLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterConversionsNumLess(Integer conversion) {
        this.filterConversionsNumLess = conversion;
        return this;
    }

    public Integer getFilterGoalCostGreater() {
        return filterGoalCostGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterGoalCostGreater(Integer cost) {
        this.filterGoalCostGreater = cost;
        return this;
    }

    public Integer getFilterGoalCostLess() {
        return filterGoalCostLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterGoalCostLess(Integer cost) {
        this.filterGoalCostLess = cost;
        return this;
    }

    public Integer getFilterProfitabilityGreater() {
        return filterProfitabilityGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterProfitabilityGreater(Integer profit) {
        this.filterProfitabilityGreater = profit;
        return this;
    }

    public Integer getFilterProfitabilityLess() {
        return filterProfitabilityLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterProfitabilityLess(Integer profit) {
        this.filterProfitabilityLess = profit;
        return this;
    }

    public Integer getFilterWinrateGreater() {
        return filterWinrateGreater;
    }

    public ReportWizardWithFiltersStatRequest withFilterWinrateGreater(Integer value) {
        this.filterWinrateGreater = value;
        return this;
    }

    public Integer getFilterWinrateLess() {
        return filterWinrateLess;
    }

    public ReportWizardWithFiltersStatRequest withFilterWinrateLess(Integer value) {
        this.filterWinrateLess = value;
        return this;
    }

    public String getFilterRetargetingEqual() {
        return filterRetargetingEqual;
    }

    public ReportWizardWithFiltersStatRequest withFilterRetargetingEqual(String value) {
        this.filterRetargetingEqual = value;
        return this;
    }

    public String getFilterRetargetingNotEqual() {
        return filterRetargetingNotEqual;
    }

    public ReportWizardWithFiltersStatRequest withFilterRetargetingNotEqual(String value) {
        this.filterRetargetingNotEqual = value;
        return this;
    }

    public String getFilterFiltersEqual() {
        return filterFiltersEqual;
    }

    public ReportWizardWithFiltersStatRequest withFilterFiltersEqual(String value) {
        this.filterFiltersEqual = value;
        return this;
    }

    public String getFilterFiltersNotEqual() {
        return filterFiltersNotEqual;
    }

    public ReportWizardWithFiltersStatRequest withFilterFiltersNotEqual(String value) {
        this.filterFiltersNotEqual = value;
        return this;
    }

    public String getFilterRetargetingCoefEqual() {
        return filterRetargetingCoefEqual;
    }

    public ReportWizardWithFiltersStatRequest withFilterRetargetingCoefEqual(String value) {
        this.filterRetargetingCoefEqual = value;
        return this;
    }

    public String getFilterRetargetingCoefNotEqual() {
        return filterRetargetingCoefNotEqual;
    }

    public ReportWizardWithFiltersStatRequest withFilterRetargetingCoefNotEqual(String value) {
        this.filterRetargetingCoefNotEqual = value;
        return this;
    }

    public String getFilterDynamicShowConditionEqual() {
        return filterDynamicShowConditionEqual;
    }

    public ReportWizardWithFiltersStatRequest withFilterDynamicShowConditionEqual(String value) {
        this.filterDynamicShowConditionEqual = value;
        return this;
    }

    public String getFilterDynamicShowConditionNotEqual() {
        return filterDynamicShowConditionNotEqual;
    }

    public ReportWizardWithFiltersStatRequest withFilterDynamicShowConditionNotEqual(String value) {
        this.filterDynamicShowConditionNotEqual = value;
        return this;
    }

    public String getFilterAdgroupIdEq() {
        return filterAdgroupIdEq;
    }

    public ReportWizardWithFiltersStatRequest withFilterAdgroupIdEq(String filterAdgroupIdEq) {
        this.filterAdgroupIdEq = filterAdgroupIdEq;
        return this;
    }

    public String getFilterAdgroupIdNotEq() {
        return filterAdgroupIdNotEq;
    }

    public ReportWizardWithFiltersStatRequest withFilterAdgroupIdNotEq(String filterAdgroupIdNotEq) {
        this.filterAdgroupIdNotEq = filterAdgroupIdNotEq;
        return this;
    }

    public String getGoals() {
        return goals;
    }

    public ReportWizardWithFiltersStatRequest withGoals(String goals) {
        this.goals = goals;
        return this;
    }
}
