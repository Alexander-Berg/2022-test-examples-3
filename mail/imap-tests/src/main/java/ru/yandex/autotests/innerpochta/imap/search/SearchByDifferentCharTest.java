package ru.yandex.autotests.innerpochta.imap.search;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.Charsets;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.SearchResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 04.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Специальные символы")
@Features({ImapCmd.SEARCH})
@Stories("#специальные символы")
@Description("Ищем по различным специальным символам и смотрим, чтобы не ломалось")
@RunWith(value = Parameterized.class)
public class SearchByDifferentCharTest extends BaseTest {
    private static Class<?> currentClass = SearchByDifferentCharTest.class;

    public static final String DIGITS = "09334571234567890";
    public static final String SPECIAL = "\"!@#$%^&*()[]<>|~`/?;:,.\"";
    public static final String CP1251 = "\"ЂЃ‚ѓ„…†‡€‰Љ‹ЊЌЋЏђ‘’“”•–— ™љ›њќћџ ЎўЈ¤Ґ¦§Ё©Є«¬­®Ї°±Ііґµ¶·ё№є»јЅѕї" +
            "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюя\"";
    public static final String ENG_CHAR = "abcdefghijklmnopqrstvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Parameterized.Parameter
    public String request;
    private SearchResponse response;
    private SearchResponse responseUid;

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(
                new Object[]{DIGITS},
                new Object[]{SPECIAL},
                new Object[]{CP1251},
                new Object[]{ENG_CHAR}
        );
    }

    @Before
    public void sendMsg() throws Exception {
        imap.select().inbox();
//        imap.append().appendRandomMessage(Folders.INBOX, imapMessage().withText(request));
    }

    @Test
    @Description("Проверяем, что имап не ломается на специальных символах в запросах на соответсвие тексту")
    @ru.yandex.qatools.allure.annotations.TestCaseId("478")
    public void searchDifferentCharSequence() {
        response = imap.request(search().charset(Charsets.UTF_8.toString()).text(request)).shouldBeOk();
        responseUid = imap.request(search().uid(true).charset(Charsets.UTF_8.toString()).text(request)).shouldBeOk();
        response.shouldHasSize(responseUid.getMessages().size());
    }
}
