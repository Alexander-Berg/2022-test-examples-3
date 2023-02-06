package ru.yandex.autotests.innerpochta.tests.screentests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MessageHTMLBodyBuilder.messageHTMLBodyBuilder;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на переводчик")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TRANSLATE)
public class TranslateScreenTest {

    private Message msg;

    private static final String MSG_TEXT = "I want you to translate this",
        ELVISH_LANGRAGE = "Эльфийский";

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
    public void prep() {
        msg = stepsTest.user().apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), MSG_TEXT, MSG_TEXT);
        stepsTest.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Должны перевести письмо")
    @TestCaseId("64")
    public void shouldTranslateMessage() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageView().translator().translateBtn())
                .shouldNotContainText(st.pages().touch().messageView().msgBody(), MSG_TEXT);

        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())).run();
    }

    @Test
    @Title("Должны перевести html-письмо")
    @TestCaseId("1098")
    public void shouldTranslateHtmlMsg() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messageBlock())
                .shouldContainText(st.pages().touch().messageView().msgBody(), MSG_TEXT)
                .clicksOn(st.pages().touch().messageView().translator().translateBtn())
                .shouldNotContainText(st.pages().touch().messageView().msgBody(), MSG_TEXT);

        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            acc.firstAcc().getSelfEmail(),
            Utils.getRandomName(),
            messageHTMLBodyBuilder(stepsProd.user()).makeBodyWithInlineAttachAndText(MSG_TEXT)
        );
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны увидеть переводчик в письме")
    @TestCaseId("1094")
    public void shouldSeeTranslator() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageView().translator().translateBtn());

        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())).run();
    }

    @Test
    @Title("Должны увидеть подтверждающий попап «Скрыть переводчик»")
    @TestCaseId("1107")
    public void shouldSeeHideTranslatorPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageView().translator().closeBtn())
                .shouldSee(st.pages().touch().messageView().translatorPopup());

        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())).run();
    }

    @Test
    @Title("Должны увидеть попап выбора языка, с которого переводим")
    @TestCaseId("1283")
    public void shouldSeeSourceLangsPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageView().translator().sourceLangBtn())
                .shouldSee(st.pages().touch().messageView().choiceLangList().get(0));

        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())).run();
    }

    @Test
    @Title("Должны увидеть попап выбора языка, на который переводим")
    @TestCaseId("1110")
    public void shouldSeeTranslateLangsPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageView().translator().translateLangBtn())
                .shouldSee(st.pages().touch().messageView().choiceLangList().get(0));

        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())).run();
    }

    @Test
    @Title("Должны увидеть ошибку во время перевода")
    @TestCaseId("1112")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-7418")
    public void shouldSeeTranslateError() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageView().translator().sourceLangBtn())
                .clicksOnElementWithText(st.pages().touch().messageView().choiceLangList(), ELVISH_LANGRAGE)
                .clicksOn(st.pages().touch().messageView().translator().translateBtn())
                .shouldSee(st.pages().touch().messageView().translator().errorTranslate());

        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())).run();
    }
}
