package ru.yandex.autotests.direct.cmd.forecast;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.forecast.AjaxDataForBudgetForecastRequest;
import ru.yandex.autotests.direct.cmd.data.forecast.RecommendationsForecastType;
import ru.yandex.autotests.direct.cmd.data.forecast.newforecast.AjaxDataForNewBudgetForecastResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * https://st.yandex-team.ru/DIRECT-50378
 */
@Aqua.Test
@Description("Новый прогнозатор для менеджеров")
@Stories(TestFeatures.Forecast.NEW_BUDGET_FORECAST)
@Features(TestFeatures.FORECAST)
@Tag(CmdTag.AJAX_DATA_FOR_NEW_BUDGET_FORECAST)
@Tag(ObjectTag.FORECAST)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class NewBudgetForecastTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule;


    private AjaxDataForBudgetForecastRequest request;
    private AjaxDataForNewBudgetForecastResponse response;

    @SuppressWarnings("unused")
    public NewBudgetForecastTest(String login) {
        cmdRule = DirectCmdRule.defaultRule().as(login);
    }

    @Parameterized.Parameters(name = "Логин: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Logins.SUPER},
                {Logins.MANAGER},
                {Logins.SUPER_READER},
                {Logins.MEDIAPLANER},
                {Logins.SUPPORT},
                {Logins.PLACER},
                {Logins.TRANSFER_MANAGER},
        });
    }

    @Before
    public void before() {

        request = new AjaxDataForBudgetForecastRequest().
                withAdvancedForecast("yes").
                withPeriod(RecommendationsForecastType.MONTH.getName()).
                withGeo("0").
                withPhrases("авто").
                withUnglue("1").
                withFixateStopWords("1");
    }

    @Test
    @Description("Получение данных из нового прогнозатора на месяц для одной фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9763")
    public void getNewForecastOnePhraseForMonth() {
        response = cmdRule.cmdSteps().forecastSteps().getAjaxDataForNewBudgetForecast(request);
        assertThat("получили прогноз для 1 фразы", response.getDataByPositions(), hasSize(1));
    }

    @Test
    @Description("Получение данных из нового прогнозатора на неделю для одной фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9764")
    public void getNewForecastOnePhraseForWeek() {
        request.setPeriod(RecommendationsForecastType.WEEK.getName());
        response = cmdRule.cmdSteps().forecastSteps().getAjaxDataForNewBudgetForecast(request);
        assertThat("получили прогноз для 1 фразы", response.getDataByPositions(), hasSize(1));
    }

    @Test
    @Description("Получение данных из нового прогнозатора на год для одной фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9765")
    public void getNewForecastOnePhraseForYear() {
        request.setPeriod(RecommendationsForecastType.YEAR.getName());
        response = cmdRule.cmdSteps().forecastSteps().getAjaxDataForNewBudgetForecast(request);
        assertThat("получили прогноз для 1 фразы", response.getDataByPositions(), hasSize(1));
    }

    @Test
    @Description("Получение данных из нового прогнозатора для нескольких фраз")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9766")
    public void getNewForecastForPhrases() {
        request.setPhrases("auto, беговел, корм для кошек");
        response = cmdRule.cmdSteps().forecastSteps().getAjaxDataForNewBudgetForecast(request);
        assertThat("получили прогноз для 3 фраз", response.getDataByPositions(), hasSize(3));
    }

    @Test
    @Description("Получение данных из нового прогнозатора с минус-словами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9767")
    public void getNewForecastWithMinusWords() {
        request.setJsonMinusWords(Collections.singletonList("велосипед"));
        response = cmdRule.cmdSteps().forecastSteps().getAjaxDataForNewBudgetForecast(request);
        assertThat("получили прогноз для 1 фразы", response.getDataByPositions(), hasSize(1));
    }
}
