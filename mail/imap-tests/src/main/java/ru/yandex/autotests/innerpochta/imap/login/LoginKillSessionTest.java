package ru.yandex.autotests.innerpochta.imap.login;

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

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 23.03.15
 * Time: 15:13
 */
@Aqua.Test
@Title("Команда LOGIN. Убиваем сессию")
@Features({ImapCmd.LOGIN})
@Stories({"#session"})
@Issue("MPROTO-1652")
@Description("Убиваем сессию во время логина 20 раз")
public class LoginKillSessionTest extends BaseTest {
    private static Class<?> currentClass = LoginKillSessionTest.class;

    public static int COUNT = 20;
    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @Title("Во время логина убиваем сессию")
    @Description("Логинимся и почти одновременно убиваем сессию " +
            "(делаем много раз, для того чтобы точно воспроизвести)\n" +
            "Падали в корку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("262")
    public void asyncLoginTestWithKillSession() throws Throwable {
        for (int i = 0; i < COUNT; i++) {
            imap.newSession();
            imap.asynchronous().request(login(currentClass.getSimpleName()));

            Thread.sleep(2);
            imap.killSession();
        }
    }
}
