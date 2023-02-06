package ru.yandex.autotests.innerpochta.imap.logout;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.requests.LogoutRequest.logout;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 23.03.15
 * Time: 15:19
 */
@Aqua.Test
@Title("Команда LOGOUT. Убиваем сессию")
@Features({ImapCmd.LOGOUT})
@Stories({"#session"})
@Issue("MPROTO-1652")
@Description("Убиваем сессию в разные моменты")
public class LogoutKillSessionTest extends BaseTest {
    private static Class<?> currentClass = LogoutKillSessionTest.class;

    public static int COUNT = 10;
    @Rule
    public ImapClient imap = new ImapClient();


    @Test
    @Title("Во время logout убиваем сессию")
    @Description("Логинимся, делаем logout и почти одновременно убиваем сессию (делаем 10 раз)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("267")
    public void asyncLogoutTestWithKillSession() throws Throwable {
        for (int i = 0; i < COUNT; i++) {
            imap.newSession();
            imap.request(login(currentClass.getSimpleName())).shouldBeOk();

            imap.asynchronous().request(logout());
            Thread.sleep(2);
            imap.killSession();
        }
    }
}
