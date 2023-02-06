package ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PerformanceCampaign {

    @SerializedName("groups")
    private List<GetPerformanceGroup> performanceGroups;

    @SerializedName("metrika_has_ecommerce")
    private String metrikaHasEcommerce;

    public List<GetPerformanceGroup> getPerformanceGroups() {
        return performanceGroups;
    }

    public void setPerformanceGroups(List<GetPerformanceGroup> performanceGroups) {
        this.performanceGroups = performanceGroups;
    }

    public String getMetrikaHasEcommerce() {
        return metrikaHasEcommerce;
    }

    public void setMetrikaHasEcommerce(String metrikaHasEcommerce) {
        this.metrikaHasEcommerce = metrikaHasEcommerce;
    }
}
