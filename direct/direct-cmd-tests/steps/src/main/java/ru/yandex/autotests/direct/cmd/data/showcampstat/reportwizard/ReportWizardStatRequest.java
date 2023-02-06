package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatGroupEnum;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatOnpageEnum;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatTypeEnum;
import ru.yandex.autotests.direct.utils.model.PerlBoolean;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ReportWizardStatRequest extends BasicDirectRequest {
    public static ReportWizardStatRequest defaultReportWizardStatRequest() {
        return new ReportWizardStatRequest().withCmd("showStat").withStatType(StatTypeEnum.MOL)
                .withShowStat(PerlBoolean.ONE);
    }

    @SerializeKey("cmd")
    private String cmd;

    @SerializeKey("stat_type")
    private StatTypeEnum statType;

    @SerializeKey("show_stat")
    private PerlBoolean showStat;

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("group_by_date")
    private StatGroupEnum groupByDate;

    @SerializeKey("page_size")
    private StatOnpageEnum pageSize;

    @SerializeKey("date_from")
    private String dateFrom;

    @SerializeKey("date_to")
    private String dateTo;

    @SerializeKey("date_from_b")
    private String dateFromB;

    @SerializeKey("date_to_b")
    private String dateToB;

    @SerializeKey("compare_periods")
    private PerlBoolean comparePeriods;

    @SerializeKey("columns")
    private String columns;

    @SerializeKey("group_by")
    private String groupBy;

    @SerializeKey("single_camp")
    private PerlBoolean singleCamp;

    @SerializeKey("with_auto_added_phrases")
    private PerlBoolean withAutoAddedPhrases;

    @SerializeKey("isStat")
    private PerlBoolean isStat;

    @SerializeKey("with_nds")
    private PerlBoolean withNds;

    @SerializeKey("sort")
    private SortEnum sortingParameter;

    @SerializeKey("reverse")
    private PerlBoolean reverse;

    public String getCid() {
        return cid;
    }

    public <T extends ReportWizardStatRequest> T withCid(String cid) {
        this.cid = cid;
        return (T) this;
    }

    public PerlBoolean getShowStat() {
        return showStat;
    }

    public <T extends ReportWizardStatRequest> T withShowStat(PerlBoolean showStat) {
        this.showStat = showStat;
        return (T) this;
    }

    public PerlBoolean getSingleCamp() {
        return singleCamp;
    }

    public <T extends ReportWizardStatRequest> T withSingleCamp(PerlBoolean singleCamp) {
        this.singleCamp = singleCamp;
        return (T) this;
    }

    public PerlBoolean getWithAutoAddedPhrases() {
        return withAutoAddedPhrases;
    }

    public <T extends ReportWizardStatRequest> T withAutoAddedPhrases(PerlBoolean withAutoAddedPhrases) {
        this.withAutoAddedPhrases = withAutoAddedPhrases;
        return (T) this;
    }

    public PerlBoolean getIsStat() {
        return isStat;
    }

    public <T extends ReportWizardStatRequest> T withIsStat(PerlBoolean isStat) {
        this.isStat = isStat;
        return (T) this;
    }

    public PerlBoolean getWithNds() {
        return withNds;
    }

    public <T extends ReportWizardStatRequest> T withNds(PerlBoolean withNds) {
        this.withNds = withNds;
        return (T) this;
    }

    public String getCmd() {
        return cmd;
    }

    public <T extends ReportWizardStatRequest> T withCmd(String cmd) {
        this.cmd = cmd;
        return (T) this;
    }

    public StatTypeEnum getStatType() {
        return statType;
    }

    public <T extends ReportWizardStatRequest> T withStatType(StatTypeEnum statType) {
        this.statType = statType;
        return (T) this;
    }

    public StatGroupEnum getGroupByDate() {
        return groupByDate;
    }

    public <T extends ReportWizardStatRequest> T withGroupByDate(StatGroupEnum groupByDate) {
        this.groupByDate = groupByDate;
        return (T) this;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public <T extends ReportWizardStatRequest> T withGroupBy(String groupBy) {
        this.groupBy = groupBy;
        return (T) this;
    }

    public StatOnpageEnum getPageSize() {
        return pageSize;
    }

    public <T extends ReportWizardStatRequest> T withPageSize(StatOnpageEnum pageSize) {
        this.pageSize = pageSize;
        return (T) this;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public <T extends ReportWizardStatRequest> T withDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
        return (T) this;
    }

    public String getDateTo() {
        return dateTo;
    }

    public <T extends ReportWizardStatRequest> T withDateTo(String dateTo) {
        this.dateTo = dateTo;
        return (T) this;
    }

    public String getColumns() {
        return columns;
    }

    public <T extends ReportWizardStatRequest> T withColumns(String columns) {
        this.columns = columns;
        return (T) this;
    }

    public SortEnum getSortingParameter() {
        return sortingParameter;
    }

    public <T extends ReportWizardStatRequest> T withSortingParameter(SortEnum param) {
        this.sortingParameter = param;
        return (T) this;
    }

    public PerlBoolean getReverse() {
        return reverse;
    }

    public <T extends ReportWizardStatRequest> T withReverse(PerlBoolean param) {
        this.reverse = param;
        return (T) this;
    }

    public String getDateFromB() {
        return dateFromB;
    }

    public <T extends ReportWizardStatRequest> T withDateFromB(String dateFromB) {
        this.dateFromB = dateFromB;
        return (T) this;
    }

    public String getDateToB() {
        return dateToB;
    }

    public <T extends ReportWizardStatRequest> T withDateToB(String dateToB) {
        this.dateToB = dateToB;
        return (T) this;
    }

    public PerlBoolean getComparePeriods() {
        return comparePeriods;
    }

    public <T extends ReportWizardStatRequest> T withComparePeriods(PerlBoolean comparePeriods) {
        this.comparePeriods = comparePeriods;
        return (T) this;
    }
}
