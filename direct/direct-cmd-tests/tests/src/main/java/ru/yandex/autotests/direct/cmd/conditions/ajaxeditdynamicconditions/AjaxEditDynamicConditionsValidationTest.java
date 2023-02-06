package ru.yandex.autotests.direct.cmd.conditions.ajaxeditdynamicconditions;


import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.group.Condition;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.dynamicconditions.AjaxEditDynamicConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.groups.DynamicGroupErrors;
import ru.yandex.autotests.direct.cmd.data.groups.DynamicGroupSource;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка сохранения фильтров ДТО контроллером ajaxEditDynamicConditions")
@Stories(TestFeatures.Conditions.AJAX_EDIT_DYNAMIC_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag(CmdTag.AJAX_EDIT_DYNAMIC_CONDITIONS)
@Tag(ObjectTag.DYN_COND)
@Tag(CampTypeTag.DYNAMIC)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AjaxEditDynamicConditionsValidationTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public DynamicGroupSource source;
    public List<Condition> conditions;
    public String error;
    private DynamicBannersRule bannersRule;

    public AjaxEditDynamicConditionsValidationTest(DynamicGroupSource source, List<Condition> conditions, String error) {
        this.source = source;
        this.conditions = conditions;
        this.error = error;
        bannersRule = new DynamicBannersRule().withSource(source).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Тип дто группы - {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {DynamicGroupSource.DOMAIN, BeanLoadHelper.loadCmdBean(
                        CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class).getConditions(),
                        DynamicGroupErrors.DOMAIN_GROUP_WITH_PERF_CONDITIONS.toString()},
                {DynamicGroupSource.FEED, BeanLoadHelper.loadCmdBean(
                        CmdBeans.COMMON_REQUEST_DYNAMIC_COND_DEFAULT, DynamicCondition.class).getConditions(),
                        DynamicGroupErrors.FEED_GROUP_WITH_DYNAMIC_CONDITIONS.toString()}
        });
    }

    @Test
    @Description("Несохранение дто условия с невалидными условиями")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9611")
    public void checkDynamicFeedGroupWithCond() {
        HashMap<String, Map<String, List<String>>> response = bannersRule.getDirectCmdSteps().ajaxEditDynamicConditionsSteps()
                .postAjaxEditDynamicConditionsInvalidData(AjaxEditDynamicConditionsRequest
                        .fromDynamicCondition(bannersRule.getCurrentGroup()
                                .getDynamicConditions().get(0).withConditions(conditions))
                        .withCid(bannersRule.getCampaignId().toString())
                        .withUlogin(CLIENT));
        assertThat("ошибка соответствует ожиданию", response
                        .get(String.valueOf(bannersRule.getGroupId()))
                        .get("errors"),
                containsInAnyOrder(error.split("\n")));
    }
}
