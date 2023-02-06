package ru.yandex.autotests.direct.cmd.data.creatives;

import com.google.gson.annotations.SerializedName;

public class SearchFilter {

    @SerializedName("filter")
    private SearchCreativesFilterEnum filter;

    @SerializedName("value")
    private String value;

    public SearchCreativesFilterEnum getFilter() {
        return filter;
    }

    public SearchFilter withFilter(SearchCreativesFilterEnum name) {
        this.filter = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public SearchFilter withValue(String value) {
        this.value = value;
        return this;
    }
}
