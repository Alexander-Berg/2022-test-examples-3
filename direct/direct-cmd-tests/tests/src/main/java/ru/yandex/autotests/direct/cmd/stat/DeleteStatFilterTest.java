package ru.yandex.autotests.direct.cmd.stat;


import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.stat.filter.FiltersFactory;
import ru.yandex.autotests.direct.cmd.data.stat.filter.JsonFiltersSet;
import ru.yandex.autotests.direct.cmd.data.stat.filter.SaveStatFilterRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestStepsEn.assumeThat;

@Aqua.Test
@Description("Удаление фильтра в МОЛ 2.0")
@Stories(TestFeatures.Stat.FILTER_DELETE)
@Features(TestFeatures.STAT)
@Tag(CmdTag.DELETE_STAT_FILTER)
@Tag(ObjectTag.STAT)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class DeleteStatFilterTest {

    private static final String FILTER_NAME = "FILTER FOR DELETE";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    @Before
    public void before() {

        SaveStatFilterRequest request = new SaveStatFilterRequest();
        JsonFiltersSet filtersSet = FiltersFactory.simpleFilter(FILTER_NAME);
        request.setJsonFiltersSet(filtersSet);
        CommonResponse response = cmdRule.cmdSteps().statSteps().saveStatFilter(request, CommonResponse.class);

        assumeThat("Фильтр успешно сохранен", response.getResult(), equalTo("ok"));
    }

    @Test
    @Description("Удаление фильтра статистического отчета кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9992")
    public void deleteStatFilter() {
        CommonResponse response = cmdRule.cmdSteps().statSteps().deleteStatFilter(FILTER_NAME);

        assertThat("Фильтр успешно удален", response.getResult(), equalTo("ok"));
    }
}
