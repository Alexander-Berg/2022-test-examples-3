package ru.yandex.autotests.direct.cmd.stat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.stat.StatErrors;
import ru.yandex.autotests.direct.cmd.data.stat.report.SaveStatReportRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
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
@Description("Негативные тесты на сохранение статистического отчета кампании в МОЛ 2.0")
@Stories(TestFeatures.Stat.REPORT_SAVE)
@Features(TestFeatures.STAT)
@Tag(CmdTag.SAVE_STAT_REPORT)
@Tag(ObjectTag.STAT)
public class SaveStatReportNegativeTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private User client = User.get("parfenoff-iury2015");
    private DateTime today;

    @Before
    public void before() {
        today = DateTime.now();
    }

    @Test
    @Description("Получение сообщения, что название отчета обязательно при сохранении")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9998")
    public void saveReportWithoutName() {
        SaveStatReportRequest request = new SaveStatReportRequest();
        request.setUlogin(client.getLogin());
        request.setDateFrom(today.minusDays(20).toString(DateUtils.PATTERN_YYYY_MM_DD));
        request.setDateTo(today.toString(DateUtils.PATTERN_YYYY_MM_DD));

        ErrorResponse errorResponse = cmdRule.cmdSteps().statSteps().saveStatReport(request, ErrorResponse.class);

        assertThat("Получили ошибку, что название отчета обязательно при сохранении", errorResponse.getError(),
                equalTo(StatErrors.SAVE_REPORT_NAME_IS_NECESSARY_ERROR.getErrorText()));
    }
}
