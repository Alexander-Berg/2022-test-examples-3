package ru.yandex.autotests.innerpochta.imap.search;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.String.valueOf;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 14.07.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по размеру письма")
@Features({ImapCmd.SEARCH})
@Stories("#поиск по размеру письма")
@Description("Вытаскиваем размер произвольного письма и ищем по нему используя ключ LARGER.")
public class SearchBySizeLargerThenTest extends BaseTest {
    private static Class<?> currentClass = SearchBySizeLargerThenTest.class;

    public static int messageSize;

    public static String messageId = "";
    public static String messageUid = "";

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @BeforeClass
    public static void setUp() throws Exception {
        imap.select().inbox();

        messageId = imap.request(search().all()).shouldNotBeEmpty().getLastMessage();
        messageSize = imap.fetch().getSize(messageId);
        messageUid = String.valueOf(imap.fetch().uid(messageId));
    }

    @Test
    @Description("Ищем сообщение, которое больше заданного значения. Знаем, какое сообщение должно найтись")
    @ru.yandex.qatools.allure.annotations.TestCaseId("531")
    public void searchSizeLargerThan() {
        imap.request(search().larger(valueOf(messageSize - 1))).shouldBeOk().shouldSeeMessages(messageId);
        imap.request(search().larger(valueOf(messageSize + 1))).shouldBeOk().shouldBeEmpty();
        imap.request(search().larger(valueOf(messageSize))).shouldBeOk().shouldBeEmpty();
    }

    @Test
    @Description("Ищем сообщение по UID, которое больше заданного значения. Знаем, какое сообщение должно найтись\n" +
            "Сравниваем по количеству с ответом без UID")
    @ru.yandex.qatools.allure.annotations.TestCaseId("532")
    public void searchSizeLargerThanWithUid() {
        imap.request(search().uid(true).larger(valueOf(messageSize + 1))).shouldBeOk().shouldBeEmpty();
        imap.request(search().uid(true).larger(valueOf(messageSize - 1))).shouldBeOk().shouldSeeMessages(messageUid);
    }
}
