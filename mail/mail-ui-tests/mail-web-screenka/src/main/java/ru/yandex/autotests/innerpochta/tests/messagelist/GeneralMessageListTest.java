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
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
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
@Title("Общие тесты")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class GeneralMessageListTest {

    private static final int MSG_COUNT = 2;
    private static final String THEME = "Оформление";

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
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MSG_COUNT);
    }

    @Test
    @Title("Открываем выпадушку «Вид»")
    @TestCaseId("2743")
    public void shouldOpenMoreServicesDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().layoutSwitchBtn())
                .shouldSee(st.pages().mail().home().layoutSwitchDropdown());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Футер - открываем выпадушку языков")
    @TestCaseId("3389")
    public void shouldSeeLangDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().footerLineBlock().languageSwitch())
                .shouldSee(st.pages().mail().home().langsDropdown());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем плашку «Выбрано N писем»")
    @TestCaseId("2822")
    public void shouldSeeNMessagesSelectedLine() {
        Consumer<InitStepsRule> actions = st ->
            st.user().messagesSteps().selectsAllDisplayedMessagesInFolder()
                .shouldSeeThatNMessagesAreSelected(MSG_COUNT);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Жмем «Снять выделение» плашки «Выбрано N писем»")
    @TestCaseId("2952")
    public void shouldDeselectAllMessages() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().selectsAllDisplayedMessagesInFolder();
            st.user().defaultSteps().clicksOn(st.pages().mail().home().inboxMsgInfoline().deselectLink());
            st.user().messagesSteps().shouldSeeThatMessagesAreNotSelected();
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем показ КМ в списке писем")
    @TestCaseId("2837")
    public void shouldSeeContextMenuInMessageList() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().rightClick(st.pages().mail().home().displayedMessages().list().get(0).subject());
            st.user().messagesSteps().shouldSeeContextMenu();
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем показ КМ папок в левой колонке")
    @TestCaseId("2838")
    public void shouldSeeContextMenuInLeftColumn() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().rightClick(st.pages().mail().home().foldersNavigation().inboxFolder());
            st.user().messagesSteps().shouldSeeContextMenu();
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем «Оформление» в попапе настроек")
    @TestCaseId("2354")
    public void shouldSeeThemesDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().settingsMenu())
                .clicksOnElementWithText(st.pages().mail().home().settingsLink(), THEME)
                .shouldSee(st.pages().mail().home().changeThemeBlock());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
