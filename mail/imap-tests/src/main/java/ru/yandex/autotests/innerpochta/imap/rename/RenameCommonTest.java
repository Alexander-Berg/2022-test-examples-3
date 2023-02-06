package ru.yandex.autotests.innerpochta.imap.rename;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.ImapConsts;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.RenameResponse;
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.sun.mail.imap.protocol.BASE64MailboxEncoder.encode;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.RenameRequest.rename;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 01.04.14
 * Time: 21:57
 */
@Aqua.Test
@Title("Команда RENAME. Общие тесты.")
@Features({ImapCmd.RENAME})
@Stories(MyStories.COMMON)
@Description("Общие тесты на RENAME. RENAME без параметров")
public class RenameCommonTest extends BaseTest {
    private static Class<?> currentClass = RenameCommonTest.class;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String folderName = encode(Utils.cyrillic());

    @Description("RENAME в кириллическую папки без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("402")
    public void renameCyrillicFolderShouldSeeBad() {
        prodImap.request(create(folderName));
        imap.list().shouldSeeFolder(folderName);

        imap.request(rename(folderName, Utils.cyrillic())).shouldBeBad()
                .statusLineContains(RenameResponse.FOLDER_ENCODING_ERROR);
    }

    @Description("RENAME без параметров\n" +
            "Должны увидеть: BAD")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("403")
    public void renameWithoutParamShouldSeeBad() {
        imap.request(rename("", "")).shouldBeBad();
    }

    @Description("Пробуем переименовать папку в папку с пустым именем")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("404")
    public void renameEmptyFolderShouldSeeBad() {
        prodImap.request(create(folderName));
        imap.request(rename("", folderName)).shouldBeBad();
    }

    @Description("Пробуем переименовать папку в папку с пустым именем")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("405")
    public void renameToFolderEmptyFolderShouldSeeBad() {
        prodImap.request(create(folderName));
        imap.request(rename(folderName, "")).shouldBeBad();
    }

    @Test
    @Issue("MAILDLV-421")
    @Title("Переименование в папку длинным именем")
    @Description("Пробуем переименовать папку в папку с слишком длинными именем")
    @ru.yandex.qatools.allure.annotations.TestCaseId("406")
    public void shouldNotRenameWithLongName() {
        prodImap.request(create(folderName));
        imap.request(rename(folderName, ImapConsts.LONG_NAME)).shouldBeBad();
    }

    @Test
    @Issue("MPROTO-2048")
    @Title("Переименование в иархию")
    @Description("Пробуем переименовать папку в иархию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("401")
    public void shouldRenameToSubfolder() {
        String newFolderName = folderName + "|" + Util.getRandomString();
        prodImap.request(create(folderName));
        imap.request(rename(folderName, newFolderName)).shouldBeOk();
        imap.list().shouldSeeFolder(newFolderName);
        imap.list().shouldSeeFolder(folderName);
    }

    @Test
    @Issue("MPROTO-2048")
    @Title("Переименование в иархию")
    @Description("Пробуем переименовать папку в иархию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("407")
    public void shouldRenameToHierarchy() {
        prodImap.request(create(folderName));
        FolderContainer hierarchy = newFolder(4);
        imap.request(rename(folderName, hierarchy.fullName())).shouldBeOk();
        imap.list().shouldNotSeeFolder(folderName);
        imap.list().shouldSeeFolders(hierarchy.foldersTreeAsList());
    }

    @Test
    @Title("Переименование в длинную иархию")
    @Description("Пробуем переименовать папку в очень длинную иархию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("408")
    public void renameToBigHierarchyShouldSeeBad() {
        prodImap.request(create(folderName));
        FolderContainer hierarchy = newFolder(15);
        imap.request(rename(folderName, hierarchy.fullName())).shouldBeBad();
        imap.list().shouldSeeFolder(folderName);
        imap.list().shouldNotSeeFolders(hierarchy.foldersTreeAsList());
    }
}
