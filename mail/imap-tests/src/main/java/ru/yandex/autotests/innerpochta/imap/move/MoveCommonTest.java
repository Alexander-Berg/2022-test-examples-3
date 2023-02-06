package ru.yandex.autotests.innerpochta.imap.move;

import java.util.List;

import com.google.common.base.Joiner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.MoveResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.model.SeverityLevel;

import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.MoveRequest.move;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.08.14
 * Time: 17:26
 * <p/>
 * http://tools.ietf.org/html/rfc6851
 * [MAILPROTO-346]
 */
@Aqua.Test
@Title("Команда MOVE. Общие тесты")
@Features({ImapCmd.MOVE})
@Stories(MyStories.COMMON)
@Issue("MAILPROTO-346")
@Description("Общие тесты на MOVE. Проверяем выдачу")
public class MoveCommonTest extends BaseTest {
    private static Class<?> currentClass = MoveCommonTest.class;

    public static final int NUMBER_OF_MESSAGES = 2;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));


    @Test
    @Description("Переносим по uid и обычным способом из пустой папки inbox в пустую папку [MAILORA-325]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("290")
    public void moveFromEmptyFolder() {
        String folderName = Utils.generateName();
        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);

        imap.select().waitNoMessagesInInbox();
        imap.select().inbox();

        //фича
        imap.request(move("*", folderName).uid(true)).shouldBeOk()
                .statusLineContains(MoveResponse.UID_CLIENT_BUG_NO_MESSAGES);
        imap.request(move("*", folderName)).shouldBeNo().statusLineContains(MoveResponse.NO_MESSAGES);

        imap.request(move("*", folderName).uid(true)).shouldBeOk()
                .statusLineContains(MoveResponse.UID_CLIENT_BUG_NO_MESSAGES);
        imap.request(move("*", folderName)).shouldBeNo().statusLineContains(MoveResponse.NO_MESSAGES);
    }

    @Test
    @Description("Пробуем перенести письма без SELECT-а папки")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("289")
    public void moveWithoutSelectShouldSeeBad() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);

        imap.request(unselect());
        imap.request(move("*", Folders.INBOX)).shouldBeBad()
                .statusLineContains(MoveResponse.WRONG_SESSION_STATE);

        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);
        imap.status().numberOfMessagesShouldBe(Folders.INBOX, NUMBER_OF_MESSAGES);
    }

    @Test
    @Description("Пробуем перенести письма с EXAMINE вместо SELECT [MAILPROTO-2317]")
    @Features("EXAMINE MOD")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("291")
    public void moveWithExamineTest() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);

        imap.request(unselect());
        imap.examine().inbox();
        imap.request(move("*", Folders.INBOX)).shouldBeEmpty();

        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);
        imap.status().numberOfMessagesInInboxShouldBe(NUMBER_OF_MESSAGES);
    }


    @Test
    @Description("Пробуем перенести письма с EXAMINE вместо SELECT [MAILPROTO-2317]")
    @Features("EXAMINE MOD")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("292")
    public void moveLettersWithExamineTest() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);

        String folderName = Util.getRandomString();
        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);

        imap.request(unselect());
        imap.examine().inbox();

        imap.request(move("1:2", folderName)).shouldBeNo().statusLineContains(MoveResponse.CAN_NOT_MOVE_FROM_RO_FOLDER);

        imap.select().waitMsgsInInbox(2);
        imap.status().numberOfMessagesInInboxShouldBe(2);

        imap.select().waitMsgs(folderName, 0);
        imap.status().numberOfMessagesShouldBe(folderName, 0);
    }

    @Test
    @Description("Аппендим пару писем, переносим их в пользовательскую папку")
    @Severity(SeverityLevel.BLOCKER)
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("293")
    public void simpleMoveTest() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);

        String folderName = Util.getRandomString();
        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);
        //for search:
        prodImap.select().inbox();
        //for move:
        Integer uidNext = imap.request(select(Folders.INBOX)).uidNext();

        imap.request(move(prodImap.search().allMessages(), folderName)).shouldBeOk()
                .shouldBeFromUids(String.format("%d:%d", uidNext - 2, uidNext - 1))
                //так как папка новая, то uid всегда такие
                .shouldBeToUids("1:" + NUMBER_OF_MESSAGES);

        imap.select().waitMsgsInInbox(0);
        imap.status().shouldBeNoMessagesInInbox();

        imap.select().waitMsgs(folderName, NUMBER_OF_MESSAGES);
        imap.status().numberOfMessagesShouldBe(folderName, NUMBER_OF_MESSAGES);
    }

    @Test
    @Description("Аппендим пару писем, переносим их в пользовательскую папку")
    @Severity(SeverityLevel.BLOCKER)
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("294")
    public void moveUidConsistencyTest() throws Exception {
        Integer countOfMessages = 10;
        Integer countMoveMessages = 3;
        prodImap.append().appendRandomMessagesInInbox(countOfMessages);
        imap.select().waitMsgsInInbox(countOfMessages);

        String folderName = Util.getRandomString();
        String seqMove = "1,3,5";
        String seqNotMove = "2,4,6,7,8,9,10";

        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);
        //for search:
        prodImap.select().inbox();
        List<String> uidsBeforeMove = prodImap.search().uidMessages(seqNotMove);

        //for move:
        imap.select().inbox();
        imap.request(move(seqMove, folderName)).shouldBeOk()
                .shouldBeFromUids(Joiner.on(",").join(prodImap.search().uidMessages(seqMove)))
                //так как папка новая, то uid всегда такие
                .shouldBeToUids("1:" + countMoveMessages);

        imap.select().waitMsgsInInbox(countOfMessages - countMoveMessages);
        imap.status().numberOfMessagesShouldBe(Folders.INBOX, countOfMessages - countMoveMessages);

        imap.request(search().all().uid(true)).shouldContain(uidsBeforeMove);

        imap.select().waitMsgs(folderName, countMoveMessages);
        imap.status().numberOfMessagesShouldBe(folderName, countMoveMessages);
    }


    @Test
    @Description("Аппендим пару писем, переносим по uid в пользовательскую папку [MAILPROTO-2318]")
    @Severity(SeverityLevel.BLOCKER)
    @Stories({MyStories.JIRA, "UID CMD"})
    @ru.yandex.qatools.allure.annotations.TestCaseId("295")
    public void simpleUidMoveTest() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);

        String folderName = Util.getRandomString();
        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);
        //for search:
        prodImap.select().inbox();
        //for move:
        imap.select().inbox();
        imap.request(move(prodImap.search().uidAllMessages(), folderName).uid(true)).shouldBeOk();

        imap.select().waitMsgsInInbox(0);
        imap.status().shouldBeNoMessagesInInbox();

        imap.select().waitMsgs(folderName, NUMBER_OF_MESSAGES);
        imap.status().numberOfMessagesShouldBe(folderName, NUMBER_OF_MESSAGES);
    }

    @Test
    @Description("Переносим письма в несуществующую папку")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("296")
    public void moveInNotExistFolder() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);
        imap.select().inbox();
        imap.request(move("*", Util.getRandomString())).shouldBeNo().statusLineContains(MoveResponse.NO_SUCH_FOLDER);

        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);
        imap.status().numberOfMessagesInInboxShouldBe(NUMBER_OF_MESSAGES);
    }

    @Test
    @Description("MOVE с пустым параметром \"\" и существующей папкой")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("297")
    public void moveWithoutMessageSequenceParam() throws Exception {
        String folderName = Util.getRandomString();
        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);

        imap.select().inbox();
        imap.request(move("", Util.getRandomString())).shouldBeBad().statusLineContains(MoveResponse.COMMAND_SYNTAX_ERROR);
        imap.status().shouldBeNoMessagesInInbox();
    }

    @Test
    @Description("MOVE с существующей последовательностью писем и без параметра папки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("298")
    public void moveWithoutFolderParam() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);
        imap.select().inbox();
        imap.request(move("*", "")).shouldBeBad().statusLineContains(MoveResponse.COMMAND_SYNTAX_ERROR);

        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);
        imap.status().numberOfMessagesInInboxShouldBe(NUMBER_OF_MESSAGES);
    }

    @Test
    @Description("Переносим письма в незаэнкоженную папку")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("299")
    public void moveNotEncodeFolder() throws Exception {
        prodImap.append().appendRandomMessageInInbox();
        imap.select().waitMsgsInInbox(1);

        String folderName = Util.getRandomString();
        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);

        imap.request(move("*", Utils.cyrillic())).shouldBeBad()
                .statusLineContains(MoveResponse.FOLDER_ENCODING_ERROR);
    }

}
