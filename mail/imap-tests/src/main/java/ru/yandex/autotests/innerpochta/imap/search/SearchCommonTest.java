package ru.yandex.autotests.innerpochta.imap.search;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.autotests.innerpochta.imap.responses.SearchResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 19.02.14
 * Time: 19:52
 * <p/>
 * [MAILPROTO-2183]
 */
@Aqua.Test
@Title("Команда SEARCH. Общие тесты")
@Features({ImapCmd.SEARCH})
@Stories(MyStories.COMMON)
@Description("Общие тесты на SEARCH. Ищем письма по uid, в пустом ящике")
public class SearchCommonTest extends BaseTest {
    private static Class<?> currentClass = SearchCommonTest.class;


    public static final String TEST_DATE = "15-Jun-2014";

    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Description("Проверяем SEARCH uid на абсолютно пустом юзере." +
            "Актуально для корпов.\n "
            + "Падали с коркой\n " +
            "[MAILPROTO-1926]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("536")
    public void searchWithEmptyMailbox() {
        imap.select().waitNoMessagesInInbox();
        imap.select().inbox();
        imap.request(search().criterion("uid 1")).shouldBeOk().statusLineContains(SearchResponse.NO_MESSAGES);
    }

    @Test
    @Description("Проверяем UID SEARCH uid на абсолютно пустом юзере.")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("537")
    public void uidSearchWithEmptyMailbox() {
        imap.select().waitNoMessagesInInbox();
        imap.select().inbox();
        imap.request(search().uid(true).all()).shouldBeOk().statusLineContains(SearchResponse.NO_MESSAGES);
    }

    @Test
    @Description("SEARCH кирилиической последовательности [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("538")
    public void searchCyrillicFolderShouldSeeBad() {
        imap.request(search().criterion(Utils.cyrillic())).shouldBeBad()
                .statusLineContains(ImapResponse.COMMAND_SYNTAX_ERROR);
    }

    @Test
    @Description("Простой тест на UID SEARCH\n" +
            "[MAILPROTO-2183]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("539")
    public void uidSearchSimpleTest() throws Exception {
        prodImap.append().appendRandomMessage(Folders.INBOX);
        imap.select().waitMsgsInInbox(1);
        imap.noop();

        Integer expected = imap.request(select(Folders.INBOX)).uidNext() - 1;
        imap.request(search().uid(true).all()).shouldBeOk().shouldContain(expected.toString());
    }

    @Test
    @Title("SEARCH без SELECT или EXAMINE")
    @ru.yandex.qatools.allure.annotations.TestCaseId("540")
    public void searchWithoutSelectShouldSeeBad() {
        imap.request(unselect());
        imap.request(search().all().since(TEST_DATE)).shouldBeBad()
                .statusLineContains(SearchResponse.WRONG_SESSION_STATE);
    }

    @Test
    @Title("UID SEARCH без SELECT или EXAMINE")
    @ru.yandex.qatools.allure.annotations.TestCaseId("541")
    public void uidSearchWithoutSelectShouldSeeBad() {
        imap.request(unselect());
        imap.request(search().uid(true).all().since(TEST_DATE)).shouldBeBad()
                .statusLineContains(SearchResponse.UID_SEARCH_WRONG_SESSION_STATE);
    }

    @Test
    @Issues({@Issue("MPROTO-337"), @Issue("MPROTO-1463")})
    @Title("Untagged response с пустой папкой")
    @Description("Не должны отдавать * SEARCH если папка пуста")
    @ru.yandex.qatools.allure.annotations.TestCaseId("542")
    public void searchInEmptyMailboxShouldSeeUntaggedResponse() {
        imap.select().waitNoMessagesInInbox();
        imap.select().inbox();
        imap.request(search().all()).shouldBeOk().shouldSeeUntaggedResponse();
    }

    @Test
    @Issue("MPROTO-337")
    @Title("Untagged response, если папка не пуста")
    @Description("Не должны отдавать * SEARCH если папка пуста. Делаем два запроса:\n" +
            "1) Возвращает 1 письмо и содержит * SEARCH\n" +
            "2) Ничего не возвращает, но все равно содержит * SEARCH")
    @ru.yandex.qatools.allure.annotations.TestCaseId("543")
    public void searchShouldSeeUntaggedResponse() throws Exception {
        prodImap.append().appendRandomMessage(Folders.INBOX);
        imap.select().waitMsgsInInbox(1);
        imap.noop();
        imap.select().inbox();
        imap.request(search().all()).shouldBeOk().shouldHasSize(1).shouldContain("1");
        imap.request(search().answered()).shouldBeOk().shouldSeeUntaggedResponse();
    }


}
