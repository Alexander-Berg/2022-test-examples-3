package ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxEditPerformanceFiltersRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("json_adgroup_performance_filters")
    private String jsonAdgroupPerformanceFilters;

    @SerializeKey("errors_by_field")
    private Integer errorsByField = 1;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getJsonAdgroupPerformanceFilters() {
        return jsonAdgroupPerformanceFilters;
    }

    public void setJsonAdgroupPerformanceFilters(String jsonAdgroupPerformanceFilters) {
        this.jsonAdgroupPerformanceFilters = jsonAdgroupPerformanceFilters;
    }

    public Integer getErrorsByField() {
        return errorsByField;
    }

    public void setErrorsByField(Integer errorsByField) {
        this.errorsByField = errorsByField;
    }

    public AjaxEditPerformanceFiltersRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public AjaxEditPerformanceFiltersRequest withJsonAdgroupPerformanceFilters(String jsonAdgroupPerformanceFilters) {
        this.jsonAdgroupPerformanceFilters = jsonAdgroupPerformanceFilters;
        return this;
    }
}
