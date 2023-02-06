package ru.yandex.autotests.innerpochta.imap.lsub;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.LsubRequest.lsub;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.rules.UnsubscribeRule.withUnsubscribeBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.ItemContainer.newItem;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.04.14
 * Time: 14:28
 * MAILPROTO-2152
 */
@Aqua.Test
@Title("Команда LSUB. Список подписанных папок. Общие тесты")
@Features({ImapCmd.LSUB})
@Stories(MyStories.COMMON)
@Description("Общие тесты на LSUB. LSUB без параметров")
public class LsubCommonTest extends BaseTest {
    private static Class<?> currentClass = LsubCommonTest.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));


    @Rule
    public ImapClient prodImap = withUnsubscribeBefore(withCleanBefore(newLoginedClient(currentClass)));

    @Description("LSUB кириллической папки без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("268")
    public void listCyrillicFolderShouldSeeBad() {
        imap.request(lsub("", Utils.cyrillic())).shouldBeBad()
                .statusLineContains(ImapResponse.COMMAND_SYNTAX_ERROR);
    }

    @Description("LSUB без параметров")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("269")
    public void lsubWithoutParam() {
        imap.request(lsub("", "")).shouldBeBad();
    }

    @Description("LSUB c \"\" \"\" \n [MAILPROTO-2152]" +
            "Должны увидеть: (\\Noselect) \"|\" \"\"")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("270")
    public void lsubWithEmptyParamsShouldSee() {
        imap.request(lsub("\"\"", "\"\"")).shouldBeOk()
                .withItems(newItem().setName("").setFlags(FolderFlags.NO_SELECT.value()).getListItem());
    }
}
