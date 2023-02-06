package ru.yandex.autotests.direct.cmd.data.forecast;

import com.google.gson.annotations.SerializedName;

/**
 * Created by aleran on 29.09.2015.
 */
public enum PhrasePositionForecastDomainEnum {

    @SerializedName("yandex")
    YANDEX("yandex");

    private String domain;

    PhrasePositionForecastDomainEnum(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }
}
