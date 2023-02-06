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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.LABEL;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SPAM_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.pages.ComposeIframePage.IFRAME_COMPOSE;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на возвраты из композа")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.REDIRECTS)
public class RedirectFromCompose {

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
    @Title("Вернуться в список писем после отправки письма из закреплённых")
    @TestCaseId("1048")
    public void shouldBackToMsgListAfterSent() {
        Message msg = steps.user().apiMessagesSteps()
            .sendMailWithNoSave(accLock.firstAcc(), getRandomString(), "");
        steps.user().apiLabelsSteps().pinLetter(msg);
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().pinnedLettersToolbar())
            .clicksOn(steps.pages().touch().messageList().headerBlock().compose())
            .shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldBeOnUrlWith(LABEL)
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldBeOnUrlWith(INBOX_FOLDER);
    }

    @Test
    @Title("Возвращаемся туда, откуда пришли, после выхода из композа")
    @TestCaseId("1055")
    public void shouldOpenPrevFolderAfterCloseComposeIframe() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SPAM_FOLDER))
            .clicksOn(steps.pages().touch().messageList().headerBlock().compose())
            .shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .clicksOn(steps.pages().touch().composeIframe().header().closeBtn())
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg())
            .shouldBeOnUrl(containsString(FOLDER_ID.makeTouchUrlPart(SPAM_FOLDER)));
    }

    @Test
    @Title("Возвращаемся туда, откуда пришли, после отправки письма")
    @TestCaseId("1054")
    public void shouldOpenPrevFolderAfterSend() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SPAM_FOLDER))
            .clicksOn(steps.pages().touch().messageList().headerBlock().compose())
            .shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg())
            .shouldBeOnUrl(containsString(FOLDER_ID.makeTouchUrlPart(SPAM_FOLDER)));
    }
}
