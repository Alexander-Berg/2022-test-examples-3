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
import ru.yandex.autotests.innerpochta.imap.responses.SearchResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 30.05.14.
 * [MAILPROTO-2322]
 */
@Aqua.Test
@Title("Команда SEARCH. Основные ключи для неявной команды AND")
@Features({ImapCmd.SEARCH})
@Stories("#поиск с AND")
@Description("Ищем используя основные ключи без доп. параметров и пересекаем с ключом ALL")
@RunWith(value = Parameterized.class)
public class SearchBasicKeysAllTest extends BaseTest {
    private static Class<?> currentClass = SearchBasicKeysAllTest.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private SearchRequest request;
    private SearchResponse response;
    private SearchResponse response2;

    public SearchBasicKeysAllTest(SearchRequest request) {
        this.request = request;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(
                new Object[]{search().answered()},
                new Object[]{search().deleted()},
                new Object[]{search().draft()},
                new Object[]{search().flagged()},
                new Object[]{search().newMessages()},
                new Object[]{search().old()},
                new Object[]{search().recent()},
                new Object[]{search().seen()},
                new Object[]{search().unanswered()},
                new Object[]{search().undraft()},
                new Object[]{search().unflagged()},
                new Object[]{search().unseen()},
                new Object[]{search().undeleted()}
        );
    }

    @BeforeClass
    public static void setUp() {
        imap.select().inbox();
    }

    @Test
    @Description("Проверяем пересечение основных ключей для поиска с командой ALL, " +
            "которая всегда используется по умолчанию. Ожидаем одинаковые ответы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("462")
    public void basicSearch() {
        response = imap.request(request).shouldBeOk();
        response2 = imap.request(request.all()).shouldBeOk();
        response.shouldContain(response2.getMessages());
        response2 = imap.request(request.uid(true)).shouldBeOk();
        response.shouldHasSize(response2.getMessages().size());
    }
}
