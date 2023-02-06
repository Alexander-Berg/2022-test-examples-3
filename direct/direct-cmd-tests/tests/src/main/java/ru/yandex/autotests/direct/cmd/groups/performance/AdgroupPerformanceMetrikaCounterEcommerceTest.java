package ru.yandex.autotests.direct.cmd.groups.performance;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceResponse;
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

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка возврата ecommerce контроллероми addPerformanceAdgroup, editPerformanceAdgroup")
@Stories(TestFeatures.Counters.COUNTER_ECOMMERCE)
@Features(TestFeatures.COUNTERS)
@Tag(ObjectTag.COUNTERS)
@Tag(CampTypeTag.PERFORMANCE)
@RunWith(Parameterized.class)
public class AdgroupPerformanceMetrikaCounterEcommerceTest {

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

    private String campaignId;
    private Long adgroupId;

    @Parameterized.Parameters(name = "Сохраняем ecommerce равеным {0} и ожидаем \"{1}\"")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {0, ""},
                {1, "1"},
        });
    }


    @Before
    public void before() {

        campaignId = bannersRule.getCampaignId().toString();
        adgroupId = bannersRule.getGroupId();
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).metrikaCountersSteps()
                .updateMetrikaCountersEcommerce(bannersRule.getCampaignId(),
                        Long.valueOf(bannersRule.getSaveCampRequest().getMetrika_counters()), 0, saveEcommerce);
    }

    @Test
    @Description("Проверка флага metrika_has_ecommerce контроллером addPerformanceAdgroup")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9816")
    public void addPerformanceAdgroupEcommerceTest() {
        EditAdGroupsPerformanceResponse actualResponse = cmdRule.cmdSteps().groupsSteps()
                .getAddAdGroupsPerformance(CLIENT, campaignId);

        assertThat("Флаг metrika_has_ecommerce вернулся",
                actualResponse.getCampaign().getMetrikaHasEcommerce(), equalTo(expectedEcommerce));
    }

    @Test
    @Description("Проверка флага metrika_has_ecommerce контроллером editPerformanceAdgroup")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9817")
    public void editPerformanceAdgroupEcommerceTest() {
        EditAdGroupsPerformanceResponse actualResponse = cmdRule.cmdSteps().groupsSteps()
                .getEditAdGroupsPerformance(CLIENT, campaignId,
                        String.valueOf(adgroupId), String.valueOf(bannersRule.getBannerId()));

        assertThat("Флаг metrika_has_ecommerce вернулся",
                actualResponse.getCampaign().getMetrikaHasEcommerce(), equalTo(expectedEcommerce));
    }
}
