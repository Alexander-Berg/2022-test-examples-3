package ru.yandex.autotests.innerpochta.tests.autotests.iframeCompose;

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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.OUTBOX_RU;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на попап отложенной отправки в композе")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ADDITIONAL)
public class ComposeDelayedSendingTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().touchSteps().openComposeViaUrl();
    }

    @Test
    @Title("Должны закрыть попап отложенной отправки")
    @TestCaseId("1514")
    public void shouldCloseDelayedSendingPopup() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().header().delayedSending())
            .shouldSee(steps.pages().touch().composeIframe().delayedSendingPopup())
            .clicksOn(steps.pages().touch().composeIframe().delayedSendingPopup().closeBtn())
            .shouldNotSee(steps.pages().touch().composeIframe().delayedSendingPopup());
    }

    @Test
    @Title("Должна появиться папка «Исходящие» после отложенной отправки письма")
    @TestCaseId("1424")
    public void shouldSeeOutboxInFolderListAfterDelayedSending() {
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().header().delayedSending())
            .clicksOn(steps.pages().touch().composeIframe().delayedSendingPopup().presets().get(0))
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldBeOnUrlWith(INBOX_FOLDER)
            .refreshPage()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldSeeElementInList(steps.pages().touch().sidebar().folderBlocks(), OUTBOX_RU);
    }

    @Test
    @Title("Должны сбросить настройки отложенной отправки")
    @TestCaseId("1513")
    public void shouldCLearDelayedSendingSettings() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().header().delayedSending())
            .clicksOn(steps.pages().touch().composeIframe().delayedSendingPopup().presets().get(0))
            .shouldSee(
                steps.pages().touch().composeIframe().header().turnedOnDelayedSending(),
                steps.pages().touch().composeIframe().header().delayedSendingBtn()
            )
            .clicksOn(steps.pages().touch().composeIframe().header().delayedSending())
            .clicksOn(steps.pages().touch().composeIframe().delayedSendingPopup().clearControl())
            .shouldNotSee(
                steps.pages().touch().composeIframe().header().turnedOnDelayedSending(),
                steps.pages().touch().composeIframe().header().delayedSendingBtn()
            );
    }
}
