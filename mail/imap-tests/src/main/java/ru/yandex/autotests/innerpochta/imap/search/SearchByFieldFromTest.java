package ru.yandex.autotests.innerpochta.imap.search;

import javax.mail.MessagingException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.consts.Headers.headers;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 14.07.14.
 * <p>
 * [MAILPROTO-2238]
 * [MAILPROTO-2332]
 */
@Aqua.Test
@Title("Команда SEARCH. Поля сообщения")
@Features({ImapCmd.SEARCH})
@Stories("#поиск по полям сообщения")
@Description("Поиск по полю FROM\n" +
        "Берём произвольное письмо, вытаскиваем из него поле FROM и смотрим, что в поиске по такому полю не пусто")
public class SearchByFieldFromTest extends BaseTest {
    private static Class<?> currentClass = SearchByFieldFromTest.class;

    public static final String BAD_RESP = "bvcxt";

    public static String rightInterval = "";
    public static String leftInterval = "";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private static String fromAddress;
    private static String messageId = "1";

    @BeforeClass
    public static void setUp() throws Exception {
        imap.select().inbox();

        fromAddress = imap.fetch().getFromField(messageId);
        rightInterval = fromAddress.substring(0, fromAddress.length() / 2);
        leftInterval = fromAddress.substring(fromAddress.length() / 2);
    }

    @Test
    @Description("Ищем письмо по полному совпадению поля From")
    @ru.yandex.qatools.allure.annotations.TestCaseId("497")
    public void searchFromField() throws MessagingException {
        imap.search().shouldSearch(search().from(fromAddress)).shouldBeOk().shouldContain(messageId);
        imap.request(search().from(fromAddress).from(rightInterval)).shouldBeOk().shouldContain(messageId);
        imap.request(search().from(fromAddress).from(leftInterval)).shouldBeOk().shouldContain(messageId);
        imap.request(search().from(fromAddress).from(BAD_RESP)).shouldBeOk().shouldNotContain(messageId);
    }

    @Test
    @Description("Ищем письмо по полному совпадению поля From используя ключ TEXT [MAILPROTO-2332]")
    @Stories({MyStories.SEARCH_TEXT, MyStories.JIRA})
    @ru.yandex.qatools.allure.annotations.TestCaseId("498")
    public void searchFromFieldOverText() throws MessagingException {
        imap.search().shouldSearch(search().text(fromAddress)).shouldBeOk().shouldContain(messageId);
        imap.request(search().text(fromAddress).text(rightInterval)).shouldBeOk().shouldContain(messageId);
        imap.request(search().text(fromAddress).text(leftInterval)).shouldBeOk().shouldContain(messageId);
        imap.request(search().text(fromAddress).text(BAD_RESP)).shouldBeOk().shouldNotContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля From  используя ключ TEXT.\n" +
            "Берём первую половину строки from. [MAILPROTO-2332]")
    @Stories({MyStories.SEARCH_TEXT, MyStories.JIRA})
    @ru.yandex.qatools.allure.annotations.TestCaseId("499")
    public void searchFromFieldRightIntervalOvetText() throws MessagingException {
        imap.search().shouldSearch(search().text(rightInterval)).shouldBeOk().shouldContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля From используя ключ TEXT.\n" +
            "Берём вторую половину строки from. [MAILPROTO-2332]")
    @Stories({MyStories.SEARCH_TEXT, MyStories.JIRA})
    @ru.yandex.qatools.allure.annotations.TestCaseId("500")
    public void searchFromFieldLeftIntervalOverText() throws MessagingException {
        imap.search().shouldSearch(search().text(leftInterval)).shouldBeOk().shouldContain(messageId);
    }

    @Test
    @Description("Ищем письмо по полному совпадению поля FROM используя ключ HEADER.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("501")
    public void searchFromFieldHeader() throws MessagingException {
        imap.search().shouldSearch(search().header(headers().from().toString(), fromAddress))
                .shouldBeOk().shouldContain(messageId);
        imap.request(search().header(headers().from().toString(), fromAddress + BAD_RESP))
                .shouldBeOk().shouldNotContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля FROM (первая половина from) используя ключ HEADER.\n" +
            "MAILPROTO-2238")
    @Stories({MyStories.JIRA, MyStories.SEARCH_HEADER})
    @ru.yandex.qatools.allure.annotations.TestCaseId("502")
    public void searchFromFieldHeaderRightInterval() throws MessagingException {
        imap.search().shouldSearch(search().header(headers().from().toString(), rightInterval))
                .shouldBeOk().shouldContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля FROM (вторая половина from) используя ключ HEADER.\n" +
            "MAILPROTO-2238")
    @Stories({MyStories.JIRA, MyStories.SEARCH_HEADER})
    @ru.yandex.qatools.allure.annotations.TestCaseId("503")
    public void searchFromFieldHeaderLeftInterval() throws MessagingException {
        imap.search().shouldSearch(search().header(headers().from().toString(), leftInterval))
                .shouldBeOk().shouldContain(messageId);
    }
}
