package ru.yandex.autotests.direct.cmd.stepzero.roles;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка контроллера stepZero для ролей, не имеющих к нему доступа")
@Stories(TestFeatures.StepZero.STEP_ZERO_ROLES)
@Features(TestFeatures.STEP_ZERO)
@RunWith(Parameterized.class)
@Tag(CmdTag.STEP_ZERO)
public class StepZeroUnallowedRolesTest {

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
                {"Проф клиент", "at-direct-b-stepzero-prof"},
                {"Легкий клиент", "at-direct-b-stepzero-light"},
                {"Вешальщик", Logins.PLACER},
                {"Медиапланер", Logins.MEDIAPLANER},
//                {"Саппорт", "at-direct-support"}, todo: remove comment, when DIRECT-40748 is fixed
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(userLogin));
    }

    @Test
    @Description("Проверяем недоступность контроллера stepZero")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10007")
    public void controllerUnavailabilityTest() {
        ErrorResponse response = cmdRule.cmdSteps().stepZeroSteps().getStepZero();
        assertThat("нет прав для выполнения операции", response.getError(),
                containsString(TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

}
