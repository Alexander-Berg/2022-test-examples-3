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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Создание/удаление/переименование вложенной папки")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsStorySubFolderTest extends BaseTest {

    private static final String FOLDER_NAME = "testтест12345!№";
    private static final String SUB_FOLDER_NAME = ",./;'\\[]<>?:\"_+";

    private String name;

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
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
    }

    public void setUp() {
        Folder parentFolder = user.apiFoldersSteps().createNewFolder(FOLDER_NAME);
        user.apiFoldersSteps().createNewSubFolder(SUB_FOLDER_NAME, parentFolder);
        user.defaultSteps().refreshPage();
        user.settingsSteps().openThreadIfCan()
            .shouldSeeFolder(FOLDER_NAME, SUB_FOLDER_NAME);
    }

    @Test
    @Title("Создание подпапки со специальными символами")
    @TestCaseId("2069")
    public void testCreateSubFolderWithSpecialSymbols() {
        user.settingsSteps().createNewFolder(FOLDER_NAME)
            .clicksOnFolder();
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().createSubFolderButton());
        user.settingsSteps().inputsFoldersName(SUB_FOLDER_NAME);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().newFolderPopUp().create())
            .shouldNotSee(onFoldersAndLabelsSetup().newFolderPopUp().create());
        user.settingsSteps().openThreadIfCan()
            .shouldSeeCustomFoldersCountOnSettingsPage(2)
            .shouldSeeParentHasSubfolder(FOLDER_NAME, SUB_FOLDER_NAME);
    }

    @Test
    @Title("Переименование родительской папки со специальными символами")
    @TestCaseId("1774")
    public void testRenameParentForFolderWithSpecialSymbols() {
        setUp();
        name = user.settingsSteps().renamesFolder();
        user.settingsSteps().shouldSeeParentHasSubfolder(name, SUB_FOLDER_NAME);
        user.leftColumnSteps().shouldSeeFoldersOnMessagePage(name, SUB_FOLDER_NAME);
    }

    @Test
    @Title("Переименование подпапки со специальными символами")
    @TestCaseId("1776")
    public void testRenameSubFolderWithSpecialSymbols() {
        setUp();
        name = user.settingsSteps().renamesFolder(SUB_FOLDER_NAME);
        user.settingsSteps().shouldSeeCustomFoldersCountOnSettingsPage(2)
            .shouldSeeParentHasSubfolder(FOLDER_NAME, name);
        user.leftColumnSteps().shouldSeeFoldersOnMessagePage(FOLDER_NAME, name);
    }

    @Test
    @Title("Удаление подпапки со специальными символами")
    @TestCaseId("1775")
    public void testDeleteSubFolderWithSpecialSymbols() {
        setUp();
        user.settingsSteps().clicksOnFolder(SUB_FOLDER_NAME);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().deleteCustomFolder());
        user.settingsSteps().shouldSeeCustomFoldersCountOnSettingsPage(1)
            .shouldSeeFolder(FOLDER_NAME)
            .shouldSeeThreadsCount(0);
        user.leftColumnSteps().shouldSeeFoldersOnMessagePage(FOLDER_NAME);
    }

    @Test
    @Title("Удаление родительской папки со специальными символами")
    @TestCaseId("1773")
    public void testDeleteParentForFolderWithSpecialSymbols() {
        setUp();
        user.settingsSteps().clicksOnFolder(FOLDER_NAME)
            .clicksOnDeleteFolder();
        user.defaultSteps().shouldNotSee(onFoldersAndLabelsSetup().setupBlock().folders().blockCreatedFolders());
    }
}
