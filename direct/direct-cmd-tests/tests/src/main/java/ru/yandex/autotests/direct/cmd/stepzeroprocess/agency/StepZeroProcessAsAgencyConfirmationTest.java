package ru.yandex.autotests.direct.cmd.stepzeroprocess.agency;

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
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessErrorCodeEnum.AWAITING_CLIENT_CONFIRMATION;
import static ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessErrorCodeEnum.NEED_CLIENT_CONFIRMATION;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка отправки запроса на подтверждение доступа к клиенту из-под агенства" +
        " (контроллер stepZeroProcess)")
@Stories(TestFeatures.StepZeroProcess.CLIENTS)
@Features(TestFeatures.STEP_ZERO_PROCESS)
@Tag(CmdTag.STEP_ZERO_PROCESS)
public class StepZeroProcessAsAgencyConfirmationTest {
    private static final String USER_LOGIN = Logins.AGENCY;
    private static final String CLIENT_FOR_CONFIRMATION = "at-direct-stzp-clienttoconfirm";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(USER_LOGIN);


    private StepZeroProcessRequest request;


    @Before
    public void before() {

        cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT_FOR_CONFIRMATION);
        request = new StepZeroProcessRequest()
                .withType(StepZeroProcessClientTypeEnum.SUBCLIENT)
                .withLogin(CLIENT_FOR_CONFIRMATION);
    }

    @Test
    @Description("Проверяем, что несервисируемому клиенту приходит запрос на подачу объявления от агенства при первой попытке дать объявление")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10008")
    public void notServicedClientReceivesAgencyRequestTest() {
        Long agencyClientId = Long.valueOf(User.get(USER_LOGIN).getClientID());
        Long clientUid = TestEnvironment.newDbSteps(CLIENT_FOR_CONFIRMATION).usersSteps().getUidByLogin(CLIENT_FOR_CONFIRMATION);
        TestEnvironment.newDbSteps(USER_LOGIN).agencyClientProveSteps().deleteAgencyClientProveIfExists(
                agencyClientId, clientUid);

        RedirectResponse actualResponse = cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request);

        assertThat("редирект соответствует ожиданию", actualResponse.getLocationParams(),
                both(hasEntry(LocationParam.CMD.toString(), CMD.STEP_ZERO.getName()))
                        .and(hasEntry(LocationParam.ERROR_CODE.toString(), NEED_CLIENT_CONFIRMATION.toString())));

        assertThat("в базе данных присутствует запись о запросе клиенту от агенства",
                TestEnvironment.newDbSteps(USER_LOGIN).agencyClientProveSteps().getAgencyClientProve(agencyClientId, clientUid),
                notNullValue());
    }

    @Test
    @Description("Получение редиректа с ошибкой при попытке дать объявление клиенту, которому уже был отправлен запрос")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10009")
    public void clientWithConfirmationSentTest() {
        cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request);
        RedirectResponse actualResponse = cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request);

        assertThat("редирект соответствует ожиданию", actualResponse.getLocationParams(),
                both(hasEntry(LocationParam.CMD.toString(), CMD.STEP_ZERO.getName()))
                        .and(hasEntry(LocationParam.ERROR_CODE.toString(), AWAITING_CLIENT_CONFIRMATION.toString())));
    }
}
