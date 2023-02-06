package ru.yandex.autotests.innerpochta.tests.search;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

@Aqua.Test
@Title("Поиск писем из строки поиска в шапке")
@Features(FeaturesConst.SEARCH_PACK)
@Tag(FeaturesConst.SEARCH_PACK)
@Stories(FeaturesConst.SEARCH)
public class MailSearchTestTus extends BaseTest {

    private static final String SEARCH = "badabum";
    private static final String COMPLEX_SEARCH = "whoop beep";
    private static final String SEARCH_RESULT_URL = "#search?request=";
    private static final String REVERT_SEARCH = "beep whoop";
    private static final String PIN_SEARCH = "Test search";
    private static final String PIN_TITLE = "Search Test 2";
    private static final String DND_SEARCH = "dnd search";
    private static final String CUSTOM_FOLDER = "UserFolder";

    @Rule
    public AccLockRule lock = AccLockRule.use().useTusAccount();

    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), SEARCH, getRandomString());
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), COMPLEX_SEARCH, getRandomString());
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), DND_SEARCH, getRandomString());
        Message msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), PIN_TITLE, getRandomString());
        user.apiLabelsSteps().pinLetter(msg);
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER);
        user.leftColumnSteps().openFolders();

    }

    @Test
    @Title("Поиск писем из строки поиска в шапке")
    @TestCaseId("1226")
    public void composeToolbarSearch() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn());
        user.hotkeySteps()
            .pressHotKeysWithDestination(onMessagePage().mail360HeaderBlock().searchInput(), Keys.ENTER.toString());
        user.defaultSteps()
            .shouldBeOnUrl(containsString(SEARCH_RESULT_URL + SEARCH));
        user.messagesSteps().shouldSeeMessageWithSubject(SEARCH)
            .shouldNotSeeMessageWithSubject(DND_SEARCH);
    }

    @Test
    @Title("Должны видеть нулевой поиск")
    @TestCaseId("3539")
    public void shouldSeeEmptyMessage() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), getRandomString())
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldSee(onMessagePage().emptyFolder());
    }

    @Test
    @Title("Соответствие выдачи поиска расширенным параметрам")
    @TestCaseId("2006")
    public void shouldMatchesWithAdvance() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchOptionsBtn())
            .clicksOn(onSearchPage().advancedSearchBlock().advancedSearchRows().get(2))
            .clicksOn(onSearchPage().folder().get(0))
            .clicksOn(onSearchPage().advancedSearchBlock().advancedSearchRows().get(5))
            .clicksOn(onSearchPage().more().get(0))
            .clicksOn(onSearchPage().mail360HeaderBlock().searchBtn()
            );
        user.messagesSteps().shouldSeeCorrectNumberOfMessages(1)
            .shouldSeeMessageWithSubject(SEARCH);
    }

    @Test
    @Title("Поиск с разным порядком слов")
    @TestCaseId("3546")
    public void differentWordOrderInSearch() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), COMPLEX_SEARCH)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldSee(onSearchPage().otherResultsHeader());
        user.messagesSteps().shouldSeeCorrectNumberOfMessages(1)
            .shouldSeeMessageWithSubject(COMPLEX_SEARCH);
        user.defaultSteps().clicksOn(onSearchPage().mail360HeaderBlock().closeSearch())
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), REVERT_SEARCH)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldSee(onSearchPage().otherResultsHeader());
        user.messagesSteps().shouldSeeCorrectNumberOfMessages(1)
            .shouldSeeMessageWithSubject(COMPLEX_SEARCH);
    }

    @Test
    @Title("Очистка строки поиска по крестику на странице результатов поиска")
    @TestCaseId("1095")
    public void shouldCleanSearchInputOnResultsPage() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .shouldSee(onMessagePage().mail360HeaderBlock().searchInput())
            .clicksOn(onSearchPage().mail360HeaderBlock().closeSearch())
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), "");
    }

    @Test
    @Title("Очистка строки поиска по крестику")
    @TestCaseId("5392")
    public void shouldCleanSearchInput() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onSearchPage().mail360HeaderBlock().closeSearch())
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), "");
    }

    @Test
    @Title("Драг-н-дроп письма из поиска")
    @TestCaseId("3935")
    public void dragMailFromSearch() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), DND_SEARCH)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldSee(onSearchPage().otherResultsHeader())
            .dragAndDrop(
                onMessagePage().displayedMessages().list().get(0).subject(),
                onMessagePage().foldersNavigation().customFolders().get(2).customFolderName()
            );
        user.messagesSteps().shouldSeeThatFolderIsEmpty();
    }

    @Test
    @Title("Запиненные письма в результатах поиска")
    @TestCaseId("3372")
    public void pinnedMailInSearch() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), PIN_SEARCH)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldSee(onSearchPage().otherResultsHeader())
            .shouldContainText(onMessagePage().displayedMessages().list().get(0).subject(), PIN_TITLE);
    }

    @Test
    @Title("Поиск закрывается по ESC")
    @TestCaseId("3535")
    public void shouldCloseSearchWithEsc() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH);
        user.hotkeySteps().pressHotKeys(Keys.ESCAPE.toString());
        user.defaultSteps().shouldSee(onMessagePage().mail360HeaderBlock().foldedSearch());
    }

    @Test
    @Title("Поиск закрывается по клику вне его")
    @TestCaseId("5395")
    public void shouldCloseSearch() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .offsetClick(10, 10);
        user.defaultSteps().shouldSee(onMessagePage().mail360HeaderBlock().foldedSearch());
    }

    @Test
    @Title("Старт поиска по кнопке «Найти»")
    @TestCaseId("5390")
    public void shouldStartSearchWithButton() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .shouldSee(onSearchPage().advancedSearchBlock());
        user.messagesSteps().shouldSeeMessageWithSubject(SEARCH);
    }
}
