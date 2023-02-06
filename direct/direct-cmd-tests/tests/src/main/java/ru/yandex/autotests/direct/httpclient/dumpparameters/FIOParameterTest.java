package ru.yandex.autotests.direct.httpclient.dumpparameters;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 24.09.14
 *         https://st.yandex-team.ru/TESTIRT-2833
 */

@Aqua.Test
@Description("Проверка отсуттвия параметра FIO и наличия параметра fio")
@Stories(TestFeatures.SeveralControllerParameters.FIO_PARAMETER_CHANGE)
@Features(TestFeatures.SEVERAL_CONTROLLER_PARAMETERS)
@Tag(TrunkTag.YES)
@Tag(OldTag.YES)
public class FIOParameterTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private Matcher matcher;
    private String login = "at-daybudget-c";
    private String SUPER = Logins.SUPER;
    private Boolean GET_VARS = true;

    @Before
    public void before() {

        cmdRule.oldSteps().onPassport().authoriseAs(SUPER, User.get(SUPER).getPassword());
        matcher = allOf(hasJsonProperty("$..FIO", is(emptyCollectionOf(Object.class))),
                hasJsonProperty("$..fio", is(not(emptyCollectionOf(Object.class)))));
    }

    @Test
    @Description("Проверяем отсутсвие параметра FIO и наличия fio для контроллера showManagerMyClients")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10456")
    public void checkChangeParameterOnShowManagerMyClients() {
        DirectResponse response = cmdRule.oldSteps().onShowManagerMyClients().openShowManagerMyClients(login);
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Description("Проверяем отсутсвие параметра FIO и наличия fio для контроллера showUserEmails")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10457")
    public void checkChangeParameterOnShowUserEmails() {
        DirectResponse response = cmdRule.oldSteps().onShowUserEmails().openShowUserEmailsAtJsonFormat();
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

}
