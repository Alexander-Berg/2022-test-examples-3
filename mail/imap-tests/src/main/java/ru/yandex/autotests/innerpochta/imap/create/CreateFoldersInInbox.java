package ru.yandex.autotests.innerpochta.imap.create;

import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.data.TestData;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.03.14
 * Time: 15:57
 * <p/>
 * [MAILPROTO-1965]
 */
@Aqua.Test
@Title("Команда CREATE. Создание папки в inbox, пользовательской папки Inbox")
@Features({ImapCmd.CREATE})
@Stories({"#папка inbox", "#подпапки в inbox"})
@Description("Создаем различныe папки в папке inbox")
@RunWith(Parameterized.class)
public class CreateFoldersInInbox extends BaseTest {
    private static Class<?> currentClass = CreateFoldersInInbox.class;


    public static final int SIZE = 6;


    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private final String inbox;
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    public CreateFoldersInInbox(String folderName) {
        this.inbox = folderName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getFolderNames() {
        return TestData.allKindsOfInbox();
    }

    @Test
    @Description("Создаем пользовательскую папку с именем системной inbox\n"
            + "Ожидаемый результат: BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("127")
    public void createInboxFolder() {
        imap.request(create(inbox)).shouldBeBad();
    }

    @Test
    @Description("Удаляем inbox\n"
            + "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("128")
    public void deleteInboxFolder() {
        imap.request(delete(inbox)).shouldBeNo();
    }
}
