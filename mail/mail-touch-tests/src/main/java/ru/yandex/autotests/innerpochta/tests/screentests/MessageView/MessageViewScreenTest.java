package ru.yandex.autotests.innerpochta.tests.screentests.MessageView;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.THREAD_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.DRAFT_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.Scrips.SCRIPT_FOR_SCROLLDOWN_THREAD;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY_ALL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MessageHTMLBodyBuilder.messageHTMLBodyBuilder;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Общие скриночные тесты на просмотр письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class MessageViewScreenTest {

    private Message msg;

    private static final String SUBJ_INLINE_ATTACH = "Инлайн аттач",
        SUBJ_LONG_MSG = "Long mail",
        MSG_TEXT = "I want you to translate this",
        LONG_TEXT = "1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private AddLabelIfNeedRule addLabel = addLabelIfNeed(() -> stepsTest.user());
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(addLabel);

    @Before
    public void prep() {
        msg = stepsTest.user().apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), MSG_TEXT, MSG_TEXT);
        stepsTest.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Разворачиваем детали письма")
    @TestCaseId("373")
    public void shouldSeeMsgDetails() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .shouldNotSee(st.pages().touch().messageView().msgLoaderInView())
                .clicksOn(st.pages().touch().messageView().toolbar())
                .clicksOn(st.pages().touch().messageView().yabbles().get(0))
                .shouldSee(st.pages().touch().messageView().unmarkLabelBtn());

        stepsProd.user().apiLabelsSteps().markWithLabel(msg, addLabel.getFirstLabel());
        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())).run();
    }

    @Test
    @Title("Разворачиваем цитирование")
    @TestCaseId("63")
    public void shouldSeeQuoteInMsg() {
        createMsgWithQuote();
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(0))
            .clicksOn(st.pages().touch().messageView().showQuoteLink());

        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("По клику в mailto должен открыться предзаполенный композ")
    @TestCaseId("148")
    public void shouldOpenComposeFromMailto() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().shouldSee(st.pages().touch().messageView().toolbar())
                .clicksOn(st.pages().touch().messageView().mailAddressInMail())
                .shouldBeOnUrlWith(COMPOSE);
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().expandComposeFields())
                .shouldSee(st.pages().touch().composeIframe().hideComposeFields());
        };
        Message msgWithMailAddress = stepsTest.user().apiMessagesSteps()
            .sendMailWithNoSave(acc.firstAcc(), getRandomName(), DEV_NULL_EMAIL);
        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msgWithMailAddress.getMid())).run();
    }

    @Test
    @Title("Должен быть отступ перед квикреплаем в длинном письме")
    @TestCaseId("560")
    public void shouldSeeGapBeforeQuickReply() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageList().messageBlock())
                .clicksOnElementWithText(st.pages().touch().messageList().subjectList(), SUBJ_LONG_MSG)
                .shouldNotSee(st.pages().touch().messageView().msgLoaderInView())
                .executesJavaScript(SCRIPT_FOR_SCROLLDOWN_THREAD)
                .shouldSee(st.pages().touch().messageView().quickReply());

        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(
            acc.firstAcc().getSelfEmail(),
            SUBJ_LONG_MSG,
            LONG_TEXT
        );
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Просмотр треда с непрочитанными письмами")
    @TestCaseId("96")
    public void shouldSeeThread() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().touch().messageView().threadCounter());

        Message thread = stepsTest.user().apiMessagesSteps().sendThread(acc.firstAcc(), getRandomName(), 2);
        stepsTest.user().apiMessagesSteps().markLetterRead(
            stepsTest.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0)
        );
        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(
                FOLDER_ID.makeTouchUrlPart("1") +
                    THREAD_ID.fragment(thread.getTid().replace("t", ""))
            ).run();
    }

    @Test
    @Title("Просмотр письма с инлайн аттачами")
    @TestCaseId("592")
    public void shouldSeeInlineAttachment() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().touch().messageList().messageBlock())
            .clicksOnElementWithText(st.pages().touch().messageList().subjectList(), SUBJ_INLINE_ATTACH)
            .shouldSee(st.pages().touch().messageView().ImgInMessage());

        stepsTest.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            acc.firstAcc().getSelfEmail(),
            SUBJ_INLINE_ATTACH,
            messageHTMLBodyBuilder(stepsTest.user()).makeBodyWithInlineAttachAndText()
        );
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("В развернутых деталях письма нет адресов из скрытой копии")
    @TestCaseId("453")
    public void shouldNotSeeBcc() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageView().toolbar())
                .shouldSeeElementsCount(st.pages().touch().messageView().msgDetailsFields(), 5);

        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())).run();
    }

    @Test
    @Title("Должны открыть письмо в списке непрочитанных")
    @TestCaseId("1057")
    public void shouldOpenMsgInUnreadLabel() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiMessagesSteps().markAllMsgUnRead();
            st.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(DRAFT_FOLDER))
                .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
                .clicksOn(st.pages().touch().sidebar().counter())
                .clicksOn(st.pages().touch().messageList().messageBlock())
                .shouldSee(st.pages().touch().messageView().toolbar());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).runSequentially();
    }

    @Test
    @Title("Должны просмотреть черновик в треде")
    @TestCaseId("1072")
    public void shouldSeeDraftInThread() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messageBlock())
                .clicksOn(st.pages().touch().messageView().toolbar())
                .shouldSee(st.pages().touch().messageView().draft());

        stepsProd.user().apiMessagesSteps().prepareDraftToThread("", MSG_TEXT, "");
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Step("Создаём письмо с цитированием")
    private void createMsgWithQuote() {
        stepsTest.user().loginSteps().forAcc(acc.firstAcc()).logins();
        stepsTest.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid()))
            .clicksOn(stepsTest.pages().touch().messageView().moreBtn())
            .clicksOnElementWithText(stepsTest.pages().touch().messageView().btnsList(), REPLY_ALL.btn());
        stepsTest.user().touchSteps().switchToComposeIframe();
        stepsTest.user().defaultSteps().appendTextInElement(stepsTest.pages().touch().composeIframe().inputBody(), Utils.getRandomName())
            .clicksOn(stepsTest.pages().touch().composeIframe().header().sendBtn())
            .shouldBeOnUrl(containsString(INBOX_FOLDER.makeTouchUrlPart()))
            .opensDefaultUrl();
    }
}
