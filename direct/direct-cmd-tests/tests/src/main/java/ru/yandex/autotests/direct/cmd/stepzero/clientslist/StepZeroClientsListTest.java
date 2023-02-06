package ru.yandex.autotests.direct.cmd.stepzero.clientslist;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * TESTIRT-3642
 */
@Aqua.Test
@Description("Проверка списка клиентов в ответе контроллера StepZero под разными ролями. " +
        "В списке пользователей должны присутствовать сервисируемые клиенты, отсутствовать - несервисируемые и архивные")
@Stories(TestFeatures.StepZero.STEP_ZERO_CLIENTS)
@Features(TestFeatures.STEP_ZERO)
@RunWith(Parameterized.class)
@Tag(CmdTag.STEP_ZERO)
public class StepZeroClientsListTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Parameterized.Parameter(value = 0)
    public String description;
    @Parameterized.Parameter(value = 1)
    public String userLogin;
    @Parameterized.Parameter(value = 2)
    public String managersAgencyLogin;
    @Parameterized.Parameter(value = 3)
    public StepZeroResponse servicedClients;
    @Parameterized.Parameter(value = 4)
    public StepZeroResponse notServicedClients;


    private StepZeroRequest request;

    @Parameterized.Parameters(name = "Роль: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"Менеджер", Logins.MANAGER, null,
                        new StepZeroResponse()
                                .withLogins("at-direct-b-stz-mcl"),
                        new StepZeroResponse()
                                .withLogins("at-direct-b-stz-notmcl", "at-direct-b-stz-arch")},
                {"Менеджер, для сервисируемого агенства", Logins.MANAGER, Logins.AGENCY,
                        new StepZeroResponse()
                                .withLogins("at-direct-b-stz-agcl"),
                        new StepZeroResponse()
                                .withLogins("at-direct-b-stz-notmcl", "at-direct-b-stz-mcl", "at-direct-b-stz-arag")},
                {"Агенство", Logins.AGENCY, null,
                        new StepZeroResponse()
                                .withLogins("at-direct-b-stz-agcl"),
                        new StepZeroResponse()
                                .withLogins("at-direct-b-stz-notmcl", "at-direct-b-stz-mcl", "at-direct-b-stz-arag")},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(userLogin));

        request = new StepZeroRequest().withForAgency(managersAgencyLogin);
    }

    @Test
    @Description("Проверка списка клиентов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10003")
    public void clientsListTest() {
        StepZeroResponse actualResponse = cmdRule.cmdSteps().stepZeroSteps().getStepZero(request);

        assertThat("вернулся ожидаемый список пользователей", actualResponse.getLogins(),
                allOf(hasItems((servicedClients.getLogins()
                                .toArray(new String[servicedClients.getLogins().size()]))),
                        not(hasItems(notServicedClients.getLogins()
                                .toArray(new String[notServicedClients.getLogins().size()])))));
    }

}
