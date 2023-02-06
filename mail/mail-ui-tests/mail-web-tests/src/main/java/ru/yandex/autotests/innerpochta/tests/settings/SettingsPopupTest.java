package ru.yandex.autotests.innerpochta.tests.settings;

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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_BACKUP;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_DOMAIN;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Тесты на попап настроек")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SETTINGS_POPUP)
public class SettingsPopupTest extends BaseTest {

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
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps()
            .clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .shouldSee(onMessagePage().mainSettingsPopupNew());
    }

    @Test
    @Title("Закрыть попап настроек по клику вне")
    @TestCaseId("6258")
    public void shouldCloseSettingsPopup() {
        user.defaultSteps().offsetClick(onMessagePage().displayedMessages(), 0, 100)
            .shouldNotSee(onMessagePage().mainSettingsPopupNew());
    }

    @Test
    @Title("Закрыть попап настроек по Esc")
    @TestCaseId("6258")
    public void shouldCloseSettingsPopupWithEsc() {
        user.hotkeySteps().pressHotKeys(Keys.ESCAPE.toString());
        user.defaultSteps().shouldNotSee(onMessagePage().mainSettingsPopupNew());
    }

    @Test
    @Title("Закрыть попап настроек по клику на шестеренку")
    @TestCaseId("6258")
    public void shouldCloseSettingsPopupWithClickOnWheel() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .shouldNotSee(onMessagePage().mainSettingsPopupNew());
    }

    @Test
    @Title("Переход на страницу Все настройки из попапа")
    @TestCaseId("6285")
    public void shouldRedirectToSettings() {
        user.defaultSteps().clicksOn(onMessagePage().mainSettingsPopupNew().moveToSettingsButton())
            .shouldBeOnUrlWith(SETTINGS)
            .shouldNotSee(onMessagePage().mainSettingsPopupNew());
    }

    @Test
    @Title("Должны перейти на страницу настройки красивого адреса")
    @TestCaseId("6275")
    public void shouldRedirectToBeuatifulEmail() {
        user.defaultSteps().clicksOn(onMessagePage().mainSettingsPopupNew().beautifulEmailPromoButton())
            .shouldBeOnUrlWith(SETTINGS_DOMAIN);
    }

    @Test
    @Title("Должны перейти на страницу настройки резервной копии")
    @TestCaseId("6275")
    public void shouldRedirectToBackup() {
        user.defaultSteps().clicksOn(onMessagePage().mainSettingsPopupNew().backupPromoButton())
            .shouldBeOnUrlWith(SETTINGS_BACKUP);
    }

    @Test
    @Title("Должны перейти в УР в попапе настроек")
    @TestCaseId("6275")
    public void shouldSeeSubscriptionsPromo() {
        user.defaultSteps().clicksOn(onMessagePage().mainSettingsPopupNew().subscriptionsPromoButton())
            .shouldSee(onUnsubscribePopupPage().subscriptionIframe());
    }
}
