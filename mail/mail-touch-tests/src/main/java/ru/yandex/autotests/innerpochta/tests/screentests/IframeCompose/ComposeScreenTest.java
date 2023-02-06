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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FEEDBACK;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.FREEZE_DONE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_ENABLE_QUOTING;


/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие скриночные тесты на композ")
@Features({FeaturesConst.COMPOSE})
@Stories(FeaturesConst.GENERAL)
public class ComposeScreenTest {

    private static final String DEV_NULL_NAME = "Имя Фамилия";

    private static final int ARBITRARY_THREAD_SIZE = 2;

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private String messageBody, addresses;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void composePrepare() {
        messageBody = Utils.getRandomString();
        addresses = DEV_NULL_EMAIL + " " + DEV_NULL_EMAIL_2 + " " + acc.firstAcc().getSelfEmail() + " " +
            getRandomName() + " " + getRandomName() + " " + getRandomName() + " " + getRandomName() +
            " " + getRandomName() + " " + getRandomName();
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            acc.firstAcc().getSelfEmail(),
            getRandomString(),
            getRandomString(),
            PDF_ATTACHMENT
        );
        stepsProd.user().apiMessagesSteps().sendThread(
            acc.firstAcc(),
            Utils.getRandomString(),
            ARBITRARY_THREAD_SIZE
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Проверяем верстку формы обратной связи")
    @TestCaseId("817")
    public void shouldSeeFeedbackForm() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().compose().composeTitle());

        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FEEDBACK.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть композ с заполенным полем To и цитированием после ответа на письмо в треде")
    @TestCaseId("83")
    public void ShouldSeeComposeWithCitationAfterSwipe() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().openActionsForMessages(0);
            st.user().defaultSteps()
                .clicksOnElementWithText(st.pages().touch().messageList().popup().btnsList(), REPLY.btn());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(
                st.pages().touch().composeIframe().quote(),
                st.pages().touch().composeIframe().inputTo()
            );
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).runSequentially();
    }

    @Test
    @Title("Кнопка прикрепления аттаче становится активной при выборе аттача")
    @TestCaseId("778")
    public void shouldSeeActiveButtonForAddAttach() {
        Consumer<InitStepsRule> act = st -> {
            addMailAttachment(st);
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().diskAttachmentsPage().attachBtn());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Выбор аттачей сбрасывается, если уйти со страницы прикрепления аттачей")
    @TestCaseId("745")
    public void shouldSeeInActiveButtonForAddAttach() {
        Consumer<InitStepsRule> act = st -> {
            addMailAttachment(st);
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().diskAttachmentsPage().closeBtn())
                .shouldSee(st.pages().touch().composeIframe().inputTo())
                .clicksOn(st.pages().touch().composeIframe().header().clip())
                .clicksOn(st.pages().touch().composeIframe().attachFilesPopup().fromDisk())
                .shouldSee(st.pages().touch().composeIframe().diskAttachmentsPage().attachBtn());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Проверяем, что появился статуслайн об отправке письма")
    @TestCaseId("149")
    public void shouldSeeStatusLineAboutSendMessage() {
        Consumer<InitStepsRule> act = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageList().headerBlock().compose());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().inputsTextInElement(st.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
                .inputsTextInElement(st.pages().touch().composeIframe().inputBody(), messageBody)
                .clicksOn(st.pages().touch().composeIframe().header().sendBtn())
                .shouldSee(st.pages().touch().messageList().statusLineInfo())
                .executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldSee(st.pages().touch().messageList().statusLineInfo());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Не должны цитировать письмо при ответе, если отключена настройка")
    @TestCaseId("139")
    public void shouldNotQuoteMsg() {
        Consumer<InitStepsRule> act = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(0))
                .clicksOn(st.pages().touch().messageView().moreBtn())
                .clicksOnElementWithText(st.pages().touch().messageView().btnsList(), REPLY.btn());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().header().sendBtn())
                .shouldNotSee(st.pages().touch().composeIframe().quote());
        };
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем настройку «Цитировать исходное письмо при ответе»",
            of(SETTINGS_ENABLE_QUOTING, STATUS_OFF)
        );
        parallelRun.withAcc(acc.firstAcc()).withActions(act).run();
    }

    @Test
    @Title("После тапа на ябл видим email")
    @TestCaseId("415")
    public void shouldSeeEmailInFieldTo() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksAndInputsText(st.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
                .clicksOn(st.pages().touch().composeIframe().composeSuggestItems().waitUntil(not(empty())).get(0))
                .shouldContainText(st.pages().touch().composeIframe().yabble(), DEV_NULL_NAME)
                .clicksOn(st.pages().touch().composeIframe().yabble())
                .shouldSeeThatElementTextEquals(st.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL);
        };
        stepsProd.user().apiAbookSteps().removeAllAbookContacts().addNewContacts(
            stepsProd.user().abookSteps().createContactWithParametrs(DEV_NULL_NAME, DEV_NULL_EMAIL));
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Несколько получателей прячутся за «Ещё»")
    @TestCaseId("365")
    public void shouldSeeMoreRecipients() {
        Consumer<InitStepsRule> actions = this::addSeveralAddresses;

        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Step("Выбрать аттач из почты")
    private void addMailAttachment(InitStepsRule st) {
        st.user().touchSteps().switchToComposeIframe();
        st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().inputTo())
            .clicksOn(st.pages().touch().composeIframe().header().clip())
            .clicksOn(st.pages().touch().composeIframe().attachFilesPopup().fromDisk())
            .clicksOn(
                st.pages().touch().composeIframe().diskAttachmentsPage().checkbox().waitUntil(not(empty())).get(0)
            );
    }

    @Step("Добавить несколько адресов")
    private void addSeveralAddresses(InitStepsRule st) {
        st.user().touchSteps().switchToComposeIframe();
        st.user().defaultSteps().inputsTextInElement(st.pages().touch().composeIframe().inputTo(), addresses)
            .clicksOn(st.pages().touch().composeIframe().expandComposeFields())
            .clicksOn(st.pages().touch().composeIframe().expandComposeFields())
            .shouldSee(st.pages().touch().composeIframe().yabbleMore());
    }
}