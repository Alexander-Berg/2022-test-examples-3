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
 * Created by kurau on 06.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по флагам")
@Features({ImapCmd.SEARCH})
@Stories("#поиск по флагам")
@Description("Для каждого флага есть свой уникальный ключ. В первую очередь ищем по этим ключам.")
@RunWith(Parameterized.class)
public class SearchByFlagsUidTest extends BaseTest {
    private static Class<?> currentClass = SearchByFlagsUidTest.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private SearchResponse response;
    private SearchResponse uidResponse;
    private SearchRequest request;

    public SearchByFlagsUidTest(SearchRequest request) {
        this.request = request;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(
                new Object[]{search().seen()},
                new Object[]{search().unseen()},
                new Object[]{search().deleted()},
                new Object[]{search().undeleted()},
                new Object[]{search().draft()},
                new Object[]{search().undraft()},
                new Object[]{search().answered()},
                new Object[]{search().unanswered()},
                new Object[]{search().flagged()},
                new Object[]{search().unflagged()},
                new Object[]{search().recent()},
                new Object[]{search().old()},
                new Object[]{search().newMessages()}
        );
    }

    @BeforeClass
    public static void setUp() {
        imap.select().inbox();
    }

    @Test
    @Description("Ищем по основным флагам. Добавляем UID. Сравниваем запросы между собой")
    @ru.yandex.qatools.allure.annotations.TestCaseId("527")
    public void searchFlags() {
        response = imap.request(request).shouldBeOk();
        uidResponse = imap.request(request.uid(true)).shouldBeOk();
        response.shouldHasSize(uidResponse.getMessages().size());
    }

}
