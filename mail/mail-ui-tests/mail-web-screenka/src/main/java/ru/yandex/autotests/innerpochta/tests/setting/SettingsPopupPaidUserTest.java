package ru.yandex.autotests.innerpochta.tests.setting;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
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

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Настройки - Попап")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SETTINGS)
@RunWith(DataProviderRunner.class)
public class SettingsPopupPaidUserTest {

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

    @Test
    @Title("Должны увидеть счетчик неразобранных рассылок в попапе рассылок")
    @TestCaseId("6345")
    public void shouldSeeNewSubsInSettingsPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().settingsMenu())
                .shouldSee(st.pages().mail().home().mainSettingsPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("[Новый попап] Должны увидеть счетчик неразобранных рассылок в попапе рассылок")
    @TestCaseId("6345")
    public void shouldSeeNewSubsInSettingsPopupNew() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().settingsMenu())
                .shouldSee(st.pages().mail().home().mainSettingsPopupNew().subsPromoSettings());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
