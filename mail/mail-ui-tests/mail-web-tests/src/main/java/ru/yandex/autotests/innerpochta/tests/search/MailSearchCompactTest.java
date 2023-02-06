package ru.yandex.autotests.innerpochta.tests.search;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Поиск писем в компактном режиме")
@Features(FeaturesConst.SEARCH_PACK)
@Tag(FeaturesConst.SEARCH_PACK)
@Stories(FeaturesConst.SEARCH)
@RunWith(DataProviderRunner.class)
public class MailSearchCompactTest extends BaseTest {

    private static final int MSG_COUNT = 4;
    private static final String SEARCH_URL_POSTFIX = "/#search?request=subj";
    private static final String SEARCH_QUERY = "subj";
    private String subj;
    private String folderName;

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
        folderName = getRandomName();
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MSG_COUNT);
        subj =
            user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), SEARCH_QUERY + " " + getRandomName(), "")
            .getSubject();
        user.apiFoldersSteps().createNewFolder(folderName);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, STATUS_ON)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessagesPresent()
            .shouldSeeMessageWithSubject(subj);
        user.messagesSteps().movesMessageToFolder(subj, folderName);
    }

    @Test
    @Title("При выборе письма в поиске строка ввода должна пропадать")
    @TestCaseId("1922")
    @Issue("DARIA-63506")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @DataProvider({STATUS_ON, EMPTY_STR})
    public void shouldNotSeeSearchInputOnMessageSelection(String minifiedStatus) {
        switchCompactMessages(minifiedStatus);
        user.defaultSteps().opensDefaultUrlWithPostFix(SEARCH_URL_POSTFIX)
            .shouldSee(onMessagePage().displayedMessages().allResults());
        user.messagesSteps().clicksOnMessageCheckBox();
        user.defaultSteps().shouldNotSee(onMessagePage().mail360HeaderBlock().searchInput());
        //TODO: падает из-за issue
        //.shouldBeEnabled(onMessagePage().mail360HeaderBlock().searchBtnCompactMode());
    }

    @Test
    @Title("При нажатии на кнопку поиска строка ввода должна появляться")
    @TestCaseId("1922")
    @Issue("DARIA-63506")
    @DataProvider({STATUS_ON, EMPTY_STR})
    public void shouldSeeSearchInputOnSearchBtnClick(String minifiedStatus) {
        switchCompactMessages(minifiedStatus);
        user.defaultSteps().opensDefaultUrlWithPostFix(SEARCH_URL_POSTFIX)
            .shouldSee(onMessagePage().displayedMessages().allResults());
        user.messagesSteps().clicksOnMessageCheckBox();
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().searchBtnCompactMode())
            .shouldSee(onMessagePage().mail360HeaderBlock().searchInput());
        //TODO: падает из-за issue
        //.shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH);
    }

    @Test
    @Title("При скролле в поиске должен появляться залипающий тулбар")
    @TestCaseId("1922")
    @DataProvider({STATUS_ON, EMPTY_STR})
    public void shouldSeeStickyToolbarOnScroll(String minifiedStatus) {
        switchCompactMessages(minifiedStatus);
        user.defaultSteps().opensDefaultUrlWithPostFix(SEARCH_URL_POSTFIX)
            .shouldSee(onMessagePage().displayedMessages().allResults());
        user.messagesSteps().clicksOnMessageCheckBox();
        user.defaultSteps().setsWindowSize(1600, 300);
        user.messagesSteps().scrollDownPage();
        user.defaultSteps().shouldSee(onMessagePage().stickyToolBar());
    }

    @Test
    @Title("Расширенный поиск в компактном меню")
    @TestCaseId("5725")
    public void shouldSearchInAdvancedMode() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().searchBtnCompactMode())
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_QUERY)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchOptionsBtn())
            .clicksOn(onSearchPage().advancedSearchBlock().advancedSearchRows().get(2))
            .clicksOn(onSearchPage().folder().get(1))
            .shouldSeeThatElementTextEquals(
                onMessagePage().mail360HeaderBlock().searchBubble(),
                folderName
            )
            .shouldSeeElementsCount(onMessagePage().displayedMessages().list(), 1);
    }

    @Step("Переключаем компактный вид писем")
    private void switchCompactMessages(String minifiedStatus) {
        user.apiSettingsSteps().callWithListAndParams(
            "Переключаем компактный вид писем",
            of(LIZA_MINIFIED, minifiedStatus)
        );
        user.defaultSteps().refreshPage();
    }
}
