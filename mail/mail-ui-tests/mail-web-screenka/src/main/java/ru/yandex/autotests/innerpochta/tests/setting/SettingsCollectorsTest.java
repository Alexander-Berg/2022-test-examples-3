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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_COLLECTORS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASS_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.SERVER_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author yaroslavna
 **/
@Aqua.Test
@Title("Настройки - Сбор почты с других ящиков")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.COLLECTORS)
public class SettingsCollectorsTest {

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
        stepsProd.user().apiCollectorSteps().createNewCollector(MAIL_COLLECTOR, PASS_COLLECTOR, SERVER_COLLECTOR);
    }

    @Test
    @Title("Разворачиваем сборщик")
    @TestCaseId("2655")
    public void shouldSeeCollectorsInfo() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().settingsCollectors().blockCollector().showCollectorInfo());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_COLLECTORS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть попап удаления сборщика")
    @TestCaseId("2339")
    public void shouldSeeCollectorsRemovePopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().settingsCollectors().blockCollector().showCollectorInfo())
            .clicksOn(st.pages().mail().settingsCollectors().blockCollector().deleteMailboxBtn())
            .shouldSee(st.pages().mail().settingsCommon().popup());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_COLLECTORS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка страницы «Сбор писем» со сборщиками")
    @TestCaseId("2876")
    public void shouldSeeColectors() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().mail().settingsCollectors().blockMain().blockConnected().collectors());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_COLLECTORS).withAcc(lock.firstAcc()).run();
    }
}
