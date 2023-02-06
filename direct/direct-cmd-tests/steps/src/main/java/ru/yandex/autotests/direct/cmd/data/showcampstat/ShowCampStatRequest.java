package ru.yandex.autotests.direct.cmd.data.showcampstat;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.utils.model.PerlBoolean;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ShowCampStatRequest extends BasicDirectRequest {

    public static ShowCampStatRequest defaultCampStatRequest(Long cid, String uLogin) {
        return new ShowCampStatRequest().withCid(String.valueOf(cid)).withIsStat(StatEnum.STAT_ON)
                .withPhraseDate(PerlBoolean.YES)
                .withUlogin(uLogin);
    }

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("phrasedate")
    private PerlBoolean phraseDate;

    @SerializeKey("isStat")
    private StatEnum isStat;

    @SerializeKey("y1")
    private String y1;

    @SerializeKey("m1")
    private String m1;

    @SerializeKey("d1")
    private String d1;

    @SerializeKey("y2")
    private String y2;

    @SerializeKey("m2")
    private String m2;

    @SerializeKey("d2")
    private String d2;

    @SerializeKey("sort")
    private StatSortEnum sort;

    @SerializeKey("save_nds")
    private PerlBoolean saveNds;

    @SerializeKey("show_banners_stat")
    private PerlBoolean showBannersStat;

    @SerializeKey("offline_stat")
    private PerlBoolean offlineStat;

    @SerializeKey("stat_type")
    private StatTypeEnum statType;

    @SerializeKey("online_stat")
    private PerlBoolean onlineStat;

    @SerializeKey("reverse")
    private PerlBoolean reverse;

    @SerializeKey("goals")
    private String goals;

    @SerializeKey("group")
    private StatGroupEnum group;

    @SerializeKey("showstat_button")
    private PerlBoolean showstatButton;

    @SerializeKey("group_by")
    private StatGroupByEnum groupBy;

    @SerializeKey("target_all")
    private PerlBoolean targetAll;

    @SerializeKey("target_0")
    private PerlBoolean target0;

    @SerializeKey("target_1")
    private PerlBoolean target1;

    @SerializeKey("detail")
    private PerlBoolean detail;

    @SerializeKey("with_nds")
    private PerlBoolean withNds;

    @SerializeKey("with_discount")
    private PerlBoolean withDiscount;

    @SerializeKey("onpage")
    private StatOnpageEnum onpage;

    @SerializeKey("with_auto_added_phrases")
    private PerlBoolean withAutoAddedPhrases;

    @SerializeKey("with_avg_position")
    private PerlBoolean withAvgPosition;

    @SerializeKey("filter_banner")
    private String filterBanner;

    @SerializeKey("xls")
    private PerlBoolean xls;

    @SerializeKey("types")
    private TypesEnum types;

    @SerializeKey("target_type")
    private TargetTypeEnum targetType;

    @SerializeKey("use_page_id")
    private PerlBoolean usePageId;

    public StatEnum getIsStat() {
        return isStat;
    }

    public ShowCampStatRequest withIsStat(StatEnum isStat) {
        this.isStat = isStat;
        return this;
    }

    public PerlBoolean getPhraseDate() {
        return phraseDate;
    }

    public ShowCampStatRequest withPhraseDate(PerlBoolean phraseDate) {
        this.phraseDate = phraseDate;
        return this;
    }

    public String getCid() {
        return cid;

    }

    public ShowCampStatRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public String getY1() {
        return y1;
    }

    public ShowCampStatRequest withY1(String y1) {
        this.y1 = y1;
        return this;
    }

    public String getM1() {
        return m1;
    }

    public ShowCampStatRequest withM1(String m1) {
        this.m1 = m1;
        return this;
    }

    public String getD1() {
        return d1;
    }

    public ShowCampStatRequest withD1(String d1) {
        this.d1 = d1;
        return this;
    }

    public String getY2() {
        return y2;
    }

    public ShowCampStatRequest withY2(String y2) {
        this.y2 = y2;
        return this;
    }

    public String getM2() {
        return m2;
    }

    public ShowCampStatRequest withM2(String m2) {
        this.m2 = m2;
        return this;
    }

    public String getD2() {
        return d2;
    }

    public ShowCampStatRequest withD2(String d2) {
        this.d2 = d2;
        return this;
    }

    public StatSortEnum getSort() {
        return sort;
    }

    public ShowCampStatRequest withSort(StatSortEnum sort) {
        this.sort = sort;
        return this;
    }

    public PerlBoolean getSaveNds() {
        return saveNds;
    }

    public ShowCampStatRequest withSaveNds(PerlBoolean saveNds) {
        this.saveNds = saveNds;
        return this;
    }

    public PerlBoolean getShowBannersStat() {
        return showBannersStat;
    }

    public ShowCampStatRequest withShowBannersStat(PerlBoolean showBannersStat) {
        this.showBannersStat = showBannersStat;
        return this;
    }

    public PerlBoolean getOfflineStat() {
        return offlineStat;
    }

    public ShowCampStatRequest withOfflineStat(PerlBoolean offlineStat) {
        this.offlineStat = offlineStat;
        return this;
    }

    public StatTypeEnum getStatType() {
        return statType;
    }

    public ShowCampStatRequest withStatType(StatTypeEnum statType) {
        this.statType = statType;
        return this;
    }

    public PerlBoolean getOnlineStat() {
        return onlineStat;
    }

    public ShowCampStatRequest withOnlineStat(PerlBoolean onlineStat) {
        this.onlineStat = onlineStat;
        return this;
    }

    public PerlBoolean getReverse() {
        return reverse;
    }

    public ShowCampStatRequest withReverse(PerlBoolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public String getGoals() {
        return goals;
    }

    public ShowCampStatRequest withGoals(String goals) {
        this.goals = goals;
        return this;
    }

    public StatGroupEnum getGroup() {
        return group;
    }

    public ShowCampStatRequest withGroup(StatGroupEnum group) {
        this.group = group;
        return this;
    }

    public PerlBoolean getShowstatButton() {
        return showstatButton;
    }

    public ShowCampStatRequest withShowstatButton(PerlBoolean showstatButton) {
        this.showstatButton = showstatButton;
        return this;
    }

    public StatGroupByEnum getGroupBy() {
        return groupBy;
    }

    public ShowCampStatRequest withGroupBy(StatGroupByEnum groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public PerlBoolean getTargetAll() {
        return targetAll;
    }

    public ShowCampStatRequest withTargetAll(PerlBoolean targetAll) {
        this.targetAll = targetAll;
        return this;
    }

    public PerlBoolean getDetail() {
        return detail;
    }

    public ShowCampStatRequest withDetail(PerlBoolean detail) {
        this.detail = detail;
        return this;
    }

    public PerlBoolean getWithNds() {
        return withNds;
    }

    public ShowCampStatRequest withNds(PerlBoolean withNds) {
        this.withNds = withNds;
        return this;
    }

    public PerlBoolean getTarget0() {
        return target0;
    }

    public ShowCampStatRequest withTarget0(PerlBoolean target0) {
        this.target0 = target0;
        return this;
    }

    public PerlBoolean getTarget1() {
        return target1;
    }

    public ShowCampStatRequest withTarget1(PerlBoolean target1) {
        this.target1 = target1;
        return this;
    }

    public PerlBoolean getWithDiscount() {
        return withDiscount;
    }

    public ShowCampStatRequest withDiscount(PerlBoolean withDiscount) {
        this.withDiscount = withDiscount;
        return this;
    }

    public StatOnpageEnum getOnpage() {
        return onpage;
    }

    public ShowCampStatRequest withOnpage(StatOnpageEnum onpage) {
        this.onpage = onpage;
        return this;
    }

    public PerlBoolean getWithAutoAddedPhrases() {
        return withAutoAddedPhrases;
    }

    public ShowCampStatRequest withAutoAddedPhrases(PerlBoolean withAutoAddedPhrases) {
        this.withAutoAddedPhrases = withAutoAddedPhrases;
        return this;
    }

    public PerlBoolean getWithAvgPosition() {
        return withAvgPosition;
    }

    public ShowCampStatRequest withAvgPosition(PerlBoolean withAvgPosition) {
        this.withAvgPosition = withAvgPosition;
        return this;
    }

    public String getFilterBanner() {
        return filterBanner;
    }

    public ShowCampStatRequest withFilterBanner(String filterBanner) {
        this.filterBanner = filterBanner;
        return this;
    }

    public ShowCampStatRequest withXls(PerlBoolean xls) {
        this.xls = xls;
        return this;
    }

    public PerlBoolean getXls() {
        return xls;
    }

    public TypesEnum getTypes() {
        return types;
    }

    public ShowCampStatRequest withTypes(TypesEnum types) {
        this.types = types;
        return this;
    }

    public TargetTypeEnum getTargetType() {
        return targetType;
    }

    public ShowCampStatRequest withTargetType(TargetTypeEnum targetType) {
        this.targetType = targetType;
        return this;
    }

    public PerlBoolean getUsePageId() {
        return usePageId;
    }

    public ShowCampStatRequest withUsePageId(PerlBoolean usePageId) {
        this.usePageId = usePageId;
        return this;
    }
}
