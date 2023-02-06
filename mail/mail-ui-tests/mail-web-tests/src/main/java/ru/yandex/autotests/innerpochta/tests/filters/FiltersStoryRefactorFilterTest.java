package ru.yandex.autotests.innerpochta.tests.filters;


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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на изменение имени правила и включение галки “Не применять остальные правила“")
@Stories(FeaturesConst.EDIT_FILTERS)
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
public class FiltersStoryRefactorFilterTest extends BaseTest {

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
    public void logIn() throws IOException {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS);
        user.defaultSteps().clicksOn(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().chooseRandomIfConditionForNewFilter();
        user.defaultSteps().turnTrue(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().deleteCheckBox());

    }

    @Test
    @Title("Переименование фильтра")
    @TestCaseId("1319")
    public void testRenameFilter() {
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.defaultSteps().clicksOn(onFiltersOverview().createdFilterBlocks().get(0).refactorFilter());
        String name = getRandomString();
        user.filtersSteps().changesFilterName(name)
            .submitsFilter(lock.firstAcc())
            .shouldSeeCorrectFilterName(name);
    }

    @Test
    @Title("Применять остальные фильтры")
    @TestCaseId("1320")
    public void doNotApplyOtherFilters() {
        user.defaultSteps().turnTrue(onFiltersCreationPage().setupFiltersCreate().dontApplyAnyOtherFilter());
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.defaultSteps().clicksOn(onFiltersOverview().createdFilterBlocks().get(0).refactorFilter());
        user.filtersSteps().shouldSeeDoNotApplyOtherFiltersCheckBoxEnabled();
    }

    @Test
    @Title("Включение и отключение фильтра")
    @TestCaseId("1321")
    public void switchOnAndOffFilters() {
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.defaultSteps().shouldSee(onFiltersOverview().createdFilterBlocks().get(0).switchOff())
            .clicksOn(onFiltersOverview().createdFilterBlocks().get(0).switchOff())
            .shouldSee(onFiltersOverview().createdFilterBlocks().get(0).switchOn())
            .clicksOn(onFiltersOverview().createdFilterBlocks().get(0).switchOn())
            .shouldSee(onFiltersOverview().createdFilterBlocks().get(0).switchOff());
    }

    @Test
    @Title("Изменение групп к которым применяется фильтр")
    @TestCaseId("1322")
    public void selectGroupOfMessagesForFilter() {
        String firstGroup = user.filtersSteps().selectsGroupOfMessagesFromFirstDropBox();
        String secondGroup = user.filtersSteps().selectsGroupOfMessagesFromSecondDropBox();
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.defaultSteps().clicksOn(onFiltersOverview().createdFilterBlocks().get(0).refactorFilter());
        user.filtersSteps().shouldSeeOptionsSelected(firstGroup, secondGroup);
    }

    @Test
    @Title("Редактирование правила с несколькими условиями")
    @TestCaseId("5410")
    public void changeFilterConditions() {
        String msgSbj = getRandomString();
        user.apiFoldersSteps().purgeFolder(user.apiFoldersSteps().getFolderBySymbol(INBOX));
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc().getSelfEmail(), msgSbj, "");
        user.defaultSteps()
            .clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
                .addConditionButton());
        user.filtersSteps().shouldOpenFromDropdown(1);
        user.defaultSteps().clicksOn(user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(1))
            .inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(1).inputCondition(), getRandomString());
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.defaultSteps().clicksOn(onFiltersOverview().createdFilterBlocks().get(0).refactorFilter())
            .clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().previewButton())
            .shouldNotSee(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().previewMessagesListHeader())
            .inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(0).inputCondition(), lock.firstAcc().getSelfEmail());
        user.filtersSteps().shouldOpenFromDropdown(1);
        user.defaultSteps().clicksOn(user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(4))
            .inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(1).inputCondition(), msgSbj)
            .clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().previewButton())
            .shouldSee(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().previewMessagesListHeader());
        user.filtersSteps().submitsFilter(lock.firstAcc());
    }
}
