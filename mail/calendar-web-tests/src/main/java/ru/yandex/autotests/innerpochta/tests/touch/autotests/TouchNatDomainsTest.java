package ru.yandex.autotests.innerpochta.tests.touch.autotests;

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
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("[Тач] Авторизация в нац доменах")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.NAT_DOMAINS)
@RunWith(DataProviderRunner.class)
public class TouchNatDomainsTest {

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain();

    @DataProvider
    public static Object[][] testDomains() {
        return new Object[][]{
            {YandexDomain.COM},
            {YandexDomain.RU},
            {YandexDomain.UZ},
            {YandexDomain.KZ},
            {YandexDomain.BY},
        };
    }

    @Test
    @Title("Авторизация в нац доменах")
    @TestCaseId("1179")
    @UseDataProvider("testDomains")
    public void shouldAuthNatDomains(YandexDomain domain) {
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
        steps.user().defaultSteps().shouldBeOnUrl(containsString(domain.getDomain()))
            .shouldSee(steps.pages().cal().touchHome().grid());
    }
}
