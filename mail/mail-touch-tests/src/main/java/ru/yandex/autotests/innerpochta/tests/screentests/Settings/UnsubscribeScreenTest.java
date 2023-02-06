package ru.yandex.autotests.innerpochta.tests.screentests.Settings;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveOldMessagesRule.removeOldMessagesRule;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на iframe рассылок")
@Features(FeaturesConst.SUBSCRIPTIONS)
@Stories(FeaturesConst.GENERAL)
public class UnsubscribeScreenTest {

    public static final String CREDS = "UnsubscribeScreenTest";
    private static final String USER_WITHOUT_SUBSCRIPTIONS = "LoginTest";
    private static final String SUBSCRIPTION_LAMODA = "[{\"displayName\": \"Lamoda\", \"messageType\": 13, \"email\": " +
        "\"newsletter@info.lamoda.ru\", \"folderId\": \"3\"}]";
    private static final String SUBSCRIPTION_MARKET = "[{\"displayName\": \"Яндекс.Маркет\", \"messageType\": 13, " +
        "\"email\": \"mailer@market.yandex.ru\", \"folderId\": \"3\"}]";
    private static final String GENERAL = "general";
    private static final String OLDER_THAN_DAYS = "14";

    private TouchScreenRulesManager rules = touchScreenRulesManager()
        .withLock(AccLockRule.use().names(CREDS, USER_WITHOUT_SUBSCRIPTIONS));
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule accLock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(removeOldMessagesRule(stepsProd.user(), INBOX, OLDER_THAN_DAYS));

    @Before
    public void prepare() {
        stepsProd.user().apiFiltersSteps().deleteAllUnsubscribeFilters();
        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(TRASH, INBOX);
    }

    @Test
    @Title("Проверяем верстку таба с активными рассылками")
    @TestCaseId("970")
    public void shouldSeeTabWithActiveSubs() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps().shouldSee(steps.pages().touch().unsubscribe().subsCheckboxes());
        };
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Проверяем верстку таба со скрытыми рассылками")
    @TestCaseId("970")
    public void shouldSeeTabWithInactiveSubs() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps().clicksOn(steps.pages().touch().unsubscribe().tabHidden())
                .shouldSee(steps.pages().touch().unsubscribe().subsCheckboxes());
        };
        stepsProd.user().apiFiltersSteps().createUnsubscribeFilters(SUBSCRIPTION_LAMODA, SUBSCRIPTION_MARKET);
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны сохранять состояние между табами")
    @TestCaseId("929")
    public void shouldSaveStateBetweenTabs() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps()
                .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
                .clicksOn(steps.pages().touch().unsubscribe().tabHidden())
                .clicksOn(steps.pages().touch().unsubscribe().tabActive())
                .shouldSee(
                    steps.pages().touch().unsubscribe().subsCheckedCheckboxes().get(0),
                    steps.pages().touch().unsubscribe().subsCheckedCheckboxes().get(1)
                );
        };
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны увидеть пустой список рассылок")
    @TestCaseId("937")
    public void shouldSeeEmptySubsList() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps().shouldSee(steps.pages().touch().unsubscribe().emptySubsList());
        };
        parallelRun.withAcc(accLock.acc(USER_WITHOUT_SUBSCRIPTIONS)).withActions(actions).run();
    }

    @Test
    @Title("Должны сбросить состояние рассылок при выходе из них")
    @TestCaseId("933")
    public void shouldResetSubsStateAfterExit() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps()
                .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
                .clicksOn(
                    steps.pages().touch().unsubscribe().tabHidden(),
                    steps.pages().touch().unsubscribe().closeSubs(),
                    steps.pages().touch().settings().subsSettingItem()
                )
                .switchTo(IFRAME_SUBS)
                .shouldBeDeselected(steps.pages().touch().unsubscribe().subsCheckboxes().get(0));
        };
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны увидеть список писем от рассылки с непрочитанным письмом")
    @TestCaseId("922")
    public void shouldSeeUnreadMsgFromSubs() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps()
                .clicksOn(steps.pages().touch().unsubscribe().tabHidden())
                .clicksOn(steps.pages().touch().unsubscribe().subscriptions().waitUntil(not(empty())).get(0))
                .shouldSee(steps.pages().touch().unsubscribe().unreadToggler());
        };
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны увидеть попап подтверждения в просмотре активной рассылки")
    @TestCaseId("924")
    public void shouldSeeConfirmPopupInSubsView() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps().clicksOn(
                steps.pages().touch().unsubscribe().subscriptions().waitUntil(not(empty())).get(0),
                steps.pages().touch().unsubscribe().subsViewBtn()
            )
                .shouldSee(steps.pages().touch().unsubscribe().confirmPopup());
        };
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны увидеть попап подтверждения в списке активных рассылок")
    @TestCaseId("924")
    public void shouldSeeConfirmPopupInSubsList() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps()
                .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
                .clicksOn(steps.pages().touch().unsubscribe().subsListBtn())
                .shouldSee(steps.pages().touch().unsubscribe().confirmPopup());
        };
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны увидеть попап подтверждения в просмотре скрытой рассылки")
    @TestCaseId("924")
    public void shouldSeeConfirmPopupInHideSubsView() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps().clicksOn(steps.pages().touch().unsubscribe().tabHidden())
                .clicksOn(
                    steps.pages().touch().unsubscribe().subscriptions().waitUntil(not(empty())).get(0),
                    steps.pages().touch().unsubscribe().subsViewBtn()
                )
                .shouldSee(steps.pages().touch().unsubscribe().confirmPopup());
        };
        stepsProd.user().apiFiltersSteps().createUnsubscribeFilters(SUBSCRIPTION_LAMODA, SUBSCRIPTION_MARKET);
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны увидеть попап подтверждения в списке скрытых рассылок")
    @TestCaseId("924")
    public void shouldSeeConfirmPopupInHideSubsList() {
        Consumer<InitStepsRule> actions = steps -> {
            switchToFrame(steps);
            steps.user().defaultSteps()
                .clicksOn(steps.pages().touch().unsubscribe().tabHidden())
                .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
                .clicksOn(steps.pages().touch().unsubscribe().subsListBtn())
                .shouldSee(steps.pages().touch().unsubscribe().confirmPopup());
        };
        stepsProd.user().apiFiltersSteps().createUnsubscribeFilters(SUBSCRIPTION_LAMODA, SUBSCRIPTION_MARKET);
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Step("Открываем настройки и переходим в управление рассылками")
    private void switchToFrame(InitStepsRule st) {
        st.user().defaultSteps().opensCurrentUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .clicksOn(st.pages().touch().settings().subsSettingItem())
            .switchTo(IFRAME_SUBS);
    }
}
