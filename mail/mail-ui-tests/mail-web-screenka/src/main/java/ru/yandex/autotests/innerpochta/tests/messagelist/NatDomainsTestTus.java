package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_SENDER;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на нацдомены")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.NAT_DOMAINS)
@RunWith(DataProviderRunner.class)
public class NatDomainsTestTus {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @DataProvider
    public static Object[][] data() {
        return new Object[][]{
            {YandexDomain.FR},
            {YandexDomain.EE}
        };
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Test
    @Title("Нажимаем на «Еще» в шапке")
    @UseDataProvider("data")
    @TestCaseId("2740")
    public void shouldOpenMoreServicesDropdown(YandexDomain domain) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
            st.user().defaultSteps().shouldSee(st.pages().mail().home().mail360HeaderBlock())
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().moreServices())
                .shouldSee(st.pages().mail().home().allServices360Popup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должна быть выпадушка доменов")
    @UseDataProvider("data")
    @TestCaseId("2334")
    public void shouldSeeDomainDropdown(YandexDomain domain) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
            st.user().defaultSteps()
                .opensFragment(SETTINGS_SENDER)
                .shouldSee(st.pages().mail().settingsSender().blockSetupSender())
                .clicksOn(
                    st.pages().mail().settingsSender().blockSetupSender().blockAliases().domainsList().get(0)
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
