package ru.yandex.autotests.innerpochta.tests.main;

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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.ns.pages.commonblocks.UserMenuBlock.ABOOK;
import static ru.yandex.autotests.innerpochta.ns.pages.commonblocks.UserMenuBlock.CALENDAR;
import static ru.yandex.autotests.innerpochta.ns.pages.commonblocks.UserMenuBlock.DISK;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SIZE_VIEW_APP;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SIZE_VIEW_APP2;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тестируем “Компактный режим“")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.COMPACT_MODE)
public class CompactModeTest extends BaseTest {

    private static final Integer SIZE_VIEW = 1200;
    private static final Integer COMPACT_SIZE_VIEW = 1500;
    private static final Integer COMPACT_DEFAULT = 0;

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
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Устанавливаем ширину почты 1200px",
            of(
                SETTINGS_SIZE_VIEW_APP, SIZE_VIEW
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Включение «Компактного режима»")
    @TestCaseId("1920")
    public void shouldTurnCompactMode() {
        TurnCompactHeaderModeOn();
        TurnCompactMessageViewOn();
        user.defaultSteps().shouldSee(onMessagePage().services());
        user.messagesSteps().shouldSeeInboxCompactWidth(COMPACT_DEFAULT);
    }

    @Test
    @Title("Компактный режим в 3-pane")
    @TestCaseId("3213")
    public void shouldTurnCompactMode3pane() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3 pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        user.defaultSteps().refreshPage();
        TurnCompactHeaderModeOn();
        user.messagesSteps().shouldSeeInboxCompactWidth(COMPACT_DEFAULT);
        user.defaultSteps().clicksOn(onMessagePage().moreServices())
            .shouldSeeElementInList(onMessagePage().services(), DISK);
        user.defaultSteps().shouldSeeElementInList(onMessagePage().services(), CALENDAR);
        user.defaultSteps().shouldSeeElementInList(onMessagePage().services(), ABOOK);
    }

    @Step("Включаем компактный режим шапки")
    private void TurnCompactHeaderModeOn() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER)
            .deselects(onOtherSettings().blockSetupOther().topPanel().show360Header());
        user.settingsSteps().saveOtherSettingsSetup();
        user.defaultSteps().opensDefaultUrl();
    }

    @Step("Включаем компактный вид писем")
    private void TurnCompactMessageViewOn() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .shouldSee(onMessagePage().mainSettingsPopupNew())
            .turnTrue(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(3));
    }

    @Test
    @Title("Изменение ширины инбокса и выключение “Компактного режима“")
    @TestCaseId("1927")
    public void shouldTurnOffCompactMode() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактный вид писем и компактную шапку, ширина инбокса 1500px",
            of(
                LIZA_MINIFIED, STATUS_ON,
                LIZA_MINIFIED_HEADER, STATUS_ON,
                SETTINGS_SIZE_VIEW_APP2, COMPACT_SIZE_VIEW
            )
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeInboxCompactWidth(COMPACT_SIZE_VIEW);
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .shouldSee(onMessagePage().mainSettingsPopupNew())
            .deselects(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(3));
        user.messagesSteps().shouldSeeInboxWidth(SIZE_VIEW)
            .shouldSeeInboxCompactWidth(COMPACT_DEFAULT);
    }

    @Test
    @Title("Проверяем наличие ссылки “Письма“, находясь в абуке")
    @TestCaseId("2154")
    public void shouldSeeLettersLinkInAbook() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактный вид писем и компактуную шапку",
            of(
                LIZA_MINIFIED, STATUS_ON,
                LIZA_MINIFIED_HEADER, STATUS_ON
            )
        );
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().moreServices())
            .clicksOnElementWithText(onMessagePage().services(), ABOOK)
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.CONTACTS)
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .shouldSee(onMessagePage().mail360HeaderBlock().userMenuDropdown());
    }

    @Test
    @Title("Не показываем промо ЯПлюс в компактной шапке")
    @TestCaseId("4588")
    public void shouldNotSeeYaPlusPromoInCompactToolbar() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, STATUS_ON)
        );
        user.defaultSteps().refreshPage()
            .shouldSee(onMessagePage().toolbar())
            .shouldNotSee(onMessagePage().yaPlusPromo());
    }
}
