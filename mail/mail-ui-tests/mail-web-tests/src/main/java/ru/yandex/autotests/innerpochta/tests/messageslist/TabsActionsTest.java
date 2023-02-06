package ru.yandex.autotests.innerpochta.tests.messageslist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.autotests.innerpochta.util.MultipleWindowsHandler;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.INBOX_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.NO_POPUP_MARK_READ;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_CURRENT_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_PAGE_AFTER_SENT;

/**
 * * @author mariya-murm
 */
@Aqua.Test
@Title("Действия с письмами в табах")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TABS)
@RunWith(DataProviderRunner.class)
public class TabsActionsTest extends BaseTest {

    private static final String ATTACH_SIZE = "324 kB";
    private static final String MESSAGE_TEXT = "Привет, как дела ?";
    private static final int MESSAGES_PER_PAGE = 2;
    private String subj = getRandomString();

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем табы",
            of(
                FOLDER_TABS, TRUE,
                NO_POPUP_MARK_READ, TRUE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();

    }

    @Test
    @Title("Сохраняем один аттач на компьютер из списка писем")
    @TestCaseId("5122")
    public void shouldSaveAttachmentFromMessageList() {
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(lock.firstAcc().getSelfEmail(), subj,
            getRandomString(), PDF_ATTACHMENT, IMAGE_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        user.apiMessagesSteps().moveAllMessagesToTab(MailConst.NEWS_TAB, INBOX);
        user.defaultSteps().opensFragment(QuickFragments.NEWS_TAB_WEB)
            .onMouseHoverAndClick(
                onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments().list().get(0)
            )
            .checkDownloadedFileNameAndSize(PDF_ATTACHMENT, ATTACH_SIZE);
    }

    @Test
    @Title("Сохраняем аттач на диск в списке писем")
    @TestCaseId("5122")
    public void shouldSaveAttachmentOnDiskFromMessageList() {
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(lock.firstAcc().getSelfEmail(), subj,
            getRandomString(), PDF_ATTACHMENT, IMAGE_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        user.apiMessagesSteps().moveAllMessagesToTab(MailConst.NEWS_TAB, INBOX);
        user.defaultSteps().opensFragment(QuickFragments.NEWS_TAB_WEB)
            .onMouseHover(
                onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments().list()
                    .get(0))
            .clicksOn(
                onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments().list()
                    .get(0).save())
            .shouldSee(onMessagePage().saveToDiskPopup());
    }

    @Test
    @Title("Открываем аттач на просмотр в списке писем")
    @TestCaseId("5122")
    public void shouldOpenAttachViewerFromMessageList() {
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(lock.firstAcc().getSelfEmail(), subj,
            getRandomString(), PDF_ATTACHMENT, IMAGE_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        user.apiMessagesSteps().moveAllMessagesToTab(MailConst.NEWS_TAB, INBOX);
        user.defaultSteps().opensFragment(QuickFragments.NEWS_TAB_WEB);
        user.messagesSteps().shouldOpenAttachInMessageList(0, 1);
    }

    @Test
    @Title("Новое письмо в тред в другой таб")
    @TestCaseId("5119")
    public void shouldSeeNewMessageInRelevantTab() {
        String subject = getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, getRandomString());
        user.apiMessagesSteps().moveMessagesToTab(
            MailConst.SOCIAL_TAB,
            user.apiMessagesSteps().getAllMessages().get(0)
        )
            .sendMessageToThreadWithSubjectWithNoSave(subject, lock.firstAcc(), MESSAGE_TEXT);
        user.defaultSteps().opensFragment(QuickFragments.SOCIAL_TAB_WEB);
        user.messagesSteps().expandsMessagesThread(subject);
        user.defaultSteps().shouldContainText(onMessagePage().displayedMessages().list().get(0).folder(), INBOX_RU);
    }

    @Test
    @Title("Пометить все письма из таба прочитанными")
    @TestCaseId("2764")
    public void shouldMarkMessagesReadInTab() {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 2)
            .moveMessagesToTab(
                MailConst.SOCIAL_TAB,
                user.apiMessagesSteps().getAllMessages().get(0),
                user.apiMessagesSteps().getAllMessages().get(1)
            );
        user.defaultSteps().opensFragment(QuickFragments.SOCIAL_TAB_WEB)
            .clicksOn(onMessagePage().markReadTabIcon());
        user.messagesSteps().clicksOnMultipleMessagesCheckBox(0, 1)
            .shouldSeeThatMessageIsRead();
    }

    @Test
    @Title("Письма не группируются в треды при выключенном тредном режиме")
    @TestCaseId("5120")
    public void shouldNotSeeThreads() {
        String subj = getRandomString();
        user.messagesSteps().disablesGroupBySubject();
        user.apiMessagesSteps()
            .sendThread(lock.firstAcc(), subj, 2);
        user.apiMessagesSteps().moveMessagesToTab(
            MailConst.SOCIAL_TAB,
            user.apiMessagesSteps().getAllMessages().get(0),
            user.apiMessagesSteps().getAllMessages().get(1)
        );
        user.defaultSteps().refreshPage()
            .opensFragment(QuickFragments.SOCIAL_TAB_WEB)
            .shouldNotSee(onMessagePage().displayedMessages().list().get(0).threadCounter());
        user.messagesSteps().shouldSeeCorrectNumberOfMessages(2);
    }

    @Test
    @Title("После отправки письма возвращаемся в текущий таб")
    @TestCaseId("5086")
    public void testPageCurrentAfterSend() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выбираем переход к текущему списку после отправки письма",
            of(SETTINGS_PARAM_PAGE_AFTER_SENT, SETTINGS_PARAM_CURRENT_LIST)
        );
        user.defaultSteps().opensFragment(QuickFragments.SOCIAL_TAB_WEB)
            .clicksOn(onMessagePage().composeButton());
        user.composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .inputsSubject(Utils.getRandomString());
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().shouldBeOnUrlWith(QuickFragments.SOCIAL_TAB_WEB);
    }

    @Test
    @Title("Проверяем работу настройки количества писем на странице")
    @TestCaseId("5082")
    public void shouldSeeMsdPerPageEqualToSettings() {
        String folderName = getRandomName();
        user.apiFoldersSteps().createNewFolder(folderName);
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 6)
            .moveMessagesToTab(
                MailConst.SOCIAL_TAB,
                user.apiMessagesSteps().getAllMessages().get(0),
                user.apiMessagesSteps().getAllMessages().get(1),
                user.apiMessagesSteps().getAllMessages().get(2)
            )
            .moveMessagesFromFolderToFolder(
                folderName,
                user.apiMessagesSteps().getAllMessages().get(3),
                user.apiMessagesSteps().getAllMessages().get(4),
                user.apiMessagesSteps().getAllMessages().get(5)
            );
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER);
        user.settingsSteps().entersMessagesPerPageCount(Integer.toString(MESSAGES_PER_PAGE));
        user.defaultSteps().clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .refreshPage()
            .opensFragment(QuickFragments.SOCIAL_TAB_WEB);
        user.messagesSteps().shouldSeeMsgCount(MESSAGES_PER_PAGE);
        user.leftColumnSteps().openFolders()
            .opensCustomFolderWithTabs(folderName);
        user.messagesSteps().shouldSeeMsgCount(MESSAGES_PER_PAGE);
    }

    @Test
    @Title("Очистка папки «Входящие» через «Настройки»")
    @TestCaseId("5133")
    public void shouldDeleteMessagesInAllTabs() {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 3)
            .moveMessagesToTab(
                MailConst.SOCIAL_TAB,
                user.apiMessagesSteps().getAllMessages().get(0)
            )
            .moveMessagesToTab(
                MailConst.NEWS_TAB,
                user.apiMessagesSteps().getAllMessages().get(1)
            );
        user.defaultSteps().opensFragment(QuickFragments.SOCIAL_TAB_WEB);
        user.messagesSteps().shouldSeeCorrectNumberOfMessages(1);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS)
            .onMouseHoverAndClick(onFoldersAndLabelsSetup().setupBlock().folders().foldersNames().get(0))
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().clearCustomFolder())
            .clicksOn(onFoldersAndLabelsSetup().cleanFolderPopUp().confirmCleaningBtn())
            .opensFragment(QuickFragments.INBOX)
            .clicksOn(onHomePage().checkMailButton());
        user.messagesSteps().shouldNotSeeMessagesPresent();
        user.defaultSteps().opensFragment(QuickFragments.SOCIAL_TAB_WEB);
        user.messagesSteps().shouldNotSeeMessagesPresent();
        user.defaultSteps().opensFragment(QuickFragments.NEWS_TAB_WEB);
        user.messagesSteps().shouldNotSeeMessagesPresent();
    }

    @Test
    @Title("Перемещение писем между табами подтягивается из другой вкладки")
    @TestCaseId("5116")
    public void shouldSeeMovedMessageFromTab() {
        String subj = getRandomString();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем открытие письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, TRUE)
        );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subj, "");
        user.apiMessagesSteps().moveMessagesToTab(
            MailConst.SOCIAL_TAB,
            user.apiMessagesSteps().getAllMessages().get(0)
        );
        user.defaultSteps().opensFragment(QuickFragments.NEWS_TAB_WEB);
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().opensFragment(QuickFragments.SOCIAL_TAB_WEB);
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .clicksOn(onMessageView().miscField().folder())
            .clicksOnElementWithText(
                onMessagePage().moveMessageDropdownMenu().customFolders(),
                MailConst.NEWS_TAB_RU
            )
            .switchesOnMainWindow(windowsHandler);
        user.messagesSteps().shouldSeeMessageWithSubject(subj);
    }

    @Test
    @Title("Видим сообщение, отправленное в другой вкладке")
    @TestCaseId("3747")
    public void shouldSeeSentMessageFromAnotherTab() {
        String subj = getRandomString();
        user.defaultSteps().opensFragment(QuickFragments.SENT);
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
        user.composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .inputsSubject(subj);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().switchesOnMainWindow(windowsHandler);
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(subj);
    }
}
