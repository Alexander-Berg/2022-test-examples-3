package ru.yandex.autotests.direct.cmd.counters;

import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.counters.AjaxCheckUserCountersResponse;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaGoal;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка доступности счетчика метрики")
@Stories(TestFeatures.Counters.AJAX_CHECK_USER_COUNTERS)
@Features(TestFeatures.COUNTERS)
@Tag(CmdTag.AJAX_CHECK_USER_COUNTERS)
@Tag(ObjectTag.COUNTERS)
public class AjaxCheckUserCountersTest {

    private final static String CLIENT = "at-direct-backend-c";
    private final static Long COUNTER_ID = 31844711L;
    private final static Long WRONG_COUNTER_ID = 111L;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Test
    @Description("Проверяем доступность верного счетчика метрики")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9662")
    public void checkCounterAllowed() {
        AjaxCheckUserCountersResponse actualResponse = cmdRule.cmdSteps()
                .ajaxCheckUserCountersSteps().getCheckUserCounters(COUNTER_ID, CLIENT);

        assertThat("доступность счетчика соответствует ожидаемому",
                actualResponse.getCounterMap().get(COUNTER_ID).getAllow(),
                equalTo(true));
    }

    @Test
    @Description("Проверяем получение целей счетчика")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9663")
    public void checkCounterGoals() {
        AjaxCheckUserCountersResponse actualResponse = cmdRule.cmdSteps().ajaxCheckUserCountersSteps()
                .getCheckUserCounters(COUNTER_ID, CLIENT);

        assertThat("количество целей соответствует ожидаемым",
                actualResponse.getCounterMap().get(COUNTER_ID).getGoals(),
                hasSize(Matchers.greaterThanOrEqualTo(2)));

        MetrikaGoal expectedGoal = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_RESPONSE_METRIKA_GOAL, MetrikaGoal.class);
        assertThat("у счетчика есть цель",
                actualResponse.getCounterMap().get(COUNTER_ID).getGoals(),
                hasItem(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));

    }

    @Test
    @Description("Проверяем недоступность неверного счетчика метрики")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9664")
    public void checkCounterNotAllowed() {
        AjaxCheckUserCountersResponse actualResponse = cmdRule.cmdSteps()
                .ajaxCheckUserCountersSteps().getCheckUserCounters(WRONG_COUNTER_ID, CLIENT);

        assertThat("доступность счетчика соответствует ожидаемому",
                actualResponse.getCounterMap().get(WRONG_COUNTER_ID).getAllow(),
                equalTo(false));
    }
}
