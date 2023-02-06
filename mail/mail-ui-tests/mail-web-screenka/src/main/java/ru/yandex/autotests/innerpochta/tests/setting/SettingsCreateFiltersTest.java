package ru.yandex.autotests.innerpochta.tests.setting;

import io.qameta.allure.junit4.Tag;
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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_FILTERS_CREATE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Настройки - Создание фильтра")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FILTERS)
public class SettingsCreateFiltersTest {

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

    @Test
    @Title("Открываем выпадушку «Применять ко всем письмам»")
    @TestCaseId("2632")
    public void shouldSeeLetterTypeConditionDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().createFilters()
                    .setupFiltersCreate().blockApplyConditionFor().letterTypeConditionDropdown())
                .shouldSee(st.pages().mail().settingsCommon().selectConditionDropdown());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS_CREATE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку «C вложениями и без вложений»")
    @TestCaseId("2633")
    public void shouldSeeWithAttachConditionDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().createFilters().setupFiltersCreate()
                    .blockApplyConditionFor().withAttachConditionDropdown())
                .shouldSee(st.pages().mail().settingsCommon().selectConditionDropdown());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS_CREATE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем дропдаун «От кого»")
    @TestCaseId("2634")
    public void shouldSeeFromDropdown() {
        Consumer<InitStepsRule> actions = st -> st.user().filtersSteps().shouldOpenFromDropdown(0);

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS_CREATE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбираем в дропдауне «От кого» пункт «Заголовок»")
    @TestCaseId("2635")
    public void shouldSeeAddHeaderPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().filtersSteps().shouldOpenFromDropdown(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsCommon()
                .selectConditionDropdown().conditionsList().get(7));
            st.user().settingsSteps().shouldSeeSettingsPopup();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS_CREATE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем дропдаун «Совпадает с»")
    @TestCaseId("2636")
    public void shouldSeeSecondConditionDropdown() {
        Consumer<InitStepsRule> actions = st -> st.user().filtersSteps().shouldOpenMatchesDropdown(0);

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS_CREATE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем дропдаун папок")
    @TestCaseId("2637")
    public void shouldSeeFoldersDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().createFilters().setupFiltersCreate()
                    .blockSelectAction().selectFolderDropdown())
                .shouldSee(st.pages().mail().settingsCommon().selectConditionDropdown());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS_CREATE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем дропдаун меток")
    @TestCaseId("2638")
    public void shouldSeeLabelsDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().turnTrue(st.pages().mail().createFilters().setupFiltersCreate()
                    .blockSelectAction().markAsCheckBox())
                .clicksOn(st.pages().mail().createFilters().setupFiltersCreate()
                    .blockSelectAction().selectLabelDropdown())
                .shouldSee(st.pages().mail().settingsCommon().selectConditionDropdown());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS_CREATE).withAcc(lock.firstAcc()).run();
    }
}
