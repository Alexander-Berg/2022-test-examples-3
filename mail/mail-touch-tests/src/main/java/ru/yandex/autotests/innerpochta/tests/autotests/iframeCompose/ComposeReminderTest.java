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

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на попап напоминаний в композе")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.REMINDER)
public class ComposeReminderTest {

    private static final String SUBJECT = "Письмо успешно доставлено";

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
    @Title("Должны закрыть попап напоминаний")
    @TestCaseId("1518")
    public void shouldCloseReminderPopup() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().header().reminders())
            .shouldSee(steps.pages().touch().composeIframe().remindersPopup())
            .clicksOn(steps.pages().touch().composeIframe().remindersPopup().closeBtn())
            .shouldNotSee(steps.pages().touch().composeIframe().remindersPopup());
    }

    @Test
    @Title("Должны получить письмо о доставке письма")
    @TestCaseId("1411")
    public void shouldGetReminderAboutDelivery() {
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().header().reminders())
            .clicksOn(steps.pages().touch().composeIframe().remindersPopup().checkbox().get(1))
            .clicksOn(steps.pages().touch().composeIframe().remindersPopup().closeBtn())
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldBeOnUrlWith(INBOX_FOLDER)
            .refreshPage()
            .shouldSeeElementInList(steps.pages().touch().messageList().subjectList(), SUBJECT);
    }
}
