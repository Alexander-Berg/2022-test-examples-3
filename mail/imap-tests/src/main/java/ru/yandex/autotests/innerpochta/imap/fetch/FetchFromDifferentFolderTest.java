package ru.yandex.autotests.innerpochta.imap.fetch;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static com.sun.mail.imap.protocol.BASE64MailboxEncoder.encode;
import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;

@Aqua.Test(title = "Команда FETCH. Втягиваем сообщения из разных папок")
@Features({ImapCmd.FETCH})
@Stories(MyStories.COMMON)
@Description("Получаем данные с помощью FETCH из разных папок с разной вложенностью")
@RunWith(value = Parameterized.class)
public class FetchFromDifferentFolderTest extends BaseTest {
    private static Class<?> currentClass = FetchFromDifferentFolderTest.class;
    // eng|rus|eng & rus|eng|rus & eng & rus & eng|rus & rus|eng
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private String folder;

    public FetchFromDifferentFolderTest(String request) {
        this.folder = encode(request);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(
                new Object[]{systemFolders().getDeleted()},
                new Object[]{systemFolders().getDrafts()},
                new Object[]{systemFolders().getSent()},
                new Object[]{systemFolders().getSpam()},
                new Object[]{"вложение"},
                new Object[]{"embedding"},
                new Object[]{"embedding|первый"},
                new Object[]{"вложение|first"},
                new Object[]{"embedding|первый|second"},
                new Object[]{"вложение|first|вторая"}
        );
    }

    @Before
    public void setUp() {
        imap.select().inbox();
    }

    @Description("Находясь в нужной папке фетчим простое сообщение")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("209")
    public void fetchFromDifferentFolder() {
        imap.request(fetch("1:*").fast()).shouldBeOk();
    }
}
