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
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на создание фильтра для новой метки и папки")
@Stories(FeaturesConst.GENERAL)
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
public class FiltersStoryCreateFilterForMarksAndFolders extends BaseTest {

    private static final String MOVE_ACTION_PATTERN = "— переместить письмо в папку «%s»";
    private static final String MARK_ACTION_PATTERN = "— пометить письмо меткой «%s»";

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
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @Title("Тест на создание фильтра для пользовательской метки")
    @TestCaseId("1306")
    public void testCreateNewAdvancedFilterForCustomMark() {
        String markName = Utils.getRandomString();
        user.apiLabelsSteps().addNewLabel(markName, LABELS_PARAM_GREEN_COLOR);
        user.defaultSteps().opensDefaultUrl()
            .clicksIfCanOn(onMessagePage().showLabels())
            .clicksOn(onMessagePage().labelsNavigation().userLabels().get(0).labelName())
            .clicksOn(onHomePage().putMarkAutomaticallyButton())
            .clicksOn(onFiltersOverview().newFilterPopUp().createComplexFilterLink());
        String condition = user.filtersSteps().chooseRandomIfConditionForNewFilter();
        user.filtersSteps().submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(condition)
            .shouldSeeSelectedActionInFilter(
                String.format(MARK_ACTION_PATTERN, markName),
                "— пометить письмо как прочитанное"
            );

    }

    @Test
    @Title("Тест на создание фильтра для пользовательской папки")
    @TestCaseId("1307")
    public void testCreateNewAdvancedFilterForCustomFolder() {
        String folderName = Utils.getRandomString();
        user.apiFoldersSteps().createNewFolder(folderName);
        user.defaultSteps().refreshPage();
        user.settingsSteps().clicksOnFolder(folderName);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().activeCreateFilterButton());
        user.filtersSteps().clicksOnMoreComplexFilterLink();
        String condition = user.filtersSteps().chooseRandomIfConditionForNewFilter();
        user.filtersSteps().submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(condition)
            .shouldSeeSelectedActionInFilter(String.format(MOVE_ACTION_PATTERN, folderName));
    }
}
