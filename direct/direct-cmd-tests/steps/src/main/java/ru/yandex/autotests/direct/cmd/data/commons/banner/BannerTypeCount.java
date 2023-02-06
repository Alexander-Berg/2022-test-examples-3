package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public class BannerTypeCount {

    @SerializedName("banner_type")
    private String bannerType;

    @SerializedName("count")
    private Integer count;

    public String getBannerType() {
        return bannerType;
    }

    public BannerTypeCount withBannerType(String bannerType) {
        this.bannerType = bannerType;
        return this;
    }

    public Integer getCount() {
        return count;
    }

    public BannerTypeCount withCount(Integer count) {
        this.count = count;
        return this;
    }
}
