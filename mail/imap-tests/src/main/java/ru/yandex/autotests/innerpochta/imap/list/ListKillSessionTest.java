package ru.yandex.autotests.innerpochta.imap.list;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 23.03.15
 * Time: 15:17
 */
@Aqua.Test
@Title("Команда LIST. Убиваем сессию")
@Features({ImapCmd.LIST})
@Stories({"#session"})
@Issue("MPROTO-1652")
@Description("Убиваем сессию во время list")
public class ListKillSessionTest extends BaseTest {
    private static Class<?> currentClass = ListKillSessionTest.class;

    public static int COUNT = 10;
    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @Title("Во время list убиваем сессию")
    @Description("Логинимся, делаем list и почти одновременно убиваем сессию (делаем 10 раз)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("233")
    public void listTestWithKillSession() throws Throwable {
        for (int i = 0; i < COUNT; i++) {
            imap.newSession();
            imap.request(login(currentClass.getSimpleName())).shouldBeOk();

            Thread.sleep(2);
            imap.asynchronous().request(list("\"\"", Folders.INBOX));
            imap.killSession();
        }
    }
}
