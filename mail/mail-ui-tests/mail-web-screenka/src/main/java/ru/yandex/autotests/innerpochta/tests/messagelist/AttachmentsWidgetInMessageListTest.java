package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.webcommon.rules.AccountsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DISABLE_INBOXATTACHS;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Аттачи в списке писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.ATTACHES)
@RunWith(DataProviderRunner.class)
public class AttachmentsWidgetInMessageListTest {

    private String subj_attaches = Utils.getRandomName();
    private String subj_eml = Utils.getRandomName();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static AccountsRule account = new AccountsRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Открываем EML через превью в инбоксе")
    @TestCaseId("1071")
    public void shouldSeeEMLOpened() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().onMouseHover(
                    st.pages().mail().home().displayedMessages().list().get(0).attachments().list().get(1)
                )
                .clicksOn(
                    st.pages().mail().home().displayedMessages().list().get(0)
                        .attachments().list().get(1).emlPreviewBtn()
                )
                .shouldSee(st.pages().mail().msgView().attachments().list().waitUntil(not(empty())).get(0));
            st.user().messagesSteps().shouldSeeAllAttachmentInMsgList();
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            subj_eml, Utils.getRandomString(), IMAGE_ATTACHMENT
        );
        forwardMessage(stepsProd);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем список аттачей в инбоксе")
    @TestCaseId("3003")
    public void shouldSeeAllAttachesPreview() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(
                st.pages().mail().home().displayedMessages().list().get(0).attachments().infoBtn()
            );
            st.user().messagesSteps().shouldSeeAllAttachmentInMsgList()
                .shouldSeeAllAttachmentInMsgWidget();
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            subj_attaches, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Просмотр картинок через просмотрщик в инбоксе")
    @TestCaseId("1069")
    public void shouldSeeImageOpened() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().refreshPage();
            st.user().messagesSteps().shouldOpenAttachInMessageList(0, 0);
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            subj_attaches, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Кликаем на скрепку")
    @TestCaseId("3006")
    public void shouldSeeAttachesWidget() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .refreshPage()
            .clicksOn(st.pages().mail().home().displayedMessages().list().get(0).paperClip())
            .shouldSee(st.pages().mail().home().messagePageAttachmentsBlock());

        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            subj_attaches, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Отключаем превью аттачей в списке писем",
            of(SETTINGS_DISABLE_INBOXATTACHS, true)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем список аттачей в списке писем")
    @TestCaseId("4396")
    public void shouldSeeAttachPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .onMouseHover(
                    st.pages().mail().home().displayedMessages().list().get(0)
                        .attachments().list().get(2)
                )
                .clicksOn(
                    st.pages().mail().home().displayedMessages().list().get(0).attachments().infoBtn()
                );
            st.user().messagesSteps().shouldSeeAllAttachmentInMsgList();
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            subj_attaches, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на количество аттачей в попапе")
    @TestCaseId("4393")
    public void shouldSeeCloseAttachPopupBtn() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .clicksOn(
                    st.pages().mail().home().displayedMessages().list().get(0)
                        .attachments().infoBtn()
                )
                .onMouseHover(st.pages().mail().home().messagePageAttachmentsBlock().counterBtn());
            st.user().messagesSteps().shouldSeeAllAttachmentInMsgList()
                .shouldSeeAllAttachmentInMsgWidget();
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            subj_attaches, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Просмотр вложений из шапки треда через просмотрщик")
    @TestCaseId("4463")
    public void shouldSeeThreadWithAttach() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().shouldOpenAttachInMessageList(0, 0);
            st.user().hotkeySteps().pressSimpleHotKey(key(Keys.ESCAPE));
            st.user().messagesSteps()
                .expandsMessagesThread(subj_attaches)
                .shouldOpenAttachInMessageList(1, 0);
        };
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subj_attaches, "");
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsToThreadWithSubject(lock.firstAcc().getSelfEmail(),
            subj_attaches, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Пересылаем письмо")
    private void forwardMessage(InitStepsRule st) {
        stepsProd.user().loginSteps().forAcc(lock.firstAcc()).logins();
        stepsProd.user().messagesSteps().clicksOnMessageWithSubject(subj_eml);
        stepsProd.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().forwardButton());
        stepsProd.user().composeSteps().clicksOnAddEmlBtn()
            .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail());
        stepsProd.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn());
        stepsProd.user().composeSteps().waitForMessageToBeSend();
        stepsProd.user().defaultSteps().opensDefaultUrl();
    }
}
