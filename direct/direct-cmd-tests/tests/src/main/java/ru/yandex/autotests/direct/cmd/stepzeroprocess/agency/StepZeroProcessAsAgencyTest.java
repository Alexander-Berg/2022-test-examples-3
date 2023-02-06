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
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasEntry;
import static ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessErrorCodeEnum.CLIENT_OF_ANOTHER_AGENCY_CODE;
import static ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessErrorCodeEnum.WRONG_CLIENT_CURRENCY_CODE;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка валидации нулевого шага под ролью агенства (контроллер stepZeroProcess)")
@Stories(TestFeatures.StepZeroProcess.CLIENTS)
@Features(TestFeatures.STEP_ZERO_PROCESS)
@Tag(CmdTag.STEP_ZERO_PROCESS)
public class StepZeroProcessAsAgencyTest {

    //хеш подтверждения, приходит в письме клиенту
    private static final String PROVE_NEW_AGENCY_CLIENTS_DATA = "53616c7465645f5fd88defa575f924ceb09dc268b1c326bed5aabf4d71c07e7400f5397258b8c6f0636803800b49825700fcb42a1579d7409375b47039c423566d564522cebb54dd225bcfb8ffe361b0";

    private static final String USER_LOGIN = Logins.AGENCY;
    private static final String CLIENT_WITH_WRONG_CURRENCY = "at-direct-client-wrongcur";
    private static final String ANOTHER_AGENCY_CLIENT = "at-direct-b-stzp-aac";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(USER_LOGIN);


    private StepZeroProcessRequest request;


    @Before
    public void before() {
        request = new StepZeroProcessRequest()
                .withType(StepZeroProcessClientTypeEnum.SUBCLIENT);

    }

    @Test
    @Description("Проверка невозможности дать объявление клиенту другого агенства")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10010")
    public void anotherAgencyClientTest() {
        request.withLogin(ANOTHER_AGENCY_CLIENT);
        RedirectResponse actualResponse = cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request);

        assertThat("редирект соответствует ожиданию", actualResponse.getLocationParams(),
                both(hasEntry(LocationParam.CMD.toString(), CMD.STEP_ZERO.getName()))
                        .and(hasEntry(LocationParam.ERROR_CODE.toString(), CLIENT_OF_ANOTHER_AGENCY_CODE.toString())));
    }

    @Test
    @Description("Проверка невозможности дать объявление клиенту с валютой, отсутствующей в списке валют агенства")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10011")
    public void clientWithWrongCurrencyTest() {
        deleteAllCampaigns();
        request.withLogin(CLIENT_WITH_WRONG_CURRENCY);
        cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request); //агентством генерируем запрос прав на клиента
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT_WITH_WRONG_CURRENCY));
        cmdRule.cmdSteps().proveNewAgencyClientsSteps().getProveNewAgencyClients(PROVE_NEW_AGENCY_CLIENTS_DATA); //подтверждаем клиентом право агентсва создавать ему кампании

        cmdRule.cmdSteps().authSteps().authenticate(User.get(USER_LOGIN));

        RedirectResponse actualResponse = cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request);

        assertThat("редирект соответствует ожиданию", actualResponse.getLocationParams(),
                both(hasEntry(LocationParam.CMD.toString(), CMD.STEP_ZERO.getName()))
                        .and(hasEntry(LocationParam.ERROR_CODE.toString(), WRONG_CLIENT_CURRENCY_CODE.toString())));
    }

    private void deleteAllCampaigns() {
        cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT_WITH_WRONG_CURRENCY);
    }
}
