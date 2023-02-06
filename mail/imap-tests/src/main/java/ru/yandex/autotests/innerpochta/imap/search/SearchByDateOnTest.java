package ru.yandex.autotests.innerpochta.imap.search;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;

/**
 * Created by kurau on 14.07.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по дате с автогенерацией даты")
@Features({ImapCmd.SEARCH})
@Stories({"#поиск по ключу ON"})
@Description("Ищем письма по датам на любом юзере.\n" +
        "Автоматически гененирурем двты для тестов.")
public class SearchByDateOnTest extends BaseTest {
    private static Class<?> currentClass = SearchByDateOnTest.class;

    public static String sendDate;
    public static String notSendDate;
    public static Integer messagesCount;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @BeforeClass
    public static void setUp() throws Exception {
        messagesCount = imap.request(select(Folders.INBOX)).exist();
        assertThat("Ящик непустой", messagesCount != 0);

        sendDate = imap.fetch().getSentDate("1");
        notSendDate = imap.fetch().getSentDateNeighbourhood("1", -2);
    }

    @Test
    @Title("Должны искать письма по конкретной дате")
    @ru.yandex.qatools.allure.annotations.TestCaseId("465")
    public void shouldSearchByDateOn() {
        imap.request(search().on(sendDate)).shouldBeOk().shouldHasSize(messagesCount);
    }

    @Test
    @Description("Ищем письма точно в конкретную известную дату которая меньше минимальной даты в диапазоне дат.\n" +
            "Таким образом такой запрос не должен искать сообщений.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("466")
    public void shouldNotSearchByWrongDate() {
        imap.request(search().on(notSendDate)).shouldBeOk().shouldBeEmpty();
    }

    @Test
    @Title("Ищем с условием по конкретным датам с условием AND")
    @ru.yandex.qatools.allure.annotations.TestCaseId("467")
    public void searchDateOnAfter() {
        imap.request(search().on(sendDate).on(sendDate)).shouldBeOk().shouldHasSize(messagesCount);
        imap.request(search().on(sendDate).on(notSendDate)).shouldBeOk().shouldBeEmpty();
    }
}
