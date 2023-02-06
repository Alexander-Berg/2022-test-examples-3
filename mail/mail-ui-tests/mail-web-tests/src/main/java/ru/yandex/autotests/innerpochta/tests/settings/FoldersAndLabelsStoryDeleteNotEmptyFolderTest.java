package ru.yandex.autotests.innerpochta.tests.settings;

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
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Удаление папок с письмами")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsStoryDeleteNotEmptyFolderTest extends BaseTest {

    private static final String FOLDER_NAME = "parentfolder";
    private static final String SUB_FOLDER_NAME = "subfolder";

    private String subject;
    private Folder parentFolder;

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
    public void logIn() throws InterruptedException, IOException {
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        parentFolder = user.apiFoldersSteps().createNewFolder(FOLDER_NAME);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Удаляем папку в которой лежат письма")
    @TestCaseId("1763")
    public void testDeleteSimpleFolderWithMail() {
        user.messagesSteps().movesMessageToFolder(subject, FOLDER_NAME);
        deleteFolder(FOLDER_NAME, parentFolder);
        user.defaultSteps().shouldNotSee(onFoldersAndLabelsSetup().setupBlock().folders().blockCreatedFolders());
    }

    @Test
    @Title("Удаляем подпапку в которой лежат письма")
    @TestCaseId("1762")
    public void testDeleteSubFolderWithMail() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        Folder subFolder = createSubFolder();
        sendAndMoveMessages();
        deleteFolder(SUB_FOLDER_NAME, subFolder);
        user.settingsSteps().shouldSeeCustomFoldersCountOnSettingsPage(1)
            .shouldSeeMessageCountInFolder(FOLDER_NAME, "1 / 1");
    }

    @Test
    @Title("Удаляем папку у которой есть подпапка и письма")
    @TestCaseId("1764")
    public void testDeleteParentFolderWithMail() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        createSubFolder();
        sendAndMoveMessages();
        deleteFolder(FOLDER_NAME, parentFolder);
        user.defaultSteps().shouldNotSee(onFoldersAndLabelsSetup().setupBlock().folders().blockCreatedFolders());
    }

    @Test
    @Title("Появление попапа при удалении папки")
    @TestCaseId("1760")
    public void testDeleteFolderPopup() {
        user.messagesSteps().movesMessageToFolder(subject, FOLDER_NAME);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        user.settingsSteps().clicksOnFolder(FOLDER_NAME);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().deleteCustomFolder())
            .shouldSee(onFoldersAndLabelsSetup().deleteFolderPopUpOld())
            .clicksOn(onFoldersAndLabelsSetup().deleteFolderPopUpOld().cancelButton())
            .shouldNotSee(onFoldersAndLabelsSetup().deleteFolderPopUpOld())
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().deleteCustomFolder())
            .clicksOn(onFoldersAndLabelsSetup().deleteFolderPopUpOld().closePopUpButton())
            .shouldNotSee(onFoldersAndLabelsSetup().deleteFolderPopUpOld());
    }

    @Test
    @Title("Помечаем прочитанными письма в корневой папке")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-51376")
    @TestCaseId("1765")
    public void testMarkAsReadParentFolder() {
        user.apiFoldersSteps().createNewSubFolder(SUB_FOLDER_NAME, parentFolder);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject + subject, "");
        user.defaultSteps().refreshPage()
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().movesMessageToFolder(subject + subject, FOLDER_NAME)
            .movesMessageToFolder(subject, SUB_FOLDER_NAME);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        user.settingsSteps().clicksOnFolder(FOLDER_NAME);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().markAsReadAllMail());
        user.settingsSteps().shouldSeeMessageCountInFolder(FOLDER_NAME, "1")
            .openThreadIfCan()
            .shouldSeeFolder(SUB_FOLDER_NAME)
            .shouldSeeMessageCountInFolder(SUB_FOLDER_NAME, "1 / 1");
    }

    @Test
    @Title("Помечаем прочитанными письма в подпапке")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-51376")
    @TestCaseId("1761")
    public void testMarkAsReadSubFolder() {
        createSubFolder();
        sendAndMoveMessages();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        user.settingsSteps().clicksOnFolder(SUB_FOLDER_NAME);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().markAsReadAllMail());
        user.settingsSteps().openThreadIfCan()
            .shouldSeeMessageCountInFolder(FOLDER_NAME, "1 / 1")
            .shouldSeeFolder(SUB_FOLDER_NAME)
            .shouldSeeMessageCountInFolder(SUB_FOLDER_NAME, "1");
    }

    private void deleteFolder(String folderName, Folder folder) {
        user.apiFoldersSteps().deleteFolder(folderName, folder);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS).refreshPage();
    }

    private void sendAndMoveMessages() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject + subject, "");
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().movesMessageToFolder(subject + subject, FOLDER_NAME)
            .movesMessageToFolder(subject, SUB_FOLDER_NAME);
    }

    private Folder createSubFolder() {
        Folder createdFolder = user.apiFoldersSteps().createNewSubFolder(SUB_FOLDER_NAME, parentFolder);
        user.defaultSteps().refreshPage();
        user.settingsSteps().openThreadIfCan();
        return createdFolder;
    }
}
