package ru.yandex.autotests.innerpochta.imap.delete;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.DeleteResponse;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 01.04.14
 * Time: 21:56
 */
@Aqua.Test
@Title("Команда DELETE. Общие тесты")
@Features({ImapCmd.DELETE})
@Stories(MyStories.COMMON)
@Description("Удаление папки")
public class DeleteCommonTest extends BaseTest {
    private static Class<?> currentClass = DeleteCommonTest.class;


    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));


    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Description("DELETE без параметров")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("154")
    public void deleteWithoutParamShouldSeeBad() {
        imap.request(delete("")).shouldBeBad().statusLineContains(ImapResponse.COMMAND_SYNTAX_ERROR);
    }

    @Description("DELETE кириллической папки без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("155")
    public void deleteCyrillicFolderShouldSeeBad() {
        imap.request(delete(Utils.cyrillic())).shouldBeBad().statusLineContains(DeleteResponse.FOLDER_ENCODING_ERROR);
    }
}
