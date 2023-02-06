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

import static com.google.common.collect.ImmutableBiMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL360_PAID;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.OPT_IN;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на опт-ин в попапе управления рассылками")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SUBSCRIPTIONS)
public class SubscriptionsOptInPaidScreenTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(MAIL360_PAID);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Должны увидеть страницу подтверждения отключения опт-ина")
    @TestCaseId("6306")
    public void shouldSeeConfirmOfTurningOptInOff() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsSubscriptions().tabNew())
                .clicksOn(st.pages().mail().settingsSubscriptions().optinDisableBtn())
                .shouldSee(st.pages().mail().settingsSubscriptions().optinDisableConfirm());
        };
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем опт-ин юзеру",
            of(OPT_IN, STATUS_ON)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка таба Новые в УР у юзера с подпиской")
    @TestCaseId("6308")
    public void shouldSeeEmptyUnsubscribeList() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsSubscriptions().tabNew());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть список писем для новой рассылки")
    @TestCaseId("6335")
    public void shouldSeeSubsMsgList() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsSubscriptions().tabNew())
                .onMouseHover(st.pages().mail().settingsSubscriptions().subscriptions().get(0))
                .clicksOn(st.pages().mail().settingsSubscriptions().moreBtn())
                .shouldSee(st.pages().mail().settingsSubscriptions().subsViewBtn());
        };
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем опт-ин юзеру",
            of(OPT_IN, STATUS_ON)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
