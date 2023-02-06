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
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на редактирование папок и меток")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
@RunWith(DataProviderRunner.class)
public class EditFoldersAndLabelsScreenTest {

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
    @Title("Должны увидеть страницу в режиме редактирования")
    @TestCaseId("1193")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeFolderAndLabelEditable() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().editOrSave())
                .shouldSee(st.pages().touch().settings().editElement());

        createLabel();
        createFolder();
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть страницу в режиме редактирования")
    @TestCaseId("1193")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeFolderAndLabelEditableTablet() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().editTablet())
                .shouldSee(st.pages().touch().settings().editElement());

        createLabel();
        createFolder();
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть страницу редактирования папки/метки")
    @TestCaseId("1194")
    @DataProvider({"0", "1"})
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeEditingFolderAndLabel(int num) {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().editOrSave())
                .clicksOn(st.pages().touch().settings().editElement().get(num))
                .shouldSee(st.pages().touch().settings().nameInput());

        createLabel();
        createFolder();
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть страницу редактирования папки/метки")
    @TestCaseId("1194")
    @DataProvider({"0", "1"})
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeEditingFolderAndLabelTablet(int num) {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().editTablet())
                .clicksOn(st.pages().touch().settings().editElement().get(num))
                .shouldSee(st.pages().touch().settings().nameInput());

        createLabel();
        createFolder();
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть ошибку при смене имени папки/метки на то, которое уже существует")
    @TestCaseId("1197")
    @DataProvider({"1", "2"})
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeErrorWhenRename(int num) {
        String name = getRandomString();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().editOrSave())
                .clicksOn(st.pages().touch().settings().editElement().waitUntil(not(empty())).get(num))
                .clicksAndInputsText(st.pages().touch().settings().nameInput(), name)
                .clicksOn(st.pages().touch().settings().editOrSave())
                .shouldSee(st.pages().touch().settings().nameExistError());

        createLabel();
        createLabel(name);
        createFolder();
        createFolder(name);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(QuickFragments.SETTINGS_TOUCH_PART.makeTouchUrlPart(FOLDER_LABELS_URLPART)).run();
    }

    @Test
    @Title("Должны увидеть ошибку при смене имени папки/метки на то, которое уже существует")
    @TestCaseId("1197")
    @DataProvider({"1", "2"})
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeErrorWhenRenameTablet(int num) {
        String name = getRandomString();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().editTablet())
                .clicksOn(st.pages().touch().settings().editElement().waitUntil(not(empty())).get(num))
                .clicksAndInputsText(st.pages().touch().settings().nameInput(), name)
                .clicksOn(st.pages().touch().settings().saveTablet())
                .shouldSee(st.pages().touch().settings().nameExistError());

        createLabel();
        createLabel(name);
        createFolder();
        createFolder(name);
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

    @Step("Создаём метку")
    private void createLabel() {
        stepsProd.user().apiLabelsSteps().addNewLabel(getRandomName(), LABELS_PARAM_GREEN_COLOR);
    }

    @Step("Создаём метку с именем «{0}»")
    private void createLabel(String name) {
        stepsProd.user().apiLabelsSteps().addNewLabel(name, LABELS_PARAM_GREEN_COLOR);
    }
}
