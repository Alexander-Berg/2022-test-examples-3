package ru.yandex.autotests.direct.cmd.steps.forecast;

import org.apache.commons.lang.StringUtils;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.forecast.AjaxDataForBudgetForecastRequest;
import ru.yandex.autotests.direct.cmd.data.forecast.AjaxDataForBudgetForecastResponse;
import ru.yandex.autotests.direct.cmd.data.forecast.RecommendationsForecastType;
import ru.yandex.autotests.direct.cmd.data.forecast.newforecast.AjaxDataForNewBudgetForecastResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

public class ForecastSteps extends DirectBackEndSteps {

    @Step("Получение данных для прогнозатора fixateStopWords = {0}, unglue = {1}")
    public AjaxDataForBudgetForecastResponse getAjaxDataForBudgetForecast(boolean fixateStopWords,
                                                                          boolean unglue,
                                                                          List<String> regionIds,
                                                                          List<String> phrases) {
        AjaxDataForBudgetForecastRequest ajaxDataForBudgetForecastRequest = new AjaxDataForBudgetForecastRequest()
        .withAdvancedForecast("yes")
        .withPeriod(RecommendationsForecastType.MONTH.getName())
        .withPeriodNum("0")
        .withFixateStopWords(fixateStopWords ? "1" : "0")
        .withUnglue(unglue ? "1" : "0")
        .withGeo(StringUtils.join(regionIds, ','))
        .withPhrases(StringUtils.join(phrases, ','));
        return getAjaxDataForBudgetForecast(ajaxDataForBudgetForecastRequest);
    }

    @Step("Получение данных для прогнозатора (cmd = ajaxDataForBudgetForecast)")
    public AjaxDataForBudgetForecastResponse getAjaxDataForBudgetForecast(AjaxDataForBudgetForecastRequest request) {
        return get(CMD.AJAX_DATA_FOR_BUDGET_FORECAST, request, AjaxDataForBudgetForecastResponse.class);
    }

    @Step("Получение данных для нового прогнозатора (cmd = ajaxDataForNewBudgetForecast)")
    public AjaxDataForNewBudgetForecastResponse getAjaxDataForNewBudgetForecast(AjaxDataForBudgetForecastRequest request) {
        return get(CMD.AJAX_DATA_FOR_NEW_BUDGET_FORECAST, request, AjaxDataForNewBudgetForecastResponse.class);
    }

    @Step("Получение данных для нового прогнозатора (cmd = ajaxDataForNewBudgetForecast)")
    public ErrorResponse getAjaxDataForNewBudgetForecastError(AjaxDataForBudgetForecastRequest request) {
        return get(CMD.AJAX_DATA_FOR_NEW_BUDGET_FORECAST, request, ErrorResponse.class);
    }
}
