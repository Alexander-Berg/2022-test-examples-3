package ru.yandex.autotests.direct.cmd.data.creatives;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SearchFilterData {

    @SerializedName("name")
    private String name;

    @SerializedName("values")
    private List<SearchFilter> values;

    public String getName() {
        return name;
    }

    public SearchFilterData withName(String name) {
        this.name = name;
        return this;
    }

    public List<SearchFilter> getValues() {
        return values;
    }

    public SearchFilterData withValues(List<SearchFilter> values) {
        this.values = values;
        return this;
    }
}
