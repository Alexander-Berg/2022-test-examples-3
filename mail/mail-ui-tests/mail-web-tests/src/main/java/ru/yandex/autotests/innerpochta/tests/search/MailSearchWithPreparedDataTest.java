package ru.yandex.autotests.innerpochta.tests.search;

import com.yandex.xplat.common.YSDate;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_OTHER;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DISABLE_INBOXATTACHS;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Расширенный поиск по подготовленным письмам")
@Features(FeaturesConst.SEARCH_PACK)
@Tag(FeaturesConst.SEARCH_PACK)
@Stories(FeaturesConst.SEARCH)
public class MailSearchWithPreparedDataTest extends BaseTest {

    private static final String SEARCH = "subj";
    private static final String SEARCH_DATE = "03.06.2019";
    private static final String SEARCH_TEST = "subj test";
    private static final String SEARCH_TEST_DIFF_CASE = "SuBj TEst";
    private static final String SEARCH_REQUEST = "Yandex";
    private static final String YANDEX_THEME = "YandexCat";
    private static final int TIMEOUT = 20;
    private String subj_attach;
    private String subj_date;

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
        subj_attach = SEARCH + " " + getRandomString();
        subj_date = SEARCH + " " + getRandomString();
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            lock.firstAcc().getSelfEmail(),
            subj_attach,
            getRandomString(),
            PDF_ATTACHMENT
        );
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        user.imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(
                        new YSDate(dateFormat.format(date.withMonth(6).withYear(2019).withDayOfMonth(3)) + "Z"))
                    .withSubject(subj_date)
                    .build()
            )
            .closeConnection();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), SEARCH_TEST, Utils.getRandomString());
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), SEARCH_REQUEST, Utils.getRandomString());
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), YANDEX_THEME, Utils.getRandomString());
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем показ вложений",
            of(SETTINGS_DISABLE_INBOXATTACHS, STATUS_ON)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Расширенный поиск: опция «С вложениями»")
    @TestCaseId("2107")
    public void shouldSearchWithAttach() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onSearchPage().mail360HeaderBlock().searchOptionsBtn())
            .shouldSee(onSearchPage().advancedSearchBlock())
            .clicksOn(onSearchPage().advancedSearchBlock().advancedSearchRows().get(0));
        user.messagesSteps().shouldSeeMessageWithSubject(SEARCH)
            .shouldSeeCorrectNumberOfMessages(1);
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages().list().get(0).attachments());
    }

    @Test
    @Title("Расширенный поиск: опция «Указать точные даты»")
    @TestCaseId("2108")
    public void shouldSearchWithCurrentDate() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH)
            .clicksOn(onSearchPage().mail360HeaderBlock().searchOptionsBtn())
            .shouldSee(onSearchPage().advancedSearchBlock())
            .clicksOn(onSearchPage().advancedSearchBlock().advancedSearchRows().get(1))
            .inputsTextInElementClearingThroughHotKeys(onSearchPage().dataRangeInputs().get(0), SEARCH_DATE)
            .shouldSee(onSearchPage().calendar())
            .clicksOn(onSearchPage().dataRangeInputs().get(1))
            .inputsTextInElementClearingThroughHotKeys(onSearchPage().dataRangeInputs().get(1), SEARCH_DATE)
            .clicksOn(
                onSearchPage().dateSearchPopup(),
                onSearchPage().dateSearch()
            );
        user.messagesSteps().shouldSeeMessageWithSubject(SEARCH)
            .shouldSeeCorrectNumberOfMessages(1);
    }

    @Test
    @Title("Поиск конкретного письма")
    @TestCaseId("5387")
    public void shouldSearchMessage() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_TEST)
            .clicksOn(onSearchPage().searchSuggestMailSubject())
            .shouldBeOnUrl(containsString("#message"));
        user.messageViewSteps().shouldSeeMessageSubject(SEARCH_TEST);
    }

    @Test
    @Title("Поиск в верхнем регистре")
    @TestCaseId("5374")
    public void shouldSearchUppercase() {
        user.defaultSteps()
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_TEST.toUpperCase())
            .shouldSeeThatElementHasText(onSearchPage().searchSuggestMailContent(), SEARCH_TEST);
    }

    @Test
    @Title("Саджест не зависит от регистра")
    @TestCaseId("5374")
    public void shouldNotBeCaseSensitive() {
        user.defaultSteps()
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_TEST_DIFF_CASE)
            .shouldSeeThatElementHasText(onSearchPage().searchSuggestMailContent(), SEARCH_TEST);
    }

    @Test
    @Title("Саджест меняется при вводе запроса")
    @TestCaseId("5355")
    public void shouldSeeSuggestChanges() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_REQUEST)
            .waitInSeconds(3)
            .shouldSeeElementInListWithWaiting(onSearchPage().searchSuggestMailThemes(), SEARCH_REQUEST, TIMEOUT);
        user.defaultSteps().shouldSeeElementInListWithWaiting(
            onSearchPage().searchSuggestMailThemes(),
            YANDEX_THEME,
            TIMEOUT
        );
        user.defaultSteps().clicksAndInputsText(onMessagePage().mail360HeaderBlock().searchInput(), "C")
            .shouldSeeWithWaiting(onSearchPage().searchSuggestMailThemes().get(1), TIMEOUT)
            .shouldSeeElementsCount(onSearchPage().searchSuggestMailThemes(), 2);
    }

    @Test
    @Title("Саджест на разных страницах")
    @TestCaseId("5402")
    public void shouldSeeSuggestFromDifferentPages() {
        user.defaultSteps().opensFragment(SETTINGS_OTHER)
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_TEST)
            .shouldSee(onSearchPage().searchSuggest())
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldSee(onSearchPage().otherResultsHeader())
            .clicksOn(
                onMessagePage().mail360HeaderBlock().closeSearch(),
                onMessagePage().mail360HeaderBlock().closeSearch()
            )
            .shouldBeOnUrlWith(SETTINGS_OTHER);
    }

    @Test
    @Title("Открыть саджест из просмотра письма")
    @TestCaseId("5402")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68418")
    public void shouldSeeSuggestFromMessagePage() {
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_TEST)
            .shouldSee(onSearchPage().searchSuggest())
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldSee(onSearchPage().otherResultsHeader())
            .clicksOn(
                onMessagePage().mail360HeaderBlock().closeSearch(),
                onMessagePage().mail360HeaderBlock().closeSearch(),
                onMessagePage().mail360HeaderBlock().closeSearch()
            )
            .shouldBeOnUrlWith(QuickFragments.MESSAGE);
    }
}
