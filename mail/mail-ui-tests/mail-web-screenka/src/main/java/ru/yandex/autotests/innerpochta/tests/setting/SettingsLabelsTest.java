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
import ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_FOLDERS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule.addMessageIfNeed;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Настройки - Папки и метки")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class SettingsLabelsTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AddMessageIfNeedRule addMsg = addMessageIfNeed(() -> stepsProd.user(), () -> lock.firstAcc());
    private AddLabelIfNeedRule addLbl = addLabelIfNeed(() -> stepsProd.user());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(addLbl)
        .around(addMsg);

    @Before
    public void setUp() {
        stepsProd.user().apiLabelsSteps().markWithLabel(addMsg.getFirstMessage(), addLbl.getFirstLabel());
    }

    @Test
    @Title("Проверяем попап создания метки")
    @TestCaseId("2346")
    public void shouldSeeNewLabelPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnCreateNewLabel();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsFoldersAndLabels().openFilterLink())
                .shouldSee(st.pages().mail().settingsFoldersAndLabels().simpleFilter());
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап изменения метки")
    @TestCaseId("2347")
    public void shouldSeeChangeLabelPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnLabel();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsFoldersAndLabels().setupBlock()
                .labels().changeLabel());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап удаления метки")
    @TestCaseId("2348")
    public void shouldSeeDeleteLabelPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnLabel();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsFoldersAndLabels().setupBlock()
                .labels().deleteLabel());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап создания правила для метки")
    @TestCaseId("2349")
    public void shouldSeeFilterForLabelPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnLabel();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsFoldersAndLabels().setupBlock()
                .labels().createFilterForLabel());
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FOLDERS).withAcc(lock.firstAcc()).run();
    }
}