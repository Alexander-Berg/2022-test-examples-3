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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DONT_SAVE_HISTORY;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DND;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

@Aqua.Test
@Title("Поиск писем из строки поиска в шапке")
@Features(FeaturesConst.SEARCH_PACK)
@Tag(FeaturesConst.SEARCH_PACK)
@Stories(FeaturesConst.SEARCH)
public class MailSearchTest extends BaseTest {

    private static final String MONOGRAM = "MY";
    private static final String CONTACT_SEARCH = "Myself";
    private static final String CUSTOM_FOLDER = "UserFolder";
    private static final String SEARCH_ATTACH = "Яндекс";
    private static final String SEARCH_NAME = "Имя";

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 2pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE)
        )
            .callWithListAndParams(
                "Включаем показ прошлых запросов",
                of(DONT_SAVE_HISTORY, false)
            )
            .callWithListAndParams(
                SETTINGS_DND,
                of(SETTINGS_DND, true)
            );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.apiMessagesSteps().moveAllMessagesFromFolderToFolder(CUSTOM_FOLDER, INBOX);
    }

    @Test
    @Title("Аватарки в поисковом саджесте")
    @TestCaseId("4130")
    public void shouldSeeAvatarsInSuggest() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), CONTACT_SEARCH)
            .shouldHasText(onSearchPage().searchUserAvatar(), MONOGRAM);
    }

    @Test
    @Title("Аттачи в саджесте поиска")
    @TestCaseId("5365")
    public void shouldSeeAttachInSearchSuggest() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_ATTACH)
            .shouldSee(onSearchPage().searchSuggestMailAttach());
    }

    @Test
    @Title("Выбор значения кликом")
    @TestCaseId("5386")
    public void shouldSeeSearchResults() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_NAME)
            .clicksOn(onSearchPage().searchSuggestContactEmail())
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), lock.firstAcc().getSelfEmail())
            .shouldSee(onSearchPage().advancedSearchBlock());
    }

    @Test
    @Title("Выбор значения хоткеями")
    @TestCaseId("5385")
    public void shouldStartSearchWithHotkeys() {
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_NAME)
            .shouldSee(onSearchPage().searchSuggest());
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressHotKeysWithDestination(onMessagePage().mail360HeaderBlock().searchInput(), Keys.ENTER.toString());
        user.defaultSteps()
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), lock.firstAcc().getSelfEmail())
            .shouldSee(onSearchPage().advancedSearchBlock());
    }

    @Test
    @Title("Запрос удаляется из истории поиска по крестику")
    @TestCaseId("5355")
    public void shouldRemoveQueryFromHistory() {
        user.defaultSteps()
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), lock.firstAcc().getSelfEmail())
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldSee(onSearchPage().advancedSearchBlock())
            .opensDefaultUrl()
            .clicksOn(onMessagePage().mail360HeaderBlock().searchInput())
            .shouldSeeThatElementHasText(onSearchPage().lastQueriesList().get(0), lock.firstAcc().getSelfEmail())
            .onMouseHover(onSearchPage().lastQueriesList().get(0))
            .onMouseHoverAndClick(onSearchPage().searchSuggestRemove())
            .shouldNotSeeElementInList(onSearchPage().lastQueriesList(), lock.firstAcc().getSelfEmail());
    }
}
