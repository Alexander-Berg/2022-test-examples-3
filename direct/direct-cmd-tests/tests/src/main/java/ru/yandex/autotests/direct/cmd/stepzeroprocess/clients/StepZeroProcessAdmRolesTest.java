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

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasEntry;
import static ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessErrorCodeEnum.NOT_A_CLIENT_ROLE_CODE;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Валидация нулевого шага для клиентских логинов с административными ролями (контроллер stepZeroProcess)")
@Stories(TestFeatures.StepZeroProcess.CLIENTS)
@Features(TestFeatures.STEP_ZERO_PROCESS)
@Tag(CmdTag.STEP_ZERO_PROCESS)
@RunWith(Parameterized.class)
public class StepZeroProcessAdmRolesTest {

    private static final String USER_LOGIN = Logins.MANAGER;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(USER_LOGIN);
    @Parameterized.Parameter(value = 0)
    public String description;
    @Parameterized.Parameter(value = 1)
    public String clientLogin;

    private StepZeroProcessRequest request;

    @Parameterized.Parameters(name = "Роль клиента: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"агенство", Logins.AGENCY},
                {"менеджер", Logins.TRANSFER_MANAGER},
                {"супер", "direct-tester7"},
                {"медиапланер", Logins.MEDIAPLANER},
                {"вешальщик", Logins.PLACER},
                {"саппорт", Logins.SUPPORT},
                {"представитель", "at-direct-ag-rep"},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        request = new StepZeroProcessRequest()
                .withType(StepZeroProcessClientTypeEnum.CLIENT)
                .withLogin(clientLogin);
    }

    @Test
    @Description("Проверка получения ошибки при попытке дать объявление клиентам с административными ролями")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10014")
    public void loginAdmRolesTest() {
        RedirectResponse redirectResponse = cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request);
        assertThat("редирект соответствует ожиданию", redirectResponse.getLocationParams(),
                both(hasEntry(LocationParam.CMD.toString(), CMD.STEP_ZERO.getName()))
                        .and(hasEntry(LocationParam.ERROR_CODE.toString(), NOT_A_CLIENT_ROLE_CODE.toString())));
    }
}
