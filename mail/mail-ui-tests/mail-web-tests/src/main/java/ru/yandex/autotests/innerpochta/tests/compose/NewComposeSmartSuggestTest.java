package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Smart Suggest")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeSmartSuggestTest extends BaseTest {

    private static String FOR_SUGGEST = "Д";
    private static String AFTER_SUGGEST = "Добрый день";

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
    }

    @Test
    @Title("Закрываем попап саджеста горячей клавишей «ESC»")
    @TestCaseId("5903")
    public void shouldCloseSmartSuggestPopupByEsc() {
        openComposeAndCallForSmartSuggest();
        user.hotkeySteps().pressSimpleHotKey(
            onComposePopup().expandedPopup().bodyInput(),
            key(Keys.ESCAPE)
        );
        user.defaultSteps().shouldNotSee(onComposePopup().expandedPopup().smartSuggestPopup());
    }

    @Test
    @Title("Закрываем попап саджеста горячей клавишей «ENTER»")
    @TestCaseId("5903")
    public void shouldCloseSmartSuggestPopupByEnter() {
        openComposeAndCallForSmartSuggest();
        user.hotkeySteps().pressSimpleHotKey(
            onComposePopup().expandedPopup().bodyInput(),
            key(Keys.ENTER)
        );
        user.defaultSteps().shouldNotSee(onComposePopup().expandedPopup().smartSuggestPopup())
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), FOR_SUGGEST);
    }

    @Test
    @Title("Переносим выбранный вариант саджеста в тело письма горячей клавишей «TAB»")
    @TestCaseId("5903")
    public void shouldPrintChosenSuggestByTab() {
        openComposeAndCallForSmartSuggest();
        user.hotkeySteps().pressSimpleHotKey(
            onComposePopup().expandedPopup().bodyInput(),
            key(Keys.TAB)
        );
        user.defaultSteps().shouldNotSee(onComposePopup().expandedPopup().smartSuggestPopup())
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), AFTER_SUGGEST);
    }

    @Test
    @Title("Включаем/выключаем автодополнение")
    @TestCaseId("5901")
    public void shouldTurnOffTurnOnSmartSuggest() {
        user.defaultSteps().clicksOn(onHomePage().composeButton())
            .clicksOn(onComposePopup().expandedPopup().composeMoreBtn())
            .clicksOn(onComposePopup().expandedPopup().composeMoreOptionsPopup().autocompleteToggle())
            .inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), FOR_SUGGEST)
            .shouldNotSee(onComposePopup().expandedPopup().smartSuggestPopup())
            .clicksOn(onComposePopup().expandedPopup().composeMoreOptionsPopup().autocompleteToggle())
            .inputsTextInElementClearingThroughHotKeys(onComposePopup().expandedPopup().bodyInput(), FOR_SUGGEST)
            .shouldSee(onComposePopup().expandedPopup().smartSuggestPopup());
    }

    @Test
    @Title("Выбираем вариант саджеста мышью")
    @TestCaseId("5902")
    public void shouldChoseSuggestByMouse() {
        openComposeAndCallForSmartSuggest();
        user.defaultSteps().onMouseHoverAndClick(onComposePopup().expandedPopup().smartSuggestOptions().get(0))
            .shouldNotSee(onComposePopup().expandedPopup().smartSuggestPopup())
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), AFTER_SUGGEST);
    }

    @Test
    @Title("Закрываем попап кликом мыши")
    @TestCaseId("5902")
    public void shouldClosePopupByMouse() {
        openComposeAndCallForSmartSuggest();
        user.defaultSteps().onMouseHoverAndClick(onComposePopup().expandedPopup().bodyInput())
            .shouldNotSee(onComposePopup().expandedPopup().smartSuggestPopup());
    }

    @Step("Открываем композ и вводим текст для автодополнения")
    private void openComposeAndCallForSmartSuggest() {
        user.defaultSteps().clicksOn(onHomePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), FOR_SUGGEST)
            .shouldSee(onComposePopup().expandedPopup().smartSuggestPopup());
    }

}
