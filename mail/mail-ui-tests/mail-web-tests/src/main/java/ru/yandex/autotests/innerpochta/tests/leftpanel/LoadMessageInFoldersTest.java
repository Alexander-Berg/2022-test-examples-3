package ru.yandex.autotests.innerpochta.tests.leftpanel;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * * @author cosmopanda
 */
@Aqua.Test
@Title("Загрузка списка писем в папках")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.LEFT_PANEL)
@RunWith(DataProviderRunner.class)
public class LoadMessageInFoldersTest extends BaseTest {

    // имена папок - константы, чтобы использовать параметризацию
    private static final String FOLDER_NAME = "main f)%^lder";
    private static final String SUBFOLDER_NAME = "sub /*&папка";
    private static final int COUNT_MSG_PER_PAGE = 5;
    private static final int COUNT_MSG = 8;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @DataProvider
    public static Object[][] folders() {
        return new Object[][]{
            {INBOX, INBOX_RU},
            {FOLDER_NAME, FOLDER_NAME},
            {SUBFOLDER_NAME, SUBFOLDER_NAME},
            {SPAM, SPAM_RU},
        };
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        Folder parent = user.apiFoldersSteps().createNewFolder(FOLDER_NAME);
        user.apiFoldersSteps().createNewSubFolder(SUBFOLDER_NAME, parent);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем показ 5 писем на странице и раскрываем все папки",
            of(
                    SETTINGS_PARAM_MESSAGES_PER_PAGE, COUNT_MSG_PER_PAGE,
                    FOLDERS_OPEN, user.apiFoldersSteps().getAllFids()
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Должны загрузиться письма в папке")
    @TestCaseId("1940")
    @UseDataProvider("folders")
    public void shouldLoadMessageInFolder(String folder, String displayName) {
        prepareFolder(folder);
        user.defaultSteps().clicksOnElementWithText(
            onMessagePage().foldersNavigation().customFolders(), displayName
        );
        user.messagesSteps().shouldSeeMessagesPresent()
            .shouldSeeMsgCount(COUNT_MSG_PER_PAGE);
        user.defaultSteps().clicksOn(onMessagePage().loadMoreMessagesButton());
        user.messagesSteps().shouldSeeMsgCount(COUNT_MSG);
    }

    @Step("Добавляем письма в папку {0}")
    private void prepareFolder(String targetFolder) {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), COUNT_MSG)
            .moveAllMessagesFromFolderToFolder(INBOX, targetFolder);
        user.defaultSteps().refreshPage();
    }
}
