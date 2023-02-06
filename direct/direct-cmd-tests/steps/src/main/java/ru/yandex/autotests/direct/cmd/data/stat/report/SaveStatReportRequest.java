package ru.yandex.autotests.direct.cmd.data.stat.report;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SaveStatReportRequest extends BasicDirectRequest {

    @SerializeKey("report_name")
    private String reportName;

    @SerializeKey("group_by_date")
    private Integer groupByDate;

    @SerializeKey("page_size")
    private String pageSize;

    @SerializeKey("date_from")
    private String dateFrom;

    @SerializeKey("date_to")
    private String dateTo;

    @SerializeKey("with_nds")
    private String withNds;

    @SerializeKey("json_filters")
    private String jsonFilters;

    @SerializeKey("json_columns")
    private String jsonColumns;

    @SerializeKey("json_columns_positions")
    private String jsonColumnsPositions;

    @SerializeKey("json_group_by")
    private String jsonGroupBy;

    @SerializeKey("json_group_by_positions")
    private String jsonGroupByPositions;

    @SerializeKey("report_id")
    private String reportId;

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public Integer getGroupByDate() {
        return groupByDate;
    }

    public void setGroupByDate(Integer groupByDate) {
        this.groupByDate = groupByDate;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getWithNds() {
        return withNds;
    }

    public void setWithNds(String withNds) {
        this.withNds = withNds;
    }

    public String getJsonFilters() {
        return jsonFilters;
    }

    public void setJsonFilters(String jsonFilters) {
        this.jsonFilters = jsonFilters;
    }

    public String getJsonColumns() {
        return jsonColumns;
    }

    public void setJsonColumns(String jsonColumns) {
        this.jsonColumns = jsonColumns;
    }

    public String getJsonColumnsPositions() {
        return jsonColumnsPositions;
    }

    public void setJsonColumnsPositions(String jsonColumnsPositions) {
        this.jsonColumnsPositions = jsonColumnsPositions;
    }

    public String getJsonGroupBy() {
        return jsonGroupBy;
    }

    public void setJsonGroupBy(String jsonGroupBy) {
        this.jsonGroupBy = jsonGroupBy;
    }

    public String getJsonGroupByPositions() {
        return jsonGroupByPositions;
    }

    public void setJsonGroupByPositions(String jsonGroupByPositions) {
        this.jsonGroupByPositions = jsonGroupByPositions;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public SaveStatReportRequest withReportName(String reportName) {
        this.reportName = reportName;
        return this;
    }

    public SaveStatReportRequest withGroupByDate(Integer groupByDate) {
        this.groupByDate = groupByDate;
        return this;
    }

    public SaveStatReportRequest withPageSize(String pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public SaveStatReportRequest withDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
        return this;
    }

    public SaveStatReportRequest withDateTo(String dateTo) {
        this.dateTo = dateTo;
        return this;
    }

    public SaveStatReportRequest withWithNds(String withNds) {
        this.withNds = withNds;
        return this;
    }

    public SaveStatReportRequest withJsonFilters(String jsonFilters) {
        this.jsonFilters = jsonFilters;
        return this;
    }

    public SaveStatReportRequest withJsonColumns(String jsonColumns) {
        this.jsonColumns = jsonColumns;
        return this;
    }

    public SaveStatReportRequest withJsonColumnsPositions(String jsonColumnsPositions) {
        this.jsonColumnsPositions = jsonColumnsPositions;
        return this;
    }

    public SaveStatReportRequest withJsonGroupBy(String jsonGroupBy) {
        this.jsonGroupBy = jsonGroupBy;
        return this;
    }

    public SaveStatReportRequest withJsonGroupByPositions(String jsonGroupByPositions) {
        this.jsonGroupByPositions = jsonGroupByPositions;
        return this;
    }

    public SaveStatReportRequest withReportId(String reportId) {
        this.reportId = reportId;
        return this;
    }
}
