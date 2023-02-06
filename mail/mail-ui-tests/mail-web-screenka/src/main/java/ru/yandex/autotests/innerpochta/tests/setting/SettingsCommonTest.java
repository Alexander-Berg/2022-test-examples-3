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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_CLIENT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_SECURITY;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TODO;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_AREAS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.ENABLE_POP;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_ENABLE_IMAP;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Настройки - Общие тесты")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.GENERAL)
public class SettingsCommonTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED_AREAS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Проверяем главную страницу настроек")
    @TestCaseId("2480")
    public void shouldSeeCommonSettingsPage() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().settingsCommon().blockSettingsNav());

        parallelRun.withActions(actions).withUrlPath(SETTINGS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Показ преимуществ IMAP")
    @TestCaseId("2481")
    public void shouldSeeImapAdvantages() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Выключаем IMAP", of(SETTINGS_PARAM_ENABLE_IMAP, FALSE));
        Consumer<InitStepsRule> actions = st -> st.user().settingsSteps().clicksOnShowImapAdvantages();

        parallelRun.withActions(actions).withUrlPath(SETTINGS_CLIENT).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Показ доп.настроек для POP3")
    @TestCaseId("2482")
    public void shouldSeePop3Settings() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams("Включаем POP3", of(ENABLE_POP, TRUE));
        Consumer<InitStepsRule> actions = st -> st.user().settingsSteps().shouldSeePop3Settings();

        parallelRun.withActions(actions).withUrlPath(SETTINGS_CLIENT).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должен появиться попап сохранения изменений тудушки")
    @TestCaseId("2662")
    public void shouldSeeSaveToDoChangesPopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().settingsCommon().setupTodo().showTodoCheckbox())
            .clicksOn(st.pages().mail().home().mail360HeaderBlock().serviceIcons().get(0))
            .shouldSee(st.pages().mail().settingsCommon().popup());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_TODO).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем попап справки про номера телефонов")
    @TestCaseId("2640")
    public void shouldSeePhoneInfo() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().settingsSecurity().blockSecurity().toggleHintAboutPhoneNumbers())
            .shouldSee(st.pages().mail().settingsCommon().popup());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SECURITY).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Раскрываем выпадушку языков в левой колонке")
    @TestCaseId("2641")
    public void shouldSeeLangDropdown() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().settingsCommon().blockSettingsNav().selectLang())
            .shouldSee(st.pages().mail().settingsCommon().langTimeDropdown());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SECURITY).withAcc(lock.firstAcc()).run();
    }
}
