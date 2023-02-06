package ru.yandex.autotests.innerpochta.tests.contextmenu;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;


@Aqua.Test
@Title("Проверяем пункт “Удалить“")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextDeleteTest extends BaseTest {

    private static final int COUNTER = 2;

    private String subject;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        String foldersFids = user.apiFoldersSteps().getAllFids();
        user.apiSettingsSteps().callWithListAndParams(
            "Раскрываем все папки",
            of(FOLDERS_OPEN, foldersFids)
        );
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Удаление одного сообщения")
    @TestCaseId("1229")
    public void deleteMessage() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).deleteMsg());
        user.messagesSteps().shouldNotSeeContextMenu()
            .shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Удаление треда")
    @TestCaseId("1230")
    public void deleteThread() {
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subject, lock.firstAcc(), "");
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).deleteMsg());
        user.messagesSteps().shouldNotSeeContextMenu()
            .shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensTrashFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .shouldSeeMessageWithSubjectCount(subject, COUNTER);
    }

    @Test
    @Title("КМ для очистки папки")
    @TestCaseId("1231")
    public void cleanFolder() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).deleteMsg());
        user.messagesSteps().shouldNotSeeContextMenu()
            .shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(QuickFragments.TRASH)
            .shouldSee(onMessagePage().foldersNavigation().cleanTrashFolder())
            .rightClick(onMessagePage().foldersNavigation().cleanTrashFolder());
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOn(onMessagePage().allMenuList().get(0).cleanFolder())
            .shouldSee(onMessagePage().clearFolderPopUp())
            .clicksOn(onMessagePage().clearFolderPopUp().clearFolderButton());
        user.messagesSteps().shouldSeeThatFolderIsEmpty();
    }

    @Test
    @Title("Удаление системной папки")
    @TestCaseId("1232")
    public void deleteSystemFolder() {
        user.defaultSteps().rightClick(onMessagePage().foldersNavigation().inboxFolder());
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().shouldNotSee(onMessagePage().allMenuList().get(0).deleteItem());
    }

    @Test
    @Title("Удаление пользовательской папки")
    @TestCaseId("1233")
    public void deleteCustomFolder() {
        String folderName = getRandomName();
        user.apiFoldersSteps().createNewFolder(folderName);
        user.defaultSteps().refreshPage();
        user.leftColumnSteps().expandFoldersIfCan()
            .rightClickOnCustomFolder(folderName);
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOn(onMessagePage().allMenuList().get(0).deleteItem())
            .shouldNotSee(onMessagePage().allMenuList().get(0))
            .shouldSeeElementsCount(onMessagePage().foldersNavigation().customFolders(), 6);
    }

    @Test
    @Title("Удаление пользовательской метки")
    @TestCaseId("1234")
    public void deleteCustomLabel() {
        String labelName = Utils.getRandomString();
        user.apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_GREEN_COLOR);
        user.defaultSteps().refreshPage();
        user.leftColumnSteps().shouldSeeCustomLabel(labelName)
            .rightClickOnCustomLabel(labelName);
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOn(onMessagePage().allMenuList().get(0).deleteItem());
        user.leftColumnSteps().shouldNotSeeCustomLabel(labelName);
    }

    @Test
    @Title("Удаление открытой пользовательской папки с письмом")
    @TestCaseId("3146")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-71237")
    public void deleteCustomFolderWhichIsOpen() {
        String folderName = getRandomName();
        user.apiFoldersSteps().createNewFolder(folderName);
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectMessageWithSubject(subject).movesMessageToFolder(folderName);
        user.leftColumnSteps().expandFoldersIfCan()
            .opensCustomFolder(folderName)
            .rightClickOnCustomFolder(folderName);
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOn(
            onMessagePage().allMenuList().get(0).deleteItem(),
            onFoldersAndLabelsSetup().deleteFolderPopUp().confirmDelete()
        );
        user.defaultSteps().shouldBeOnUrlWith(QuickFragments.INBOX);
        user.leftColumnSteps().shouldNotSeeFoldersWithName(folderName);
    }

    @Test
    @Title("Удаление открытой пользовательской метки с письмом")
    @TestCaseId("3147")
    public void deleteCustomLabelWhichIsOpen() {
        String labelName = Utils.getRandomString();
        user.apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_GREEN_COLOR);
        user.apiLabelsSteps().addNewLabel(getRandomName(), LABELS_PARAM_GREEN_COLOR);
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectMessageWithSubject(subject)
            .markMessageWithCustomLabel(labelName);
        user.defaultSteps().refreshPage();
        user.leftColumnSteps().clickOnCustomLabel(labelName)
            .shouldSeeCustomLabel(labelName)
            .rightClickOnCustomLabel(labelName);
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOn(
            onMessagePage().allMenuList().get(0).deleteItem(),
            onFoldersAndLabelsSetup().deleteLabelPopUp().deleteBtn()
        )
            .shouldBeOnUrlWith(QuickFragments.INBOX);
        user.leftColumnSteps().shouldNotSeeCustomLabel(labelName);
    }

    @Test
    @Title("Удаление письма в папке Удаленные через КМ")
    @TestCaseId("640")
    public void shouldDeleteMessageInTrashFolder() {
        user.apiMessagesSteps().deleteMessages(user.apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0));
        user.defaultSteps().refreshPage()
            .opensFragment(QuickFragments.TRASH);
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).deleteMsg());
        user.messagesSteps().shouldNotSeeContextMenu()
            .shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
    }
}
