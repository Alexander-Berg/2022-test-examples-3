package ru.yandex.autotests.direct.cmd.conditions;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.conditions.AjaxGetFilterSchemaResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.FilterConditions;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.contains;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка параметров фильтров в конфиге контроллером ajaxGetFilterSchema")
@Stories(TestFeatures.Conditions.AJAX_EDIT_PERFORMANCE_FILTERS)
@Features(TestFeatures.CONDITIONS)
@Tag(CmdTag.AJAX_EDIT_PERFORMANCE_FILTERS)
@Tag(ObjectTag.PERFORMANCE_FILTER)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AjaxGetFilterSchemaTest {

    protected static final String SUPER = Logins.SUPER;
    protected static final String CLIENT = "at-direct-back-perf-filters";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    @Parameterized.Parameter(0)
    public FilterConditions filterConditions;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {FilterConditions.HOTELS_GOOGLEHOTELS},
                {FilterConditions.REALTY_YANDEXREALTY},
                {FilterConditions.AUTO_AUTORU},
                {FilterConditions.RETAIL_YANDEXMARKET},
        });
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9597")
    public void checkFilterConfigItems() {
        AjaxGetFilterSchemaResponse response = cmdRule.cmdSteps().ajaxGetFilterSchemaSteps()
                .getFilterSchema(filterConditions.getFilterType());
        assertThat("параметры соответствуют ожиданию", response.getSchema().getDefinitions().getItemsOrder(),
                contains(filterConditions.getConditions()));
    }
}
