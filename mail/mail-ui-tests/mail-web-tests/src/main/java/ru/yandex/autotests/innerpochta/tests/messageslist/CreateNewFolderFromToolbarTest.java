package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created by mabelpines on 30.11.15.
 */

@Aqua.Test
@Title("Создание новой папки через тулбар")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class CreateNewFolderFromToolbarTest extends BaseTest {

    private String subject;
    private String folderName;
    private String parentFolderName;
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
        subject = Utils.getRandomName();
        folderName = Utils.getRandomString();
        parentFolderName = Utils.getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создаем новую папку через тулбар в инбоксе")
    @TestCaseId("907")
    public void shouldCreateNewFolderFromToolbar() {
        user.messagesSteps().selectMessageWithSubject(subject)
            .createsNewFolderFromDropDownMenu(folderName);
        user.leftColumnSteps().openFolders()
            .shouldSeeFoldersWithName(folderName)
            .opensCustomFolder(folderName);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Создаем новую вложенную папку через тулбар в инбоксе и перемещаем выделенное письмо")
    @TestCaseId("4293")
    public void shouldCreateNewSubfolderFromToolbar() {
        user.apiFoldersSteps().createNewFolder(parentFolderName);
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectMessageWithSubject(subject);
        createSubFolderFromToolbar(folderName, parentFolderName);
        user.leftColumnSteps().opensCustomFolder(folderName);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Добавляем кнопку на перемещение письма в новую вложенную папку через тулбар")
    @TestCaseId("4579")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-63623")
    public void shouldCreateCustomButtonForNewSubfolder() {
        user.apiFoldersSteps().createNewFolder(parentFolderName);
        user.defaultSteps().refreshPage();
        createSubFolderFromCustomButtonsMenu(folderName, parentFolderName);
        user.messagesSteps().selectMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().autoMoveButtonIcon());
        user.leftColumnSteps().opensCustomFolder(folderName);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Step("Создаём новую подпапку{0} внутри папки{1} через кнопку «В папку» в тулбаре")
    private void createSubFolderFromToolbar(String subName, String parentName) {
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown())
            .onMouseHoverAndClick(onMessagePage().moveMessageDropdownMenu().createNewFolder())
            .shouldSee(onMessagePage().createFolderPopup());
        fillSubFolderInputs(subName, parentName);
        user.defaultSteps().clicksOn(onMessagePage().createFolderPopup().create());
        user.leftColumnSteps().openFolders()
            .shouldSeeFoldersWithName(subName);
    }

    @Step("Создаём новую подпапку{0} внутри папки{1} через создание пользовательских кнопок")
    private void createSubFolderFromCustomButtonsMenu(String subName, String parentName) {
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .clicksOn(onCustomButtons().overview().moveToFolder())
            .selectsOption(onCustomButtons().configureFoldersButton().folderSelect(), "Новая папка");
        fillSubFolderInputs(subName, parentName);
        user.defaultSteps().clicksOn(
            onCustomButtons().configureFoldersButton().saveButton(),
            onCustomButtons().overview().saveChangesButton()
        );
        user.leftColumnSteps().shouldSeeFoldersWithName(subName);
    }

    @Step("Заполняем поля для новой подпапки{0} внутри папки{1}")
    private void fillSubFolderInputs(String subName, String parentName) {
        user.defaultSteps()
            .inputsTextInElement(onMessagePage().createFolderPopup().folderName(), subName)
            .clicksOn(onMessagePage().createFolderPopup().selectFolderButton())
            .clicksOnElementWithText(onMessagePage().selectFolderDropDown(), parentName);
    }
}
