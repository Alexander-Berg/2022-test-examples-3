package ru.yandex.autotests.innerpochta.imap.login;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.AuthenticateRequest;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.imap.requests.AuthenticateRequest.authenticate;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.requests.LogoutRequest.logout;
import static ru.yandex.autotests.innerpochta.imap.utils.OAuthTokenator.oauthFor;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.03.14
 * Time: 13:44
 */
@Aqua.Test
@Title("Команда LOGIN. Авторизация. Различные кейсы")
@Features({ImapCmd.LOGIN})
@Stories(MyStories.COMMON)
@Description("LOGIN: позитивные и негативные кейсы")
public class LoginCommonTest extends BaseTest {
    private static Class<?> currentClass = LoginCommonTest.class;

    public static final String BAD_LOGIN = "blahblahblah";
    public static final String BAD_PWD = "badbadpwd";


    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @Description("Авторизируемся неправильным логином и неправильным паролем\n" +
            "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("253")
    public void testWithIncorrectLoginAndPwdShouldSeeNo() {
        imap.request(login(BAD_LOGIN, BAD_PWD)).shouldBeNo();
    }

    @Test
    @Description("Авторизируемся с правильным логином, но неправильным паролем\n" +
            "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("254")
    public void testWithIncorrectPwdShouldSeeNo() {
        imap.request(login(props().account(this).getLogin(), BAD_PWD)).shouldBeNo();
    }

    @Test
    @Description("Проверка отсутстствия авторизации с различными пробелами перед логином\n"
            + "Ожидаемый результат: BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("255")
    public void testWithSpaceBeforeLoginShouldSeeOk() {
        imap.request(login(" " + props().account(this).getLogin(),
                props().account(this).getPassword(), true))
                .shouldBeBad();
    }

    @Test
    @Description("Проверка отсутстствия авторизации если пароль в скобочках\n" +
            "Ожидаемый результат: Ok")
    @ru.yandex.qatools.allure.annotations.TestCaseId("256")
    public void testWithQuotedLoginShouldSeeBad() {
        imap.request(login(Utils.quoted(props().account(this).getLogin()),
                props().account(this).getPassword(), true))
                .shouldBeOk();
        imap.request(logout()).shouldBeOk();
    }

    @Test
    @Description("Проверка что нельзя залогиниться дважды\n" +
            "Ожидаемый результат: BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("257")
    public void loginTwice() {
        imap.request(login(props().account(this))).shouldBeOk();
        imap.request(login(props().account(this))).shouldBeBad().statusLineContains("LOGIN wrong state for this command");
        imap.request(logout()).shouldBeOk();
    }

    @Test
    @Web
    @Description("Логинимся после авторизации с помощью AUTHENTICATION")
    @ru.yandex.qatools.allure.annotations.TestCaseId("260")
    public void loginAfterAuthenticated() {
        imap.request(authenticate(AuthenticateRequest.XOAUTH, oauthFor(props().account(this).getLogin(),
                props().account(this).getPassword()).token())).shouldBeOk();
        imap.request(login(props().account(this))).shouldBeBad().statusLineContains("LOGIN wrong state for this command");
    }
}
