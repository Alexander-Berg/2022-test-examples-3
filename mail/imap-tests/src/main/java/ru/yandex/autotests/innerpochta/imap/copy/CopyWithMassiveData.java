package ru.yandex.autotests.innerpochta.imap.copy;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.model.SeverityLevel;

import static ru.yandex.autotests.innerpochta.imap.requests.CopyRequest.copy;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 06.05.14
 * Time: 18:37
 */
@Aqua.Test
@Title("Команда COPY. Копируем много писем")
@Features({ImapCmd.COPY})
@Stories(MyStories.BIG_DATA)
@Description("Копируем большое количество сообщений")
public class CopyWithMassiveData extends BaseTest {
    private static Class<?> currentClass = CopyWithMassiveData.class;

    public static final int NUMBER_OF_MESSAGES = 1000;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Before
    public void prepareMessages() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(Folders.INBOX, NUMBER_OF_MESSAGES);
        prodImap.select().waitMsgs(Folders.INBOX, NUMBER_OF_MESSAGES);
    }

    @Severity(SeverityLevel.MINOR)
    @Description("Копируем ~1000 сообщений из inbox-а в inbox")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("116")
    public void testMassiveCopyToThisFolder() throws Exception {
        imap.request(select(Folders.INBOX)).shouldBeOk();
        imap.request(copy("1:" + NUMBER_OF_MESSAGES, Folders.INBOX)).shouldBeOk();
        imap.select().waitMsgs(Folders.INBOX, NUMBER_OF_MESSAGES * 2);
    }

    @Severity(SeverityLevel.MINOR)
    @Description("Копируем ~1000 сообщений из inbox-а в пользовательскую папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("115")
    public void testMassiveCopyToUserFolder() throws Exception {
        String folderName = Utils.generateName();
        prodImap.request(create(folderName)).shouldBeOk();

        imap.list().shouldSeeFolder(folderName);
        imap.request(select(Folders.INBOX)).shouldBeOk();
        imap.request(copy("1:" + NUMBER_OF_MESSAGES, folderName)).shouldBeOk();
        imap.select().waitMsgs(Folders.INBOX, NUMBER_OF_MESSAGES);
    }
}
