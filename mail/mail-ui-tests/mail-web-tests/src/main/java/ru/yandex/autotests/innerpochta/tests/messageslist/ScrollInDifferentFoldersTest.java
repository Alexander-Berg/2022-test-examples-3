package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.KeysOwn;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.api.folders.DoFoldersAddHandler.doFoldersAddHandler;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Отскролливание в списке писем разных папок")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class ScrollInDifferentFoldersTest extends BaseTest {

    private static String FOLDER_NAME = "folder";
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        createFolders(6);
        user.apiFoldersSteps().createNewFolder(FOLDER_NAME);
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 3)
            .moveAllMessagesFromFolderToFolder(INBOX, FOLDER_NAME)
            .sendCoupleMessages(lock.firstAcc(), 3);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.leftColumnSteps().openFolders();
    }

    @Test
    @Title("При переходе из папки в папку список писем скроллится в начало")
    @TestCaseId("901")
    public void shouldScrollUpAfterFolderChange() {
        user.defaultSteps().setsWindowSize(1800, 300);
        user.messagesSteps().scrollDownPage();
        user.defaultSteps().shouldSee(onMessagePage().toolbar().topBtn());
        user.leftColumnSteps().opensCustomFolder(6);
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().topBtn());
    }

    @Step("Создаем пользовательские папки в количестве {0}")
    private void createFolders(int numOfFld) {
        for (int fld = 1; fld <= numOfFld; ++fld) {
            String folderName = fld + FOLDER_NAME;
            doFoldersAddHandler().withAuth(auth).withFolderName(folderName).callDoFoldersAddHandler();
        }
    }
}
