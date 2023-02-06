package ru.yandex.autotests.innerpochta.tests.search;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.MailConst.SENT_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created by cosmopanda
 */
@Aqua.Test
@Title("Расширенный поиск писем")
@Features(FeaturesConst.SEARCH_PACK)
@Tag(FeaturesConst.SEARCH_PACK)
@Stories(FeaturesConst.SEARCH)
public class MailAdvancedSearchTest extends BaseTest {

    private static final String SUBJECT = Utils.getRandomName();
    private static final String FOLDER = Utils.getRandomString();
    private static final String LABEL = Utils.getRandomString();
    private static final String SEARCH = "subj";
    private static final int MSG_COUNT = 2;
    private static final String SEARCH_WRONG = "ыгио";

    private Message msg1, msg2;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSearchSteps().cleanSuggestHistory();
        user.apiFoldersSteps().createNewFolder(FOLDER);
        user.apiLabelsSteps().addNewLabel(LABEL, LABELS_PARAM_GREEN_COLOR);
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MSG_COUNT)
            .sendMailWithNoSave(lock.firstAcc(), SUBJECT, SEARCH);
        List<Message> messages = user.apiMessagesSteps().getAllMessages();
        msg1 = messages.get(1);
        msg2 = messages.get(2);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(SUBJECT, msg1.getSubject(), msg2.getSubject());
    }

    @Test
    @Title("Поиск по теме письма")
    @TestCaseId("2104")
    public void shouldSearchInSubject() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onSearchPage().mail360HeaderBlock().searchOptionsBtn())
            .clicksOn(onSearchPage().advancedSearchBlock().advancedSearchRows().get(5))
            .clicksOn(
                onSearchPage().more().get(0),
                onSearchPage().mail360HeaderBlock().searchBtn()
            );
        user.messagesSteps().shouldSeeMessageWithSubject(msg1.getSubject())
            .shouldSeeMessageWithSubject(msg2.getSubject())
            .shouldNotSeeMessageWithSubject(SUBJECT);
    }

    @Test
    @Title("Запрос с ошибками")
    @TestCaseId("5375")
    public void shouldSeeCorrectSuggest() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .opensFragment(QuickFragments.INBOX)
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_WRONG)
            .shouldSeeThatElementHasText(onSearchPage().lastQueriesList().get(0), SEARCH);
    }

    @Test
    @Title("Поиск из пользовательской папки")
    @TestCaseId("5378")
    public void shouldSeeFolderBubbleInSearch() {
        user.apiSettingsSteps().callWithListAndParams(
                "Раскрываем все папки",
                of(FOLDERS_OPEN, user.apiFoldersSteps().getAllFids())
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeMessagesPresent()
            .selectMessageWithSubject(msg2.getSubject())
            .movesMessageToFolder(FOLDER);
        user.leftColumnSteps().opensCustomFolder(FOLDER);
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().searchInput())
            .shouldSeeThatElementHasText(onSearchPage().mail360HeaderBlock().searchBubble(), FOLDER)
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .shouldSeeThatElementHasText(onSearchPage().searchSuggestMailSubject(), msg2.getSubject());
    }

    @Test
    @Title("Поиск из системной папки")
    @TestCaseId("5379")
    public void shouldSeeSystemFolderBubbleInSearch() {
        user.messagesSteps().shouldSeeMessagesPresent()
            .selectMessageWithSubject(msg2.getSubject())
            .movesMessageToFolder(SENT_RU);
        user.leftColumnSteps().opensSentFolder();
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().searchInput())
            .shouldSeeThatElementHasText(onSearchPage().mail360HeaderBlock().searchBubble(), SENT_RU)
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .shouldSeeThatElementHasText(onSearchPage().searchSuggestMailSubject(), msg2.getSubject());
    }


    @Test
    @Title("Поиск по пользовательской папке")
    @TestCaseId("2105")
    public void shouldSearchInCustomFolder() {
        user.messagesSteps().shouldSeeMessagesPresent()
            .selectMessageWithSubject(msg2.getSubject())
            .movesMessageToFolder(FOLDER);
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onSearchPage().mail360HeaderBlock().searchOptionsBtn())
            .clicksOn(onSearchPage().advancedSearchBlock().advancedSearchRows().get(2))
            .clicksOn(
                onSearchPage().folder().get(1),
                onSearchPage().mail360HeaderBlock().searchBtn()
            )
            .shouldSeeThatElementHasText(onSearchPage().mail360HeaderBlock().searchBubble(), FOLDER);
        user.messagesSteps().shouldSeeMessageWithSubject(msg2.getSubject())
            .shouldNotSeeMessageWithSubject(msg1.getSubject())
            .shouldNotSeeMessageWithSubject(SUBJECT);
    }
}
