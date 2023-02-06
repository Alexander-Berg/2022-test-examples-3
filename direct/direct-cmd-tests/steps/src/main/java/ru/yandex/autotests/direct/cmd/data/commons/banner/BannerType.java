package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public enum BannerType {
    @SerializedName("text")
    TEXT("text"),
    @SerializedName("mobile_content")
    MOBILE_CONTENT("mobile_content"),
    @SerializedName("image_ad")
    IMAGE_AD("image_ad"),
    @SerializedName("mcbanner")
    MCBANNER("mcbanner"),
    @SerializedName("performance")
    PERFORMANCE("performance");

    private String value;

    BannerType(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }
}
