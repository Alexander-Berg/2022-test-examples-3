package ru.yandex.autotests.innerpochta.imap.list;

import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags;
import ru.yandex.autotests.innerpochta.imap.consts.flags.SystemFolderFlags;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;
import static ru.yandex.autotests.innerpochta.imap.structures.ItemContainer.newItem;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 21.04.14
 * Time: 18:58
 */
@Aqua.Test
@Title("Команда LIST. Список существующих папок. Общие тесты")
@Features({ImapCmd.LIST})
@Stories(MyStories.COMMON)
@Description("LIST без парамеров\n"
        + "Позитивное и негативное тестирование")
public class ListCommonTest extends BaseTest {
    private static Class<?> currentClass = ListCommonTest.class;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));

    @Description("LIST кириллической папки без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("228")
    public void listCyrillicFolderShouldSeeBad() {
        imap.request(list("", Utils.cyrillic())).shouldBeBad()
                .statusLineContains(ImapResponse.COMMAND_SYNTAX_ERROR);
    }

    @Test
    @Title("Много раз делаем list. Ловим корку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("229")
    public void shouldSeeOkManyTimes() {
        for (int i = 0; i < 100; i++) {
            imap.request(list("\"\"", "*")).shouldBeOk();
        }
    }

    @Description("LIST без параметров")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("230")
    public void listWithoutParamsShouldSeeBad() {
        imap.request(list("", "")).shouldBeBad();
    }

    @Description("LIST c \"\" \"\" \n" +
            "Должны увидеть: (\\Noselect) \"|\" \"\"")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("231")
    public void listWithEmptyParamsShouldSee() {
        imap.request(list("\"\"", "\"\"")).shouldBeOk()
                .withItems(newItem().setName("").setFlags(FolderFlags.NO_SELECT.value()).getListItem());
    }

    @Description("Должны разрешать создавать папки на корпах [MAILPROTO-2189]\n" +
            "Но при этом не должны создавать папки в вэбах")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("232")
    public void inboxFlagsTest() {
        imap.request(list("\"\"", "*")).shouldBeOk()
                .withItems(SystemFolderFlags.getINBOXItem(),
                        SystemFolderFlags.getSentItem(),
                        SystemFolderFlags.getTrashItem(),
                        SystemFolderFlags.getSpamItem(),
                        SystemFolderFlags.getDraftsItem(),
                        SystemFolderFlags.getOutgoingItem());

        imap.request(list("\"\"", "%")).shouldBeOk()
                .withItems(SystemFolderFlags.getINBOXItem(),
                        SystemFolderFlags.getSentItem(),
                        SystemFolderFlags.getTrashItem(),
                        SystemFolderFlags.getSpamItem(),
                        SystemFolderFlags.getDraftsItem(),
                        SystemFolderFlags.getOutgoingItem());

        imap.request(list("\"\"", Folders.INBOX)).shouldBeOk()
                .withItems(SystemFolderFlags.getINBOXItem());
    }
}
