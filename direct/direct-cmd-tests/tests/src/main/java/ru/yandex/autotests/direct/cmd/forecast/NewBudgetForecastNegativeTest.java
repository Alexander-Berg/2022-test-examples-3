package ru.yandex.autotests.direct.cmd.forecast;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.forecast.AjaxDataForBudgetForecastRequest;
import ru.yandex.autotests.direct.cmd.data.forecast.RecommendationsForecastType;
import ru.yandex.autotests.direct.cmd.data.forecast.newforecast.AjaxDataForNewBudgetForecastResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * https://st.yandex-team.ru/DIRECT-50378
 */
@Aqua.Test
@Description("Негативные тесты на новый прогнозатор для менеджеров")
@Stories(TestFeatures.Forecast.NEW_BUDGET_FORECAST)
@Features(TestFeatures.FORECAST)
@Tag(CmdTag.AJAX_DATA_FOR_NEW_BUDGET_FORECAST)
@Tag(ObjectTag.FORECAST)
public class NewBudgetForecastNegativeTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private AjaxDataForBudgetForecastRequest request;
    private AjaxDataForNewBudgetForecastResponse response;

    @Before
    public void before() {

        request = new AjaxDataForBudgetForecastRequest().
                withAdvancedForecast("yes").
                withPeriod(RecommendationsForecastType.WEEK.getName()).
                withGeo("0").
                withPhrases("auto").
                withUnglue("1").
                withFixateStopWords("1");
    }

    @Test
    @Description("Получение данных из нового прогнозатора для пустой фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9761")
    public void getNewForecastEmptyPhrase() {
        request.withPhrases("");
        response = cmdRule.cmdSteps().forecastSteps().getAjaxDataForNewBudgetForecast(request);
        assertThat("получили пустой прогноз", response.getDataByPositions(), nullValue());
    }

    @Test
    @Description("Получение данных из нового прогнозатора если не указана фраза")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9762")
    public void getNewForecastNullPhrase() {
        request.withPhrases(null);
        response = cmdRule.cmdSteps().forecastSteps().getAjaxDataForNewBudgetForecast(request);
        assertThat("получили пустой прогноз", response.getDataByPositions(), nullValue());
    }
}
