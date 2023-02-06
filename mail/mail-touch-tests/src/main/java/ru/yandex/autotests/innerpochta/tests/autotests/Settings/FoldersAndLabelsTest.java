package ru.yandex.autotests.innerpochta.tests.autotests.Settings;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие тесты в настройки папок и меток")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsTest {

    private static final String FOLDER_LABELS_URLPART = "folders";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)
        );
    }

    @Test
    @Title("Должны создать вложенную папку")
    @TestCaseId("1190")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCreateSubFolder() {
        createFolder();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().create())
            .clicksOn(steps.pages().touch().settings().putInFolder())
            .clicksOn(steps.pages().touch().settings().notChosen())
            .clicksOn(
                steps.pages().touch().settings().folders().get(0),
                steps.pages().touch().settings().closeBtn(),
                steps.pages().touch().settings().editOrSave()
            )
            .waitInSeconds(1)
            .opensDefaultUrlWithPostFix(
                QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)
            ) //TODO: убрать решфреш после фикса QUINN-7633
            .shouldSee(steps.pages().touch().settings().create())
            .shouldSee(steps.pages().touch().settings().subfolders().waitUntil(not(empty())).get(0))
            .shouldSeeElementsCount(steps.pages().touch().settings().folders(), 2);
    }

    @Test
    @Title("Должны создать вложенную папку")
    @TestCaseId("1190")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldCreateSubFolderTablet() {
        createFolder();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().create())
            .clicksOn(steps.pages().touch().settings().putInFolder())
            .clicksOn(steps.pages().touch().settings().notChosen())
            .clicksOn(
                steps.pages().touch().settings().folders().get(0),
                steps.pages().touch().settings().backTablet(),
                steps.pages().touch().settings().saveTablet()
            )
            .refreshPage() //TODO: убрать решфреш после фикса QUINN-7632
            .shouldSee(steps.pages().touch().settings().create())
            .shouldSee(steps.pages().touch().settings().subfolders().waitUntil(not(empty())).get(0))
            .shouldSeeElementsCount(steps.pages().touch().settings().folders(), 2);
    }

    @Test
    @Title("Должны изменить имя папки")
    @TestCaseId("1194")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldChangeFolderName() {
        String folderName = getRandomName();
        createFolder();
        changeName(folderName);
        steps.user().defaultSteps()
            .shouldSeeThatElementTextEquals(steps.pages().touch().settings().folders().get(0), folderName);
    }

    @Test
    @Title("Должны изменить имя папки")
    @TestCaseId("1194")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldChangeFolderNameTablet() {
        String folderName = getRandomName();
        createFolder();
        changeNameTablet(folderName);
        steps.user().defaultSteps()
            .shouldSeeThatElementTextEquals(steps.pages().touch().settings().folders().get(0), folderName);
    }

    @Test
    @Title("Должны изменить имя метки")
    @TestCaseId("1222")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldChangeLabelName() {
        String labelName = getRandomString();
        createLabel();
        changeName(labelName);
        steps.user().defaultSteps()
            .shouldSeeThatElementTextEquals(steps.pages().touch().settings().labels().get(0), labelName);
    }

    @Test
    @Title("Должны изменить имя метки")
    @TestCaseId("1222")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldChangeLabelNameTablet() {
        String labelName = getRandomString();
        createLabel();
        changeNameTablet(labelName);
        steps.user().defaultSteps()
            .shouldSeeThatElementTextEquals(steps.pages().touch().settings().labels().get(0), labelName);
    }

    @Test
    @Title("Должны удалить пустую папку")
    @TestCaseId("1198")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldDeleteEmptyFolder() {
        createFolder();
        deleteElement();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().settings().folders());
    }

    @Test
    @Title("Должны удалить пустую папку")
    @TestCaseId("1198")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldDeleteEmptyFolderTablet() {
        createFolder();
        deleteElementTablet();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().settings().folders());
    }

    @Test
    @Title("Должны удалить непустую папку")
    @TestCaseId("1200")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldDeleteNotEmptyFolder() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomName(), "");
        steps.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, createFolder().getName());
        steps.user().defaultSteps().refreshPage();
        deleteElementWithApprove(TRASH);
    }

    @Test
    @Title("Должны удалить непустую папку")
    @TestCaseId("1200")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldDeleteNotEmptyFolderTablet() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomName(), "");
        steps.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, createFolder().getName());
        steps.user().defaultSteps().refreshPage();
        deleteElementWithApproveTablet(TRASH);
    }

    @Test
    @Title("Должны удалить папку с непустой подпапкой")
    @TestCaseId("1201")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldDeleteFolderWithNotEmptySubfolder() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomName(), "");
        steps.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(
            INBOX,
            steps.user().apiFoldersSteps().createNewSubFolder(getRandomName(), createFolder()).getName()
        );
        steps.user().defaultSteps().refreshPage();
        deleteElementWithApprove(TRASH);
    }

    @Test
    @Title("Должны удалить папку с непустой подпапкой")
    @TestCaseId("1201")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldDeleteFolderWithNotEmptySubfolderTablet() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomName(), "");
        steps.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(
            INBOX,
            steps.user().apiFoldersSteps().createNewSubFolder(getRandomName(), createFolder()).getName()
        );
        steps.user().defaultSteps().refreshPage();
        deleteElementWithApproveTablet(TRASH);
    }

    @Test
    @Title("Должны выйти из режима редактирования")
    @TestCaseId("1206")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCloseEditMode() {
        createFolder();
        createLabel();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().editOrSave())
            .shouldSee(steps.pages().touch().settings().deleteElement())
            .clicksOn(steps.pages().touch().settings().editOrSave())
            .shouldNotSee(steps.pages().touch().settings().deleteElement());
    }

    @Test
    @Title("Должны выйти из режима редактирования")
    @TestCaseId("1206")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldCloseEditModeTablet() {
        createFolder();
        createLabel();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().editTablet())
            .shouldSee(steps.pages().touch().settings().deleteElement())
            .clicksOn(steps.pages().touch().settings().editTablet())
            .shouldNotSee(steps.pages().touch().settings().deleteElement());
    }

    @Test
    @Title("Должны создать папку")
    @TestCaseId("1243")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCreateFolder() {
        String folderName = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().create())
            .clicksAndInputsText(steps.pages().touch().settings().nameInput(), folderName)
            .clicksOn(steps.pages().touch().settings().editOrSave())
            .shouldSee(steps.pages().touch().settings().create())
            .refreshPage() //TODO: убрать решфреш после фикса QUINN-7632
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().settings().folders().waitUntil(not(empty())).get(0),
                folderName
            );
    }

    @Test
    @Title("Должны создать папку")
    @TestCaseId("1243")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldCreateFolderTablet() {
        String folderName = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().create())
            .clicksAndInputsText(steps.pages().touch().settings().nameInput(), folderName)
            .clicksOn(steps.pages().touch().settings().saveTablet())
            .shouldSee(steps.pages().touch().settings().create())
            .refreshPage() //TODO: убрать решфреш после фикса QUINN-7632
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().settings().folders().waitUntil(not(empty())).get(0),
                folderName
            );
    }

    @Test
    @Title("Должны удалить метку, которой отмечено письмо")
    @TestCaseId("1228")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldDeleteLabelWithMessage() {
        steps.user().apiLabelsSteps().markWithLabel(
            steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomName(), ""),
            createLabel()
        );
        steps.user().defaultSteps().refreshPage();
        deleteElementWithApprove(INBOX);
    }

    @Test
    @Title("Должны удалить метку, которой отмечено письмо")
    @TestCaseId("1228")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldDeleteLabelWithMessageTablet() {
        steps.user().apiLabelsSteps().markWithLabel(
            steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomName(), ""),
            createLabel()
        );
        steps.user().defaultSteps().refreshPage();
        deleteElementWithApproveTablet(INBOX);
    }

    @Step("Создаём папку")
    private Folder createFolder() {
        Folder folder = steps.user().apiFoldersSteps().createNewFolder(getRandomName());
        steps.user().defaultSteps().refreshPage();
        return folder;
    }

    @Step("Создаём метку")
    private Label createLabel() {
        Label label = steps.user().apiLabelsSteps().addNewLabel(getRandomName(), LABELS_PARAM_GREEN_COLOR);
        steps.user().defaultSteps().refreshPage();
        return label;
    }

    @Step("Редактируем название")
    private void changeName(String name) {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().editOrSave())
            .clicksOn(steps.pages().touch().settings().editElement().waitUntil(not(empty())).get(0))
            .clicksAndInputsText(steps.pages().touch().settings().nameInput(), name)
            .clicksOn(
                steps.pages().touch().settings().editOrSave(),
                steps.pages().touch().settings().closeBtn()
            );
    }

    @Step("Редактируем название")
    private void changeNameTablet(String name) {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().editTablet())
            .clicksOn(steps.pages().touch().settings().editElement().waitUntil(not(empty())).get(0))
            .clicksAndInputsText(steps.pages().touch().settings().nameInput(), name)
            .clicksOn(
                steps.pages().touch().settings().saveTablet(),
                steps.pages().touch().settings().backTablet()
            );
    }

    @Step("Удаляем папку/метку")
    private void deleteElement() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().editOrSave())
            .clicksOn(steps.pages().touch().settings().deleteElement().waitUntil(not(empty())).get(0));
    }

    @Step("Удаляем папку/метку")
    private void deleteElementTablet() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().editTablet())
            .clicksOn(steps.pages().touch().settings().deleteElement().waitUntil(not(empty())).get(0));
    }

    @Step("Подтвердить удаление в попапе")
    private void deleteElementWithApprove(String folder) {
        deleteElement();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().popup().yesBtn())
            .shouldNotSee(steps.pages().touch().settings().folders());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folder, 1);
    }

    @Step("Подтвердить удаление в попапе")
    private void deleteElementWithApproveTablet(String folder) {
        deleteElementTablet();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().settings().popup().yesBtn())
            .shouldNotSee(steps.pages().touch().settings().folders());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folder, 1);
    }
}
