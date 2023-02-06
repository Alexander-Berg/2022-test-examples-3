package ru.yandex.autotests.innerpochta.tests.homer;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetUrlForDomainRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Тесты на шорткаты")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.SHORTCUTS)
@RunWith(DataProviderRunner.class)
public class ShortcutTest extends BaseTest {

    private static final String FOLDER_SHORTCUT = "/messages?current_folder=";
    private static final String COMPOSE_SHORTCUT = "/compose?body=body&subj=subj&to=test@example.com";

    private String msgSubject;
    private String folderName;

    @ClassRule
    public static SetUrlForDomainRule setUrlForDomainRule = new SetUrlForDomainRule();

    @DataProvider
    public static Object[][] messageShortcut() {
        return new Object[][]{
            {"/message?ids=", "#message"},
            {"/msg?ids=", "#message"},
        };
    }

    @DataProvider
    public static Object[][] pagesShortcut() {
        return new Object[][]{
            {"/abook", "#contacts"},
            {"/messages", "#inbox"},
            {"/messages?extra_cond=only_new", "#unread"},
            {"/search?request=wtf", "#search"},
            {"/setup", "#setup"},
        };
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    private AllureStepStorage user;

    @Before
    public void setUp() {
        RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock).withAcc(lock.firstAcc()).login();
        user = new AllureStepStorage(webDriverRule, auth);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        folderName = getRandomString();
        user.apiFoldersSteps().deleteAllCustomFolders()
            .createNewFolder(folderName);
        msgSubject = getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), msgSubject, "");
    }

    @Test
    @Title("Быстрая ссылка для сообщения")
    @TestCaseId("162")
    @UseDataProvider("messageShortcut")
    public void shouldGoToMessageWithMessageShortcut(String shortcut, String path) {
        user.defaultSteps().opensDefaultUrlWithPostFix(
            shortcut + user.apiMessagesSteps().getMessageWithSubject(msgSubject).getMid()
        )
            .shouldBeOnUrl(containsString(path));
        user.messageViewSteps().shouldSeeMessageSubject(msgSubject);
    }

    @Test
    @Title("Быстрая ссылка для папки")
    @TestCaseId("197")
    public void shouldGoToFolderWithShortcut() {
        String folderFid = user.apiFoldersSteps().getFolderByName(folderName).getFid();
        user.defaultSteps().opensDefaultUrlWithPostFix(FOLDER_SHORTCUT + folderFid)
            .shouldBeOnUrl(containsString(FOLDER.makeUrlPart(folderFid)));
    }

    @Test
    @Title("Быстрые ссылки на страницы почты")
    @TestCaseId("198")
    @UseDataProvider("pagesShortcut")
    public void shouldGoToPageWithShortcut(String shortcut, String path) {
        user.defaultSteps().opensDefaultUrlWithPostFix(shortcut)
            .shouldBeOnUrl(containsString(path));
    }

    @Test
    @Title("Быстрая ссылка на страницу композа")
    @TestCaseId("198")
    public void shouldGoToComposeWithShortcut() {
        user.defaultSteps().opensDefaultUrlWithPostFix(COMPOSE_SHORTCUT)
            .shouldSee(onComposePopup().expandedPopup());
    }
}