package ru.yandex.autotests.direct.cmd.stepzero.foragency;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroRequest;
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
@Description("Проверка доступности контроллера stepZero под разными ролями для агенства")
@Stories(TestFeatures.StepZero.STEP_ZERO_FOR_AGENCY)
@Features(TestFeatures.STEP_ZERO)
@RunWith(Parameterized.class)
@Tag(CmdTag.STEP_ZERO)
public class StepZeroForAgencyAvailabilityTest {

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
                {"Менеджер, сервисируемое агенство", Logins.MANAGER},
                {"Супер", Logins.SUPER},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(userLogin));
    }


    @Test
    @Description("Проверка доступности контроллера stepZero")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10004")
    public void controllerAvailabilityTest() {
        StepZeroRequest request = new StepZeroRequest().withForAgency(Logins.AGENCY);
        StepZeroResponse response = cmdRule.cmdSteps().stepZeroSteps().getStepZero(request);

        assertThat("нет ошибок", response.getError(), nullValue());
        assertThat("контроллер соответствует ожидаемому", response.getCmd(), equalTo(CMD.STEP_ZERO.getName()));
    }
}
