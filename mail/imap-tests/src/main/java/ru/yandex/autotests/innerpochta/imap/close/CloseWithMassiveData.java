package ru.yandex.autotests.innerpochta.imap.close;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.StoreRequest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.base.Joiner.on;
import static ru.yandex.autotests.innerpochta.imap.requests.CloseRequest.close;
import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 29.04.14
 * Time: 20:38
 */
@Aqua.Test
@Title("Команда CLOSE. Закрываем папки. Работа с двумя сессиями и большими данными")
@Features({ImapCmd.CLOSE})
@Stories(MyStories.BIG_DATA)
@Description("Проверяем реакцию на закрытие с двумя сессиями и некорректными данными\n" +
        "Непараметризованный тест")
public class CloseWithMassiveData extends BaseTest {
    private static Class<?> currentClass = CloseWithMassiveData.class;


    public static final int NUMBER_OF_MESSAGES = 10000;


    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient imap2 = newLoginedClient(currentClass);

    @Description("В одной сессии помечаем письма /Deleted, в другой закрываем")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("96")
    public void closeWithTwoSession() throws Exception {
        imap2.append().appendRandomMessage(Folders.INBOX);
        imap2.select().waitMsgs(Folders.INBOX, 1);

        imap2.request(select(Folders.INBOX)).shouldBeOk().repeatUntilOk(imap);
        Iterable<String> lettersToDelete = imap2.request(search().all()).shouldBeOk().getMessages();
        imap2.request(store(on(",").join(lettersToDelete), StoreRequest.FLAGS, roundBraceList(MessageFlags.DELETED.value()))).shouldBeOk();
        imap2.request(unselect()).shouldBeOk();

        //добавить ожидание флагов
        imap.request(fetch("1").flags()).shouldBeOk();

        imap.request(select(Folders.INBOX)).shouldBeOk();
        imap.request(close()).shouldBeOk();

        imap2.select().waitMsgs(Folders.INBOX, 0);
        imap2.status().numberOfMessagesShouldBe(Folders.INBOX, 0);
    }

    @Test
    @Web
    @Description("Закрываем папку с ~10000 сообщениями")
    @ru.yandex.qatools.allure.annotations.TestCaseId("95")
    public void closeWithMassiveData() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(Folders.INBOX, NUMBER_OF_MESSAGES);
        prodImap.select().waitMsgs(Folders.INBOX, NUMBER_OF_MESSAGES);
        imap.request(select(Folders.INBOX)).shouldBeOk();
        imap.store().deletedOnSequence("1:*");

        imap.request(close()).shouldBeOk();
        prodImap.select().waitMsgs(Folders.INBOX, 0);
    }
}
