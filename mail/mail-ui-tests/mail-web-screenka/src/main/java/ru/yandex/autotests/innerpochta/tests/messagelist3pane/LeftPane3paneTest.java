package ru.yandex.autotests.innerpochta.tests.messagelist3pane;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_RED_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Левая колонка в 3pane")
@Features(FeaturesConst.THREE_PANE)
@Tag(FeaturesConst.THREE_PANE)
@Stories(FeaturesConst.LEFT_PANEL)
public class LeftPane3paneTest {

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

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-vertical, разворачиваем левую колонку",
            of(
                SIZE_LAYOUT_LEFT, "300",
                SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL
            )
        );
        Message msg = stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), "");
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), "");
        stepsProd.user().apiMessagesSteps().moveMessagesToSpam(msg);
    }

    @Test
    @Title("Должны видеть все письма с меткой")
    @TestCaseId("3156")
    public void shouldSeeMessagesWithLabel() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().labelsNavigation().userLabels().get(0).labelName());

        stepsProd.user().apiLabelsSteps().markWithLabel(
            stepsProd.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0),
            stepsProd.user().apiLabelsSteps().addNewLabel(getRandomName(), LABELS_PARAM_RED_COLOR)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть пустой результат поиска по метке")
    @TestCaseId("3157")
    public void shouldSeeNoMessagesWithLabel() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().labelsNavigation().userLabels().get(0).labelName());

        stepsProd.user().apiLabelsSteps().addNewLabel(getRandomName(), LABELS_PARAM_RED_COLOR);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть попап очистки сообщений в Спаме")
    @TestCaseId("3158")
    public void shouldSeeClearSpamPopupIn3pane() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(
                    st.pages().mail().home().foldersNavigation().spamFolder(),
                    st.pages().mail().home().foldersNavigation().cleanSpamFolder()
                )
                .shouldSee(st.pages().mail().home().clearFolderPopUp());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть свернутую ЛК")
    @TestCaseId("3159")
    public void shouldSeeFoldedLeftPane() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().refreshPage()
                .clicksOn(st.pages().mail().home().toolbar().layoutSwitchBtn())
                .turnTrue(st.pages().mail().home().layoutSwitchDropdown().compactLeftColumnSwitch())
                .shouldSee(st.pages().mail().home().compactLeftPanel());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть ссылки в футере ЛК")
    @TestCaseId("6414")
    public void shouldSeeLeftPaneFooter() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().leftPanelFooterLineBlock());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
