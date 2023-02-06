package ru.yandex.autotests.innerpochta.imap.expunge;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.ExpungeRequest.expunge;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 01.05.14
 * Time: 18:28
 */
@Aqua.Test
@Title("Команда EXPUNGE. Удаление большого количества писем")
@Features({ImapCmd.EXPUNGE})
@Stories(MyStories.BIG_DATA)
@Description("Удаляем большое количество писем. Работа с двумя сессиями")
public class ExpungeWithMassiveData extends BaseTest {
    private static Class<?> currentClass = ExpungeWithMassiveData.class;

    public static final int NUMBER_OF_MESSAGES = 1000;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Web
    @Description("Удаляем ~ 1000 сообщений в inbox-е")
    @ru.yandex.qatools.allure.annotations.TestCaseId("187")
    public void deleteMassiveData() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(Folders.INBOX, NUMBER_OF_MESSAGES);
        prodImap.select().waitMsgs(Folders.INBOX, NUMBER_OF_MESSAGES);
        imap.request(select(Folders.INBOX)).shouldBeOk();
        imap.store().deletedOnSequence("1:*");

        imap.request(expunge()).shouldBeOk();

        prodImap.select().waitMsgs(Folders.INBOX, 0);
    }
}
