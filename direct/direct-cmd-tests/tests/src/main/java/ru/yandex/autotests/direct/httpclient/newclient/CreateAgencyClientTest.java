package ru.yandex.autotests.direct.httpclient.newclient;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
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

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.11.14
 *         https://st.yandex-team.ru/TESTIRT-3207
 */

@Aqua.Test
@Description("Проверка контроллера createAgencyClient")
@Stories(TestFeatures.NewClient.SHOW_REGISTER_LOGIN_PAGE)
@Features(TestFeatures.NEW_CLIENT)
@Tag(TrunkTag.YES)
@Tag(CmdTag.PAY)
@Tag(OldTag.YES)
@RunWith(Parameterized.class)
public class CreateAgencyClientTest {

    private static final boolean GET_VARS = true;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(value = 0)
    public String login;
    @Parameterized.Parameter(value = 1)
    public String description;

    private DirectResponse response;

    @Parameterized.Parameters(name = "Пользователь: {0}, роль: {1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {Logins.AGENCY, "Агентство"},
                {Logins.SUPER, "Суперпользователь"},
                {Logins.MANAGER, "Менеджер"},
                {Logins.SUPPORT, "Саппорт"}
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {

        cmdRule.oldSteps().onPassport().authoriseAs(login, User.get(login).getPassword());
        response = cmdRule.oldSteps().onShowRegisterLoginPage().openShowRegisterLoginPage();
    }

    @Test
    @Description("Проверяем наличие track_id в ответе контроллера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10556")
    public void checkTrackIdParameter() {
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response,
                hasJsonProperty("$.track_id", is(not(emptyCollectionOf(Object.class)))));
    }

    @Test
    @Description("Проверяем наличие captcha_id в ответе контроллера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10557")
    public void checkCaptchaIdParameter() {
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response,
                hasJsonProperty("$.captcha_id", is(not(emptyCollectionOf(Object.class)))));
    }

    @Test
    @Description("Проверяем наличие captcha_url в ответе контроллера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10558")
    public void checkCaptchaUrlParameter() {
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response,
                hasJsonProperty("$.captcha_url", is(not(emptyCollectionOf(Object.class)))));
    }
}
