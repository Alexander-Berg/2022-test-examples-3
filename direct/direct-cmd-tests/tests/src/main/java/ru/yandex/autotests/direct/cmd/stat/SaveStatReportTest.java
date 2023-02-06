package ru.yandex.autotests.direct.cmd.stat;


import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.stat.report.SaveStatReportRequest;
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

@Aqua.Test
@Description("Сохранение отчета в МОЛ 2.0")
@Stories(TestFeatures.Stat.REPORT_SAVE)
@Features(TestFeatures.STAT)
@Tag(CmdTag.SAVE_STAT_REPORT)
@Tag(ObjectTag.STAT)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class SaveStatReportTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private User client = User.get("parfenoff-iury2015");
    private SaveStatReportResponse response;
    private DateTime today;

    @Before
    public void before() {

        today = DateTime.now();
    }

    @After
    public void after() {
        if (response != null) {
            cmdRule.cmdSteps().statSteps().deleteStatReport(response.getReportId());
        }
    }

    @Test
    @Description("Сохранение статистического отчета кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9999")
    public void saveStatReportForDate() {

        response = cmdRule.cmdSteps().statSteps().saveStatReport(
                client.getLogin(),
                today.minusMonths(1).toString(DateUtils.PATTERN_YYYY_MM_DD),
                today.toString(DateUtils.PATTERN_YYYY_MM_DD),
                "report");

        assertThat("Отчет по кампании успешно сохранен", response.getStatus(), equalTo("ok"));
    }

    @Test
    @Description("Сохранение статистического отчета кампании для дат в будущем")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10000")
    public void saveReportFutureDays() {
        SaveStatReportRequest request = new SaveStatReportRequest();
        request.setUlogin(client.getLogin());
        request.setDateFrom(today.plusDays(1).toString(DateUtils.PATTERN_YYYY_MM_DD));
        request.setDateTo(today.plusDays(20).toString(DateUtils.PATTERN_YYYY_MM_DD));
        request.setReportName("report in future");

        response = cmdRule.cmdSteps().statSteps().saveStatReport(request, SaveStatReportResponse.class);

        assertThat("Отчет для дат в будущем успешно сохранен", response.getStatus(), equalTo("ok"));
    }
}
