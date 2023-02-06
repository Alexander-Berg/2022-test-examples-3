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
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMPORTANT_LABEL_NAME_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVEL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_LABEL;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

@Aqua.Test
@Title("Тест на создание фильтров со страницы папок и меток")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsStoryCreateFilterTest extends BaseTest {

    private static final String USER_FOLDER = "folder";
    private static final String USER_LABEL = "label";

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
        user.apiFiltersSteps().deleteAllUserFilters();
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @Title("Попытка создать фильтр с неполными данными для пользовательской папки")
    @TestCaseId("1757")
    public void testAttemptToCreateFilterForFolderWithInsufficientDataFromFoldersAndLabelsSettings() {
        user.apiFoldersSteps().createNewFolder(USER_FOLDER);
        user.defaultSteps().refreshPage()
            .onMouseHoverAndClick(onFoldersAndLabelsSetup().setupBlock().folders().inboxFolderCounter())
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().activeCreateFilterButton())
            .clicksOn(onFiltersOverview().newFilterPopUp().submitFilterButton())
            .shouldSee(onFiltersOverview().newFilterPopUp().emptyNotification())
            .clicksOn(onFiltersOverview().newFilterPopUp().cancelButton());
        user.settingsSteps().clicksOnFolder(USER_FOLDER);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().activeCreateFilterButton())
            .clicksOn(onFiltersOverview().newFilterPopUp().submitFilterButton())
            .shouldSee(onFiltersOverview().newFilterPopUp().emptyNotification());
    }

    @Test
    @Title("Создание расширенного фильтра для папки")
    @TestCaseId("1752")
    public void testComplexFilterForFolderLinkFromFoldersAndLabelsSettings() {
        user.apiFoldersSteps().createNewFolder(USER_FOLDER);
        user.defaultSteps().refreshPage();
        user.settingsSteps().clicksOnFolder(USER_FOLDER);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().activeCreateFilterButton())
            .clicksOn(onFiltersOverview().newFilterPopUp().createComplexFilterLink())
            .shouldBeOnUrl(containsString("#setup/filters-create/folder="))
            .shouldSeeCheckBoxesInState(
                true,
                onFiltersCreationPage().setupFiltersCreate().blockSelectAction().moveToFolderCheckBox()
            )
            .shouldContainText(onFiltersCreationPage().setupFiltersCreate().blockSelectAction()
                .selectFolderDropdown(), USER_FOLDER);
    }

    @Test
    @Title("Создание фильтра для папки")
    @TestCaseId("1756")
    public void testCreateFilterForFolderFromFoldersAndLabelsSettings() {
        String address = Utils.getRandomString();
        String subject = Utils.getRandomString();
        Folder folder = user.apiFoldersSteps().createNewFolder(USER_FOLDER);
        user.apiFiltersSteps().createsFilterAndGoesToRefactorPage(
            address,
            subject,
            FILTERS_ADD_PARAM_MOVE_FOLDER,
            folder.getFid(),
            FILTERS_ADD_PARAM_CLICKER_MOVE
        );
        user.defaultSteps().shouldSeeCheckBoxesInState(
            true,
            onFiltersCreationPage().setupFiltersCreate().blockSelectAction().moveToFolderCheckBox()
        );
        user.defaultSteps().shouldContainText(
            onFiltersCreationPage().setupFiltersCreate().blockSelectAction()
                .selectFolderDropdown(),
            USER_FOLDER
        );
        checksFilterConditions(address, subject);

    }

    @Test
    @Title("Попытка создать фильтр с неполными данными для пользовательской метки")
    @TestCaseId("1753")
    public void testAttemptToCreateFilterForLabelWithInsufficientDataFromFoldersAndLabelsSettings() {
        user.apiLabelsSteps().addNewLabel(USER_LABEL, LABELS_PARAM_GREEN_COLOR);
        user.defaultSteps().refreshPage()
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().userLabelsList().get(0))
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().createFilterForLabel())
            .clicksOn(onFiltersOverview().newFilterPopUp().submitFilterButton())
            .shouldSee(onFiltersOverview().newFilterPopUp().emptyNotification())
            .clicksOn(onFiltersOverview().newFilterPopUp().cancelButton())
            .clicksOnElementWithText(
                onFoldersAndLabelsSetup().setupBlock().labels().defaultLabels(),
                IMPORTANT_LABEL_NAME_RU
            )
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().createFilterForLabel())
            .clicksOn(onFiltersOverview().newFilterPopUp().submitFilterButton())
            .shouldSee(onFiltersOverview().newFilterPopUp().emptyNotification());
    }

    @Test
    @Title("Создание расширенного фильтра для метки")
    @TestCaseId("1751")
    public void testComplexFilterForLabelLinkFromFoldersAndLabelsSettings() {
        user.apiLabelsSteps().addNewLabel(USER_LABEL, LABELS_PARAM_GREEN_COLOR);
        user.defaultSteps().refreshPage()
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().userLabelsList().get(0))
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().createFilterForLabel())
            .clicksOn(onFiltersOverview().newFilterPopUp().createComplexFilterLink())
            .shouldBeOnUrl(containsString("#setup/filters-create/label=label"))
            .shouldSeeCheckBoxesInState(
                true,
                onFiltersCreationPage().setupFiltersCreate().blockSelectAction().markAsCheckBox()
            )
            .shouldContainText(
                onFiltersCreationPage().setupFiltersCreate().blockSelectAction().selectLabelDropdown(),
                USER_LABEL
            );
    }

    @Test
    @Title("Создание фильтра для метки")
    @TestCaseId("1754")
    public void testCreateFilterForLabelFromFoldersAndLabelsSettings() {
        String address = Utils.getRandomString();
        String subject = Utils.getRandomString();
        Label label = user.apiLabelsSteps().addNewLabel(USER_LABEL, LABELS_PARAM_GREEN_COLOR);
        user.apiFiltersSteps().createsFilterAndGoesToRefactorPage(
            address,
            subject,
            FILTERS_ADD_PARAM_MOVE_LABEL,
            label.getLid(),
            FILTERS_ADD_PARAM_CLICKER_MOVEL
        );
        user.defaultSteps().shouldSeeCheckBoxesInState(
            true,
            onFiltersCreationPage().setupFiltersCreate().blockSelectAction().markAsCheckBox()
        );
        checksFilterConditions(address, subject);
        user.defaultSteps().shouldContainText(
            onFiltersCreationPage().setupFiltersCreate().blockSelectAction().selectLabelDropdown(),
            USER_LABEL
        );
    }

    @Test
    @Title("Должны видеть в настройках кнопку “Создать правило“")
    @TestCaseId("1755")
    public void testShouldSeeCreateNewFilterButton() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS);
        user.settingsSteps().clicksOnRulesOfFilteringLink();
    }

    private void checksFilterConditions(String address, String subject) {
        System.out.println(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList()
            .get(0).inputCondition().getAttribute("value"));
        user.defaultSteps()
            .shouldContainValue(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList()
                    .get(0).inputCondition(),
                subject
            )
            .shouldContainText(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList()
                    .get(0).firstConditionDropDown(),
                "Тема"
            )
            .shouldContainText(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList()
                    .get(0).secondConditionDropDown(),
                "содержит"
            )
            .shouldContainValue(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList()
                    .get(1).inputCondition(),
                address
            )
            .shouldContainText(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList()
                    .get(1).firstConditionDropDown(),
                "От кого"
            )
            .shouldContainText(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList()
                    .get(1).secondConditionDropDown(),
                "содержит"
            )
            .shouldContainText(
                onFiltersCreationPage().setupFiltersCreate().blockApplyConditionFor()
                    .withAttachConditionDropdown(),
                "с вложениями"
            );
        user.filtersSteps().shouldSeeLogicForFilterConditions("выполняются все условия одновременно");
    }
}
