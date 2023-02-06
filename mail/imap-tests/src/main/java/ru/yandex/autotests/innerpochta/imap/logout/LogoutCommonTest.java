package ru.yandex.autotests.innerpochta.imap.logout;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.LogoutRequest.logout;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.responses.LogoutResponse.BYE_MESSAGE;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.03.14
 * Time: 15:23
 */
@Aqua.Test
@Title("Команда LOGOUT. Общие тесты")
@Features({ImapCmd.LOGOUT})
@Stories(MyStories.COMMON)
@Description("Общие тесты на logout с селектом")
public class LogoutCommonTest extends BaseTest {
    private static Class<?> currentClass = LogoutCommonTest.class;


    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Test
    @Description("Проверяем выдачу logout\n"
            + "без выбором определенной папки (без select условно говоря)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("265")
    public void testNotSelectedLogout() {
        imap.request(logout()).shouldBeOk().shouldSeeBuyMessage(BYE_MESSAGE);
    }

    @Test
    @Description("Проверяем выдачу logout\n"
            + " с выбором INBOX")
    @ru.yandex.qatools.allure.annotations.TestCaseId("266")
    public void testSelectedLogout() {
        imap.request(select(Folders.INBOX));
        imap.request(logout()).shouldBeOk().shouldSeeBuyMessage(BYE_MESSAGE);
    }
}
