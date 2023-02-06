package ru.yandex.autotests.innerpochta.imap.search;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.quoted;

/**
 * Created by kurau on 05.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по дате на фиксированном пользователе.")
@Features({ImapCmd.SEARCH})
@Stories({"#поиск по дате", "#фиксированный пользователь"})
@Description("Ищем письма по датам на фиксированном юзере. Пересекаем между собой before, since и on.\n" +
        "Ищем в специальной папке в которой подготовлены нужные письма с нужными датами.")
@Web
public class SearchByDateTest extends BaseTest {
    private static Class<?> currentClass = SearchByDateTest.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    public static Integer messagesCount;
    private static String messageDate;
    private static String dayBefore;
    private static String nextDay;

    @BeforeClass
    public static void setUp() throws Exception {
        messagesCount = imap.request(select(Folders.INBOX)).exist();
        assertThat("Ящик непустой", messagesCount != 0);

        messageDate = imap.fetch().getSentDate("1");
        dayBefore = imap.fetch().getSentDateNeighbourhood("1", -1);
        nextDay = imap.fetch().getSentDateNeighbourhood("1", 1);
    }


    @Test
    @Description("Тест на работу before. Ищем в кастомной папке.\n" +
            "Знаем, какое количестко писем должно найтись по каждому запросую.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("471")
    public void shouldSearchByDateBefore() {
        imap.search().shouldSearch(search().before(nextDay)).shouldBeOk().shouldHasSize(messagesCount);
        imap.request(search().before(dayBefore)).shouldBeOk().shouldBeEmpty();

        imap.request(search().before(nextDay).before(nextDay)).shouldBeOk().shouldHasSize(messagesCount);
        imap.request(search().before(nextDay).not().before(nextDay)).shouldBeOk().shouldBeEmpty();
    }

    @Test
    @Description("Тест на работу since. Ищем в кастомной папке.\n" +
            "Знаем, какое количестко писем должно найтись по каждому запросу.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("472")
    public void searchDateSince() {
        imap.search().shouldSearch(search().since(dayBefore)).shouldBeOk().shouldHasSize(messagesCount);
        imap.request(search().since(nextDay)).shouldBeOk().shouldBeEmpty();

        imap.request(search().since(dayBefore).since(dayBefore)).shouldBeOk().shouldHasSize(messagesCount);
        imap.request(search().since(dayBefore).not().since(dayBefore)).shouldBeOk().shouldBeEmpty();

    }

    @Test
    @Description("Тест на одеовременный поиск по before и since.\n" +
            "Проверяем пустое и непустое переесечение. Знаем, сколько писем должно найтись по каждому запросу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("473")
    public void searchDateSinceVsBefore() {
        imap.search().shouldSearch(search().since(dayBefore).before(nextDay))
                .shouldBeOk().shouldHasSize(messagesCount);
        imap.request(search().since(nextDay).before(dayBefore)).shouldBeOk().shouldBeEmpty();
    }

    @Ignore
    @Test
    @Issue("MPROTO-1454")
    @Description("Ищем письма по дате в кавычках. Сравниваем с результатом без кавычек")
    @ru.yandex.qatools.allure.annotations.TestCaseId("474")
    public void searchQuotesData() {
        imap.search().shouldSearch(search().since(quoted(dayBefore)))
                .shouldBeOk().shouldHasSize(messagesCount);

        imap.search().shouldSearch(search().before(quoted(nextDay)))
                .shouldBeOk().shouldHasSize(messagesCount);

        imap.search().shouldSearch(search().on(quoted(messageDate)))
                .shouldBeOk().shouldHasSize(messagesCount);
    }
}
