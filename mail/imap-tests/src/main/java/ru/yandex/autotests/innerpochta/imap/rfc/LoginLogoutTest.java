package ru.yandex.autotests.innerpochta.imap.rfc;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.requests.LogoutRequest.logout;

@Aqua.Test
@Title("Запросы LOGIN и LOGOUT")
@Features({"RFC"})
@Stories("6.2.3 LOGIN & LOGOUT")
@Description("http://tools.ietf.org/html/rfc3501#section-6.2.3")
public class LoginLogoutTest extends BaseTest {
    private static Class<?> currentClass = LoginLogoutTest.class;

    private static final String WRONG_PASSWORD = "blahblahblah";
    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @Title("Должны успешно сделать логин-логаут с правильной парой логин-пароль")
    @ru.yandex.qatools.allure.annotations.TestCaseId("446")
    public void correctPassword() {
        imap.request(login(currentClass.getSimpleName())).shouldBeOk();
        imap.request(logout()).shouldBeOk();
    }

    //lgnfail-imap-test-prac@yandex.ru
    @Test
    @Title("Должны вернуть NO на неправильную пару логин-пароль")
    @ru.yandex.qatools.allure.annotations.TestCaseId("447")
    public void wrongPassword() {
        imap.request(login(props().account(currentClass.getSimpleName()).getSelfEmail(), WRONG_PASSWORD)).shouldBeNo();
    }
}
