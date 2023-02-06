package ru.yandex.autotests.innerpochta.imap.fetch;

import com.google.common.base.Joiner;
import org.junit.ClassRule;
import org.junit.Ignore;
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

import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.03.15
 * Time: 17:15
 */
@Aqua.Test
@Title("Команда FETCH. Прибиваем сессию во время выполнения")
@Features({ImapCmd.FETCH})
@Stories("#session")
@Issue("MPROTO-1342")
@Description("Прибиваем сессию во время выполнения FETCH")
public class FetchKillSessionTest extends BaseTest {
    private static Class<?> currentClass = FetchKillSessionTest.class;


    public static final int COUNT_OF_LETTERS = 15;

    @ClassRule
    public static final ImapClient imap = newLoginedClient(currentClass);

    @ClassRule
    public static final ImapClient imap2 = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Ignore
    @Test
    @Issue("MPROTO-1342")
    @Title("Обрыв сессии во время FETCH")
    @Description("При обрыве сессии, должны осталять флаг /Recent у писем")
    @ru.yandex.qatools.allure.annotations.TestCaseId("210")
    public void killSessionFetchTest() throws Throwable {
        prodImap.append().appendRandomMessagesInInbox(COUNT_OF_LETTERS);

        imap.request(select(Folders.INBOX)).recentShouldBe(COUNT_OF_LETTERS);
        imap.select().inbox();
        imap.asynchronous().request(fetch("1:" + COUNT_OF_LETTERS).bodystructure());
        Thread.sleep(10);
        imap.killSession();

        imap.newSession();
        imap.request(login(currentClass.getSimpleName()));
        imap.request(select(Folders.INBOX)).recentShouldBe(COUNT_OF_LETTERS);
    }

    @Ignore
    @Test
    @Issue("MPROTO-1342")
    @Title("Обрыв сессии во время UID FETCH")
    @Description("При обрыве сессии, должны осталять флаг /Recent у писем")
    @ru.yandex.qatools.allure.annotations.TestCaseId("211")
    public void killSessionUidFetchTest() throws Throwable {
        prodImap.append().appendRandomMessagesInInbox(COUNT_OF_LETTERS);

        imap.request(select(Folders.INBOX)).recentShouldBe(COUNT_OF_LETTERS);
        String uids = Joiner.on(",").join(imap.search().uidAllMessages());
        imap.select().inbox();
        imap.asynchronous().request(fetch(uids).uid(true).bodystructure());
        Thread.sleep(10);
        imap.killSession();

        imap.newSession();
        imap.request(login(currentClass.getSimpleName()));
        imap.request(select(Folders.INBOX)).recentShouldBe(COUNT_OF_LETTERS);
    }
}
