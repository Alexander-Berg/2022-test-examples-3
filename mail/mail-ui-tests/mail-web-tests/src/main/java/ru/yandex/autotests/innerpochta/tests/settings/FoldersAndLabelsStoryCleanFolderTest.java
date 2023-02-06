package ru.yandex.autotests.innerpochta.tests.settings;

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
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Очистка папок")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsStoryCleanFolderTest extends BaseTest {

    private String subject;
    private Folder parentFolder;
    private Folder subfolder;

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
        subject = Utils.getRandomName();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, Utils.getRandomName());
        parentFolder = user.apiFoldersSteps().createNewFolder(Utils.getRandomName());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Очистка кастомной папки из настроек")
    @TestCaseId("1750")
    public void testCleanSimpleFolder() {
        user.messagesSteps().movesMessageToFolder(subject, parentFolder.getName());
        cleanFolder(parentFolder);
        user.settingsSteps().shouldSeeMessageCountInFolder(parentFolder.getName(), "–");
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Очистка кастомной подпапки из настроек")
    @TestCaseId("1749")
    public void testCleanSubFolder() {
        subfolder = user.apiFoldersSteps().createNewSubFolder(Utils.getRandomName(), parentFolder);
        sendAndMoveMessages();
        cleanFolder(subfolder);
        user.settingsSteps().shouldSeeMessageCountInFolder(parentFolder.getName(), "1 / 1")
            .openThreadIfCan()
            .shouldSeeMessageCountInFolder(subfolder.getName(), "–");
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Очистка папки с подпапкой папки из настроек")
    @TestCaseId("1748")
    public void testCleanParentFolder() {
        subfolder = user.apiFoldersSteps().createNewSubFolder(Utils.getRandomName(), parentFolder);
        sendAndMoveMessages();
        cleanFolder(parentFolder);
        user.settingsSteps().shouldSeeMessageCountInFolder(parentFolder.getName(), "–")
            .openThreadIfCan()
            .shouldSeeMessageCountInFolder(subfolder.getName(), "1 / 1");
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        user.messagesSteps().shouldSeeMessageWithSubject(subject + subject);
    }

    private void cleanFolder(Folder folder) {
        user.apiMessagesSteps().deleteAllMessagesInFolder(folder);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS).refreshPage();
    }

    private void sendAndMoveMessages() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject + subject, Utils.getRandomString());
        user.defaultSteps().opensFragment(QuickFragments.INBOX).refreshPage();
        user.messagesSteps().movesMessageToFolder(subject + subject, parentFolder.getName())
            .movesMessageToFolder(subject, subfolder.getName());
    }
}
