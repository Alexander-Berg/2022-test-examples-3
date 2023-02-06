package ru.yandex.autotests.direct.cmd.stat;


import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.stat.StatErrors;
import ru.yandex.autotests.direct.cmd.data.stat.report.SaveStatReportRequest;
import ru.yandex.autotests.direct.cmd.data.stat.report.SaveStatReportResponse;
import ru.yandex.autotests.direct.cmd.data.stat.report.StatReports;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
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
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

@Aqua.Test
@Description("Пересохранение отчета кампании в МОЛ 2.0")
@Stories(TestFeatures.Stat.REPORT_SAVE)
@Features(TestFeatures.STAT)
@Tag(CmdTag.SAVE_STAT_REPORT)
@Tag(ObjectTag.STAT)
@Tag(TrunkTag.YES)
public class ReSaveStatReportTest {

    private final static String REPORT_NAME = "reportForReSave";
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
        StatReports report = cmdRule.cmdSteps().statSteps().showStatReport(client.getLogin(), today.minusMonths(1)
                .toString(DateUtils.PATTERN_YYYY_MM_DD), today.toString(DateUtils.PATTERN_YYYY_MM_DD))
                .getStatReports().stream().findFirst().orElse(null);
        if (report != null) {
            CommonResponse commonResponse = cmdRule.cmdSteps().statSteps().deleteStatReport(report.getReportId());
            assumeThat("Отчет успешно удален", commonResponse.getResult(), equalTo("ok"));
        }
        cmdRule.cmdSteps().statSteps().deleteStatReport(REPORT_NAME);
        response = cmdRule.cmdSteps().statSteps().saveStatReport(
                client.getLogin(),
                today.minusMonths(1).toString(DateUtils.PATTERN_YYYY_MM_DD),
                today.toString(DateUtils.PATTERN_YYYY_MM_DD),
                REPORT_NAME);

        assumeThat("Отчет по кампании успешно сохранен", response.getStatus(), equalTo("ok"));
    }


    @Test
    @Description("Получение сообщения, при пересохранении отчета под тем же именем")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9996")
    public void reSaveStatReportMessage() {
        SaveStatReportRequest request = new SaveStatReportRequest();
        request.setUlogin(client.getLogin());
        request.setDateFrom(today.minusDays(20).toString(DateUtils.PATTERN_YYYY_MM_DD));
        request.setDateTo(today.toString(DateUtils.PATTERN_YYYY_MM_DD));
        request.setReportName(REPORT_NAME);

        ErrorResponse errorResponse = cmdRule.cmdSteps().statSteps().saveStatReport(request, ErrorResponse.class);

        assertThat("Получили ошибку, что отчет с таким именем уже есть", errorResponse.getError(),
                equalTo(StatErrors.SAVE_REPORT_NAME_ALREADY_EXIST_ERROR.getErrorText()));
    }

    @Test
    @Description("Пересохранение статистического отчета кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9995")
    public void reSaveStatReport() {
        SaveStatReportRequest request = new SaveStatReportRequest();
        request.setUlogin(client.getLogin());
        request.setDateFrom(today.minusDays(20).toString(DateUtils.PATTERN_YYYY_MM_DD));
        request.setDateTo(today.toString(DateUtils.PATTERN_YYYY_MM_DD));
        request.setReportName(REPORT_NAME);
        request.setReportId(response.getReportId());

        SaveStatReportResponse saveStatReportResponse = cmdRule.cmdSteps().statSteps().saveStatReport(request, SaveStatReportResponse.class);

        assertThat("Пересохранение отчета прошло успешно", saveStatReportResponse, beanEquivalent(response));
    }
}
