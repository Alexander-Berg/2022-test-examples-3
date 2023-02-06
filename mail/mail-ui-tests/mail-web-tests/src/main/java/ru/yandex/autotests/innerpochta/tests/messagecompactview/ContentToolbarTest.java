package ru.yandex.autotests.innerpochta.tests.messagecompactview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_USER_NAME;

/**
 * @author mabelpines
 */
@Aqua.Test
@Title("Проверяем Тулбар в просмотре письма.")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class ContentToolbarTest extends BaseTest {

    private static final String PREFIX_FORWARD = "Fwd: ";
    private static final String PREFIX_REPLY = "Re: ";
    private static final String CUSTOM_FOLDER = Utils.getRandomString();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Message msg;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем открытие письма в списке писем, раскрываем все папки",
            of(
                SETTINGS_OPEN_MSG_LIST, TRUE,
                FOLDERS_OPEN, user.apiFoldersSteps().getAllFids()
            )
        );
        msg = user.apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), webDriverRule.getBaseUrl());
        user.apiFoldersSteps().createArchiveFolder();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Создаем правило из тулбара в просмотре письма")
    @TestCaseId("1984")
    public void shouldCreateFilterFromContentToolbar() {
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSee(onMessageView().miscField())
            .clicksOn(onMessageView().miscField().createFilter())
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.SETTINGS_FILTERS_CREATE, "message=" + msg.getMid())
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(0).inputCondition(), lock.firstAcc().getSelfEmail())
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(1).inputCondition(), msg.getSubject());
    }

    @Test
    @Title("Создаем новую метку из тулбара в просмотре письма")
    @TestCaseId("910")
    public void shouldCreateLabelFromContentToolBar() {
        String labelName = Utils.getRandomString();
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSee(onMessageView().miscField())
            .clicksOn(onMessageView().miscField().label())
            .shouldSee(onMessageView().labelsDropdownMenu())
            .clicksOn(onMessageView().labelsDropdownMenu().createNewLabel());
        user.settingsSteps().inputsLabelName(labelName);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().newLabelPopUp().createMarkButton())
            .shouldNotSee(onFoldersAndLabelsSetup().newLabelPopUp())
            .shouldSeeThatElementTextEquals(onMessageView().messageLabel().get(0), labelName);
    }

    @Test
    @Title("Создаем новую папку из тулбара в просмотре письма")
    @TestCaseId("1983")
    public void shouldCreateFolderFromContentToolbar() {
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSee(onMessageView().miscField())
            .clicksOn(onMessageView().miscField().folder())
            .shouldSee(onMessageView().moveMessageDropdownMenu())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().createNewFolder());
        user.settingsSteps().inputsFoldersName(CUSTOM_FOLDER);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().newFolderPopUp().create())
            .shouldBeOnUrlWith(QuickFragments.INBOX);
        user.leftColumnSteps().shouldSeeFoldersWithName(CUSTOM_FOLDER);
    }

    @Test
    @Title("Закрепляем письмо из тулбара в просмотре письма")
    @TestCaseId("1985")
    public void shouldPinLetterFromContentToolbar() {
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSee(onMessageView().miscField())
            .clicksOn(onMessageView().miscField().pin())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeThatMessageIsPinned(msg.getSubject());
        user.apiLabelsSteps().unPinLetter(msg);
    }

    @Test
    @Title("Пересылаем письмо из тулбара в просмотре письма")
    @TestCaseId("1981")
    public void shouldForwardMessageFromContentToolbar() {
        user.defaultSteps().shouldSee(onMessageView().contentToolbarBlock().forwardButton())
            .clicksOn(onMessageView().contentToolbarBlock().forwardButton())
            .shouldContainText(onComposePopup().expandedPopup().popupTitle(), PREFIX_FORWARD)
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), msg.getFirstline());
        user.composeSteps().clicksOnAddEmlBtn()
            .shouldSeeSubject(PREFIX_FORWARD + msg.getSubject())
            .shouldSeeMessageAsAttachment(0, msg.getSubject());
    }

    @Test
    @Title("Архивируем письмо в просмотре письма»")
    @TestCaseId("1982")
    public void shouldArchiveMessageFromContentToolbar() {
        user.defaultSteps().shouldSee(onMessageView().contentToolbarBlock())
            .clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSee(onMessageView().miscField())
            .onMouseHoverAndClick(onMessageView().miscField().archiveButton())
            .opensFragment(QuickFragments.ARCHIVE);
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Отвечаем на письмо в просмотре письма")
    @TestCaseId("1626")
    public void shouldReplyFromContentToolbar() {
        String selfName = user.apiSettingsSteps().getUserSettings(SETTINGS_USER_NAME);
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().replyBtn())
            .shouldContainText(onComposePopup().expandedPopup().popupTitle(), PREFIX_REPLY);
        user.composeSteps().revealQuotes();
        user.defaultSteps().shouldContainText(onComposePopup().expandedPopup().bodyInput(), msg.getFirstline());
        user.composeSteps().shouldSeeSendToAreaHas(selfName)
            .shouldSeeSubject(PREFIX_REPLY + msg.getSubject());
    }

    @Test
    @Title("Переносим письмо в папку при просмотре письма")
    @TestCaseId("1038")
    public void shouldMoveMessageFromContentToolbar() {
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER);
        user.defaultSteps().refreshPage()
            .clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSee(onMessageView().miscField())
            .clicksOn(onMessageView().miscField().folder())
            .shouldSee(onMessageView().moveMessageDropdownMenu())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().customFolders().get(1));
        user.messagesSteps().shouldNotSeeMessageWithSubject(msg.getSubject());
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Отправляем письмо в спам в просмотре письма»")
    @TestCaseId("1027")
    public void shouldMoveMessageToSpamFromContentToolbar() {
        user.defaultSteps().shouldSee(onMessageView().contentToolbarBlock())
            .clicksOn(onMessageView().contentToolbarBlock().spamButton())
            .opensFragment(QuickFragments.SPAM);
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps().shouldSee(onMessageView().dangerNotification());
    }

    @Test
    @Title("Восстанавливаем письмо из спама в просмотре письма»")
    @TestCaseId("1027")
    public void shouldRestoreMessageFromSpamFromContentToolbar() {
        user.defaultSteps().shouldSee(onMessageView().contentToolbarBlock())
            .clicksOn(onMessageView().contentToolbarBlock().spamButton())
            .opensFragment(QuickFragments.SPAM);
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().notSpamButton())
            .clicksIfCanOn(onMessagePage().rightSubmitActionBtn())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps().shouldNotSee(onMessageView().dangerNotification());
    }
}
