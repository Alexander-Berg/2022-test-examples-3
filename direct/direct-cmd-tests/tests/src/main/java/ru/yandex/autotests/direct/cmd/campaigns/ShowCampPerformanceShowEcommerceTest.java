package ru.yandex.autotests.direct.cmd.campaigns;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка возврата ecommerce контроллером showCamp для перформанс кампании")
@Stories(TestFeatures.Counters.COUNTER_ECOMMERCE)
@Features(TestFeatures.COUNTERS)
@Tag(ObjectTag.COUNTERS)
@Tag(CampTypeTag.PERFORMANCE)
@RunWith(Parameterized.class)
public class ShowCampPerformanceShowEcommerceTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public Integer saveEcommerce;
    @Parameterized.Parameter(1)
    public String expectedEcommerce;
    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    @Parameterized.Parameters(name = "Сохраняем ecommerce равеным {0} и ожидаем \"{1}\"")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {0, ""},
                {1, "1"},
        });
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).metrikaCountersSteps()
                .updateMetrikaCountersEcommerce(bannersRule.getCampaignId(),
                        Long.valueOf(bannersRule.getSaveCampRequest().getMetrika_counters()), 0, saveEcommerce);
    }

    @Test
    @Description("Проверка флага metrika_has_ecommerce контроллером showCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9374")
    public void showCampEcommerceTest() {
        ShowCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, String.valueOf(bannersRule.getCampaignId()));

        assertThat("Флаг metrika_has_ecommerce вернулся",
                actualResponse.getMetrikaHasEcommerce(), equalTo(expectedEcommerce));
    }

}
