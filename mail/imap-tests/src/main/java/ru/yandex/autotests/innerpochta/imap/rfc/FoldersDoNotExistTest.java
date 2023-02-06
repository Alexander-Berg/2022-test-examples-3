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
@Title("Несуществующие папки")
@Features({"RFC"})
@Stories({"Работа с папками", "Негативные тесты"})
@RunWith(Parameterized.class)
public class FoldersDoNotExistTest extends BaseTest {
    private static Class<?> currentClass = FoldersDoNotExistTest.class;

    private final String folderName;
    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    public FoldersDoNotExistTest(String folderName) {
        this.folderName = folderName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> getFolderNames() {
        return Arrays.asList(new String[][]{
                {"Inbox1"},
                {"blah"},
                {"blahblah"}
        });
    }

    @Test
    @Features(ImapCmd.SELECT)
    @ru.yandex.qatools.allure.annotations.TestCaseId("437")
    public void selectOnNotExistFolderReturnsNo() {
        imap.request(select(folderName)).shouldBeNo();
    }

    @Test
    @Features(ImapCmd.EXAMINE)
    @ru.yandex.qatools.allure.annotations.TestCaseId("438")
    public void examineOnNotExistFolderReturnsNo() {
        imap.request(examine(folderName)).shouldBeNo();
    }
}
