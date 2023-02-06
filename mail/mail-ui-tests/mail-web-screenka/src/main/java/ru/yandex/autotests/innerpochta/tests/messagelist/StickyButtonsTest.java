package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProvider;
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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.CONTACTS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.LEFT_PANEL_COMPACT_SIZE;
import static ru.yandex.autotests.innerpochta.util.MailConst.LEFT_PANEL_FULL_SIZE;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_FOR_SCROLLDOWN_LEFT_COLUMN;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCROLL_PAGE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author mariya-murm
 */

@Aqua.Test
@Title("Тесты на залипающие кнопки")
@Features({FeaturesConst.MESSAGE_LIST, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class StickyButtonsTest {

    private static final String COLORFUL_THEME = "colorful";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().className();
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
                "Включаем 2пейн, дефолтную тему, просмотр письма в списке  писем;" +
                    " выключаем компактную левую колонку и компактную шапку",
                of(
                    SIZE_LAYOUT_LEFT, LEFT_PANEL_FULL_SIZE,
                    SETTINGS_OPEN_MSG_LIST, STATUS_ON,
                    LIZA_MINIFIED_HEADER, EMPTY_STR,
                    COLOR_SCHEME, COLORFUL_THEME,
                    SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE)
            );
        stepsProd.user().defaultSteps().refreshPage();
    }

    @Test
    @Title("Залипание кнопок в 2 пейн в темных и светлых темах")
    @TestCaseId("5326")
    @DataProvider({"colorful", "lamp"})
    public void shouldSeeStickyButtons(String theme) {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем тему",
            of(COLOR_SCHEME, theme)
        );
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().scrollDownPage();
            st.user().defaultSteps().shouldSee(
                st.pages().mail().home().composeButton(),
                st.pages().mail().home().toolbar().topBtn(),
                st.pages().mail().home().stickyToolBar()
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отскролл по кнопке Наверх")
    @TestCaseId("5326")
    public void shouldScrollUp() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().scrollDownPage();
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().topBtn())
                .shouldNotSee(st.pages().mail().home().toolbar().topBtn());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нет тулбара при открытом письме в списке")
    @TestCaseId("5327")
    public void shouldNotSeeStickyToolbar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().scrollDownPage()
                .clicksOnMessageByNumber(10);
            st.user().defaultSteps().shouldNotSee(st.pages().mail().home().stickyToolBar());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Тулбар появляется после закрытия письма в списке")
    @TestCaseId("5327")
    public void shouldSeeStickyToolbarAfterMessageClose() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().scrollDownPage()
                .clicksOnMessageByNumber(10);
            st.user().defaultSteps().shouldNotSee(st.pages().mail().home().toolbar().topBtn())
                .clicksOn(st.pages().mail().msgView().closeMsgBtn())
                .shouldSee(
                    st.pages().mail().home().composeButton(),
                    st.pages().mail().home().stickyToolBar()
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Кнопки в списке писем в узкой ЛК")
    @TestCaseId("5328")
    public void shouldSeeStickyToobarCompactLC() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().layoutSwitchBtn())
                .turnTrue(st.pages().mail().home().layoutSwitchDropdown().compactLeftColumnSwitch());
            st.user().messagesSteps().scrollDownPage();
            st.user().defaultSteps().onMouseHover(st.pages().mail().home().composeButton());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Залипающие кнопки в композе в узкой ЛК")
    @TestCaseId("5406")
    public void shouldSeeStickyToobarCompose() {
        stepsProd.user().leftColumnSteps().setLeftColumnSize(LEFT_PANEL_COMPACT_SIZE);
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().composeButton());
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Залипающие кнопки в компактном меню")
    @TestCaseId("5330")
    public void shouldSeeStickyToobarCompactMenu() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, STATUS_ON)
        );
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().scrollDownPage();
            st.user().defaultSteps().shouldSee(st.pages().mail().home().composeButton());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Залипающие кнопки в контактах")
    @TestCaseId("5332")
    public void shouldSeeStickyToobarContacts() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().toolbar())
            .executesJavaScript(SCROLL_PAGE_SCRIPT)
            .shouldSee(
                st.pages().mail().home().stickyToolBar(),
                st.pages().mail().abook().composeButton()
            );

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Залипающие кнопки в 3 пейн")
    @TestCaseId("5334")
    public void shouldSeeStickyToobar3pane() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-vertical",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .waitInSeconds(2) // высота левой колонки пересчитывается уже после загрузки, надо этого дождаться
                .executesJavaScript(SCRIPT_FOR_SCROLLDOWN_LEFT_COLUMN)
                .shouldSee(
                    st.pages().mail().home().leftPanelFooterLineBlock().languageSwitch3pane(),
                    st.pages().mail().home().composeButton()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ресайз ЛК с залипающими кнопками")
    @TestCaseId("5335")
    @DataProvider({"260", "200", "150", "60"})
    public void shouldSeeStickyButtonsResize(String size) {
        stepsProd.user().leftColumnSteps().setLeftColumnSize(size);
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().scrollDownPage();
            st.user().defaultSteps().shouldSee(
                st.pages().mail().home().composeButton(),
                st.pages().mail().home().toolbar().topBtn(),
                st.pages().mail().home().stickyToolBar()
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

}
