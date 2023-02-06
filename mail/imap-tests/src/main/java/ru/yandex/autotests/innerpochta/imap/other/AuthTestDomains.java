package ru.yandex.autotests.innerpochta.imap.other;

import java.io.IOException;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.AuthenticateRequest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ch.lambdaj.collection.LambdaCollections.with;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.imap.converters.ToObjectConverter.wrap;
import static ru.yandex.autotests.innerpochta.imap.requests.AuthenticateRequest.XOAUTH2;
import static ru.yandex.autotests.innerpochta.imap.requests.AuthenticateRequest.authenticate;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.utils.OAuthTokenator.getXOAuth2;
import static ru.yandex.autotests.innerpochta.imap.utils.OAuthTokenator.oauthFor;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.03.14
 * Time: 13:16
 */
@Aqua.Test
@Title("Команда LOGIN. Авторизируемся с различными доменами")
@Features({ImapCmd.LOGIN, ImapCmd.AUTHENTICATE})
@Stories("#вэбные домены")
@Description("Авторизируемся для вэбов с всевозможными доменами. " +
        "Позитивное тестирование")
@Web
@RunWith(Parameterized.class)
public class AuthTestDomains extends BaseTest {
    private static Class<?> currentClass = AuthTestDomains.class;

    public static final String LOGIN = props().account(currentClass.getSimpleName()).getLogin();
    public static final String PWD = props().account(currentClass.getSimpleName()).getPassword();


    @Rule
    public ImapClient imap = new ImapClient();

    private String domain;

    public AuthTestDomains(String domain) {
        this.domain = domain;
    }

    @Parameterized.Parameters(name = "domain - {0}")
    public static Collection<Object[]> domains() {
        return with(
                "",
                "@yandex.ru",
                "@yandex.by",
                "@yandex.kz",
                "@yandex.com",
                "@yandex.com.tr",
                "@ya.ru"
        ).convert(wrap());
    }

    @Description("Тестируем авторизацию вместе с доменом\n" +
            "для большого имап:\n" +
            " \"@yandex.ru\",\n" +
            " \"@yandex.by\",\n" +
            " \"@yandex.kz\",\n" +
            " \"@yandex.com\",\n" +
            " \"@yandex.com.tr\",\n" +
            " \"@ya.ru\",\n"
    )
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("328")
    public void testLoginWithDomains() {
        imap.request(login(LOGIN + domain, PWD)).shouldBeOk();
    }

    @Test
    @Stories({MyStories.STARTREK, MyStories.JIRA})
    @Description("Аутенфикация через OAUTH" +
            "с различными доменами для корпов\n" +
            "Должно работать только при ssl подключении")
    @ru.yandex.qatools.allure.annotations.TestCaseId("329")
    public void testAuthenticateOAuthWithDomains() throws IOException {
        imap.request(authenticate(AuthenticateRequest.XOAUTH, oauthFor(LOGIN, PWD).token())).shouldBeOk();
    }

    @Test
    @Stories({MyStories.STARTREK, MyStories.JIRA})
    @Description("[MPROTO-292][MAILPROTO-2340] Аутенфикация через OAUTH, используем гугловскую форму " +
            "с различными доенами\n" +
            "Должно работать только при ssl подключении")
    @ru.yandex.qatools.allure.annotations.TestCaseId("330")
    public void testAuthenticateOAuth2WithDomains() throws IOException {
        imap.request(authenticate(XOAUTH2, getXOAuth2(LOGIN, domain, PWD))).shouldBeOk();
    }
}
