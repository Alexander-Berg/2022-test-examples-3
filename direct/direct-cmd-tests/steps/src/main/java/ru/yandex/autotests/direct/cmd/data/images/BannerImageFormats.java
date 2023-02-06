package ru.yandex.autotests.direct.cmd.data.images;

import com.google.gson.annotations.SerializedName;

public class BannerImageFormats {

    @SerializedName("orig")
    private Formats orig;


    public Formats getOrig() {
        return orig;
    }

    public BannerImageFormats withOrig(Formats orig) {
        this.orig = orig;
        return this;
    }
}
