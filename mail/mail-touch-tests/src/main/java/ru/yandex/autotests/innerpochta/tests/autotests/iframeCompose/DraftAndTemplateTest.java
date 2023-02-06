package ru.yandex.autotests.innerpochta.tests.autotests.iframeCompose;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE_MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SENT_FOLDER;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TEMPLATE;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на черновики и шаблоны")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.TEMPLATES)
public class DraftAndTemplateTest {

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
        steps.user().apiMessagesSteps().createTemplateMessage(accLock.firstAcc());
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().touchSteps().openComposeViaUrl();
    }

    @Test
    @Title("Письмо должно сохраниться в черновики")
    @TestCaseId("873")
    public void shouldSaveDraft() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().header().sendBtn())
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().header().closeBtn())
            .shouldSee(steps.pages().touch().messageList().headerBlock().compose());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(DRAFT, 1);
    }

    @Test
    @Title("Шаблон не должен удаляться после отправки")
    @TestCaseId("647")
    public void shouldNotDeleteTemplateAfterSend() {
        String msgMid = steps.user().apiMessagesSteps().getAllMessagesInFolder(TEMPLATE).get(0).getMid();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(COMPOSE_MSG_FRAGMENT.makeTouchUrlPart(msgMid));
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().header().sendBtn())
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(TEMPLATE, 1);
    }

    @Test
    @Title("Должны отправить изменный шаблон")
    @TestCaseId("1082")
    public void shouldSendEditTemplate() {
        String sbj = getRandomString();
        steps.user().apiMessagesSteps().createTemplateMessage(accLock.firstAcc());
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(
                FOLDER_ID.makeTouchUrlPart(steps.user().apiFoldersSteps().getFolderBySymbol(TEMPLATE).getFid()))
            .clicksOn(steps.pages().touch().messageList().messageBlock());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .inputsTextInElement(steps.pages().touch().composeIframe().inputSubject(), sbj)
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldSee(steps.pages().touch().messageList().headerBlock().compose())
            .opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER))
            .shouldSeeThatElementHasText(steps.pages().touch().messageList().messageBlock().subject(), sbj);
    }

    @Test
    @Title("Перейти в шаблон из поиска")
    @TestCaseId("1085")
    public void shouldSeeTemplateFromSearchResult() {
        String sbj = steps.user().apiMessagesSteps().createTemplateMessage(accLock.firstAcc());
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH.makeTouchUrlPart())
            .clicksAndInputsText(steps.pages().touch().search().header().input(), sbj)
            .clicksOn(steps.pages().touch().search().header().find())
            .clicksOn(steps.pages().touch().search().messages().get(0))
            .clicksOn(steps.pages().touch().messageView().toolbar().openDraft())
            .shouldBeOnUrlWith(QuickFragments.COMPOSE);
    }

    @Test
    @Title("Шаблон не должен меняться после отправки")
    @TestCaseId("1127")
    public void shouldNotChangeTemplateAfterSend() {
        String msgMid = steps.user().apiMessagesSteps().getAllMessagesInFolder(TEMPLATE).get(0).getMid();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(COMPOSE_MSG_FRAGMENT.makeTouchUrlPart(msgMid));
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().header().sendBtn());
        String text = steps.pages().touch().composeIframe().inputBody().getText();
        steps.user().defaultSteps().clicksAndInputsText(steps.pages().touch().composeIframe().inputBody(), getRandomString())
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .opensDefaultUrlWithPostFix(COMPOSE_MSG_FRAGMENT.makeTouchUrlPart(msgMid));
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldSeeThatElementTextEquals(steps.pages().touch().composeIframe().inputBody(), text);
    }

    @Test
    @Title("Должен сохраниться черновик при неинтерфейсном выходе из композа")
    @TestCaseId("791")
    public void shouldSaveDraftAfterBack() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().header().sendBtn())
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL);
        steps.user().hotkeySteps().pressHotKeys(steps.pages().touch().composeIframe().inputTo(), Keys.ENTER);
        steps.getDriver().navigate().back();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().headerBlock().compose());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(DRAFT, 1);
    }
}
