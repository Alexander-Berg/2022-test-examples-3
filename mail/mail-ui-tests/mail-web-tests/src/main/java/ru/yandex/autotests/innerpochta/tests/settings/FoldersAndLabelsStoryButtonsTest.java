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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static java.lang.String.format;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMPORTANT_LABEL_NAME_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.UNREAD_LABEL_NAME_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

@Aqua.Test
@Title("Кнопки для работы с папками и метками")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsStoryButtonsTest extends BaseTest {

    private static final String SYSTEM_FOLDER_NAME = "Входящие";
    private static final String NAME_OF_EMPTY_FOLDER = "emptyfolder";
    private static final String NAME_OF_NON_EMPTY_FOLDER = "folder";
    private static final String NOTIFICATION_DUPLICATE = "Папка с таким именем уже существует. Придумайте другое.";
    private static final String NOTIFICATION_SYSTEM_FOLDER = "Невозможно создать папку «%s», так как это имя " +
        "является системным. Придумайте другое.";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));


    @Before
    public void logIn() throws InterruptedException, IOException {
        user.apiFoldersSteps().createNewFolder(NAME_OF_NON_EMPTY_FOLDER);
        user.apiFoldersSteps().createNewFolder(NAME_OF_EMPTY_FOLDER);
        user.apiMessagesSteps().sendMail(lock.firstAcc(), Utils.getRandomName(), "");
        user.apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, NAME_OF_NON_EMPTY_FOLDER);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @Title("Доступные кнопки для пустой системной папки")
    @TestCaseId("1734")
    public void testButtonsForEmptySystemFolder() {
        user.defaultSteps().onMouseHoverAndClick(onFoldersAndLabelsSetup().setupBlock().folders().inboxFolderCounter());
        user.defaultSteps().shouldBeEnabled(
            onFoldersAndLabelsSetup().setupBlock().folders().newFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().createFilterButton()
        );
        user.defaultSteps().shouldBeDisabled(
            onFoldersAndLabelsSetup().setupBlock().folders().clearCustomFolder(),
            onFoldersAndLabelsSetup().setupBlock().folders().createSubFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().renameFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().deleteCustomFolder()
        );
    }

    @Test
    @Title("Доступные кнопки для не пустой системной папки")
    @TestCaseId("1735")
    public void testButtonsForNotEmptySystemFolder() {
        user.defaultSteps().onMouseHoverAndClick(onFoldersAndLabelsSetup().setupBlock().folders().foldersNames().get(4));
        user.defaultSteps().shouldBeEnabled(
            onFoldersAndLabelsSetup().setupBlock().folders().newFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().createFilterButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().clearCustomFolder()
        );
        user.defaultSteps().shouldBeDisabled(
            onFoldersAndLabelsSetup().setupBlock().folders().createSubFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().renameFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().deleteCustomFolder()
        );
    }

    @Test
    @Title("Доступные кнопки для пустой пользовательской папки")
    @TestCaseId("1736")
    public void testButtonsForEmptyCustomFolder() {
        user.settingsSteps().clicksOnFolder(NAME_OF_EMPTY_FOLDER);
        user.defaultSteps().shouldBeEnabled(
            onFoldersAndLabelsSetup().setupBlock().folders().newFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().createFilterButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().createSubFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().renameFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().deleteCustomFolder()
        );
        user.defaultSteps().shouldBeDisabled(onFoldersAndLabelsSetup().setupBlock().folders().clearCustomFolder());
    }

    @Test
    @Title("Доступные кнопки для не пустой пользовательской папки")
    @TestCaseId("1737")
    public void testButtonsForNotEmptyCustomFolder() {
        user.settingsSteps().clicksOnFolder(NAME_OF_NON_EMPTY_FOLDER);
        user.defaultSteps().shouldBeEnabled(
            onFoldersAndLabelsSetup().setupBlock().folders().newFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().createFilterButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().createSubFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().renameFolderButton(),
            onFoldersAndLabelsSetup().setupBlock().folders().deleteCustomFolder(),
            onFoldersAndLabelsSetup().setupBlock().folders().clearCustomFolder()
        );
    }

    @Test
    @Title("Доступные кнопки для метки метки «Важные»")
    @TestCaseId("1738")
    public void testButtonsForImportantLabel() {
        user.defaultSteps().clicksOnElementWithText(
            onFoldersAndLabelsSetup().setupBlock().labels().defaultLabels(),
            IMPORTANT_LABEL_NAME_RU
        );
        user.defaultSteps().shouldBeEnabled(
            onFoldersAndLabelsSetup().setupBlock().labels().newLabel(),
            onFoldersAndLabelsSetup().setupBlock().labels().createFilterForLabel()
        );
        user.defaultSteps().shouldBeDisabled(
            onFoldersAndLabelsSetup().setupBlock().labels().changeLabel(),
            onFoldersAndLabelsSetup().setupBlock().labels().deleteLabel()
        );
    }

    @Test
    @Title("Доступные кнопки для метки метки «Непрочитанные»")
    @TestCaseId("1739")
    public void testButtonsForUnreadLabel() {
        user.defaultSteps().clicksOnElementWithText(
            onFoldersAndLabelsSetup().setupBlock().labels().defaultLabels(),
            UNREAD_LABEL_NAME_RU
        );
        user.defaultSteps().shouldBeEnabled(onFoldersAndLabelsSetup().setupBlock().labels().newLabel());
        user.defaultSteps().shouldBeDisabled(
            onFoldersAndLabelsSetup().setupBlock().labels().createFilterForLabel(),
            onFoldersAndLabelsSetup().setupBlock().labels().changeLabel(),
            onFoldersAndLabelsSetup().setupBlock().labels().deleteLabel()
        );
    }

    @Test
    @Title("Переименование папки в невалидное имя")
    @TestCaseId("1740")
    public void testAttemptToRenameFolderWithInvalidName() {
        user.settingsSteps().clicksOnFolder(NAME_OF_NON_EMPTY_FOLDER);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().renameFolderButton());
        user.settingsSteps().inputsFoldersName(NAME_OF_EMPTY_FOLDER);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().newFolderPopUp().create())
            .shouldSee(onFoldersAndLabelsSetup().newFolderPopUp().invalidNameNotify())
            .shouldSeeThatElementTextEquals(
                onFoldersAndLabelsSetup().newFolderPopUp().invalidNameNotify(),
                NOTIFICATION_DUPLICATE
            );
    }

    @Test
    @Title("Создание системной папки")
    @TestCaseId("1741")
    public void testAttemptToCreateSystemFolder() {
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().newFolderButton());
        user.settingsSteps().inputsFoldersName(SYSTEM_FOLDER_NAME);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().newFolderPopUp().create())
            .shouldSee(onFoldersAndLabelsSetup().newFolderPopUp().invalidNameNotify())
            .shouldSeeThatElementTextEquals(
                onFoldersAndLabelsSetup().newFolderPopUp().invalidNameNotify(),
                format(NOTIFICATION_SYSTEM_FOLDER, SYSTEM_FOLDER_NAME)
            );
    }
}
