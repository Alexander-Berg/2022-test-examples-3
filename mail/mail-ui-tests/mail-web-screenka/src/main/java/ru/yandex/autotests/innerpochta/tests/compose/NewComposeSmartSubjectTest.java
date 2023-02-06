package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SMART_SUBJECT;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Smart Subject")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeSmartSubjectTest {

    private String msg_body = getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare()
        .withProdSteps(stepsProd)
        .withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ smart subject",
            of(SMART_SUBJECT, TRUE)
        );
    }

    @Test
    @Title("Показ попапа Smart Subject")
    @TestCaseId("5949")
    public void shouldSeeSmartSubjectPopup() {
        Consumer<InitStepsRule> actions = st -> {
            callSmartSubjectPopup(st);
            st.user().defaultSteps().shouldSee(st.pages().mail().composePopup().smartSubjectPopup());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("5951")
    @Title("Верстка попапа Smart Subject после ввода темы")
    public void shouldSeeSmartSubjectPopupAfterThemeInput() {
        Consumer<InitStepsRule> actions = st -> {
            callSmartSubjectPopup(st);
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().composePopup().smartSubjectPopup().themeInput(), msg_body)
                .shouldSee(st.pages().mail().composePopup().smartSubjectPopup());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбор одного из вариантов подсказки темы")
    @TestCaseId("5954")
    public void shouldChooseSuggest() {
        Consumer<InitStepsRule> actions = st -> {
            callSmartSubjectPopup(st);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().composePopup().smartSubjectPopup().suggestItem().get(1));
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Вызов попапа Smart Subject")
    private void callSmartSubjectPopup(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
            .inputsTextInElement(
                st.pages().mail().composePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), msg_body)
            .clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn());
    }
}
