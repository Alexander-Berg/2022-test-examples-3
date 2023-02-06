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
 * [MAILPROTO-2238]
 */
@Aqua.Test
@Title("Команда SEARCH. Поля сообщения")
@Features({ImapCmd.SEARCH})
@Stories("#поиск по полям сообщения")
@Description("Поиск по полю To\n" +
        "Берём произвольное письмо, вытаскиваем из него поле TO и смотрим, что в поиске по такому полю не пусто")
public class SearchByFieldToTest extends BaseTest {
    private static Class<?> currentClass = SearchByFieldToTest.class;


    public static final String BAD_RESP = "bvcxt";
    public static String rightInterval = "";
    public static String leftInterval = "";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private static String to;
    private static String messageId = "1";

    @BeforeClass
    public static void setUp() throws Exception {
        imap.select().inbox();

        to = imap.fetch().getToField(messageId);
        rightInterval = to.substring(0, to.length() / 2);
        leftInterval = to.substring(to.length() / 2);
    }

    @Test
    @Description("Ищем письмо по полному совпадению поля TO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("515")
    public void shouldSearchByFieldTo() throws MessagingException {
        imap.search().shouldSearch(search().to(to)).shouldBeOk().shouldSeeMessages(messageId);
        imap.request(search().to(to).to(rightInterval)).shouldBeOk().shouldSeeMessages(messageId);
        imap.request(search().to(to).to(leftInterval)).shouldBeOk().shouldSeeMessages(messageId);
        imap.request(search().to(to).to(BAD_RESP)).shouldBeOk().shouldNotContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля TO. Берём первую половину строки to.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("517")
    public void searchToFieldRightInterval() throws MessagingException {
        imap.search().shouldSearch(search().to(rightInterval)).shouldBeOk().shouldSeeMessages(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля TO используя ключ TEXT.\n" +
            "Берём первую половину строки to.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("518")
    public void searchToFieldRightIntervalOverText() throws MessagingException {
        imap.search().shouldSearch(search().text(rightInterval)).shouldBeOk().shouldSeeMessages(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля TO. Берём вторую половину строки to.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("519")
    public void searchToFieldLeftInterval() throws MessagingException {
        imap.search().shouldSearch(search().to(leftInterval)).shouldBeOk().shouldSeeMessages(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля TO используя ключ TEXT.\n" +
            "Берём вторую половину строки to.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("520")
    public void searchToFieldLeftIntervalOverText() throws MessagingException {
        imap.search().shouldSearch(search().to(leftInterval)).shouldBeOk().shouldSeeMessages(messageId);
    }

    @Test
    @Description("Ищем письмо по полному совпадению поля TO используя ключ HEADER.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("521")
    public void searchToFieldHeader() throws MessagingException {
        imap.search().shouldSearch(search().header(headers().to(), to))
                .shouldBeOk().shouldSeeMessages(messageId);
        imap.request(search().header(headers().to(), BAD_RESP)).shouldBeOk().shouldNotContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля TO (первая половина to).\n" +
            "Ищем используя ключ HEADER. MAILPROTO-2238")
    @Stories({MyStories.JIRA, MyStories.SEARCH_HEADER})
    @ru.yandex.qatools.allure.annotations.TestCaseId("522")
    public void searchToFieldHeaderRightInterval() throws MessagingException {
        imap.search().shouldSearch(search().header(headers().to(), rightInterval))
                .shouldBeOk().shouldSeeMessages(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля TO (вторая половина to).\n" +
            "Ищем используя ключ HEADER. MAILPROTO-2238")
    @Stories({MyStories.JIRA, MyStories.SEARCH_HEADER})
    @ru.yandex.qatools.allure.annotations.TestCaseId("523")
    public void searchToFieldHeaderLeftInterval() throws MessagingException {
        imap.search().shouldSearch(search().header(headers().to().toString(), leftInterval))
                .shouldBeOk().shouldSeeMessages(messageId);
    }
}
