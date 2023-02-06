package ru.yandex.autotests.direct.cmd.stat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.stat.report.SaveStatReportResponse;
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

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestStepsEn.assumeThat;

@Aqua.Test
@Description("Удаление отчета кампании в МОЛ 2.0")
@Stories(TestFeatures.Stat.REPORT_DELETE)
@Features(TestFeatures.STAT)
@Tag(CmdTag.DELETE_STAT_REPORT)
@Tag(ObjectTag.STAT)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class DeleteStatReportTest {

    private final static String REPORT_NAME = "reportForDelete";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private User client = User.get("parfenoff-iury2015");
    private DateTime today;
    private SaveStatReportResponse response;

    @Before
    public void before() {

        today = DateTime.now();
        response = cmdRule.cmdSteps().statSteps().saveStatReport(
                client.getLogin(),
                today.minusMonths(1).toString(DateUtils.PATTERN_YYYY_MM_DD),
                today.toString(DateUtils.PATTERN_YYYY_MM_DD),
                REPORT_NAME);

        assumeThat("Отчет по кампании успешно сохранен", response.getStatus(), equalTo("ok"));
    }

    @Test
    @Description("Удаление статистического отчета кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9993")
    public void deleteStatReportForDate() {
        CommonResponse deleteResponse = cmdRule.cmdSteps().statSteps().deleteStatReport(response.getReportId());

        assertThat("Отчет успешно удален", deleteResponse.getResult(), equalTo("ok"));
    }
}
