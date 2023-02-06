package ru.yandex.autotests.innerpochta.imap.other;

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
 * Date: 20.03.15
 * Time: 21:13
 */
@Aqua.Test
@Title("Команда LOGIN и LOGOUT. Убиваем сессию")
@Features({ImapCmd.LOGIN})
@Stories({"#session"})
@Issue("MPROTO-1652")
@Description("Убиваем сессию в разные моменты")
public class KillSessionTest extends BaseTest {
    private static Class<?> currentClass = KillSessionTest.class;

    public static int COUNT = 10;
    @Rule
    public ImapClient imap = new ImapClient();


    @Test
    @Title("Множество сессий без закрытия")
    @Description("Открываем и убиваем сессию. Так 10 раз")
    @ru.yandex.qatools.allure.annotations.TestCaseId("339")
    public void manySessionOpenTest() throws Throwable {
        for (int i = 0; i < COUNT; i++) {
            imap.newSession();
            imap.request(login(currentClass.getSimpleName())).shouldBeOk();
            imap.killSession();
        }
    }

    @Test
    @Title("Множество сессий с закрытием")
    @Description("Открываем и закрываем (logout) сессию 10 раз")
    @ru.yandex.qatools.allure.annotations.TestCaseId("340")
    public void manySessionOpenAndCloseTest() throws Throwable {
        for (int i = 0; i < COUNT; i++) {
            imap.newSession();
            imap.request(login(currentClass.getSimpleName())).shouldBeOk();
            imap.request(logout());
        }
    }
}
