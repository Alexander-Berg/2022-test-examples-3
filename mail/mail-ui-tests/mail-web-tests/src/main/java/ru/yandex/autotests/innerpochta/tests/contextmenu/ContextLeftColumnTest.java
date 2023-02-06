package ru.yandex.autotests.innerpochta.tests.contextmenu;

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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Проверяем пункт “Создать папку/метку“ и “Настройки“ для папок/меток")
@Description("Проверяем пункт “Создать папку/метку“ и “Настройки“ для папок/меток")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextLeftColumnTest extends BaseTest {

    private static final String CUSTOM_FOLDER = "CustomFolder";
    private static final String CUSTOM_FOLDER_2 = "CustomFolder2";
    private static final String CUSTOM_LABEL = "customlabel";
    private static final String CUSTOM_LABEL_2 = "customlabel2";
    private static final String[] FOLDER_LIST = {"Входящие", CUSTOM_FOLDER, "Отправленные", "Спам", "Удалённые",
        "Черновики", "Новая папка…"};
    private static final String SETTINGS_FOLDER = "Настроить папки";
    private static final String SETTINGS_LABEL = "Настроить метки";

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
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().rightClick(onMessagePage().foldersNavigation().inboxFolder());
        user.messagesSteps().shouldSeeContextMenu();
    }

    @Test
    @Title("Создаём папку из левой колонки")
    @TestCaseId("1240")
    public void createFolder() {
        user.defaultSteps().clicksOn(onMessagePage().allMenuList().get(0).createFolderLabel())
            .shouldSee(onMessagePage().createFolderPopup())
            .inputsTextInElement(onMessagePage().createFolderPopup().folderName(), CUSTOM_FOLDER)
            .clicksOn(onMessagePage().createFolderPopup().create())
            .shouldNotSee(onMessagePage().createFolderPopup())
            .rightClick(user.pages().MessagePage().displayedMessages().list().get(0).subject());
        user.messagesSteps().shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).moveToFolder());
        user.messagesSteps().shouldSeeItemsInAdditionalContextMenu(FOLDER_LIST);
    }

    @Test
    @Title("Создаём метку из левой колонки")
    @TestCaseId("1242")
    public void createLabel() {
        user.apiLabelsSteps().addNewLabel(CUSTOM_LABEL, "red");
        user.defaultSteps().refreshPage()
            .rightClick(onMessagePage().labelsNavigation().userLabels().get(0));
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOn(onMessagePage().allMenuList().get(0).createFolderLabel())
            .shouldSee(onMessagePage().createLabelPopup())
            .inputsTextInElement(onMessagePage().createLabelPopup().markNameInbox(), CUSTOM_LABEL_2)
            .clicksOn(onMessagePage().createLabelPopup().createMarkButton())
            .shouldNotSee(onMessagePage().createLabelPopup())
            .shouldSeeElementInList(onMessagePage().labelsNavigation().userLabels(), CUSTOM_LABEL_2);
    }

    @Test
    @Title("Переименовываем метку из левой колонки")
    @TestCaseId("3141")
    public void shouldRenameLabel() {
        user.apiLabelsSteps().addNewLabel(CUSTOM_LABEL, "red");
        user.defaultSteps().refreshPage()
            .rightClick(onMessagePage().labelsNavigation().userLabels().get(0))
            .clicksOn(onMessagePage().allMenuList().get(0).itemList().get(1))
            .shouldSee(onHomePage().changeLabelFolderPopup())
            .shouldContainCSSAttributeWithValue(
                onHomePage().changeLabelFolderPopup().nameInput(),
                "value",
                CUSTOM_LABEL
            )
            .inputsTextInElement(onHomePage().changeLabelFolderPopup().nameInput(), CUSTOM_LABEL_2)
            .clicksOn(onHomePage().changeLabelFolderPopup().colors().get(5))
            .clicksOn(onHomePage().changeLabelFolderPopup().confirmButtons().get(0))
            .shouldSeeElementInList(onMessagePage().labelsNavigation().userLabels(), CUSTOM_LABEL_2);
    }

    @Test
    @Title("Переименовываем папку из левой колонки")
    @TestCaseId("3145")
    public void shouldRenameFolder() {
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER);
        user.defaultSteps().refreshPage()
            .clicksIfCanOn(onMessagePage().foldersNavigation().expandInboxFolders())
            .rightClick(onMessagePage().foldersNavigation().customFolders().get(2))
            .clicksOn(onMessagePage().allMenuList().get(0).itemList().get(2))
            .shouldSee(onHomePage().changeLabelFolderPopup())
            .shouldContainCSSAttributeWithValue(
                onHomePage().changeLabelFolderPopup().nameInput(),
                "value",
                CUSTOM_FOLDER
            )
            .inputsTextInElement(onHomePage().changeLabelFolderPopup().nameInput(), CUSTOM_FOLDER_2)
            .clicksOn(onHomePage().changeLabelFolderPopup().confirmButtons().get(0))
            .refreshPage()
            .shouldSeeElementInList(onMessagePage().foldersNavigation().customFolders(), CUSTOM_FOLDER_2);
    }

    @Test
    @Title("Переходим в настройки папок из левой колонки")
    @TestCaseId("1241")
    public void folderSetup() {
        user.defaultSteps().clicksOnElementWithText(onMessagePage().allMenuList().get(0).itemList(), SETTINGS_FOLDER)
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @Title("Переходим в настройки меток из левой колонки")
    @TestCaseId("1241")
    public void labelSetup() {
        user.apiLabelsSteps().addNewLabel(CUSTOM_LABEL, "red");
        user.defaultSteps().refreshPage()
            .rightClick(onMessagePage().labelsNavigation().userLabels().get(0));
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOnElementWithText(onMessagePage().allMenuList().get(0).itemList(), SETTINGS_LABEL)
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_FOLDERS);
    }
}
