package ru.yandex.autotests.innerpochta.imap.fetch;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Delchain;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;
import static ru.yandex.autotests.innerpochta.imap.requests.NoOpRequest.noOp;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.06.14
 * Time: 20:25
 * <p/>
 * [AUTOTESTPERS-134]
 */
@Aqua.Test
@Title("Команда FETCH. Фетчим письма внутри второй сессии")
@Features({ImapCmd.FETCH})
@Stories({MyStories.TWO_SESSION})
@Description("Работа с папками в двух сессиях параллельно")
@Delchain
public class FetchWithTwoSessionTest extends BaseTest {
    private static Class<?> currentClass = FetchWithTwoSessionTest.class;

    public static final int NUMBER_OF_MESSAGES = 5;


    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient imap2 = newLoginedClient(currentClass);

    @Test
    @Stories({MyStories.TWO_SESSION, MyStories.HARD_CASES, MyStories.STARTREK, MyStories.JIRA})
    @Title("Фетчим письма, которые заапендили в другой сессии")
    @Description("AUTOTESTPERS-134")
    @ru.yandex.qatools.allure.annotations.TestCaseId("215")
    public void fetchAfterAppendShouldSeeLetters() throws Exception {
        int uidNext = imap.status().getUidNext(Folders.INBOX);

        imap.select().inbox();
        imap.append().appendRandomMessageInInbox();

        imap2.select().inbox();
        imap2.append().appendRandomMessageInInbox();

        imap2.request(fetch("*").uid(true).flags()).shouldBeOk().uidShouldBe(uidNext + 1);

        imap2.append().appendRandomMessageInInbox();

        //добавить провeрку на fetch... здесь почему-то его нет
        imap.request(noOp()).shouldBeOk().existsShouldBe(3).recentShouldBe(2);
        imap2.request(noOp()).shouldBeOk().shouldBeEmpty();

    }

    @Test
    @Stories({MyStories.TWO_SESSION, MyStories.HARD_CASES, MyStories.STARTREK, MyStories.JIRA})
    @Title("Фетчим письма, которые заапендили в другой сессии")
    @Description("Фетчим последнее письмо после APPEND на пустом INBOX-е\n" +
            "[AUTOTESTPERS-134] - 1й кейс, [MAILPROTO-2203]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("216")
    public void fetchAfterAppendWithEmptyMailbox() throws Exception {
        int uidNext = imap.status().getUidNext(Folders.INBOX);

        imap.select().inbox();
        imap2.select().inbox();

        imap.append().appendRandomMessageInInbox();
        imap2.request(fetch("*").uid(true).flags()).shouldBeOk().uidShouldBe(uidNext);

        imap2.append().appendRandomMessageInInbox();

        //фетчем снимает 1 recent
        //добавить провeрку на fetch
        imap.request(noOp()).shouldBeOk().existsShouldBe(2).recentShouldBe(1);
        imap2.request(noOp()).shouldBeOk().shouldBeEmpty();
    }
}
