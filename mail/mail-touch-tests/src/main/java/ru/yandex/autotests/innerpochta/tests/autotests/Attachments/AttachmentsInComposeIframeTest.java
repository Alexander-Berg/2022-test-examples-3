package ru.yandex.autotests.innerpochta.tests.autotests.Attachments;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.DRAFT_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SENT_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.FORWARD;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на аттачи в композе")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.COMPOSE)
public class AttachmentsInComposeIframeTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount(DISK_USER_TAG));
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().touchSteps().openComposeViaUrl();
    }

    @Test
    @Title("Удаление аттача")
    @TestCaseId("185")
    public void deleteAttachmentsInCompose() {
        addDiskAttach(1);
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().attachments().uploadedAttachment());
        addMailAttach();
        deleteAttachments();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().composeIframe().attachments());
    }

    @Test
    @Title("Троббер на аттаче до загрузки аттача")
    @TestCaseId("431")
    public void shouldSeeThrobberOnAttachments() {
        addDiskAttach(1);
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().attachments().trobber());
    }

    @Test
    @Title("Отправка аттача с диска")
    @TestCaseId("368")
    public void shouldSendMailWithDiskAttachments() {
        addDiskAttach(1);
        fillAddressCheckAttachAndSend();
        steps.user().defaultSteps().shouldBeOnUrlWith(INBOX_FOLDER);
    }

    @Test
    @Title("Отправка аттача из почты")
    @TestCaseId("450")
    public void shouldSendMailWithMailAttachments() {
        addMailAttach();
        fillAddressCheckAttachAndSend();
        steps.user().defaultSteps().shouldBeOnUrlWith(INBOX_FOLDER);
    }

    @Test
    @Title("Отмена загрузки аттача")
    @TestCaseId("743")
    public void shouldCancelUploadAttachment() {
        addDiskAttach(1);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().attachments().cancelUpload())
            .shouldNotSee(steps.pages().touch().composeIframe().attachments());
    }

    @Test
    @Title("Аттачи корректно сохраняются в черновик")
    @TestCaseId("236")
    public void shouldSeeAttachmentsInDraft() {
        addDiskAttach(1);
        addMailAttach();
        assertThat(
            "Аттач не загрузился",
            steps.pages().touch().composeIframe().attachments().uploadedAttachment(),
            withWaitFor(hasSize(2), 15000)
        );
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().closeBtn())
            .shouldBeOnUrl(containsString(INBOX_FOLDER.makeTouchUrlPart()))
            .opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(DRAFT_FOLDER))
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageList().subjectList().get(0));
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps()
            .shouldSeeElementsCount(steps.pages().touch().composeIframe().attachments().attachments(), 2);
    }

    @Test
    @Title("Удаление нескольких аттачей при пересылке")
    @TestCaseId("563")
    public void shouldDeleteFewAttachesWhenForward() {
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock().subject());
        int attachNum = steps.pages().touch().messageView().attachments().waitUntil(not(empty())).size();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().moreBtn())
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), FORWARD.btn());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps()
            .shouldSeeElementsCount(steps.pages().touch().composeIframe().attachments().attachments(), attachNum);
        deleteAttachments();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().composeIframe().attachments());
    }

    @Test
    @Title("Переслать письмо с аттачами")
    @TestCaseId("92")
    public void shouldSeeAttachInForwardMessage() {
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock().subject());
        int num = steps.pages().touch().messageView().attachments().waitUntil(not(empty())).size();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), FORWARD.btn());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldSeeElementsCount(
            steps.pages().touch().composeIframe().attachments().attachments().waitUntil(not(empty())),
            num
        )
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldNotSee(steps.pages().touch().composeIframe().header().sendBtn());
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format(
                "%s/%s",
                FOLDER_ID.makeTouchUrlPart(SENT_FOLDER),
                MSG_FRAGMENT.fragment(steps.user().apiMessagesSteps().getAllMessagesInFolder(SENT).get(0).getMid())
            )
        )
            .waitInSeconds(2)
            .shouldSeeElementsCount(steps.pages().touch().messageView().attachments().waitUntil(not(empty())), num);
    }

    @Test
    @Title("Отправить письмо до того как загрузился аттач")
    @TestCaseId("648")
    public void shouldSeeInactiveSendBtn() {
        steps.user().defaultSteps().clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL);
        addDiskAttach(2);
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().attachments().trobber())
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldSee(steps.pages().touch().composeIframe().cantSendMailPopup())
            .clicksOn(steps.pages().touch().composeIframe().confirmBtn());
        assertThat(
            "Аттач не загрузился",
            steps.pages().touch().composeIframe().attachments().uploadedAttachment(),
            withWaitFor(hasSize(1), 15000)
        );
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldBeOnUrlWith(INBOX_FOLDER);
    }

    @Test
    @Title("На странице дисковых аттачей папка открывается по тапу в строку")
    @TestCaseId("768")
    public void shouldOpenDiskFolder() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().clip())
            .shouldSee(steps.pages().touch().composeIframe().attachFilesPopup())
            .clicksOn(steps.pages().touch().composeIframe().attachFilesPopup().fromMail())
            .clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments().waitUntil(not(empty())).get(0))
            .shouldNotSee(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments())
            .clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().backBtn())
            .shouldSee(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments());
    }

    @Test
    @Title("Должны удалить все аттачи кнопкой «Удалить все»")
    @TestCaseId("1397")
    public void shouldDeleteAllAttaches() {
        addDiskAttach(2);
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().composeIframe().attachments().deleteAll());
        addDiskAttach(2);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().attachments().deleteAll())
            .shouldNotSee(steps.pages().touch().composeIframe().attachments());
    }

    @Step("Добавляем в композ аттач с диска")
    private void addDiskAttach(int num) {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().clip())
            .shouldSee(steps.pages().touch().composeIframe().attachFilesPopup())
            .clicksOn(steps.pages().touch().composeIframe().attachFilesPopup().fromDisk())
            .shouldSee(steps.pages().touch().composeIframe().diskAttachmentsPage())
            .turnTrue(steps.pages().touch().composeIframe().diskAttachmentsPage().checkbox().get(num))
            .clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().attachBtn());
    }

    @Step("Добавляем в композ аттач из почты")
    private void addMailAttach() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().clip())
            .shouldSee(steps.pages().touch().composeIframe().attachFilesPopup())
            .clicksOn(steps.pages().touch().composeIframe().attachFilesPopup().fromMail())
            .shouldSee(steps.pages().touch().composeIframe().diskAttachmentsPage())
            .clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments().get(1))
            .turnTrue(steps.pages().touch().composeIframe().diskAttachmentsPage().checkbox().get(0))
            .clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().attachBtn());
    }

    @Step("Заполняем поле кому, проверяем, что аттач догрузился в композ, и отправляем письмо")
    private void fillAddressCheckAttachAndSend() {
        steps.user().defaultSteps().clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL);
        assertThat(
            "Аттач не загрузился",
            steps.pages().touch().composeIframe().attachments().uploadedAttachment(),
            withWaitFor(hasSize(1), 15000)
        );
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().sendBtn());
    }

    @Step("Удаляем все аттачи")
    private void deleteAttachments() {
        int attNum = steps.pages().touch().composeIframe().attachments().attachmentsDelete().waitUntil(not(empty())).size();
        for (int i = attNum - 1; i >= 0; i--)
            steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().attachments().attachmentsDelete().get(0));
    }
}
