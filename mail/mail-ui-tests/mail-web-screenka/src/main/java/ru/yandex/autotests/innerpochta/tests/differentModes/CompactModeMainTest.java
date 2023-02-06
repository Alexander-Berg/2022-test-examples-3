package ru.yandex.autotests.innerpochta.tests.differentModes;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule.addFolderIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASS_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.SERVER_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Компактный режим писем и меню")
@Features(FeaturesConst.COMPACT_MODE)
@Tag(FeaturesConst.COMPACT_MODE)
@Stories(FeaturesConst.GENERAL)
public class CompactModeMainTest {

    private String subject = getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();

    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AddFolderIfNeedRule addFolderIfNeed = addFolderIfNeed(() -> stepsProd.user());
    private AddLabelIfNeedRule addLabelIfNeed = addLabelIfNeed(() -> stepsProd.user());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(addFolderIfNeed)
        .around(addLabelIfNeed);

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем компактное меню и письма",
            of(
                LIZA_MINIFIED, STATUS_ON,
                LIZA_MINIFIED_HEADER, STATUS_ON
            )
        );
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, getRandomString());
        stepsProd.user().apiCollectorSteps().createNewCollector(MAIL_COLLECTOR, PASS_COLLECTOR, SERVER_COLLECTOR);
    }

    @Test
    @Title("Открываем выпадушку залогина")
    @TestCaseId("1926")
    public void shouldOpenUserMenuDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .shouldSee(st.pages().mail().home().userMenuDropdown());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на шестеренку в шапке")
    @TestCaseId("3103")
    public void shouldSeeSettingsPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().settingsMenu())
                .shouldSee(st.pages().mail().home().mainSettingsPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку тем")
    @TestCaseId("3104")
    public void shouldSeeThemeDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().changeThemeBtn())
                .shouldSee(st.pages().mail().home().changeThemeBlock());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на «Написать»")
    @TestCaseId("3107")
    public void shouldOpenCompose() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Включаем компактный вид писем")
    @TestCaseId("3210")
    public void shouldSeeOnlyCompactMessages() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiSettingsSteps().callWithListAndParams(LIZA_MINIFIED, of(LIZA_MINIFIED, EMPTY_STR))
                .callWithListAndParams(LIZA_MINIFIED_HEADER, of(LIZA_MINIFIED_HEADER, EMPTY_STR));
            st.user().defaultSteps().refreshPage()
                .clicksOn(st.pages().mail().home().toolbar().layoutSwitchBtn())
                .turnTrue(st.pages().mail().home().layoutSwitchDropdown().compactModeSwitch())
                .shouldNotSee(st.pages().mail().home().layoutSwitchDropdown());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Включаем компактное меню")
    @TestCaseId("3391")
    public void shouldNotSeeHeader() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiSettingsSteps().callWithListAndParams(LIZA_MINIFIED, of(LIZA_MINIFIED, EMPTY_STR))
                .callWithListAndParams(LIZA_MINIFIED_HEADER, of(LIZA_MINIFIED_HEADER, EMPTY_STR));
            st.user().defaultSteps().refreshPage()
                .clicksOn(st.pages().mail().home().toolbar().layoutSwitchBtn())
                .turnTrue(st.pages().mail().home().layoutSwitchDropdown().compactHeaderSwitch())
                .shouldNotSee(
                    st.pages().mail().home().mail360HeaderBlock().moreServices(),
                    st.pages().mail().home().layoutSwitchDropdown()
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Сворачиваем ЛК в компактном режиме")
    @TestCaseId("2296")
    public void shouldSeeCompactLeftPanel() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().layoutSwitchBtn())
                .turnTrue(st.pages().mail().home().layoutSwitchDropdown().compactLeftColumnSwitch())
                .shouldSee(st.pages().mail().home().compactLeftPanel());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть цветные иконки в компактном тулбаре")
    @TestCaseId("4206")
    public void shouldSeeEnabledIconsOnCompactToolbar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().displayedMessages().list().get(0).checkBox())
                .shouldSee(st.pages().mail().home().toolbar());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Компактное меню в Настройках")
    @TestCaseId("5095")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68622")
    public void shouldSeeCompactMenuInSettings() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensFragment(QuickFragments.SETTINGS)
                .shouldSee(st.pages().mail().settingsCommon().services());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Показываем иконостас в компактном меню")
    @TestCaseId("5603")
    public void shouldSeeAdvancedCompactSearch() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().refreshPage()
                .onMouseHoverAndClick(st.pages().mail().home().mail360HeaderBlock().searchBtnCompactMode())
                .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchInput(), subject)
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchBtn())
                .shouldSee(st.pages().mail().search().advancedSearchBlock());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Аватарки при компактном виде писем")
    @TestCaseId("4105")
    public void shouldSeeAvatarsInCompactMode() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().avatarImgList());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Иконки в тулбаре скрываются под «Еще» в компактном меню")
    @TestCaseId("4480")
    public void shouldSeeMoreInCompactMode() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().setsWindowSize(1000, 600)
                .shouldSee(st.pages().mail().home().toolbar().moreBtn())
                .shouldNotSee(st.pages().mail().home().toolbar().pinBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Иконки в тулбаре скрываются под «Еще»")
    @TestCaseId("3943")
    public void shouldSeeMoreInToolbar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().setsWindowSize(1000, 600)
                .shouldSee(st.pages().mail().home().toolbar().moreBtn())
                .shouldNotSee(st.pages().mail().home().toolbar().pinBtn());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, EMPTY_STR)
        );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
