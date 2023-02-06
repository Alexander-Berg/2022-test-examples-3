package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.LEFT_PANEL_COMPACT_SIZE;
import static ru.yandex.autotests.innerpochta.util.MailConst.LEFT_PANEL_FULL_SIZE;
import static ru.yandex.autotests.innerpochta.util.MailConst.LEFT_PANEL_HALF_COMPACT_SIZE;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на табы в ЛК")
@Features(FeaturesConst.LEFT_PANEL)
@Tag(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.TABS)
@RunWith(DataProviderRunner.class)
public class TabsScreenTest {

    private static final int MSG_COUNT = 2;

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
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Выключаем компактную левую колонку, включаем табы",
                of(
                    SIZE_LAYOUT_LEFT, LEFT_PANEL_FULL_SIZE,
                    FOLDER_TABS, TRUE,
                    SETTINGS_PARAM_MESSAGES_PER_PAGE, MSG_COUNT
                )
            );
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 3);
        stepsProd.user().defaultSteps().refreshPage();
    }

    @DataProvider
    public static Object[][] userInterface() {
        return new Object[][]{
            {LAYOUT_2PANE},
            {SETTINGS_LAYOUT_3PANE_HORIZONTAL},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Test
    @Title("Верстка табов в свернутой ЛК")
    @TestCaseId("5104")
    @UseDataProvider("userInterface")
    public void shouldSeeTabsInCompactLC(String layout) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiSettingsSteps().callWithListAndParams(
                "Включаем " + layout + " и компактную ЛК",
                of(
                    SETTINGS_PARAM_LAYOUT, layout,
                    SIZE_LAYOUT_LEFT, LEFT_PANEL_COMPACT_SIZE
                )
            );
            st.user().defaultSteps().shouldSee(st.pages().mail().home().newsTab());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выделить все письма в табе")
    @TestCaseId("5073")
    public void shouldSeeAllMessagesSelected() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().opensDefaultUrlWithPostFix("#tabs/news");
            st.user().messagesSteps().selectsAllDisplayedMessagesInFolder()
                .shouldSeeThatNMessagesAreSelected(MSG_COUNT);
            st.user().defaultSteps().clicksOn(st.pages().mail().home().selectAllMessagesInFolder());
        };
        stepsProd.user().apiMessagesSteps().moveAllMessagesToTab(MailConst.NEWS_TAB, INBOX);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Табы в компактном режиме")
    @TestCaseId("5078")
    public void shouldSeeTabsInCompactMode() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiSettingsSteps().callWithListAndParams(
                "Включаем компактный режим",
                of(
                    LIZA_MINIFIED, STATUS_OFF,
                    LIZA_MINIFIED_HEADER, STATUS_ON
                )
            );
            st.user().messagesSteps().shouldSeeCorrectNumberOfMessages(2);
            st.user().defaultSteps().shouldSee(st.pages().mail().home().newsTab());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Табы в полукомпактной ЛК")
    @TestCaseId("5105")
    public void shouldSeeTabsInHalfCompactLC() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiSettingsSteps().callWithListAndParams(
                "Включаем полукомпактную ЛК",
                of(SIZE_LAYOUT_LEFT, LEFT_PANEL_HALF_COMPACT_SIZE)
            );
            st.user().defaultSteps().shouldSee(st.pages().mail().home().newsTab());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
