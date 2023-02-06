package ru.yandex.autotests.direct.web.api.tests.mediareach;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.CampaignStrategy;
import ru.yandex.autotests.direct.web.api.models.CpmForecastRequest;
import ru.yandex.autotests.direct.web.api.models.CpmForecastResponse;
import ru.yandex.autotests.direct.web.api.models.ForecastSector;
import ru.yandex.autotests.direct.web.api.models.ImpressionLimit;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка получения прогноза cpm по кампании")
@Stories(TestFeatures.Mediareach.GET_CAMPAIGN_FORECAST)
@Features(TestFeatures.MEDIAREACH)
@Tag(Tags.MEDIAREACH)
@RunWith(Parameterized.class)
public class CampaignForecastPositiveTest {

    private static final String CLIENT_LOGIN = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(5).as(CLIENT_LOGIN);

    @ClassRule
    public static DirectRule directRule = DirectRule.defaultRule().as(CLIENT_LOGIN);

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public Integer exampleType;

    @Parameterized.Parameter(2)
    public String strategyType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"Новая кампания типа 0 со стратегией типа MIN_CPM", 0, "MIN_CPM"},
                {"Новая кампания типа 0 со стратегией типа MAX_REACH", 0, "MAX_REACH"},
                {"Новая кампания типа 1 со стратегией типа MIN_CPM", 1, "MIN_CPM"},
                {"Новая кампания типа 1 со стратегией типа MAX_REACH", 1, "MAX_REACH"},
                {"Существующая кампания со стратегией типа MIN_CPM", null, "MIN_CPM"},
                {"Существующая кампания со стратегией типа MAX_REACH", null, "MAX_REACH"},
        });
    }

    private Long campaignId;

    @Before
    public void before() {
        int shard = api.userSteps.clientFakeSteps().getUserShard(CLIENT_LOGIN);
        long clientId = Long.parseLong(api.userSteps.clientFakeSteps().getClientData(CLIENT_LOGIN).getClientID());

        campaignId = api.userSteps.campaignSteps().addDefaultCpmBannerCampaign();
        Long creativeId = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .perfCreativesSteps().saveDefaultCanvasCreativesForClient(clientId);
        long adGroupId = api.userSteps.adGroupsSteps().addDefaultCpmBannerUserProfileAdGroup(campaignId);
        api.userSteps.adsSteps().addDefaultCpmBannerAdBuilderAd(adGroupId, creativeId);
    }

    @Test
    public void checkSuccessResponse() {
        CpmForecastResponse response =
                directRule.webApiSteps().mediareachSteps().getCampaignForecast(createRequest(), CLIENT_LOGIN);

        assertThat("Должен вернуться результат без ошибок", response.getSuccess(), is(true));
    }

    @Test
    public void checkRequestId() {
        CpmForecastResponse response =
                directRule.webApiSteps().mediareachSteps().getCampaignForecast(createRequest(), CLIENT_LOGIN);

        assertThat("Request id должен быть не null", response.getRequestId(), notNullValue());
    }

    @Test
    public void checkRecommendedPrice() {
        CpmForecastResponse response =
                directRule.webApiSteps().mediareachSteps().getCampaignForecast(createRequest(), CLIENT_LOGIN);

        assertThat("Рекомендуемая ставка должна быть не null", response.getResult().getRecommendedPrice(),
                notNullValue());
    }

    @Test
    public void checkSectorsCount() {
        CpmForecastResponse response =
                directRule.webApiSteps().mediareachSteps().getCampaignForecast(createRequest(), CLIENT_LOGIN);

        assumeThat("Должен вернуться результат без ошибок", response.getSuccess(), is(true));

        assertThat("Должно вернуться три сектора",
                response.getResult().getGradient().size(), is(3));
    }

    @Test
    public void checkSectorsColor() {
        CpmForecastResponse response =
                directRule.webApiSteps().mediareachSteps().getCampaignForecast(createRequest(), CLIENT_LOGIN);

        assumeThat("Должен вернуться результат без ошибок", response.getSuccess(), is(true));

        List<ForecastSector> sectors = response.getResult().getGradient();

        assumeThat("Должно вернуться три сектора", sectors.size(), is(3));

        assertThat("Первый сектор должен быть красного цвета", sectors.get(0).getColor(), is("red"));
        assertThat("Первый сектор должен быть красного цвета", sectors.get(1).getColor(), is("yellow"));
        assertThat("Первый сектор должен быть красного цвета", sectors.get(2).getColor(), is("green"));
    }

    @Test
    public void checkSectorsEndings() {
        CpmForecastResponse response =
                directRule.webApiSteps().mediareachSteps().getCampaignForecast(createRequest(), CLIENT_LOGIN);

        assumeThat("Должен вернуться результат без ошибок", response.getSuccess(), is(true));

        List<ForecastSector> sectors = response.getResult().getGradient();

        assumeThat("Должно вернуться три сектора", sectors.size(), is(3));

        assertThat("Начало второго сектора должно лежать после конца первого",
                sectors.get(0).getMax(), lessThanOrEqualTo(sectors.get(1).getMin()));
        assertThat("Начало третьего сектора должно лежать после конца второго",
                sectors.get(1).getMax(), lessThanOrEqualTo(sectors.get(2).getMin()));
    }

    private CpmForecastRequest createRequest() {
        return new CpmForecastRequest().withStrategy(defaultStrategy().withType(strategyType))
                .withNewCampaignExampleType(exampleType)
                .withCampaignId(exampleType == null ? campaignId : null);
    }

    private static CampaignStrategy defaultStrategy() {
        return new CampaignStrategy()
                .withType("MAX_REACH")
                .withBudget(10000000.0)
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now().plusMonths(1L))
                .withImpressionLimit(defaultImpressionLimit());
    }

    private static ImpressionLimit defaultImpressionLimit() {
        return new ImpressionLimit()
                .withDays(5L)
                .withImpressions(5L);
    }
}
