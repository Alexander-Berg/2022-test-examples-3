package ru.yandex.autotests.innerpochta.tests.setting;

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
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.webcommon.rules.AccountsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_FOLDERS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule.addFolderIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule.addMessageIfNeed;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Настройки - Папки и метки")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class SettingsFoldersTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AddFolderIfNeedRule addFolder = addFolderIfNeed(() -> stepsProd.user());
    private AddMessageIfNeedRule addMsg = addMessageIfNeed(() -> stepsProd.user(), () -> lock.firstAcc());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static AccountsRule account = new AccountsRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(addFolder)
        .around(addMsg);

    @Before
    public void setUp() {
        stepsProd.user().apiMessagesSteps()
            .moveAllMessagesFromFolderToFolder(INBOX, addFolder.getFirstFolder().getName());
    }

    @Test
    @Title("Проверяем попап для создания папки")
    @TestCaseId("2340")
    public void shouldSeeCreateNewFolderPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnCreateNewFolder();
            st.user().defaultSteps().clicksOn(
                    st.pages().mail().settingsFoldersAndLabels().openFilterLink(),
                    st.pages().mail().settingsFoldersAndLabels().newFolderPopUp().folderName(),
                    st.pages().mail().settingsFoldersAndLabels().newFolderPopUp().putInFolder()
                )
                .shouldSee(st.pages().mail().settingsFoldersAndLabels().putInFolderSelect());
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап создания вложенной папки")
    @TestCaseId("2341")
    public void shouldSeeNestedFolderPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnFolder();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsFoldersAndLabels().setupBlock()
                .folders().createSubFolderButton());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап переименования папки")
    @TestCaseId("2342")
    public void shouldSeeRenameFolderPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnFolder();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsFoldersAndLabels().setupBlock()
                .folders().renameFolderButton());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап очистки папки")
    @TestCaseId("2343")
    public void shouldSeeCleanFolderPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnFolder();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsFoldersAndLabels().setupBlock()
                .folders().clearCustomFolder());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап создания правила для папки")
    @TestCaseId("2344")
    public void shouldSeeCreateFilterForFolderPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnFolder();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsFoldersAndLabels().setupBlock()
                .folders().createFilterButton());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап при удалении папки с правилом")
    @TestCaseId("2345")
    public void shouldSeeDeleteFolderWithFilterPopup() {
        stepsProd.user().apiFiltersSteps().deleteAllUserFilters()
            .createFilterForFolderOrLabel(
                Util.getRandomAddress(),
                Util.getRandomString(),
                FILTERS_ADD_PARAM_MOVE_FOLDER,
                addFolder.getFirstFolder().getFid(),
                FILTERS_ADD_PARAM_CLICKER_MOVE,
                true
            );

        Consumer<InitStepsRule> actions = st ->
            st.user().settingsSteps().clicksOnFolder()
                .clicksOnDeleteFolder()
                .shouldSeeSettingsPopup();

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }
}
