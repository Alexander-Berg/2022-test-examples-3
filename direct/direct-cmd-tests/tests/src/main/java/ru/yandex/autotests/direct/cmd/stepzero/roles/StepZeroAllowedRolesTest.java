package ru.yandex.autotests.direct.cmd.stepzero.roles;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка работы контроллера StepZero для ролей, имеющих к нему доступ")
@Stories(TestFeatures.StepZero.STEP_ZERO_ROLES)
@Features(TestFeatures.STEP_ZERO)
@RunWith(Parameterized.class)
@Tag(CmdTag.STEP_ZERO)
public class StepZeroAllowedRolesTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Parameterized.Parameter(value = 0)
    public String description;
    @Parameterized.Parameter(value = 1)
    public String userLogin;


    @Parameterized.Parameters(name = "Роль: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"Менеджер", Logins.MANAGER},
                {"Агенство", Logins.AGENCY},
                {"Супер", Logins.SUPER},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(userLogin));
    }

    @Test
    @Description("Проверка доступности контроллера stepZero (нулевой шаг создания кампании)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10006")
    public void controllerAvailabilityTest() {
        StepZeroResponse response = cmdRule.cmdSteps().stepZeroSteps().getStepZero();

        assertThat("нет ошибок в ответе контроллера stepZero", response.getError(), nullValue());
        assertThat("контроллер соответствует ожидаемому", response.getCmd(), equalTo(CMD.STEP_ZERO.getName()));
    }

}
