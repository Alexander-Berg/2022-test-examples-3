package ru.yandex.autotests.innerpochta.imap.search;

import javax.mail.MessagingException;

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
 * Created by kurau on 18.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по размеру письма")
@Features({ImapCmd.SEARCH})
@Stories("#поиск по размеру письма")
@Description("Пересекаем между собой разные запросы на поиск письма по размеру." +
        "\nБольше, меньше, равно дате произвольного письма.")
public class SearchBySizeCrossingTest extends BaseTest {
    private static Class<?> currentClass = SearchBySizeCrossingTest.class;

    public static int messageSize;
    public static String messageId = "";
    public static String messageUid = "";

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @BeforeClass
    public static void setUp() throws MessagingException {
        imap.select().inbox();
        messageId = imap.request(search().all()).shouldNotBeEmpty().getLastMessage();
        messageSize = imap.fetch().getSize(messageId);
        messageUid = String.valueOf(imap.fetch().uid(messageId));
    }

    @Test
    @Description("Ищем сообщение, которое меньше одного и больше другого заданного значения.\n " +
            "Знаем, какое сообщение должно найтись")
    @ru.yandex.qatools.allure.annotations.TestCaseId("529")
    public void searchSizeLargerVsSmaller() {
        imap.request(search().larger(valueOf(messageSize - 1)).smaller(valueOf(messageSize + 1))).shouldBeOk()
                .shouldContain(messageId);
        imap.request(search().larger(valueOf(messageSize + 1)).smaller(valueOf(messageSize - 1))).shouldBeOk()
                .shouldBeEmpty();
        imap.request(search().larger(valueOf(messageSize)).smaller(valueOf(messageSize))).shouldBeOk()
                .shouldNotContain(messageId);
    }
}
