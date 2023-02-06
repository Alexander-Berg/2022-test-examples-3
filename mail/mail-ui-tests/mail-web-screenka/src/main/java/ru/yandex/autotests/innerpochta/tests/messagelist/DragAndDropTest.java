package ru.yandex.autotests.innerpochta.tests.messagelist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Драг-н-дроп")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class DragAndDropTest {

    private final static String ROOT_FOLDER_ID = "0";
    private static final String PARENT_FOLDER_NAME = "ParentFolder";
    private static final String SUB_FOLDER_NAME = "SubFolder";
    private static final String ROOT_FOLDER_NAME = "RootFolder";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 5);
    }

    @Test
    @Title("Выделяем несколько писем драг-н-дропом")
    @TestCaseId("1946")
    public void shouldSelectMessages() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().dragAndDrop(
                st.pages().mail().home().displayedMessages().list().get(0).avatarAndCheckBox(),
                st.pages().mail().home().displayedMessages().list().get(3).avatarAndCheckBox()
            )
            .shouldSee(st.pages().mail().home().inboxMsgInfoline());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Перекладываем папку в папку")
    @TestCaseId("3305")
    public void shouldSeeFoldersTree() {
        String folderOneName = Utils.getRandomString();
        String folderTwoName = Utils.getRandomString();
        Consumer<InitStepsRule> actions = st -> {
            createFolders(folderOneName, folderTwoName);
            st.user().defaultSteps().refreshPage();
            st.user().apiFoldersSteps().moveFolderToFolder(
                st.user().apiFoldersSteps().getFolderByName(
                    st.pages().mail().home().foldersNavigation().customFolders().get(3).customFolderName().getText()
                ).getFid(),
                ROOT_FOLDER_ID
            );
            st.user().defaultSteps().refreshPage()
                .dragAndDrop(
                    st.pages().mail().home().foldersNavigation().customFolders().get(2).customFolderName(),
                    st.pages().mail().home().foldersNavigation().customFolders().get(3).customFolderName()
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Вытаскиваем папку из папки")
    @TestCaseId("3306")
    public void shouldNotSeeFoldersTree() {
        String folderOneName = Utils.getRandomString();
        String folderTwoName = Utils.getRandomString();
        Consumer<InitStepsRule> actions = st -> {
            createFolders(folderOneName, folderTwoName);
            st.user().defaultSteps().refreshPage();
            MailElement fldr1 = st.pages().mail().home().foldersNavigation().customFolders().get(2).customFolderName();
            st.user().defaultSteps().dragAndDrop(
                    fldr1,
                    st.pages().mail().home().foldersNavigation().customFolders().get(3).customFolderName()
                ).shouldNotSee(fldr1)
                .dragAndDrop(
                    st.pages().mail().home().foldersNavigation().customFolders().get(3).customFolderName(),
                    st.pages().mail().home().foldersNavigation().inboxFolder()
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Драг-н-дроп папки во Входящие")
    @TestCaseId("4329")
    public void shouldDragFolderToInbox() {
        prepareFoldersTree();
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().dragAndDrop(
                st.pages().mail().home().foldersNavigation().customFolders().get(4),
                st.pages().mail().home().foldersNavigation().inbox()
            );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Драг-н-дроп ветки папок")
    @TestCaseId("176")
    public void shouldDragFoldersTree() {
        prepareFoldersTree();
        stepsProd.user().apiFoldersSteps().createNewFolder(ROOT_FOLDER_NAME);

        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().dragAndDrop(
                st.pages().mail().home().foldersNavigation().customFolders().get(2),
                st.pages().mail().home().foldersNavigation().customFolders().get(5)
            );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Создаем новые папки")
    private void createFolders(String nameOne, String nameTwo) {
        stepsProd.user().apiFoldersSteps().deleteAllCustomFolders()
            .createNewFolder(nameOne);
        stepsProd.user().apiFoldersSteps().createNewFolder(nameTwo);
        String foldersFids = stepsProd.user().apiFoldersSteps().getAllFids();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Раскрываем все папки",
            of(FOLDERS_OPEN, foldersFids)
        );
        stepsProd.user().defaultSteps().refreshPage();
    }

    @Step("Создаем дерево папок")
    private void prepareFoldersTree() {
        stepsProd.user().apiFoldersSteps().deleteAllCustomFolders();
        String folderName = Utils.getRandomString();
        Folder parentFolder = stepsProd.user().apiFoldersSteps().createNewFolder(PARENT_FOLDER_NAME);
        Folder subFolder = stepsProd.user().apiFoldersSteps().createNewSubFolder(SUB_FOLDER_NAME, parentFolder);
        stepsProd.user().apiFoldersSteps().createNewSubFolder(folderName, subFolder);
        String foldersFids = stepsProd.user().apiFoldersSteps().getAllFids();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Раскрываем все папки",
            of(FOLDERS_OPEN, foldersFids)
        );
    }
}
