package ru.yandex.autotests.innerpochta.pop3.common;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.base.Pop3Cmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.core.pop3.Pop3Client;
import ru.yandex.autotests.innerpochta.imap.steps.ClearSteps;
import ru.yandex.autotests.innerpochta.pop3.base.BaseTest;
import ru.yandex.autotests.innerpochta.pop3.steps.WMISteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;


@Web
@Aqua.Test
@Features({MyStories.POP3})
@Stories({Pop3Cmd.QUIT})
@Title("Проверям, что письмо не помечаются прочитанными в web при не проставленной настройке")
@Description("При получении почты по POP3 письма в почтовом ящике Яндекс.Почты помечать как прочитанные")
public class Pop3NotMakesReadTest extends BaseTest {


    public static final String USER_FOLDER = "userFolder";

    public static final String INBOX = "inbox";
    @ClassRule
    public static ImapClient prodImap = imap(LOGIN_GROUP);
    private static ClearSteps clearSteps;
    private static List<String> msgInbox;
    private static List<String> msgFolder;
    public Pop3Client pop3 = new Pop3Client().pop3(LOGIN_GROUP);
    private WMISteps wmiSteps = new WMISteps();

    @Before
    public void prepareData() throws Exception {
        clearSteps = new ClearSteps(prodImap);
        clearSteps.clearFolder(INBOX);
        clearSteps.clearFolder(USER_FOLDER);
        prodImap.select().waitMsgsInInbox(0);
    }

    @Before
    public void before() {
        wmiSteps.withLogin(pop3.getUser(), pop3.getPass());
    }

    @Test
    @Title("Не должны пометить письмо прочитанным после получения по протоколу pop3 в инбоксе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("666")
    public void shouldNotMakeMessageAsReadAfterClosePopSession() throws Exception {
        msgInbox = prodImap.append().appendRandomMessagesToFolder(INBOX, 2);
        prodImap.select().waitMsgsInInbox(2);

        prodImap.list().shouldSeeFolder(USER_FOLDER);
        msgFolder = prodImap.append().appendRandomMessagesToFolder(USER_FOLDER, 1);
        prodImap.select().waitMsgs(USER_FOLDER, 1);


        wmiSteps.shouldNotSeeMessageAsRead(msgInbox.get(0), INBOX);
        wmiSteps.shouldNotSeeMessageAsRead(msgInbox.get(1), INBOX);
        wmiSteps.shouldNotSeeMessageAsRead(msgFolder.get(0), USER_FOLDER);

        pop3.connect();
        pop3.retr(1);
        pop3.retr(2);
        pop3.retr(3);

        wmiSteps.shouldNotSeeMessageAsRead(msgInbox.get(0), INBOX);
        wmiSteps.shouldNotSeeMessageAsRead(msgInbox.get(1), INBOX);
        wmiSteps.shouldNotSeeMessageAsRead(msgFolder.get(0), USER_FOLDER);

        pop3.quit(true);

        wmiSteps.shouldNotSeeMessageAsRead(msgInbox.get(0), INBOX);
        wmiSteps.shouldNotSeeMessageAsRead(msgInbox.get(1), INBOX);
        wmiSteps.shouldNotSeeMessageAsRead(msgFolder.get(0), USER_FOLDER);
    }

    @After
    public void after() {
        pop3.disconnect();
    }
}
