package ru.yandex.autotests.innerpochta.tests.screentests.IframeCompose;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.FORWARD;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY_ALL;
import static ru.yandex.autotests.innerpochta.util.MessageHTMLBodyBuilder.messageHTMLBodyBuilder;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на инлайн аттачи в композе")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.GENERAL)
public class InlineAttachmentsInComposeScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            acc.firstAcc().getSelfEmail(),
            Utils.getRandomName(),
            messageHTMLBodyBuilder(stepsProd.user()).makeBodyWithInlineAttachAndText()
        );
        stepsProd.user().apiFoldersSteps().purgeFolder(stepsProd.user().apiFoldersSteps().getFolderBySymbol(SENT));
    }

    @Test
    @Title("Инлайн-аттачи отображаются в композе при пересылке письма")
    @TestCaseId("653")
    public void shouldSeeInlineAttachInCompose() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps()
                .rightSwipe(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
            st.user().defaultSteps()
                .offsetClick(st.pages().touch().messageList().messages().get(0).swipeFirstBtn(), 11, 11)
                .shouldSee(st.pages().touch().messageList().popup())
                .clicksOnElementWithText(st.pages().touch().messageView().btnsList(), FORWARD.btn());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().inputBody());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Инлайн-аттачи отображаются в композе при ответе на письмо")
    @TestCaseId("1080")
    public void shouldSeeInlineAttachInComposeForReply() {
        Consumer<InitStepsRule> act = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageList().messageBlock().subject())
                .clicksOn(st.pages().touch().messageView().moreBtn())
                .clicksOnElementWithText(st.pages().touch().messageView().btnsList(), REPLY_ALL.btn());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().inputBody());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }
}
