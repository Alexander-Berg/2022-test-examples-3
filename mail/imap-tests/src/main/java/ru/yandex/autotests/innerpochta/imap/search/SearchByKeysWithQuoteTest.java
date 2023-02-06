package ru.yandex.autotests.innerpochta.imap.search;

import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.SearchRequest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.consts.Headers.headers;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.quoted;

/**
 * Created by kurau on 15.07.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск с экранированием")
@Features({ImapCmd.SEARCH})
@Stories({"#поиск с экранированием", "#негативные тесты"})
@Description("Ищем по тексту с пробелами. Если не экранировать такой текст кавычками, то получаем BAD в респонсе")
@RunWith(value = Parameterized.class)
public class SearchByKeysWithQuoteTest extends BaseTest {
    private static Class<?> currentClass = SearchByKeysWithQuoteTest.class;

    public static final String VALUE = "aaa bb%&*@#$%%;:b";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private SearchRequest request;
    private SearchRequest quotedRequest;

    public SearchByKeysWithQuoteTest(SearchRequest request, SearchRequest quotedRequest) {
        this.request = request;
        this.quotedRequest = quotedRequest;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(
                new Object[]{search().from(VALUE), search().from(quoted(VALUE))},
                new Object[]{search().subject(VALUE), search().subject(quoted(VALUE))},
                new Object[]{search().cc(VALUE), search().cc(quoted(VALUE))},
                new Object[]{search().bcc(VALUE), search().bcc(quoted(VALUE))},
                new Object[]{search().to(VALUE), search().to(quoted(VALUE))},
                new Object[]{search().text(VALUE), search().text(quoted(VALUE))},
                new Object[]{search().from(VALUE), search().from(quoted(VALUE))},
                new Object[]{search().header(headers().from().toString(), VALUE), search()
                        .header(headers().from().toString(), quoted(VALUE))}
        );
    }

    @BeforeClass
    public static void setUp() {
        imap.select().inbox();
    }

    @Test
    @Description("Проверяем, что экранированный запрос всегда OK, а без кавычек BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("528")
    public void searchQuoted() {
        imap.request(request).shouldBeBad();
        imap.request(quotedRequest).shouldBeOk();
    }

}
