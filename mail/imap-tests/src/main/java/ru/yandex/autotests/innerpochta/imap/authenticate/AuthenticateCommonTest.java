package ru.yandex.autotests.innerpochta.imap.authenticate;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.AuthenticateResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.imap.requests.AuthenticateRequest.XOAUTH;
import static ru.yandex.autotests.innerpochta.imap.requests.AuthenticateRequest.XOAUTH2;
import static ru.yandex.autotests.innerpochta.imap.requests.AuthenticateRequest.authenticate;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.requests.LogoutRequest.logout;
import static ru.yandex.autotests.innerpochta.imap.utils.OAuthTokenator.getXOAuth2;
import static ru.yandex.autotests.innerpochta.imap.utils.OAuthTokenator.oauthFor;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.09.14
 * Time: 14:17
 * <p>
 * MPROTO-292
 * MAILPROTO-1944
 * <p>
 * У юзера должен быть специальный scope для того чтобы получить токен
 */
@Aqua.Test
@Title("Команда AUTHENTICATE. Авторизация через токен. Общие кейсы")
@Features({ImapCmd.AUTHENTICATE})
@Stories(MyStories.COMMON)
@Description("Проверяем AUTHENTICATE с OAuth, OAuth2")
@Web
public class AuthenticateCommonTest extends BaseTest {
    private static Class<?> currentClass = AuthenticateCommonTest.class;

    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @Description("[MAILPROTO-1944] Аутенфикация через OAUTH\n" +
            "Должно работать только при ssl подключении")
    @Issue("MAILPROTO-1944")
    @ru.yandex.qatools.allure.annotations.TestCaseId("48")
    public void authenticateWithOauth() {
        imap.request(authenticate(XOAUTH, oauthFor(
                props().account(this).getLogin(),
                props().account(this).getPassword()
        ).token())).shouldBeOk();
        imap.request(logout()).shouldBeOk();
    }

    @Test
    @Issues({@Issue("MPROTO-292"), @Issue("MAILPROTO-2340")})
    @Stories({MyStories.STARTREK})
    @Description("[MPROTO-292][MAILPROTO-2340] Аутенфикация через OAUTH, используем гугловскую форму\n" +
            "Должно работать только при ssl подключении")
    @ru.yandex.qatools.allure.annotations.TestCaseId("49")
    public void authenticateWithOauth2GoogleFormat() throws IOException {
        imap.request(authenticate(XOAUTH2, getXOAuth2(props().account(this).getLogin(),
                props().account(this).getPassword()))).shouldBeOk();
        imap.request(logout()).shouldBeOk();
    }

    @Test
    @Description("Проверяем AUTHENTICATE c Oauth после стандартного логина: " +
            "Ожидаемый результат: [CLIENTBUG] AUTHENTICATE wrong state for this command")
    @ru.yandex.qatools.allure.annotations.TestCaseId("50")
    public void authenticateWithOauthAfterLogin() {
        imap.request(login(props().account(this).getLogin(), props().account(this).getPassword())).shouldBeOk();
        imap.request(authenticate(XOAUTH, oauthFor(
                props().account(this).getLogin(),
                props().account(this).getPassword()
        ).token())).shouldBeBad()
                .statusLineContains(AuthenticateResponse.AUTHENTICATE_WRONG_STATE);
        imap.request(logout()).shouldBeOk();

    }

    @Test
    @Description("Проверяем AUTHENTICATE c Oauth2 после стандартного логина: " +
            "Ожидаемый результат: [CLIENTBUG] AUTHENTICATE wrong state for this command")
    @ru.yandex.qatools.allure.annotations.TestCaseId("51")
    public void authenticateWithOauth2AfterLogin() throws IOException {
        imap.request(login(props().account(this).getLogin(), props().account(this).getPassword()));
        imap.request(authenticate(XOAUTH2, getXOAuth2(props().account(this).getLogin(),
                props().account(this).getPassword()))).shouldBeBad()
                .statusLineContains(AuthenticateResponse.AUTHENTICATE_WRONG_STATE);
        imap.request(logout()).shouldBeOk();
    }

    @Test
    @Description("Проверяем AUTHENTICATE c неправильным токеном")
    @ru.yandex.qatools.allure.annotations.TestCaseId("52")
    public void testWithWrongToken() {
        imap.request(authenticate(XOAUTH, Utils.generateName()))
                .shouldBeNo().statusLineContains(AuthenticateResponse.AUTHENTICATE_INVALID_CREDENTAILS);
    }

    @Test
    @Description("Проверяем AUTHENTICATE c AUTH - неправильным механизмом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("53")
    public void testWithWrongMechanism() throws IOException {
        imap.request(authenticate("AUTH", oauthFor(
                props().account(this).getLogin(),
                props().account(this).getPassword()
        ).token())).shouldBeBad().statusLineContains(AuthenticateResponse.AUTHENTICATE_SYNTAX_ERROR);
    }
}
