package ru.yandex.autotests.innerpochta.tests.messagelist;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.LK_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LK_PROMO_SHOW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на левую колонку")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.LEFT_PANEL)
public class LeftPanelTest {

    private static final String ORANGE_THEME = "orangetree";
    private static final String FULL_SIZE = "220";
    private String labelName;

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

    @Before
    public void setUp() {
        prepareDataIfNeed();
        labelName = getRandomString();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Сбрасываем настройку раскрытия промки в ЛК",
            of(LK_PROMO_SHOW, EMPTY_STR)
        );

    }

    @Test
    @Title("Открываем попап пометки прочитанными")
    @TestCaseId("2973")
    public void shouldSeeMarkAllUnreadPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().foldersNavigation().markReadIcon())
                .shouldSee(st.pages().mail().home().markAsReadPopup());

        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), "");
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап создания папки")
    @TestCaseId("2974")
    public void shouldSeeCreateFolderPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().createFolderBtn())
                .shouldSee(st.pages().mail().home().createFolderPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап создания метки")
    @TestCaseId("2975")
    public void shouldSeeCreateLabelPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().createLabelPlusBtn())
                .shouldSee(st.pages().mail().home().createLabelPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем пустую метку")
    @TestCaseId("2976")
    public void shouldSeeEmptyMsgList() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOnElementWithText(st.pages().mail().home().labelsNavigation().userLabels(), labelName)
                .shouldSee(st.pages().mail().home().putMarkAutomaticallyButton());

        stepsProd.user().apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_GREEN_COLOR);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап создания правила для метки")
    @TestCaseId("2977")
    public void shouldSeeCreateFilterPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOnElementWithText(st.pages().mail().home().labelsNavigation().userLabels(), labelName)
                .clicksOn(st.pages().mail().home().putMarkAutomaticallyButton())
                .shouldSee(st.pages().mail().filtersCommon().newFilterPopUp());

        stepsProd.user().apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_GREEN_COLOR);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем метку с письмами")
    @TestCaseId("2978")
    public void shouldOpenLabel() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().labelsNavigation().userLabels().get(0).labelName())
                .shouldSee(st.pages().mail().home().displayedMessages().list().get(0).labels().get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем полный список меток")
    @TestCaseId("2979")
    public void shouldShowAllLabels() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .shouldSee(st.pages().mail().home().labelsNavigation().userLabels());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }


    @Test
    @Title("Нажимаем на кнопку “Очистить папку“")
    @TestCaseId("3029")
    public void shouldSeeClearSpamPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(
                    st.pages().mail().home().foldersNavigation().spamFolder(),
                    st.pages().mail().home().notificationBlock().cleanSpamFolderButton()
                )
                .shouldSee(st.pages().mail().home().clearFolderPopUpOld());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем КМ меток")
    @TestCaseId("3552")
    public void shouldSeeLabelsContextMenu() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .rightClick(st.pages().mail().home().labelsNavigation().userLabels().get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Меняем тему и сворачиваем ЛК")
    @TestCaseId("2315")
    public void shouldSeeCompactLeftPanel() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .refreshPage()
                .clicksOn(st.pages().mail().home().toolbar().layoutSwitchBtn())
                .turnTrue(st.pages().mail().home().layoutSwitchDropdown().compactLeftColumnSwitch())
                .shouldSee(st.pages().mail().home().compactLeftPanel());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем тему апельсин и выключаем компактную левую колонку",
            of(
                COLOR_SCHEME, ORANGE_THEME,
                SIZE_LAYOUT_LEFT, FULL_SIZE
            )
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть промку 360 в ЛК")
    @TestCaseId("6048")
    public void shouldSeePromo360InLC() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().addExperimentsWithYexp(LK_PROMO)
                .shouldSee(st.pages().mail().home().promo360LC());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ промо",
            of(DISABLE_PROMO, FALSE)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Step("Подготоваливаем письма")
    private void prepareDataIfNeed() {
        Message msg = stepsProd.user().apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), "");
        Message msg2 = stepsProd.user().apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), "");
        stepsProd.user().apiLabelsSteps().markWithLabel(
            msg,
            stepsProd.user().apiLabelsSteps().getAllUserLabels().get(0)
        );
        stepsProd.user().apiMessagesSteps().moveMessagesToSpam(msg2);
    }
}
