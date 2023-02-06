package ru.yandex.autotests.innerpochta.tests.screentests.grid;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на элементы в шапке")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class HeaderTest {

    public static final String WS_LOGO_CREDS = "WSCustomLogo";
    public static final String WS_CREDS = "WSFree";
    public static final String WS_NO_BURGER = "WSNoBurger";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use()
        .names(WS_LOGO_CREDS, WS_CREDS, WS_NO_BURGER));
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain();

    @Test
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("MAYA-2130")
    @Title("Сверяем список сервисов из выпадушки в шапке")
    @TestCaseId("668")
    @DataProvider({WS_CREDS, WS_LOGO_CREDS})
    public void shouldSeeServersInBurger(String account) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().calHeaderBlock().more())
            .shouldSee(st.pages().cal().home().calHeaderBlock().moreItem().get(0));

        parallelRun.withActions(actions).withAcc(lock.acc(account)).run();
    }

    @Test
    @Title("Не показываем бургер с выключенной настройкой коннектной шапки")
    @TestCaseId("1250")
    @DataProvider(WS_NO_BURGER)
    public void shouldNotSeeBurger(String account) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldNotSee(st.pages().cal().home().calHeaderBlock().burger());

        parallelRun.withActions(actions).withAcc(lock.acc(account)).run();
    }
}
