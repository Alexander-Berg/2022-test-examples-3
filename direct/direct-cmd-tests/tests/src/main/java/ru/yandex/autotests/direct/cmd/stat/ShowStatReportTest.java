package ru.yandex.autotests.direct.cmd.stat;


import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.DateUtils;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Открытие отчета в МОЛ 2.0")
@Stories(TestFeatures.Stat.REPORT_OPEN)
@Features(TestFeatures.STAT)
@Tag(CmdTag.SHOW_STAT)
@Tag(ObjectTag.STAT)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@Ignore("не работает")
public class ShowStatReportTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private User client = User.get("parfenoff-iury2015");

    @Before
    public void before() {
    }

    @Test
    @Description("Получение статистического отчета кампании за определенный период")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10002")
    public void showStatReportForDate() {
        DateTime today = DateTime.now();
        ShowStatResponse response = cmdRule.cmdSteps().statSteps().showStatReport(
                client.getLogin(),
                today.minusMonths(6).toString(DateUtils.PATTERN_YYYY_MM_DD),
                today.toString(DateUtils.PATTERN_YYYY_MM_DD));

        assertThat("Получили статистический отчет по кампании", response.getDataArray(), hasSize(greaterThan(0)));
    }

}
