package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Smart Suggest")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeSmartSuggestTest {

    private static String AUTOCOMPLETE_BUTTON_TEXT = "Автодополнение";
    private static String FOR_SUGGEST = "Д";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Верстка кнопок и попапа Smart Suggest")
    @TestCaseId("5900")
    public void shouldSeeSmartSuggestPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().composeMoreBtn())
                .shouldSee(st.pages().mail().composePopup().expandedPopup().composeMoreOptionsPopup().autocompleteToggle())
                .shouldContainText(
                    st.pages().mail().composePopup().expandedPopup().composeMoreOptionsPopup().autocompleteOption(),
                    AUTOCOMPLETE_BUTTON_TEXT
                )
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), FOR_SUGGEST)
                .shouldSee(st.pages().mail().composePopup().expandedPopup().smartSuggestPopup());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбираем вариант саджеста горячей клавишей «ARROW DOWN»")
    @TestCaseId("5903")
    public void shouldChooseOptionByArrowDown() {
        Consumer<InitStepsRule> actions = st -> {
            openComposeAndCallForSmartSuggest(st);
            st.user().hotkeySteps().pressSimpleHotKey(
                st.pages().mail().composePopup().expandedPopup().bodyInput(),
                key(Keys.ARROW_DOWN)
            );
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбираем вариант саджеста горячей клавишей «ARROW UP»")
    @TestCaseId("5903")
    public void shouldChooseOptionByArrowUp() {
        Consumer<InitStepsRule> actions = st -> {
            openComposeAndCallForSmartSuggest(st);
            st.user().hotkeySteps().pressSimpleHotKey(
                st.pages().mail().composePopup().expandedPopup().bodyInput(),
                key(Keys.ARROW_DOWN)
            );
            st.user().hotkeySteps().pressSimpleHotKey(
                st.pages().mail().composePopup().expandedPopup().bodyInput(),
                key(Keys.ARROW_UP)
            );
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем композ и вводим текст для автодополнения")
    private void openComposeAndCallForSmartSuggest(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
            .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), FOR_SUGGEST)
            .shouldSee(st.pages().mail().composePopup().expandedPopup().smartSuggestPopup());
    }
}
