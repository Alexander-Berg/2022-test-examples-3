package ru.yandex.autotests.direct.cmd.stepzeroprocess.agency;

import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessClientTypeEnum;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessErrorCodeEnum.NOT_YOUR_AGENCY_CODE;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка поля for_agency (контроллер stepZeroProcess)")
@Stories(TestFeatures.StepZeroProcess.CLIENTS)
@Features(TestFeatures.STEP_ZERO_PROCESS)
@Tag(CmdTag.STEP_ZERO_PROCESS)
public class StepZeroProcessForAgencyFieldTest {

    private static final String USER_LOGIN = Logins.TRANSFER_MANAGER;
    private static final String SERVICED_AGENCY = Logins.AGENCY;
    private static final String NOT_SERVICED_AGENCY = Logins.ADGROUPS_AGENCY;
    private static final String CLIENT_LOGIN = "at-direct-b-stzp-trc";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(USER_LOGIN);


    private StepZeroProcessRequest request;

    @Before
    public void before() {
        request = new StepZeroProcessRequest()
                .withType(StepZeroProcessClientTypeEnum.CLIENT)
                .withLogin(CLIENT_LOGIN);

    }

    @Test
    @Description("Проверка получения ошибки при вызове контроллера для несервисируемого агенства")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10012")
    public void notServicedAgencyTest() {
        request.withForAgency(NOT_SERVICED_AGENCY);
        RedirectResponse actualResponse = cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request);

        assertThat("редирект соответствует ожиданию", actualResponse.getLocationParams(),
                both(hasEntry(LocationParam.CMD.toString(), CMD.STEP_ZERO.getName()))
                        .and(hasEntry(LocationParam.ERROR_CODE.toString(), NOT_YOUR_AGENCY_CODE.toString())));
    }

    @Test
    @Description("Проверка успешного перехода на следующий шаг при вызове контроллера для сервисируемого агенства")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10013")
    public void servicedAgencyTest() {
        request.withForAgency(SERVICED_AGENCY);
        Document actualResponse = cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcessDocument(request);
        assertThat("открылась станица создания субклиента", actualResponse.toString(),
                containsString("cmd: 'stepZeroProcess'"));
    }

}
