package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

@Aqua.Test
@Title("Попап удаления папки с письмами")
@RunWith(Parameterized.class)
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsStoryDeleteFolderPopUpTest extends BaseTest {

    @Parameterized.Parameter(0)
    public String folderName;

    @Parameterized.Parameter(1)
    public String message;

    @Parameterized.Parameter(2)
    public int messagesNumber;

    @Parameterized.Parameters(name = "Folder - “{0}“")
    public static Collection<Object[]> testData() {
        Object[][] data = new Object[][]{
            {"FolderName", "В папке «%s» одно письмо. Письмо будет удалено вместе с папкой.", 1},
            {"ИмяПапки", "В папке «%s» 2 письма. Письма будут удалены вместе с папкой.", 2},
            {"ИмяFolder", "В папке «%s» 3 письма. Письма будут удалены вместе с папкой.", 3},
            {",./;'\\[]<>?:\"_+", "В папке «%s» 4 письма. Письма будут удалены вместе с папкой.", 4},
            {"{}!@#$%^&*()-=", "В папке «%s» 5 писем. Письма будут удалены вместе с папкой.", 5},
            {"1234567890!№", "В папке «%s» одно письмо. Письмо будет удалено вместе с папкой.", 1},

        };
        return Arrays.asList(data);
    }

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
    public void logIn() throws InterruptedException {
        user.apiFoldersSteps().createNewFolder(folderName);
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), messagesNumber)
            .moveAllMessagesFromFolderToFolder(INBOX, folderName);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @Title("Попап при удалении папки с письмами")
    @TestCaseId("1758")
    public void testDeleteFolderWithMailPopUp() {
        user.settingsSteps().clicksOnFolder(folderName);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().deleteCustomFolder())
            .shouldSee(onFoldersAndLabelsSetup().deleteFolderPopUp())
            .shouldSeeThatElementTextEquals(
                onFoldersAndLabelsSetup().deleteFolderPopUp().notification(),
                String.format(message, folderName)
            );
    }
}
