package ru.yandex.autotests.direct.cmd.stat;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.stat.ShowStat;
import ru.yandex.autotests.direct.cmd.data.stat.StatErrors;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Негативные тесты на открытие отчета в МОЛ 2.0")
@Stories(TestFeatures.Stat.REPORT_OPEN)
@Features(TestFeatures.STAT)
@Tag(CmdTag.SHOW_STAT)
@Tag(ObjectTag.STAT)
public class ShowStatReportNegativeTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private User client = User.get("parfenoff-iury2015");

    @Before
    public void before() {
    }

    @Test
    @Description("Получение ошибки при запросе статистического отчета кампании без указания периода")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10001")
    public void showStatReport() {
        ShowStatRequest showStatRequest = new ShowStatRequest();
        showStatRequest.setUlogin(client.getLogin());
        showStatRequest.setShowStat(ShowStat.SHOW.getName());
        ErrorResponse errorResponse = cmdRule.cmdSteps().statSteps().showStatReport(showStatRequest, ErrorResponse.class);

        assertThat("Получили ошибку о некорректной дате периода", errorResponse.getError(),
                equalTo(StatErrors.SHOW_REPORT_DATE_START_ERROR.getErrorText()));
    }
}
