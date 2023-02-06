package ru.yandex.autotests.innerpochta.tests.messagelist;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.YA_DISK_URL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SAVE_SENT;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Верстка превью аттачей из Диска")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.ATTACHES)
public class DiskAttachmentsPreviewTest {

    private String msgWithAttaches = Utils.getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(DISK_USER_TAG);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем сохранение в Отправленных",
            of(SETTINGS_SAVE_SENT, FALSE)
        );
    }

    @Test
    @Title("Верстка превью аттачей одиночного письма в списке писем")
    @TestCaseId("4703")
    public void shouldSeeAttachmentsInMessageList() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().clicksOn(
            st.user().messagesSteps().findMessageBySubject(msgWithAttaches).attachments().infoBtn()
        );
        sendMessage(stepsProd);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка превью аттачей одиночного письма на отдельной странице")
    @TestCaseId("4703")
    public void shouldSeeAttachmentsInMessageView() {
        Consumer<InitStepsRule> actions = st ->
            st.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);

        sendMessage(stepsProd);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка превью аттачей одиночного письма при открытии в списке писем")
    @TestCaseId("4703")
    public void shouldSeeAttachmentsInMessageCompactView() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем открытие письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);

        sendMessage(stepsProd);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка превью аттачей внутри eml")
    @TestCaseId("4703")
    public void shouldSeeAttachmentsInEml() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().onMouseHover(
            st.user().messagesSteps().findMessageBySubject("Fwd: " + msgWithAttaches).attachments().list().get(1)
        ).clicksOn(
            st.user().messagesSteps().findMessageBySubject("Fwd: " + msgWithAttaches).attachments().list().get(1)
                .emlPreviewBtn()
        );

        sendMessage(stepsProd);
        forwardMessage(stepsProd);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка превью аттачей в шапке треда")
    @TestCaseId("4703")
    public void shouldSeeAttachmentsInMessageListThreadHead() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().clicksOn(
            st.user().messagesSteps().findMessageBySubject(msgWithAttaches).attachments().infoBtn()
        );

        sendMessage(stepsProd);
        replyOnMessage(stepsProd);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка превью аттачей в письме внутри треда")
    @TestCaseId("4703")
    public void shouldSeeAttachmentsInMessageListThreadMessage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().expandsMessagesThread(msgWithAttaches);
            st.user().defaultSteps().clicksOn(
                st.user().pages().MessagePage().displayedMessages().messagesInThread().get(0).attachments().infoBtn()
            );
        };
        sendMessage(stepsProd);
        replyOnMessage(stepsProd);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Пересылаем письмо")
    private void forwardMessage(InitStepsRule st) {
        stepsProd.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);
        stepsProd.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().forwardButton());
        stepsProd.user().composeSteps().clicksOnAddEmlBtn()
            .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail());
        stepsProd.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn());
        stepsProd.user().composeSteps().waitForMessageToBeSend();
        stepsProd.user().defaultSteps().opensDefaultUrl();
    }

    @Step("Присылаем письмо с аттачами с диска")
    private void sendMessage(InitStepsRule st) {
        stepsProd.user().loginSteps().forAcc(lock.firstAcc()).logins();
        stepsProd.user().defaultSteps().opensUrl(YA_DISK_URL)
            .opensDefaultUrlWithPostFix("/compose");
        stepsProd.user().composeSteps().addAttachFromDisk(0);
        stepsProd.user().composeSteps().inputsSubject(msgWithAttaches)
            .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail());
        stepsProd.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn());
        stepsProd.user().composeSteps().waitForMessageToBeSend();
        stepsProd.user().apiMessagesSteps().deleteMessages(
            stepsProd.user().apiMessagesSteps().getMessageWithSubjectInFolder(msgWithAttaches, SENT));
        stepsProd.user().apiMessagesSteps().deleteMessages(
            stepsProd.user().apiMessagesSteps().getMessageWithSubjectInFolder(msgWithAttaches, TRASH));
        stepsProd.user().defaultSteps().opensDefaultUrl();
    }

    @Step("Формируем тред")
    private void replyOnMessage(InitStepsRule st) {
        stepsProd.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);
        stepsProd.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().replyButton());
        stepsProd.user().composeSteps().addAttachFromDisk(1);
        stepsProd.user().composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail());
        stepsProd.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn());
        stepsProd.user().composeSteps().waitForMessageToBeSend();
        stepsProd.user().defaultSteps().opensDefaultUrl();
    }
}
