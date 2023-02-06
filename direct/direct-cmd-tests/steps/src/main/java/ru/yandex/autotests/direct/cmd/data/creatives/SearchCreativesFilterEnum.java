package ru.yandex.autotests.direct.cmd.data.creatives;

import com.google.gson.annotations.SerializedName;

public enum SearchCreativesFilterEnum {

    @SerializedName("business_type")
    BUSINESS_TYPE("business_type"),
    @SerializedName("theme")
    THEME("theme"),
    @SerializedName("layout")
    LAYOUT("layout"),
    @SerializedName("size")
    SIZE("size"),
    @SerializedName("create_time")
    CREATE_TIME("create_time"),
    @SerializedName("campaigns")
    CAMPAIGNS("campaigns"),
    @SerializedName("status_moderate")
    STATUS_MODERATE("status_moderate"),
    @SerializedName("group_id")
    GROUP_ID("group_id");

    private String value;

    SearchCreativesFilterEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
