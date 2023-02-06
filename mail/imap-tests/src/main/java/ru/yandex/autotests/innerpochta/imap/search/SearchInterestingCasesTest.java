package ru.yandex.autotests.innerpochta.imap.search;

import javax.mail.MessagingException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.FetchStoreResponse;
import ru.yandex.autotests.innerpochta.imap.responses.SearchResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.consts.folders.Folders.INBOX;
import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 03.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Необычные кейсы")
@Features({ImapCmd.SEARCH})
@Stories("#поиск разное")
@Description("Общие тесты на search. Интересные случаи которые пока никуда не вписались")
public class SearchInterestingCasesTest extends BaseTest {
    private static Class<?> currentClass = SearchInterestingCasesTest.class;

    public static final String BAD_UID = "10000";
    public static final String EXPECTED = "%3a%28%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f" +
            "%5c%3f%5c%3f%5c%3f%5c%2b%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%2b%5c%3f%5c%3f%5c%3f%5c%2b%5c%3f%5c%3f%5c" +
            "%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%2b%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%2b%5c%3f" +
            "%5c%3f%5c%2b%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%3f%5c%2bESET%5c%2bNOD32%2a";
    public static final String UTF_8 = "utf-8";
    public static final String CP_1251 = "cp1251";
    public static String uid;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    public static String folderName = INBOX;

    private FetchStoreResponse fetchById;
    private FetchStoreResponse fetchByUid;
    private SearchResponse searchResult;
    private SearchResponse response;

    @BeforeClass
    public static void setUp() throws Exception {
        imap.select().folder(folderName);

        int last = imap.status().getNumberOfMessages(INBOX);
        uid = String.valueOf(imap.request(fetch(String.valueOf(last)).uid()).shouldBeOk().uid());
    }

    @Web
    @Test
    @Description("Ищем сообщение по UID. Проверяем, что нашли одно и то же сообщение")
    @ru.yandex.qatools.allure.annotations.TestCaseId("546")
    public void searchByUid() throws MessagingException {
        response = imap.request(search().uid(uid)).shouldBeOk().shouldHasSize(1);
        fetchById = imap.request(fetch(response.getMessages().get(0)).rfc822Header().uid()).shouldBeOk();
        fetchByUid = imap.request(fetch(uid).uid(true).rfc822Header()).shouldBeOk();
        fetchById.shouldHasMessageId(fetchByUid.constructMimeMessage().getMessageID());
    }

    @Test
    @Title("Ищем сообщение по не существующему UID")
    @ru.yandex.qatools.allure.annotations.TestCaseId("547")
    public void searchByNotExistUid() {
        response = imap.request(search().uid(BAD_UID)).shouldBeOk().shouldHasSize(0);
    }

    @Test
    @Title("Пустой запрос. Пустая кодировка")
    @ru.yandex.qatools.allure.annotations.TestCaseId("549")
    public void searchByEmptyString() {
        imap.request(search().text("")).shouldBeBad();
        imap.request(search().charset("").text("")).shouldBeBad();
    }

    @Test
    @Title("Неподдерживаемая кодировка")
    @ru.yandex.qatools.allure.annotations.TestCaseId("550")
    public void searchNonSupportCharset() {
        imap.request(search().charset("iso2022-hjp").text("fff")).shouldBeNo()
                .statusLineContains(SearchResponse.BADCHARSET);
    }

    @Test
    @Title("Ищем в неправильной кодировке")
    @ru.yandex.qatools.allure.annotations.TestCaseId("551")
    public void searchNonCorrespondsCharset() throws Exception {
        imap.select().folder(folderName);
        imap.request(search().charset(CP_1251).text("привет")).shouldBeOk().shouldHasSize(0);
        imap.request(search().charset(UTF_8).text("привет")).shouldBeOk().shouldHasSize(2);
    }

    @Test
    @Title("Ищем по очень большому размеру сообщения")
    @ru.yandex.qatools.allure.annotations.TestCaseId("552")
    public void searchSizeBigValue() {
        imap.request(search().larger("9876543210")).shouldBeBad();
        imap.request(search().larger("987654321")).shouldBeOk();
    }
}
