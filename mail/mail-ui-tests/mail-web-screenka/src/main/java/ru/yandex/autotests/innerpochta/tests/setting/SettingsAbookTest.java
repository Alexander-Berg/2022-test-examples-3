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
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_ABOOK;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Настройки - Контакты")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SETTINGS_ABOOK)
public class SettingsAbookTest {

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
        stepsProd.user().apiAbookSteps().addNewAbookGroup(Utils.getRandomName());
    }

    @Test
    @Title("Проверяем попап загрузки контактов из файла")
    @TestCaseId("2470")
    public void shouldSeeImportContactsPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsContacts()
                .blockSetupAbook().importExportView().importBtn());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_ABOOK).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап загрузки контактов в файл")
    @TestCaseId("2473")
    public void shouldSeeExportContactsPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsContacts()
                .blockSetupAbook().importExportView().exportBtn());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_ABOOK).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап для создания группы")
    @TestCaseId("2471")
    public void shouldSeeCreateGroupPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsContacts()
                .blockSetupAbook().groupsManage().createGroupButton());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_ABOOK).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап для переименования группы")
    @TestCaseId("2472")
    public void shouldSeeRenameGroupPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsContacts()
                    .blockSetupAbook().groupsManage().createdGroups().get(0))
                .clicksOn(st.pages().mail().settingsContacts().blockSetupAbook()
                    .groupsManage().editGroupButton());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_ABOOK).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем появление кнопки сохранения")
    @TestCaseId("2474")
    public void shouldSeeSaveChangesBtn() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().deselects(st.pages().mail().settingsContacts()
                    .blockSetupAbook().importExportView().autoCollectContacts())
                .shouldSee(st.pages().mail().settingsContacts()
                    .blockSetupAbook().importExportView().saveChangeButton());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_ABOOK).withAcc(lock.firstAcc()).run();
    }
}
