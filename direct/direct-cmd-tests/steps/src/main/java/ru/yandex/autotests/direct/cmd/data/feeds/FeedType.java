package ru.yandex.autotests.direct.cmd.data.feeds;

public enum FeedType {
    PERFORMANCE("performance"),
    DYNAMIC_CONDITIONS("dynamic_conditions"),
    AUTORU("AutoRu"),
    GOOGLE_HOTELS("GoogleHotels"),
    YANDEX_REALTY("YandexRealty"),
    YANDEX_MARKET("YandexMarket");

    private String value;

    FeedType(String value) {
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
