package ru.yandex.autotests.direct.cmd.stepzeroprocess.clients;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessClientTypeEnum;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessErrorCodeEnum;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroProcessRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка валидации для неверного логина клиента (контроллер stepZeroProcess)")
@Stories(TestFeatures.StepZeroProcess.CLIENTS)
@Features(TestFeatures.STEP_ZERO_PROCESS)
@Tag(CmdTag.STEP_ZERO_PROCESS)
public class StepZeroProcessWrongLoginText {

    private static final String USER_LOGIN = Logins.MANAGER;
    private static final String NON_EXISTENT_LOGIN = "at-direct-nonexistentuser";
    private static final String EXISTED_LOGIN = "at-direct-b-stzp-agcl";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(USER_LOGIN);


    private StepZeroProcessRequest request;

    @Before
    public void before() {
        request = new StepZeroProcessRequest()
                .withType(StepZeroProcessClientTypeEnum.CLIENT);
    }

    @Test
    @Description("Проверка получения ошибки при пустом поле login")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10017")
    public void emptyLoginTest() {
        check();
    }

    @Test
    @Description("Проверка получения ошибки при несуществующем login")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10018")
    public void nonexistentLoginTest() {
        request.withLogin(NON_EXISTENT_LOGIN);
        check();
    }

    @Test
    @Description("Проверка получения ошибки при несуществующем new_login")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10019")
    public void nonexistentNewLoginTest() {
        request.withNewLogin(NON_EXISTENT_LOGIN);
        check();
    }

    @Test
    @Description("Проверка получения ошибки при передаче логина уже существующего пользователя в new_login")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10020")
    public void existedUserAsNewLoginTest() {
        request.withNewLogin(EXISTED_LOGIN);
        check();
    }

    private void check() {
        RedirectResponse actualResponse = cmdRule.cmdSteps().stepZeroProcessSteps().getStepZeroProcess(request);
        assertThat("код ошибки соответствует ожидаемому", actualResponse.getLocationParam(LocationParam.ERROR_CODE),
                equalTo(StepZeroProcessErrorCodeEnum.CLIENT_NOT_FOUND_CODE.toString()));
    }
}
