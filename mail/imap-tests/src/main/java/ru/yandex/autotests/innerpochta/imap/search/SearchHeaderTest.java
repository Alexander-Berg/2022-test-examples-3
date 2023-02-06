package ru.yandex.autotests.innerpochta.imap.search;

import javax.mail.MessagingException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.SearchResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 20.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по заголовкам")
@Features({ImapCmd.SEARCH})
@Stories("#поиск по заголовкам")
@Description("Ищем по полю HEADER. Берём сообщение и вытаскиваем из него HEADER. Ищем по полям из HEADER. " +
        "Некоторое количество проверок сделано в тестах на поля to, subject, cc, bcc, from")
@Ignore
public class SearchHeaderTest extends BaseTest {
    private static Class<?> currentClass = SearchHeaderTest.class;


    public static final String HEADER_MESSAGE_ID = "Message-Id";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private static String mimeMessageId;
    private static String messageId = "1";
    private static String messageUid;
    private SearchResponse response;
    private SearchResponse responseUid;

    @BeforeClass
    public static void setUp() throws Exception {
        imap.select().inbox();

        messageUid = String.valueOf(imap.fetch().uid(messageId));
        mimeMessageId = imap.request(fetch(messageId).body("header"))
                .shouldBeOk()
                .constructMimeMessage().getMessageID();
    }

    @Test
    @Description("Простой поиск по HEADER. Вытакиваем у сообщения id. Ищем по этому id")
    @Stories(MyStories.SEARCH_HEADER)
    @ru.yandex.qatools.allure.annotations.TestCaseId("544")
    public void searchHeader() throws MessagingException {
        imap.search().shouldSearch(search().header(HEADER_MESSAGE_ID, mimeMessageId))
                .shouldBeOk()
                .shouldSeeMessages(messageId);
        imap.request(search().header(HEADER_MESSAGE_ID, mimeMessageId).all())
                .shouldBeOk()
                .shouldSeeMessages(messageId);
        imap.request(search().header(HEADER_MESSAGE_ID, mimeMessageId).header(HEADER_MESSAGE_ID, mimeMessageId))
                .shouldBeOk().shouldContain(messageId);
        imap.request(search().header(HEADER_MESSAGE_ID, mimeMessageId).not()
                .header(HEADER_MESSAGE_ID, mimeMessageId))
                .shouldBeOk().shouldBeEmpty();
    }

    @Test
    @Description("Простой поиск по HEADER. Сравниваем UID [MPROTO-261]")
    @Stories({MyStories.SEARCH_HEADER, MyStories.JIRA})
    @ru.yandex.qatools.allure.annotations.TestCaseId("545")
    public void searchHeaderUid() throws MessagingException {
        response = imap.search().shouldSearch(search().header(HEADER_MESSAGE_ID, mimeMessageId))
                .shouldBeOk()
                .shouldContain(messageId);
        responseUid = imap.request(search().uid(true).header(HEADER_MESSAGE_ID, mimeMessageId))
                .shouldBeOk()
                .shouldContain(messageUid);
        response.shouldHasSize(responseUid.getMessages().size());
    }
}
