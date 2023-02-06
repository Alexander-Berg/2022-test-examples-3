package ru.yandex.autotests.innerpochta.tests.leftpanel;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;


@Aqua.Test
@Title("Тесты на сворачивание/разворачивание папок")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.LEFT_PANEL)
public class ExpandFoldersTest extends BaseTest {

    private static final String PARENT_FOLDER_NAME = "ParentFolder";
    private static final String SUB_FOLDER_NAME = "SubFolder";

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
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Сворачиваем/разворачиваем папку")
    @TestCaseId("3555")
    public void testCollapseExpandFolders() {
        String folderName = Utils.getRandomString();
        Folder parentFolder = user.apiFoldersSteps().createNewFolder(PARENT_FOLDER_NAME);
        Folder subFolder = user.apiFoldersSteps().createNewSubFolder(SUB_FOLDER_NAME, parentFolder);
        user.apiFoldersSteps().createNewSubFolder(folderName, subFolder);

        user.defaultSteps().refreshPage();
        user.leftColumnSteps().openFolders();
        user.defaultSteps().clicksOn(user.pages().MessagePage().foldersNavigation().collapseFoldersList().get(2))
            .clicksOn(user.pages().MessagePage().foldersNavigation().collapseFoldersList().get(0));
        user.leftColumnSteps().shouldNotSeeFoldersWithName(folderName);
        user.defaultSteps().clicksOn(user.pages().MessagePage().foldersNavigation().expandFoldersList().get(0))
            //expandFoldersList() содержит все стрелки раскрытия папок. Клик на первую (родительскую)
            .clicksOn(user.pages().MessagePage().foldersNavigation().expandFoldersList().get(0));
        //expandFoldersList() содержит новый список, так как на предыдущем шаге раскрыли одну из папок.
        //Клик на первую (у пользовательской папки)
        user.leftColumnSteps().shouldSeeFoldersWithName(folderName);
    }
}
