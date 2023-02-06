package ru.yandex.autotests.innerpochta.tests.autotests.Settings;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveOldMessagesRule.removeOldMessagesRule;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на отписки от рассылок")
@Features(FeaturesConst.SUBSCRIPTIONS)
@Stories(FeaturesConst.GENERAL)
public class UnsubscribeTest {

    private static final String SUBSCRIPTION_LAMODA = "[{\"displayName\": \"Lamoda\", \"messageType\": 13, \"email\": " +
        "\"newsletter@info.lamoda.ru\", \"folderId\": \"3\"}]";
    private static final String SUBSCRIPTION_MARKET = "[{\"displayName\": \"Яндекс.Маркет\", \"messageType\": 13, " +
    "\"email\": \"mailer@market.yandex.ru\", \"folderId\": \"3\"}]";
    private static final String GENERAL = "general";
    private static final String OLDER_THAN_DAYS = "14";

    private int subsCount;

    private TouchRulesManager rules = touchRulesManager();
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(removeOldMessagesRule(steps.user(), INBOX, OLDER_THAN_DAYS));

    @Before
    public void prepare() {
        steps.user().apiFiltersSteps().deleteAllUnsubscribeFilters();
        steps.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(TRASH, INBOX);
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .clicksOn(steps.pages().touch().settings().subsSettingItem())
            .switchTo(IFRAME_SUBS);
        subsCount = steps.pages().touch().unsubscribe().subscriptions().size();
    }

