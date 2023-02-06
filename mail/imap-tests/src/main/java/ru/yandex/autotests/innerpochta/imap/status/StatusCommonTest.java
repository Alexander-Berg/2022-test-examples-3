package ru.yandex.autotests.innerpochta.imap.status;

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
import ru.yandex.autotests.innerpochta.imap.responses.StatusResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.matchers.ListItemMatcher.listItem;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.StatusRequest.status;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.quoted;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 14.04.14
 * Time: 16:17
 */
@Aqua.Test
@Title("Команда STATUS. Общие тесты")
@Features({ImapCmd.STATUS})
@Stories(MyStories.COMMON)
@Description("Общие тесты на STATUS. STATUS без параметров")
public class StatusCommonTest extends BaseTest {
    private static Class<?> currentClass = StatusCommonTest.class;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));
    private static String nameWithSpecialSymbols = "abc\\\"abc";
    private static String expectedName = "abc\"abc";
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Description("STATUS без параметров\n" +
            "Должны увидеть: BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("590")
    public void statusWithEmptyParamShouldSeeBad() {
        imap.request(status("")).shouldBeBad().statusLineContains(ImapResponse.COMMAND_SYNTAX_ERROR);
    }

    @Test
    @Description("Статус папки с русским именем без энкодинга [MAILPROTO-2141]\n" +
            "Ожидаемый резултат: BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("591")
    public void statusCyrillicFolderShouldSeeBad() {
        imap.request(status(Utils.cyrillic())).shouldBeBad().statusLineContains(StatusResponse.COMMAND_SYNTAX_ERROR);
        imap.list().shouldSeeOnlySystemFoldersWithFlags();
    }

    @Test
    @Issue("MPROTO-1532")
    @Title("STATUS для папки со спецсимволом")
    @Description("Не работал STATUS папки для папки abc\\\"abc со спецсимволом: раньше возвращали " +
            "NO [TRYCREATE] status No such folder. sc=fGQBV40eT4Yv")
    @ru.yandex.qatools.allure.annotations.TestCaseId("592")
    public void statusWithEncodedSymbolTest() {
        imap.request(create(quoted(nameWithSpecialSymbols))).shouldBeOk();
        imap.request(select(quoted(nameWithSpecialSymbols))).shouldBeOk();
        //литералы
        String name = imap.request(list("\"\"", quoted(nameWithSpecialSymbols))).shouldBeOk()
                .withItem(listItem("|", expectedName, FolderFlags.UNMARKED.value(),
                        FolderFlags.HAS_NO_CHILDREN.value())).lines().get(1);
        assertThat(name, equalTo(expectedName));

        imap.request(status(quoted(nameWithSpecialSymbols)).messages()).shouldBeOk();
    }
}
