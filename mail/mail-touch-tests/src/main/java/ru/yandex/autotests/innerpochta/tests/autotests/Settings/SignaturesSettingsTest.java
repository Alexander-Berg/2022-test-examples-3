package ru.yandex.autotests.innerpochta.tests.autotests.Settings;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
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
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на настройку подписей")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SIGNATURES_SETTINGS)
@RunWith(DataProviderRunner.class)
public class SignaturesSettingsTest {

    private static final String SETTINGS = "Настройки";
    private static final String GENERAL_SECTION_NAME = "Основные";
    private static final String SIGNATURES_URL_PART = "general/signatures";

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
        steps.user().apiSettingsSteps().changeSignsAmountTo(1);
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART));
    }

    @Test
    @Title("Должны открыть раздел управления подписями и вернуться из него")
    @TestCaseId("1346")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldBackFromSignaturesSettingsSection() {
        openSettings();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().settings().settingsItem(), GENERAL_SECTION_NAME)
            .clicksOn(steps.pages().touch().settings().signaturesItem())
            .shouldBeOnUrl(containsString(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)))
            .clicksOn(steps.pages().touch().settings().closeBtn())
            .shouldSee(steps.pages().touch().settings().tabsToggler());
    }

    @Test
    @Title("Должны удалить последнюю подпись")
    @TestCaseId("1351")
    public void shouldDeleteSignature() {
        openSignAndClickDelete(0);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().popup().yesBtn())
            .shouldSee(steps.pages().touch().settings().emptyList());
    }

    @Test
    @Title("Должны удалить одну из подписей")
    @TestCaseId("1351")
    public void shouldDeleteOneOfSignature() {
        steps.user().apiSettingsSteps().changeSignsAmountTo(3);
        steps.user().defaultSteps().refreshPage();
        openSignAndClickDelete(1);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().popup().yesBtn())
            .shouldSeeElementsCount(steps.pages().touch().settings().signatures(), 2);
    }

    @Test
    @Title("Должны закрыть попап удаления подписи")
    @TestCaseId("1352")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCloseSignDeletingPopup() {
        openSignAndClickDelete(0);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().popup().closeBtn())
            .shouldNotSee(steps.pages().touch().settings().popup())
            .clicksOn(steps.pages().touch().settings().closeBtn())
            .shouldSeeElementsCount(steps.pages().touch().settings().signatures(), 1);
    }

    @Test
    @Title("Должны отменить удаление подписи")
    @TestCaseId("1353")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCancelSignDeleting() {
        openSignAndClickDelete(0);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().popup().noBtn())
            .shouldNotSee(steps.pages().touch().settings().popup())
            .clicksOn(steps.pages().touch().settings().closeBtn())
            .shouldSeeElementsCount(steps.pages().touch().settings().signatures(), 1);
    }

    @Test
    @Title("Должны вернуться со страницы создания подписи")
    @TestCaseId("1350")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldBackFromSignCreation() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().create())
            .clicksOn(steps.pages().touch().settings().closeBtn())
            .shouldSeeElementsCount(steps.pages().touch().settings().signatures(), 1);
    }

    @Test
    @Title("Должны вернуться со страницы редактирования подписи")
    @TestCaseId("1355")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldBackFromSignEdit() {
        String text = getRandomString();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().settings().signatures().waitUntil(not(empty())).get(0))
            .clicksAndInputsText(steps.pages().touch().settings().signatureInput(), text)
            .clicksOn(steps.pages().touch().settings().closeBtn())
            .shouldNotContainText(steps.pages().touch().settings().signatures().waitUntil(not(empty())).get(0), text);
    }

    @Test
    @Title("Должны вернуться со страницы создания подписи")
    @TestCaseId("1357")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldCancelSignCreationTablet() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().create())
            .clicksOn(steps.pages().touch().settings().cancelTablet())
            .shouldSeeElementsCount(steps.pages().touch().settings().signatures(), 1);
    }

    @Test
    @Title("Должны вернуться со страницы редактирования подписи")
    @TestCaseId("1358")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldCancelSignEditTablet() {
        String text = getRandomString();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().settings().signatures().waitUntil(not(empty())).get(0))
            .clicksAndInputsText(steps.pages().touch().settings().signatureInput(), text)
            .clicksOn(steps.pages().touch().settings().cancelTablet())
            .shouldNotContainText(steps.pages().touch().settings().signatures().waitUntil(not(empty())).get(0), text);
    }

    @Step("Открываем настройки из левой колонки")
    private void openSettings() {
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(steps.pages().touch().sidebar().leftPanelItems().waitUntil(not(empty())).get(0))
            .clicksOnElementWithText(steps.pages().touch().sidebar().leftPanelItems(), SETTINGS);
    }

    @Step("Открываем подпись и нажимаем на кнопку «Удалить подпись»")
    private void openSignAndClickDelete(int signNum) {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().settings().signatures().waitUntil(not(empty())).get(signNum))
            .clicksOn(steps.pages().touch().settings().removeSign());
    }
}
