package ru.yandex.autotests.innerpochta.tests.screentests.Settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на верстку страниц управления папками и метками")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
@RunWith(DataProviderRunner.class)
public class GeneralFoldersAndLabelsScreenTest {

    private static final String FOLDER_LABELS_URLPART = "folders";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Должны увидеть пустой раздел папок и меток")
    @TestCaseId("1187")
    public void shouldSeeFolderAndLabelEmpty() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().settings().create())
                .shouldNotSee(st.pages().touch().settings().folders())
                .shouldNotSee(st.pages().touch().settings().labels());

        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть только папку в разделе папок и меток")
    @TestCaseId("1205")
    public void shouldSeeFolderInFolderAndLabel() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().settings().folders())
                .shouldNotSee(st.pages().touch().settings().labels());

        Folder parentFolder = createFolder();
        Folder subFolder = stepsProd.user().apiFoldersSteps().createNewSubFolder(getRandomName(), parentFolder);
        Folder secondSubFolder = stepsProd.user().apiFoldersSteps().createNewSubFolder(getRandomName(), subFolder);
        stepsProd.user().apiFoldersSteps().createNewSubFolder(getRandomName(), secondSubFolder);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть только метку в разделе папок и меток")
    @TestCaseId("1188")
    public void shouldSeeLabelInFolderAndLabel() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldNotSee(st.pages().touch().settings().folders())
                .shouldSee(st.pages().touch().settings().labels());

        createLabel();
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть папку и метку в разделе папок и меток")
    @TestCaseId("1188")
    public void shouldSeeFolderAndLabel() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().settings().folders())
                .shouldSee(st.pages().touch().settings().labels());

        createLabel();
        createFolder();
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть попап удаления непустой папки/метки")
    @TestCaseId("1228")
    @DataProvider({"0", "1"})
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeDeletePopup(int num) {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().editOrSave())
                .clicksOn(st.pages().touch().settings().deleteElement().get(num))
                .shouldSee(st.pages().touch().settings().popup());

        stepsProd.user().apiLabelsSteps().markWithLabel(
            stepsProd.user().apiMessagesSteps().sendMailWithNoSave(acc.firstAcc(), getRandomName(), ""),
            createLabel()
        );
        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, createFolder().getName());
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть попап удаления непустой папки/метки")
    @TestCaseId("1228")
    @DataProvider({"0", "1"})
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeDeletePopupTablet(int num) {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().editTablet())
                .clicksOn(st.pages().touch().settings().deleteElement().get(num))
                .shouldSee(st.pages().touch().settings().popup());

        stepsProd.user().apiLabelsSteps().markWithLabel(
            stepsProd.user().apiMessagesSteps().sendMailWithNoSave(acc.firstAcc(), getRandomName(), ""),
            createLabel()
        );
        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, createFolder().getName());
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Step("Создаём папку")
    private Folder createFolder() {
        return stepsProd.user().apiFoldersSteps().createNewFolder(getRandomName());
    }

    @Step("Создаём метку")
    private Label createLabel() {
        return stepsProd.user().apiLabelsSteps().addNewLabel(getRandomName(), LABELS_PARAM_GREEN_COLOR);
    }
}

