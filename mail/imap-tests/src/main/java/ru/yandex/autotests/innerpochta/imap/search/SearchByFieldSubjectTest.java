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
 * [MAILPROTO-2332]
 * [MAILPROTO-2238]
 */
@Aqua.Test
@Title("Команда SEARCH. Поля сообщения")
@Features({ImapCmd.SEARCH})
@Stories("#поиск по полям сообщения")
@Description("Поиск по полю SUBJECT\n" +
        "Берём произвольное письмо, вытаскиваем из него поле SUBJECT и смотрим, что в поиске по такому полю не пусто")
public class SearchByFieldSubjectTest extends BaseTest {
    private static Class<?> currentClass = SearchByFieldSubjectTest.class;

    public static final String BAD_RESP = "bvcxt";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private static String rightInterval = "";
    private static String leftInterval = "";
    private static String subject;
    private static String messageId = "1";

    @BeforeClass
    public static void setUp() throws Exception {
        imap.select().inbox();

        subject = imap.fetch().getSubject(messageId);
        rightInterval = subject.substring(0, subject.length() / 2);
        leftInterval = subject.substring(subject.length() / 2);
    }

    @Test
    @Description("Ищем письмо по полному совпадению поля SUBJECT.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("504")
    public void searchSubjectField() throws MessagingException {
        imap.search().shouldSearch(search().subject(subject)).shouldBeOk().shouldContain(messageId);
        imap.request(search().subject(subject).subject(rightInterval)).shouldBeOk().shouldContain(messageId);
        imap.request(search().subject(subject).subject(leftInterval)).shouldBeOk().shouldContain(messageId);
        imap.request(search().subject(subject).subject(BAD_RESP)).shouldBeOk().shouldNotContain(messageId);
    }

    @Test
    @Description("Ищем письмо по полному совпадению поля SUBJECT используя ключ TEXT. [MAILPROTO-2332]")
    @Stories({MyStories.JIRA, MyStories.SEARCH_TEXT})
    @ru.yandex.qatools.allure.annotations.TestCaseId("506")
    public void searchSubjectFieldOverText() throws MessagingException {
        imap.search().shouldSearch(search().text(subject)).shouldBeOk().shouldContain(messageId);
        imap.request(search().text(subject).text(rightInterval)).shouldBeOk().shouldContain(messageId);
        imap.request(search().text(subject).text(leftInterval)).shouldBeOk().shouldContain(messageId);
        imap.request(search().text(subject).text(BAD_RESP)).shouldBeOk().shouldNotContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля SUBJECT. Берём первую половину строки subject.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("507")
    public void searchSubjectFieldRightInterval() throws MessagingException {
        imap.search().shouldSearch(search().subject(rightInterval)).shouldBeOk().shouldContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля SUBJECT используя ключ TEXT.\n" +
            "Берём первую половину строки subject. [MAILPROTO-2332]")
    @Stories({MyStories.JIRA, MyStories.SEARCH_TEXT})
    @ru.yandex.qatools.allure.annotations.TestCaseId("508")
    public void searchSubjectFieldRightIntervalOverText() throws MessagingException {
        imap.search().shouldSearch(search().text(rightInterval)).shouldBeOk().shouldContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля SUBJECT. Берём вторую половину строки subject.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("509")
    public void searchSubjectFieldLeftInterval() throws MessagingException {
        imap.search().shouldSearch(search().subject(leftInterval)).shouldBeOk().shouldContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля SUBJECT используя ключ TEXT.\n" +
            "Берём вторую половину строки subject. [MAILPROTO-2332]")
    @Stories({MyStories.JIRA, MyStories.SEARCH_TEXT})
    @ru.yandex.qatools.allure.annotations.TestCaseId("510")
    public void searchSubjectFieldLeftIntervalOverText() throws MessagingException {
        imap.search().shouldSearch(search().text(leftInterval)).shouldBeOk().shouldContain(messageId);
    }

    @Test
    @Description("Ищем письмо по полному совпадению поля SUBJECT. Ищем используя ключ HEADER.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("505")
    public void searchSubjectFieldHeader() throws MessagingException {
        imap.search().shouldSearch(search().header(headers().subject().toString(), subject))
                .shouldBeOk().shouldContain(messageId);
        imap.request(search().header(headers().subject().toString(), subject + BAD_RESP))
                .shouldBeOk().shouldNotContain(messageId);
    }


    @Test
    @Description("Ищем письмо по частичному совпадению поля SUBJECT (первая половина subject).\n" +
            "Ищем используя ключ HEADER. MAILPROTO-2238")
    @Stories({MyStories.JIRA, MyStories.SEARCH_HEADER})
    @ru.yandex.qatools.allure.annotations.TestCaseId("511")
    public void searchSubjectFieldHeaderRightInterval() throws MessagingException {
        imap.search().shouldSearch(search().header(headers().subject().toString(), rightInterval))
                .shouldBeOk().shouldContain(messageId);
    }

    @Test
    @Description("Ищем письмо по частичному совпадению поля SUBJECT (вторая половина subject).\n" +
            "Ищем используя ключ HEADER. MAILPROTO-2238")
    @Stories({MyStories.JIRA, MyStories.SEARCH_HEADER})
    @ru.yandex.qatools.allure.annotations.TestCaseId("512")
    public void searchSubjectFieldHeaderLeftInterval() throws MessagingException {
        imap.search().shouldSearch(search().header(headers().subject().toString(), leftInterval))
                .shouldBeOk().shouldContain(messageId);
    }

}
