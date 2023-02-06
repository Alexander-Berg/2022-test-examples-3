package ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;

public class PerformanceFilterMap {

    @SerializedName("edited")
    private HashMap<String, PerformanceFilter> performanceFilterMap;

    @SerializedName("deleted")
    private List<String> deleted;

    public HashMap<String, PerformanceFilter> getPerformanceFilterMap() {
        return performanceFilterMap;
    }

    public void setPerformanceFilterMap(HashMap<String, PerformanceFilter> performanceFilterMap) {
        this.performanceFilterMap = performanceFilterMap;
    }

    public PerformanceFilterMap withPerformanceFilterMap(HashMap<String,PerformanceFilter> performanceFilterMap){
        this.performanceFilterMap = performanceFilterMap;
        return this;
    }

    public List<String> getDeleted() {
        return deleted;
    }

    public PerformanceFilterMap withDeleted(List<String> deleted) {
        this.deleted = deleted;
        return this;
    }

    public PerformanceFilterMap withEdited(String filterId, PerformanceFilter performanceFilter) {
        if (performanceFilterMap == null) performanceFilterMap = new HashMap<>();
        performanceFilterMap.put(filterId, performanceFilter);
        return this;
    }
}
