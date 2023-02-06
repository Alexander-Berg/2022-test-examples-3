package ru.yandex.autotests.direct.cmd.data.banners;

import com.google.gson.annotations.SerializedName;

public enum  MobileBannerPrimaryAction {

    @SerializedName("download")
    DOWNLOAD,
    @SerializedName("get")
    GET,
    @SerializedName("install")
    INSTALL,
    @SerializedName("more")
    MORE,
    @SerializedName("open")
    OPEN,
    @SerializedName("update")
    UPDATE,
    @SerializedName("play")
    PLAY,
    @SerializedName("buy")
    BUY,
    @SerializedName("invalid")
    INVALID;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
