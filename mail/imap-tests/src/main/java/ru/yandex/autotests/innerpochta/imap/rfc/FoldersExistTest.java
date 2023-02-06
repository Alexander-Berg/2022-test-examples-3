package ru.yandex.autotests.innerpochta.imap.rfc;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;

@Aqua.Test
@Title("Существующие папки")
@Features({"RFC"})
@Stories("Работа с папками")
@RunWith(Parameterized.class)
public class FoldersExistTest extends BaseTest {
    private static Class<?> currentClass = FoldersExistTest.class;

    private final String folderName;
    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    public FoldersExistTest(String folderName) {
        this.folderName = folderName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> getFolderNames() {
        return Arrays.asList(new String[][]{
                {"Inbox"},
                {"INBOX"},
                {"inbox"},
                {"iNBOX"}
        });
    }

    @Test
    @Features(ImapCmd.SELECT)
    @ru.yandex.qatools.allure.annotations.TestCaseId("439")
    public void selectFolderInAnyCaseReturnsOk() {
        imap.request(select(folderName)).shouldBeOk();
    }

    @Test
    @Features(ImapCmd.EXAMINE)
    @ru.yandex.qatools.allure.annotations.TestCaseId("440")
    public void examineFolderInAnyCaseReturnsOk() {
        imap.request(examine(folderName)).shouldBeOk();
    }
}
