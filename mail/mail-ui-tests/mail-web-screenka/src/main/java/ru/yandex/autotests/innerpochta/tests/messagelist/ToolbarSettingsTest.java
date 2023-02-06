package ru.yandex.autotests.innerpochta.tests.messagelist;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Настройка тулбара")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class ToolbarSettingsTest {

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
    @Title("Открываем дропдаун настройки ПК")
    @TestCaseId("2747")
    public void shouldSeeSettingsToolbarDropdown() {
        Consumer<InitStepsRule> actions = this::openCustomBtnSettings;

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем настройку ПК «В папку»")
    @TestCaseId("2748")
    public void shouldSeeFolderBtnSettings() {
        Consumer<InitStepsRule> actions = st -> {
            openCustomBtnSettings(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().customButtons().overview().moveToFolder())
                .shouldSee(st.pages().mail().customButtons().configureFoldersButton());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем настройку ПК «Метка»")
    @TestCaseId("2749")
    public void shouldSeeLabelBtnSettings() {
        Consumer<InitStepsRule> actions = st -> {
            openCustomBtnSettings(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().customButtons().overview().label())
                .shouldSee(st.pages().mail().customButtons().configureLabelButton());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем настройку ПК «Автоответ»")
    @TestCaseId("2750")
    public void shouldSeeAutoReplayBtnSettings() {
        Consumer<InitStepsRule> actions = st -> {
            openCustomBtnSettings(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().customButtons().overview().autoReply())
                .shouldSee(st.pages().mail().customButtons().autoReplyButtonConfigure());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Открываем настройку ПК «Переслать»")
    @TestCaseId("2751")
    public void shouldSeeForwardBtnSettings() {
        Consumer<InitStepsRule> actions = st -> {
            openCustomBtnSettings(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().customButtons().overview().forward())
                .shouldSee(st.pages().mail().customButtons().configureForwardButton());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем настройку ПК «Архив»")
    @TestCaseId("2752")
    public void shouldSeeArchiveBtnSettings() {
        Consumer<InitStepsRule> actions = st -> {
            openCustomBtnSettings(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().customButtons().overview().archive())
                .shouldSee(st.pages().mail().home().toolbar().archiveButton());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Жмем «Отмена» в ПК")
    @TestCaseId("2819")
    public void shouldNotSeeSettingsToolbarDropdown() {
        Consumer<InitStepsRule> actions = st -> {
            openCustomBtnSettings(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().customButtons().overview().cancel());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем настройку ПК кнопок")
    private void openCustomBtnSettings(InitStepsRule st) {
        st.user().defaultSteps().onMouseHoverAndClick(st.pages().mail().home().toolbar().configureCustomButtons())
            .shouldSee(st.pages().mail().customButtons().overview());
    }
}
