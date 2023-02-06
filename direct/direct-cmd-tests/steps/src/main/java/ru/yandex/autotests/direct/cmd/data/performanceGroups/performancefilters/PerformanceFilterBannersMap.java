package ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.httpclientlite.core.support.gson.NullValuesAdapterFactory;

import java.util.HashMap;

public class PerformanceFilterBannersMap {

    public static PerformanceFilterBannersMap forPerformanceFilter(String adgroupId, String filterId, PerformanceFilter filter) {
        HashMap<String, PerformanceFilter> filterMap = new HashMap<>();
        filterMap.put(String.valueOf(filterId), filter);
        return new PerformanceFilterBannersMap().withPerformanceFilterBannerMap(adgroupId,
                new PerformanceFilterMap().withPerformanceFilterMap(filterMap));
    }

    @SerializedName("banners")
    private HashMap<String, PerformanceFilterMap> performanceFilterBannerMap;

    public HashMap<String, PerformanceFilterMap> getPerformanceFilterBannerMap() {
        return performanceFilterBannerMap;
    }

    public void setPerformanceFilterBannerMap(HashMap<String, PerformanceFilterMap> performanceFilterBannerMap) {
        this.performanceFilterBannerMap = performanceFilterBannerMap;
    }

    public PerformanceFilterBannersMap withPerformanceFilterBannerMap(String adGroupId, PerformanceFilterMap filterBannerMap) {
        if (performanceFilterBannerMap == null) performanceFilterBannerMap = new HashMap<>();
        performanceFilterBannerMap.put(adGroupId, filterBannerMap);
        return this;
    }

    public String toJson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new NullValuesAdapterFactory()).create()
                .toJson(getPerformanceFilterBannerMap());
    }
}
