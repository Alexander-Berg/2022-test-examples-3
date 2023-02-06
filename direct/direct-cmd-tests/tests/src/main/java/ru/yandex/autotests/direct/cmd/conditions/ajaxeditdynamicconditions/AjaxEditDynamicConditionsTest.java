package ru.yandex.autotests.direct.cmd.conditions.ajaxeditdynamicconditions;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.group.Condition;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
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
import java.util.List;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

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
public class AjaxEditDynamicConditionsTest {

    private static final String CLIENT = "at-direct-b-bannersmultiedit";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private DynamicBannersRule bannersRule;
    private Long campaignId;
    private Long adgroupId;
    private DynamicCondition expectedDynamicCondition;

    public AjaxEditDynamicConditionsTest(DynamicGroupSource dynamicGroupSource, List<Condition> conditions, String desc) {
        expectedDynamicCondition = getDefaultDynamicCondition().withConditions(conditions);
        bannersRule = new DynamicBannersRule().withSource(dynamicGroupSource)
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {DynamicGroupSource.DOMAIN, BeanLoadHelper.loadCmdBean(
                        CmdBeans.COMMON_REQUEST_DYNAMIC_COND_DEFAULT, DynamicCondition.class).getConditions(),
                        "ДТО: Условие нацеливания сайта"},
                {DynamicGroupSource.FEED, BeanLoadHelper.loadCmdBean(
                        CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class).getConditions(),
                        "ДТО: Условие нацеливания фида"}
        });
    }

    private static DynamicCondition getDefaultDynamicCondition() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_DYNAMIC_COND_DEFAULT, DynamicCondition.class);
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        adgroupId = bannersRule.getGroupId();

        String dynCondId = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, String.valueOf(campaignId))
                .getGroups().get(0).getDynamicConditions().get(0).getDynId();
        expectedDynamicCondition.withDynId(dynCondId);
    }

    @Test
    @Description("Проверяем редактирование фильтров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9610")
    public void checkSaveFilter() {
        cmdRule.cmdSteps().ajaxEditDynamicConditionsSteps()
                .dynamicConditionsChangeWithAssumption(campaignId, adgroupId, expectedDynamicCondition, CLIENT);

        DynamicCondition actualCondition = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, String.valueOf(campaignId))
                .getGroups().get(0).getDynamicConditions().get(0);
        assertThat("ДМО-фильтр был правильно отредактирован", actualCondition,
                beanDiffer(expectedDynamicCondition).useCompareStrategy(onlyExpectedFields()));
    }

}
