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
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_SENDER;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Настройки - Красивый адрес для премиум пользователя")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.DOMAIN_SETTINGS)
public class SettingsPaidUserWithDomainTest {

    public static final String CREDS = "DomainSettingsPaidTest";
    public static final String CREDS2 = "DomainSettingsDisabledDomainTest";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().names(CREDS, CREDS2));
    private AccLockRule lock = rules.getLock();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Верстка попапа отключения домена")
    @TestCaseId("6054")
    public void shouldSeeDisableDomainPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().domainSettingsPage().actionButtons().get(1))
                .shouldSee(st.pages().mail().home().domainDisablePopupHeader());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_DOMAIN).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка страницы подключенного домена")
    @TestCaseId("6094")
    public void shouldSeeEnabledDomainStatus() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().domainSettingsPage().domainStatus());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_DOMAIN).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка страницы отключенного домена")
    @TestCaseId("6055")
    public void shouldSeeDisabledDomainStatus() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSeeThatElementHasText(
                st.pages().mail().domainSettingsPage().domainStatus(),
                "Отключен. Подключить заново можно с 2 октября 2022 года."
            );

        parallelRun.withActions(actions).withUrlPath(SETTINGS_DOMAIN).withAcc(lock.acc(CREDS2)).run();
    }

    @Test
    @Title("Нет красивого домена в списке алиасов, если домен отключен")
    @TestCaseId("6055")
    public void shouldNotSeeDomainAlias() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSeeElementsCount(
                st.pages().mail().settingsSender().blockSetupSender().blockAliases().logins(),
                2
            );

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.acc(CREDS2)).run();
    }
}
