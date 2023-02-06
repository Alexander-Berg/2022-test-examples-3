package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
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
import ru.yandex.autotests.innerpochta.steps.ImapSteps;
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
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_SCROLL_3PANE_MESSAGELIST_DOWN;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCROLL_PAGE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAMP_THEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Список писем - выбрать все письма в папке")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SelectAllMessagesPopupTest {

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
        ImapSteps imapConnection = stepsProd.user().imapSteps().connectByImap();
        for (int i = 0; i < 21; i++)
            imapConnection.addMessage(new MessageSpecBuilder().withDefaults().build());
        imapConnection.closeConnection();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Отключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
    }

    @DataProvider
    public static Object[][] data() {
        return new Object[][]{
            {LAYOUT_2PANE, SCROLL_PAGE_SCRIPT},
            {SETTINGS_LAYOUT_3PANE_HORIZONTAL, SCRIPT_SCROLL_3PANE_MESSAGELIST_DOWN},
            {LAYOUT_3PANE_VERTICAL, SCRIPT_SCROLL_3PANE_MESSAGELIST_DOWN},
        };
    }

    @Test
    @Title("Должны видеть плашку «Выбрать все письма»")
    @TestCaseId("4065")
    @UseDataProvider("data")
    public void shouldSeeSelectAllMessagesPopup(String layout, String script) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().selectsAllDisplayedMessagesInFolder();
            st.user().defaultSteps().shouldSee(st.pages().mail().home().selectAllMessagesPopup())
                .executesJavaScript(script)
                .shouldSee(st.pages().mail().home().selectAllMessagesPopup());
        };

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Меняем лэйаут",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Функциональные нотификации в темной теме")
    @TestCaseId("4641")
    public void shouldSeeNotifyInDarkTheme() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().shouldSeeMessagesPresent();
            st.user().defaultSteps().refreshPage();
            st.user().messagesSteps().selectsAllDisplayedMessagesInFolder();
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().deleteButton())
                .shouldSee(st.user().pages().HomePage().notificationDark());
        };

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем темную тему",
            of(COLOR_SCHEME, LAMP_THEME)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Функциональные нотификации в цветной теме")
    @TestCaseId("4641")
    public void shouldSeeNotifyInColorfulTheme() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().shouldSeeMessagesPresent();
            st.user().defaultSteps().refreshPage();
            st.user().messagesSteps().selectsAllDisplayedMessagesInFolder();
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().deleteButton())
                .shouldSee(st.user().pages().HomePage().notificationColourful());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
