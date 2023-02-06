package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

/*
* todo javadoc
*/
public enum BannersAdditionsType {
    @SerializedName("callout")
    CALLOUT("callout"),
    DISCLAIMER("disclaimer");

    private String value;

    BannersAdditionsType(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }
}
