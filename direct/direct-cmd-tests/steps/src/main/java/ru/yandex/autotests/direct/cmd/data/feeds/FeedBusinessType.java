package ru.yandex.autotests.direct.cmd.data.feeds;

import com.google.gson.annotations.SerializedName;

public enum FeedBusinessType {
    @SerializedName("retail")
    RETAIL("retail"), //Товары
    @SerializedName("auto")
    AUTO("auto"), //Автомобили
    @SerializedName("hotels")
    HOTELS("hotels"), //Отели
    @SerializedName("realty")
    REALTY("realty"), //Недвижимость
    @SerializedName("flights")
    FLIGHTS("flights"),
    @SerializedName("site")
    SITE("site"),
    @SerializedName("asdf")
    WRONG_TYPE("asdf");

    private String value;

    FeedBusinessType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
