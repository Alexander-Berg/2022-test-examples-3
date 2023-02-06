package ru.yandex.autotests.innerpochta.imap.expunge;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.NoopExpungeResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.String.valueOf;
import static ru.yandex.autotests.innerpochta.imap.requests.ExpungeRequest.uidExpunge;
import static ru.yandex.autotests.innerpochta.imap.requests.NoOpRequest.noOp;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.06.14
 * Time: 12:25
 * <p/>
 * [MAILPROTO-2204]
 * [AUTOTESTPERS-134]
 */
@Aqua.Test
@Title("Команда EXPUNGE. Работа с двумя сессиями")
@Features({ImapCmd.EXPUNGE})
@Stories({MyStories.TWO_SESSION})
@Description("Делаем UID EXPUNGE с разными письмами в двух паралельных сессиях")
public class ExpungeWithTwoSessionTest extends BaseTest {
    private static Class<?> currentClass = ExpungeWithTwoSessionTest.class;

    public static final int NUMBER_OF_MESSAGES = 5;


    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient imap2 = newLoginedClient(currentClass);

    @Test
    @Stories({MyStories.TWO_SESSION, MyStories.HARD_CASES, MyStories.STARTREK})
    @Title("Делаем параллельный EXPUNGE в двух сессиях. Пересекающиеся множества писем")
    @Description("Делаем параллельный EXPUNGE вместе с пересекающиеся множествами [AUTOTESTPERS-134]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("188")
    public void uidExpungeWithIntersectingSets() throws Exception {
        int uidNext = imap.status().getUidNext(Folders.INBOX);
        imap.select().inbox();
        imap2.select().inbox();
        imap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.store().deletedOnMessages(imap.search().allMessages());

//        imap2.asynchronous().request(uidExpunge(String.valueOf(uidNext)))
        imap.request(uidExpunge(valueOf(uidNext))).shouldBeOk().expungeShouldBe(1);    //добавить проверку

        imap2.request(noOp()).shouldBeOk().existsShouldBe(NUMBER_OF_MESSAGES - 1);
    }

    @Ignore("Баг [MAILPROTO-2205]")
    @Test
    @Stories({MyStories.TWO_SESSION, MyStories.HARD_CASES, MyStories.STARTREK})
    @Title("Делаем параллельный EXPUNGE в двух сессиях. Непересекающиеся множества писем")
    @Description("Делаем параллельный EXPUNGE вместе с непересекающимися множествами писем " +
            "[AUTOTESTPERS-134][MAILPROTO-2220][MAILPROTO-2205]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("189")
    public void uidExpungeWithDisjointSets() throws Exception {
        int uidNext = imap.status().getUidNext(Folders.INBOX);
        imap.select().inbox();
        imap2.select().inbox();
        imap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.store().deletedOnMessages(imap.search().allMessages());

        //одновременно
        imap.asynchronous().request(uidExpunge(String.valueOf(uidNext)));
        imap2.asynchronous().request(uidExpunge(String.valueOf(uidNext + NUMBER_OF_MESSAGES - 1)));

        imap.readFuture(NoopExpungeResponse.class).expungeShouldBe(1);
        imap2.readFuture(NoopExpungeResponse.class).expungeShouldBe(1, 5)
                .recentShouldBe(NUMBER_OF_MESSAGES, NUMBER_OF_MESSAGES - 2).existsShouldBe(NUMBER_OF_MESSAGES);

        imap.request(noOp()).shouldBeOk();
        imap2.request(noOp()).shouldBeOk();
    }

    @Test
    @Stories({MyStories.TWO_SESSION, MyStories.HARD_CASES, MyStories.STARTREK, MyStories.JIRA})
    @Title("Делаем параллельный EXPUNGE в двух сессиях. Совпадающие множества писем")
    @Description("Делаем параллельный EXPUNGE вместе с совпадающими множествами " +
            "[MAILPROTO-2204][AUTOTESTPERS-134][MAILPROTO-2137]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("190")
    public void uidExpungeMatchingSets() throws Exception {
        int uidNext = imap.status().getUidNext(Folders.INBOX);
        imap.select().inbox();
        imap2.select().inbox();

        imap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.store().deletedOnMessages(imap.search().allMessages());

        imap2.request(uidExpunge(String.valueOf(uidNext))).recentShouldBe(NUMBER_OF_MESSAGES, NUMBER_OF_MESSAGES - 1)
                .existsShouldBe(NUMBER_OF_MESSAGES).expungeShouldBe(1);
        imap.request(uidExpunge(String.valueOf(uidNext))).expungeShouldBe(1);

        imap2.request(noOp()).shouldBeOk();
    }

}
