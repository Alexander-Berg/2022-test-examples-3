package ru.yandex.autotests.direct.web.api.tests.campaign_metrika_counters;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.WebSuccessResponse;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Редактирование счетчиков метрики через массовые действия")
@Stories(TestFeatures.Campaign.CAMP_METRIKA_COUNTERS)
@Features(TestFeatures.CAMPAIGN)
@Tag(Tags.CAMPAIGN)
@Tag(Tags.CAMP_METRIKA_COUNTERS)
public class CampMetrikaCountersTest {

    private static final String CLIENT_LOGIN = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private TextBannersRule textBannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    private DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(textBannersRule)
            .as(CLIENT_LOGIN);
    @Rule
    public DirectRule directRule = DirectRule.defaultRule()
            .withRules(cmdRule)
            .as(CLIENT_LOGIN);

    @Test
    public void addMetrikaCountersTest() {
        WebSuccessResponse response = directRule.webApiSteps().campaignSteps().addMetrikaCounters(
                singletonList(textBannersRule.getCampaignId()), asList(15L, 16L), CLIENT_LOGIN);
        assertThat("в ответе флаг success должен быть равен true", response.getSuccess(), is(true));
    }

    @Test
    public void replaceMetrikaCountersTest() {
        WebSuccessResponse response = directRule.webApiSteps().campaignSteps().replaceMetrikaCounters(
                singletonList(textBannersRule.getCampaignId()), asList(17L, 18L), CLIENT_LOGIN);
        assertThat("в ответе флаг success должен быть равен true", response.getSuccess(), is(true));

    }

    @Test
    public void deleteMetrikaCountersTest() {
        WebSuccessResponse addResponse = directRule.webApiSteps().campaignSteps().addMetrikaCounters(
                singletonList(textBannersRule.getCampaignId()), asList(15L, 16L), CLIENT_LOGIN);
        assumeThat("в ответе запроса на добавление флаг success должен быть равен true",
                addResponse.getSuccess(), is(true));

        WebSuccessResponse deleteResponse = directRule.webApiSteps().campaignSteps().deleteMetrikaCounters(
                singletonList(textBannersRule.getCampaignId()), CLIENT_LOGIN);
        assertThat("в ответе запроса на удаление флаг success должен быть равен true",
                deleteResponse.getSuccess(), is(true));
    }
}
