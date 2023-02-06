package ru.yandex.autotests.innerpochta.tests.differentModes;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
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

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDD_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Компактный режим для пользователя WS")
@Features(FeaturesConst.COMPACT_MODE)
@Tag(FeaturesConst.COMPACT_MODE)
@Stories(FeaturesConst.WORKSPACE)
public class CompactModeWSTest {

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".mail-CollectorsList-Item"),
        cssSelector(".mail-App-Footer-Group_journal")
    );

    private ScreenRulesManager rules = screenRulesManager();
    private AccLockRule lock = rules.getLock().useTusAccount(PDD_USER_TAG);
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();

    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORE_THIS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Включаем компактную шапку", of(LIZA_MINIFIED_HEADER, STATUS_ON));
    }

    @Test
    @Title("Включаем компактное меню для пользователя WS")
    @TestCaseId("3283")
    public void shouldNotSeeHeader() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().layoutSwitchBtn())
                .turnTrue(st.pages().mail().home().layoutSwitchDropdown().compactHeaderSwitch())
                .shouldNotSee(
                    st.pages().mail().home().mail360HeaderBlock().moreServices(),
                    st.pages().mail().home().layoutSwitchDropdown()
                );
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Выключаем компактную шапку", of(LIZA_MINIFIED_HEADER, EMPTY_STR));
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку «Сервисы» для пользователя WS")
    @TestCaseId("3283")
    public void shouldOpenMoreServices() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().moreServices());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку залогина для пользователя WS")
    @TestCaseId("3283")
    public void shouldOpenUserMenuDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .shouldSee(st.pages().mail().home().userMenuDropdown());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
