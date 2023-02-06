package ru.yandex.autotests.innerpochta.tests.screentests.Settings;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomNumber;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на создание папок и меток")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class CreateFoldersAndLabelsScreenTest {

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
    @Title("Должны увидеть страницу создания папок")
    @TestCaseId("1243")
    public void shouldSeeFolderCreation() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .shouldSee(st.pages().touch().settings().nameInput());

        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть страницу создания меток")
    @TestCaseId("1242")
    public void shouldSeeLabelCreation() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .clicksOn(st.pages().touch().settings().labelTab())
                .clicksOn(st.pages().touch().settings().colors().waitUntil(not(empty())).get(0));

        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть ошибку при создании папки с существующим именем")
    @TestCaseId("1192")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeErrorWhenCreateFolder() {
        String name = getRandomName();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .clicksAndInputsText(st.pages().touch().settings().nameInput(), name)
                .clicksOn(st.pages().touch().settings().editOrSave())
                .shouldSee(st.pages().touch().settings().nameExistError());

        createFolder(name);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть ошибку при создании папки с существующим именем")
    @TestCaseId("1192")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeErrorWhenCreateFolderTablet() {
        String name = getRandomName();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .clicksAndInputsText(st.pages().touch().settings().nameInput(), name)
                .clicksOn(st.pages().touch().settings().saveTablet())
                .shouldSee(st.pages().touch().settings().nameExistError());

        createFolder(name);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть ошибку при создании метки с существующим именем")
    @TestCaseId("1221")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeErrorWhenCreateLabel() {
        String name = getRandomString();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .clicksOn(st.pages().touch().settings().labelTab())
                .clicksOn(st.pages().touch().settings().colors().waitUntil(not(empty())).get(0))
                .clicksAndInputsText(st.pages().touch().settings().nameInput(), name)
                .clicksOn(st.pages().touch().settings().editOrSave())
                .shouldSee(st.pages().touch().settings().nameExistError());

        createLabel(name);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть ошибку при создании метки с существующим именем")
    @TestCaseId("1221")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeErrorWhenCreateLabelTablet() {
        String name = getRandomString();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .clicksOn(st.pages().touch().settings().labelTab())
                .clicksOn(st.pages().touch().settings().colors().waitUntil(not(empty())).get(0))
                .clicksAndInputsText(st.pages().touch().settings().nameInput(), name)
                .clicksOn(st.pages().touch().settings().saveTablet())
                .shouldSee(st.pages().touch().settings().nameExistError());

        createLabel(name);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны создать метку")
    @TestCaseId("1242")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCreateLabel() {
        String labelName = getRandomString();
        int colorNum = getRandomNumber(7, 0);
        Consumer<InitStepsRule> act = st -> {
            st.user().apiLabelsSteps().deleteAllCustomLabels();
            st.user().defaultSteps().refreshPage()
                .clicksOn(st.pages().touch().settings().create())
                .clicksOn(st.pages().touch().settings().labelTab())
                .clicksOn(st.pages().touch().settings().colors().get(colorNum))
                .clicksAndInputsText(st.pages().touch().settings().nameInput(), labelName)
                .clicksOn(st.pages().touch().settings().editOrSave())
                .shouldSee(st.pages().touch().settings().labels())
                .shouldSeeThatElementTextEquals(st.pages().touch().settings().labels().get(0), labelName);
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).runSequentially();
    }

    @Test
    @Title("Должны создать метку")
    @TestCaseId("1242")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldCreateLabelTablet() {
        String labelName = getRandomName();
        int colorNum = getRandomNumber(7, 0);
        Consumer<InitStepsRule> act = st -> {
            st.user().apiLabelsSteps().deleteAllCustomLabels();
            st.user().defaultSteps().refreshPage()
                .clicksOn(st.pages().touch().settings().create())
                .clicksOn(st.pages().touch().settings().labelTab())
                .clicksOn(st.pages().touch().settings().colors().get(colorNum))
                .clicksAndInputsText(st.pages().touch().settings().nameInput(), labelName)
                .clicksOn(st.pages().touch().settings().saveTablet())
                .shouldSee(st.pages().touch().settings().labels())
                .shouldSeeThatElementTextEquals(st.pages().touch().settings().labels().get(0), labelName);
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).runSequentially();
    }

    @Test
    @Title("Экран выбора папки для вложения")
    @TestCaseId("1190")
    public void shouldSeeSubfolderPage() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .clicksOn(st.pages().touch().settings().putInFolder())
                .shouldSee(st.pages().touch().settings().notChosen());

        createFolder();
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Step("Создаём папку")
    private void createFolder() {
        stepsProd.user().apiFoldersSteps().createNewFolder(getRandomName());
    }

    @Step("Создаём папку c именем «{0}»")
    private void createFolder(String name) {
        stepsProd.user().apiFoldersSteps().createNewFolder(name);
    }

    @Step("Создаём метку с именем «{0}»")
    private void createLabel(String name) {
        stepsProd.user().apiLabelsSteps().addNewLabel(name, LABELS_PARAM_GREEN_COLOR);
    }
}
