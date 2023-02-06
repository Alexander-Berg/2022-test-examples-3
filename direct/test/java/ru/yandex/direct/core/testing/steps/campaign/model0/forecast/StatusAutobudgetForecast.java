package ru.yandex.direct.core.testing.steps.campaign.model0.forecast;

public enum StatusAutobudgetForecast {

    /**
     * Значение нужно пересчитать
     */
    NEW,

    /**
     * Значение актуальное, автобюджет с указанными значениями выполним
     */
    VALID,

    /**
     * Значение актуальное, автобюджет с указанными значениями невыполним
     */
    WRONG
}
