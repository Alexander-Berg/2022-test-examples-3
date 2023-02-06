package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author yaroslavna
 */
@Aqua.Test
@Title("Шапка формы на странице письма")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.HEAD)
public class ComposeHeadTest {

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
    public RuleChain chain = rules.createRuleChain()
        .around(addLabelIfNeed(() -> stepsProd.user()));

    @Test
    @Title("Должны увидеть алерт о пустом поле кому")
    @TestCaseId("2721")
    public void shouldSeeAlertComposeEmptyTo() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn())
                .shouldSee(st.pages().mail().composePopup().confirmClosePopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть алерт о некорректном адресе")
    @TestCaseId("2722")
    public void shouldSeeAlertComposeWrongEmail() {
        String invalidEmail = Utils.getRandomString();
        String subj = Utils.getRandomString();
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), invalidEmail)
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().sbjInput(), subj)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn())
                .shouldSee(st.pages().mail().composePopup().confirmClosePopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем поля Кому, Копия, Скрытая")
    @TestCaseId("2723")
    public void shouldSeeToCcBcc() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn())
                .shouldSee(
                    st.pages().mail().composePopup().expandedPopup().popupCc(),
                    st.pages().mail().composePopup().expandedPopup().popupBcc(),
                    st.pages().mail().composePopup().expandedPopup().popupFrom()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку адресов отправителя")
    @TestCaseId("3215")
    public void shouldSeeAliasesDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().popupFrom())
                .shouldSee(st.pages().mail().composePopup().fromSuggestList());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть выпадушку c метками")
    @TestCaseId("2725")
    public void shouldSeeLabelDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().composeMoreBtn())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().composeMoreOptionsPopup().addLabelsOption())
                .shouldSee(st.pages().mail().composePopup().labelsPopup());

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ставим метки на письмо")
    @TestCaseId("3228")
    public void shouldSeeLabelsInCompose() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().composeMoreBtn())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().composeMoreOptionsPopup().addLabelsOption())
                .clicksOn(st.pages().mail().composePopup().labelsPopup().importantLabel())
                .shouldSee(st.pages().mail().composePopup().expandedPopup().labels().importantLabel());

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

}
