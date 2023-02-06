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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_DOMAIN;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Настройки - Красивый адрес")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.DOMAIN_SETTINGS)
public class SettingsDomainsTest {

    private static final String UNAVAILABLE_DOMAIN = "test";

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
    @Title("Верстка саджеста - свободный домен")
    @TestCaseId("6024")
    public void shouldSeeAvailableDomainStatusInSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().domainSettingsPage().availableStatus());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_DOMAIN).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка саджеста - домен занят")
    @TestCaseId("6024")
    public void shouldSeeUnavailableDomainStatusInSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().domainSettingsPage().availableStatus())
                .inputsTextInElementClearingThroughHotKeys(
                    st.pages().mail().domainSettingsPage().domainNameInput().get(1),
                    UNAVAILABLE_DOMAIN
                )
                .shouldSee(st.pages().mail().domainSettingsPage().unavailableStatus());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_DOMAIN).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка саджеста - подгрузка вариантов по кнопке Еще")
    @TestCaseId("6026")
    public void shouldSeeMoreSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().domainSettingsPage().enableButton())
                .clicksOn(st.pages().mail().domainSettingsPage().moreButton())
                .onMouseHover(st.pages().mail().domainSettingsPage().domainSuggestList().get(0));

        parallelRun.withActions(actions).withUrlPath(SETTINGS_DOMAIN).withAcc(lock.firstAcc()).run();
    }
}
