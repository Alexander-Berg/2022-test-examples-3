package ru.yandex.direct.core.testing.steps.campaign.model0.forecast;

import java.time.LocalDateTime;

/**
 * Прогноз бюджета
 */
public class AutobudgetForecast {

    /**
     * Прогноз возможного расхода средств за неделю в спецразмещении;
     * в валюте кампании, без НДС
     */
    private Double forecast;

    /**
     * Время последнего подсчёта прогноза
     */
    private LocalDateTime forecastDate;

    /**
     * Статус прогноза
     */
    private StatusAutobudgetForecast status;
}
