package ru.yandex.autotests.direct.cmd.data.stat.report;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.stat.DataArray;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;
import ru.yandex.autotests.direct.cmd.data.stat.UserOptions;

import java.util.ArrayList;
import java.util.List;

public class ShowStatResponse {

    @SerializedName("with_nds")
    private String withNds;

    @SerializedName("date_from")
    private String dateFrom;

    @SerializedName("date_to")
    private String dateTo;

    @SerializedName("page_size")
    private String pageSize;

    @SerializedName("group_by_date")
    private String groupByDate;

    @SerializedName("columns")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<String> columns;

    @SerializedName("group_by_positions")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<String> groupByPositions;

    @SerializedName("columns_positions")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<String> columnsPositions;

    @SerializedName("group_by")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<String> groupBy;

    @SerializedName("USER_OPTIONS")
    private UserOptions UserOptions;

    @SerializedName("data_array")
    private ArrayList<DataArray> dataArray = new ArrayList<DataArray>();

    @SerializedName("stat_reports")
    private List<StatReports> statReports;

    public List<StatReports>  getStatReports() {
        return statReports;
    }

    public ArrayList<DataArray> getDataArray() {
        return dataArray;
    }

    public void setDataArray(ArrayList<DataArray> dataArray) {
        this.dataArray = dataArray;
    }

    public UserOptions getUserOptions() {
        return UserOptions;
    }


    public void setUserOptions(UserOptions userOptions) {
        UserOptions = userOptions;
    }

    public String getWithNds() {
        return withNds;
    }

    public void setWithNds(String withNds) {
        this.withNds = withNds;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getGroupByDate() {
        return groupByDate;
    }

    public void setGroupByDate(String groupByDate) {
        this.groupByDate = groupByDate;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<String> getGroupByPositions() {
        return groupByPositions;
    }

    public void setGroupByPositions(List<String> groupByPositions) {
        this.groupByPositions = groupByPositions;
    }

    public List<String> getColumnsPositions() {
        return columnsPositions;
    }

    public void setColumnsPositions(List<String> columnsPositions) {
        this.columnsPositions = columnsPositions;
    }

    public List<String> getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(List<String> groupBy) {
        this.groupBy = groupBy;
    }

}