    @Test
    @Title("Должны закрыть попап рассылок из просмотра рассылки")
    @TestCaseId("961")
    public void shouldCloseSubsFromSubsView() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().touch().unsubscribe().subscriptions().waitUntil(not(empty())).get(0),
            steps.pages().touch().unsubscribe().closeSubs()
        )
            .shouldNotSee(steps.pages().touch().unsubscribe().closeSubs())
            .shouldSee(steps.pages().touch().settings().subsSettingItem());
    }

    @Test
    @Title("Должны закрыть попап рассылок из списка рассылок")
    @TestCaseId("931")
    public void shouldCloseSubsFromSubsList() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().unsubscribe().closeSubs())
            .shouldSee(steps.pages().touch().settings().subsSettingItem());
    }

    @Test
    @Title("Должны закрыть подтверждающий попап при отписке от одной рассылки")
    @TestCaseId("934")
    public void shouldCloseConfirmUnsubscribePopupInSubsView() {
        cancelActionWithOneSubsFromSubsView();
    }

    @Test
    @Title("Должны закрыть подтверждающий попап при активации одной рассылки")
    @TestCaseId("939")
    public void shouldCloseConfirmSubscribePopupInSubsView() {
        steps.user().apiFiltersSteps().createUnsubscribeFilters(SUBSCRIPTION_LAMODA, SUBSCRIPTION_MARKET);
        steps.user().defaultSteps().refreshPage()
            .switchTo(IFRAME_SUBS)
            .clicksOn(steps.pages().touch().unsubscribe().tabHidden());
        cancelActionWithOneSubsFromSubsView();
    }

    @Test
    @Title("Должны закрыть подтверждающий попап при отписке от нескольких рассылок")
    @TestCaseId("930")
    public void shouldCloseConfirmUnsubscribePopup() {
        cancelActionWithTwoSubsFromSubsView();
    }

    @Test
    @Title("Должны закрыть подтверждающий попап при активации нескольких рассылок")
    @TestCaseId("940")
    public void shouldCloseConfirmSubscribePopup() {
        steps.user().apiFiltersSteps().createUnsubscribeFilters(SUBSCRIPTION_LAMODA, SUBSCRIPTION_MARKET);
        steps.user().defaultSteps().refreshPage()
            .switchTo(IFRAME_SUBS)
            .clicksOn(steps.pages().touch().unsubscribe().tabHidden());
        cancelActionWithTwoSubsFromSubsView();
    }

    @Test
    @Title("Должны вернуться из просмотра рассылки в список рассылок")
    @TestCaseId("943")
    public void shouldBackFromSubsViewToSubsList() {
        steps.user().apiFiltersSteps().createUnsubscribeFilters(SUBSCRIPTION_LAMODA, SUBSCRIPTION_MARKET);
        steps.user().defaultSteps().refreshPage()
            .switchTo(IFRAME_SUBS)
            .clicksOn(steps.pages().touch().unsubscribe().tabHidden())
            .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
            .clicksOn(
                steps.pages().touch().unsubscribe().subscriptions().waitUntil(not(empty())).get(0),
                steps.pages().touch().unsubscribe().backFromSubsView()
            )
            .shouldSee(
                steps.pages().touch().unsubscribe().subsCheckedCheckboxes().get(0),
                steps.pages().touch().unsubscribe().subsCheckedCheckboxes().get(1)
            );
    }

    @Test
    @Title("Должны отписаться от нескольких рассылок без удаления писем")
    @TestCaseId("916")
    public void shouldHideSubs() {
        steps.user().defaultSteps()
            .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
            .clicksOn(steps.pages().touch().unsubscribe().subsListBtn());
        confirmUnsubscribe(2, false);
    }

    @Test
    @Title("Должны отписаться от нескольких рассылок с удалением писем")
    @TestCaseId("917")
    public void shouldHideSubsAndDeleteMsges() {
        steps.user().defaultSteps()
            .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
            .clicksOn(steps.pages().touch().unsubscribe().subsListBtn())
            .turnTrue(steps.pages().touch().unsubscribe().deleteMsgesCheckbox());
        confirmUnsubscribe(2, true);
    }

    @Test
    @Title("Должны отписаться от одной рассылки без удаления писем")
    @TestCaseId("918")
    public void shouldHideSubsFromSubsView() {
        steps.user().defaultSteps()
            .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
            .clicksOn(
                steps.pages().touch().unsubscribe().subscriptions().get(0),
                steps.pages().touch().unsubscribe().subsViewBtn()
            );
        confirmUnsubscribe(1, false);
    }

    @Test
    @Title("Должны отписаться от одной рассылки с удалением писем")
    @TestCaseId("919")
    public void shouldHideSubsAndDeleteMsgesFromSubsView() {
        steps.user().defaultSteps()
            .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
            .clicksOn(
                steps.pages().touch().unsubscribe().subscriptions().get(0),
                steps.pages().touch().unsubscribe().subsViewBtn()
            )
            .turnTrue(steps.pages().touch().unsubscribe().deleteMsgesCheckbox());
        confirmUnsubscribe(1, true);
    }

    @Test
    @Title("Должны активировать одну рассылку")
    @TestCaseId("938")
    public void shouldActivateSubs() {
        steps.user().apiFiltersSteps().createUnsubscribeFilters(SUBSCRIPTION_LAMODA, SUBSCRIPTION_MARKET);
        steps.user().defaultSteps().refreshPage()
            .switchTo(IFRAME_SUBS)
            .clicksOn(steps.pages().touch().unsubscribe().tabHidden())
            .clicksOn(
                steps.pages().touch().unsubscribe().subscriptions().waitUntil(not(empty())).get(0),
                steps.pages().touch().unsubscribe().subsViewBtn()
            );
        confirmSubscribe(1);
    }

    @Test
    @Title("Должны активировать несколько рассылок")
    @TestCaseId("932")
    public void shouldActivateSeveralSubs() {
        steps.user().apiFiltersSteps().createUnsubscribeFilters(SUBSCRIPTION_LAMODA, SUBSCRIPTION_MARKET);
        steps.user().defaultSteps().refreshPage()
            .switchTo(IFRAME_SUBS)
            .clicksOn(steps.pages().touch().unsubscribe().tabHidden())
            .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
            .clicksOn(steps.pages().touch().unsubscribe().subsListBtn());
        confirmSubscribe(2);
    }

    @Step("Открываем рассылку, нажимаем на кнопку, закрываем попап подтверждения, проверяем на каком экране находимся")
    private void cancelActionWithOneSubsFromSubsView() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().touch().unsubscribe().subscriptions().waitUntil(not(empty())).get(0),
            steps.pages().touch().unsubscribe().subsViewBtn()
        )
            .shouldSee(steps.pages().touch().unsubscribe().confirmPopup())
            .clicksOn(steps.pages().touch().unsubscribe().confirmPopupClose())
            .shouldNotSee(steps.pages().touch().unsubscribe().confirmPopup())
            .shouldSee(steps.pages().touch().unsubscribe().subsViewBtn());
    }

    @Step("Выделяем несколько рассылок, нажимаем на кнопку, закрываем попап подтверждения и проверяем выделение")
    private void cancelActionWithTwoSubsFromSubsView() {
        steps.user().defaultSteps()
            .turnTrue(steps.pages().touch().unsubscribe().subsCheckboxes().waitUntil(not(empty())).subList(0, 2))
            .clicksOn(steps.pages().touch().unsubscribe().subsListBtn())
            .shouldSee(steps.pages().touch().unsubscribe().confirmPopup())
            .clicksOn(steps.pages().touch().unsubscribe().confirmPopupClose())
            .shouldNotSee(steps.pages().touch().unsubscribe().confirmPopup())
            .shouldSee(
                steps.pages().touch().unsubscribe().subsCheckedCheckboxes().get(0),
                steps.pages().touch().unsubscribe().subsCheckedCheckboxes().get(1)
            );
    }

    @Step("Соглашаемся на всё при отписывании/активации рассылок, проверяем количество рассылок и писем")
    private void confirmUnsubscribe(int usedSubsCount, boolean flag) {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().unsubscribe().confirmPopup())
            .clicksOn(
                steps.pages().touch().unsubscribe().confirmPopupBtn(),
                steps.pages().touch().unsubscribe().successPopupBtn()
            )
            .shouldNotSee(steps.pages().touch().unsubscribe().confirmPopup())
            .shouldContainText(steps.pages().touch().unsubscribe().tabActive(), Integer.toString(usedSubsCount))
            .shouldSeeElementsCount(steps.pages().touch().unsubscribe().subscriptions(), subsCount - usedSubsCount);
        if (flag) {
            assertTrue(
                "Неверное количество писем в папке Удалённые",
                steps.user().apiMessagesSteps().getAllMessagesInFolder(TRASH).size() > 0
            );
        } else {
            assertEquals(
                "Неверное количество писем в папке Удалённые",
                0,
                steps.user().apiMessagesSteps().getAllMessagesInFolder(TRASH).size()
            );
        }
    }

    @Step("Соглашаемся на всё при отписывании/активации рассылок, проверяем количество рассылок и писем")
    private void confirmSubscribe(int usedSubsCount) {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().unsubscribe().confirmPopup())
            .clicksOn(
                steps.pages().touch().unsubscribe().confirmSubscribePopupBtn(),
                steps.pages().touch().unsubscribe().successSubscribePopupBtn()
            )
            .shouldNotSee(steps.pages().touch().unsubscribe().confirmPopup())
            .shouldContainText(steps.pages().touch().unsubscribe().tabActive(), Integer.toString(usedSubsCount))
            .shouldSeeElementsCount(steps.pages().touch().unsubscribe().subscriptions(), subsCount - usedSubsCount);
        assertEquals(
            "Неверное количество писем в папке Удалённые",
            0,
            steps.user().apiMessagesSteps().getAllMessagesInFolder(TRASH).size()
        );
    }
}
