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
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 07.09.12
 * Time: 15:47
 */

@Aqua.Test
@Title("Тест на создание простого фильтра")
@Stories(FeaturesConst.SIMPLE_FILTERS)
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
public class FiltersStoryCreateNewSimpleFilterTest extends BaseTest {

    private static final String CONDITION_PATTERN = "Если\n«Тема» содержит «%s»\nи\n«От кого» содержит «%s»";
    private static final String LABEL = "CustomLabel";
    private static final String FOLDER = "CustomFolder";

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
        user.apiLabelsSteps().addNewLabel(LABEL, LABELS_PARAM_GREEN_COLOR);
        user.apiFoldersSteps().createNewFolder(FOLDER);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS);
    }

    @Test
    @Title("Тест на создание фильтра на «удаление»")
    @TestCaseId("1314")
    public void testCreateSimpleFilterForDeletingMail() {
        user.defaultSteps().clicksOn(onFiltersOverview().createSimpleFilterForDeleting());
        String subject = user.filtersSteps().inputsSubjectForNewSimpleFilter();
        String address = user.filtersSteps().inputsAddressForNewSimpleFilter();
        user.filtersSteps().submitsSimpleFilter();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FILTERS);
        user.filtersSteps()
            .shouldSeeSelectedConditionInFilter(String.format(CONDITION_PATTERN, subject, address))
            .shouldSeeSelectedActionInFilter("— удалить письмо");
    }

    @Test
    @Title("Тест на создание фильтра на «метку»")
    @TestCaseId("1315")
    public void testCreateSimpleFilterForLabelingMail() {
        user.defaultSteps().clicksOn(onFiltersOverview().createSimpleFilterForLabeling());
        String subject = user.filtersSteps().inputsSubjectForNewSimpleFilter();
        String address = user.filtersSteps().inputsAddressForNewSimpleFilter();
        user.filtersSteps().selectsLabelForFilter(LABEL)
            .submitsSimpleFilter();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FILTERS);
        user.defaultSteps().shouldSee(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().shouldSeeSelectedConditionInFilter(String.format(CONDITION_PATTERN, subject, address))
            .shouldSeeSelectedActionInFilter("— пометить письмо меткой «" + LABEL + "»");
    }

    @Test
    @Title("Тест на создание фильтра на перемещение")
    @TestCaseId("1316")
    public void testCreateSimpleFilterForMovingMail() {
        user.defaultSteps().clicksOn(onFiltersOverview().createSimpleFilterForMoving());
        String subject = user.filtersSteps().inputsSubjectForNewSimpleFilter();
        String address = user.filtersSteps().inputsAddressForNewSimpleFilter();
        user.filtersSteps().selectsFolderForFilter(FOLDER)
            .submitsSimpleFilter();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FILTERS);
        user.defaultSteps().shouldSee(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().shouldSeeSelectedConditionInFilter(String.format(CONDITION_PATTERN, subject, address))
            .shouldSeeSelectedActionInFilter("— переместить письмо в папку «" + FOLDER + "»");
    }
}
