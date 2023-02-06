package ru.yandex.autotests.direct.cmd.data.forecast;

public enum RecommendationsForecastType {
    WEEK,
    MONTH,
    YEAR;

    public String getName() {
        return name().toLowerCase();
    }
}
