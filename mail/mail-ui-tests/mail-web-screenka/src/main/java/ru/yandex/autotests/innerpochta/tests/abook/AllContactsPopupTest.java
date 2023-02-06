package ru.yandex.autotests.innerpochta.tests.abook;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Попап контактов в композе")
@Features(FeaturesConst.ABOOK)
@Tag(FeaturesConst.ABOOK)
@Stories(FeaturesConst.GENERAL)
@Description("Подготовлен юзер с контактами")
public class AllContactsPopupTest {

    private static final String SCRIPT_FOR_SCROLLDOWN = "$('.js-allow-scroll-in-popup').scrollTop(5000)";
    public static final String EMAIL = "bkwkQgUv";
    public static final String CONTACT_NAME = "0deoce ee eee";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Должны видеть все контакты")
    @TestCaseId("2833")
    public void shouldSeeAllContactsInPopup() {
        Consumer<InitStepsRule> actions = st -> {
            openAbookPopup(st);
            scrollPopup(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().compose().abookPopup().showAllContactsBtn());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Еще контакты в попапе")
    @TestCaseId("3252")
    public void shouldOpenMoreContacts() {
        Consumer<InitStepsRule> actions = st -> {
            openAbookPopup(st);
            scrollPopup(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().compose().abookPopup().moreContactsBtn());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Контакт с несколькими адресами")
    @TestCaseId("2830")
    public void shouldCheckContactWithFewEmails() {
        Consumer<InitStepsRule> actions = st -> {
            openAbookPopup(st);
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().compose().abookPopup().searchInput(), CONTACT_NAME)
                .waitInSeconds(3)
                .shouldSeeElementsCount(st.pages().mail().compose().abookPopup().contacts(), 5)
                .clicksOnElementWithWaiting(st.pages().mail().compose().abookPopup().contacts().get(0));
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Поиск контакта в попапе")
    @TestCaseId("3251")
    public void shouldSearchContact() {
        Consumer<InitStepsRule> actions = st -> {
            openAbookPopup(st);
            st.user().defaultSteps().inputsTextInElement(st.pages().mail().compose().abookPopup().searchInput(), EMAIL)
                .shouldSee(st.pages().mail().compose().abookPopup().contacts().get(0).email());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем попап абука")
    private void openAbookPopup(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().popupTo())
            .clicksOn(st.pages().mail().composePopup().abookBtn())
            .shouldSee(st.pages().mail().compose().abookPopup());
    }

    @Step("Скроллим контакты вниз")
    private void scrollPopup(InitStepsRule st) {
        st.user().defaultSteps().executesJavaScript(SCRIPT_FOR_SCROLLDOWN);
    }
}
