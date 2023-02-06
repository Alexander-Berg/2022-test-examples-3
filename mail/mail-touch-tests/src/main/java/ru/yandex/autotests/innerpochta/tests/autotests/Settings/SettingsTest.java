package ru.yandex.autotests.innerpochta.tests.autotests.Settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие тесты в настройки")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SettingsTest {

    private static final String SETTINGS = "Настройки";
    private static final String SUPPORT_URLPART = "support";
    private static final String FEEDBACK_AND_HELP = "Справка и поддержка";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] serviceLines() {
        return new Object[][]{
            {"feedback", "Обратная связь"},
            {"support/m-mail/touch.html", "Помощь"},
            {"/restor", "Я не помню свой пароль"},
        };
    }

    @Test
    @Title("Должны выйти из настроек по крестику")
    @TestCaseId("1262")
    public void shouldCloseSettingsWithCross() {
        openSettings();
        closeSettings();
    }

    @Test
    @Title("Переходим в «Помощь», «Обратную связь» и «Я не помню свой пароль»")
    @TestCaseId("1265")
    @UseDataProvider("serviceLines")
    public void shouldGoToHelp(String url, String line) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(SUPPORT_URLPART))
            .clicksOnElementWithText(steps.pages().touch().settings().settingSectionItems().waitUntil(not(empty())), line)
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(url));
    }

    @Test
    @Title("Должны вернуться со страницы «Справка и поддержка»")
    @TestCaseId("1269")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldBackFromSupport() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SETTINGS_TOUCH.makeTouchUrlPart())
            .clicksOnElementWithText(
                steps.pages().touch().settings().settingsItem().waitUntil(not(empty())),
                FEEDBACK_AND_HELP
            )
            .shouldSee(steps.pages().touch().settings().settingSectionItems())
            .clicksOn(steps.pages().touch().settings().closeBtn())
            .shouldNotSee(steps.pages().touch().settings().settingSectionItems());
    }

    @Test
    @Title("Должны закрыть настройки со страница «Справка и поддержка»")
    @TestCaseId("1271")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldCloseSettingsFromSupport() {
        openSettings();
        steps.user().defaultSteps().clicksOnElementWithText(
            steps.pages().touch().settings().settingsItem().waitUntil(not(empty())),
            FEEDBACK_AND_HELP
        );
        closeSettings();
    }

    @Step("Открываем настройки из левой колонки")
    private void openSettings() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(steps.pages().touch().sidebar().leftPanelItems().waitUntil(not(empty())).get(0))
            .clicksOnElementWithText(steps.pages().touch().sidebar().leftPanelItems(), SETTINGS);
    }

    @Step("Закрываем настройки по крестику")
    private void closeSettings() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().closeBtn())
            .shouldSee(steps.pages().touch().sidebar().leftPanelBox());
    }
}
