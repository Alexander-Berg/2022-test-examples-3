package ru.yandex.autotests.direct.cmd.stepzeroprocess.clients;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


@Aqua.Test
@Description("Проверка редиректа на создание кампании (контроллер stepZeroProcess)")
@Stories(TestFeatures.StepZeroProcess.SUCCESSFUL_REDIRECT)
@Features(TestFeatures.STEP_ZERO_PROCESS)
@Tag(CmdTag.STEP_ZERO_PROCESS)
@RunWith(Parameterized.class)
public class StepZeroProcessSuccessfulRedirectTest {

    private static final String CLIENT_LOGIN = "at-direct-ag-client";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule;
    public String description;
    public String userLogin;
    public CMD redirectLocation;
    public StepZeroProcessClientTypeEnum clientType;

    private StepZeroProcessRequest request;

    public StepZeroProcessSuccessfulRedirectTest(String description, String userLogin, CMD redirectLocation,
                                                 StepZeroProcessClientTypeEnum clientType) {
        this.description = description;
        this.userLogin = userLogin;
        this.redirectLocation = redirectLocation;
        this.clientType = clientType;
        cmdRule = DirectCmdRule.defaultRule().as(userLogin);
    }


    @Parameterized.Parameters(name = "Роль: {0}, редирект на cmd={2}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"агенство", Logins.AGENCY, CMD.NEW_CAMP_TYPE, StepZeroProcessClientTypeEnum.SUBCLIENT},
                {"менеджер", Logins.MANAGER, CMD.NEW_CAMP_TYPE, StepZeroProcessClientTypeEnum.CLIENT},
                {"супер", Logins.SUPER, CMD.SHOW_CAMPS, StepZeroProcessClientTypeEnum.CLIENT},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        request = new StepZeroProcessRequest()
                .withType(clientType)
                .withLogin(CLIENT_LOGIN);
    }

    @Test
    @Description("Проверка успешного выполнения нулевого шага")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10016")
    public void redirectTest() {
        RedirectResponse actualResponse = cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request);
        assertThat("редирект соответствует ожиданию", actualResponse.getLocationParam(LocationParam.CMD),
                equalTo(redirectLocation.getName()));
    }
}
