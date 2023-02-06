package ru.yandex.autotests.innerpochta.tests.filters;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVEL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_LABEL;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 07.09.12
 * Time: 15:47
 */

@Aqua.Test
@Title("Тест на создание простого фильтра для пустой метки")
@RunWith(Parameterized.class)
@Stories({FeaturesConst.SIMPLE_FILTERS})
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
public class FiltersStoryCreateSimpleFilterForLabelTest extends BaseTest {

    private static final String MARK_ACTION = "— пометить письмо меткой «%s»";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private Label label;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Parameterized.Parameter(0)
    public String address;
    @Parameterized.Parameter(1)
    public String subject;
    @Parameterized.Parameter(2)
    public Boolean containsAttachments;

    @Parameterized.Parameters(name = "address: {0}, subject: {1}, attachments: {2}")
    public static Collection<Object[]> testData() {
        Object[][] data = new Object[][]{
            {Utils.getRandomString(), Utils.getRandomString(), true},
            {Utils.getRandomString(), Utils.getRandomString(), false},
            {Utils.getRandomString(), "", true},
            {"", Utils.getRandomString(), true},
            {Utils.getRandomString(), "", false},
            {"", Utils.getRandomString(), false},
        };
        return Arrays.asList(data);
    }

    @Before
    public void logIn() throws IOException {
        label = user.apiLabelsSteps().addNewLabel(Utils.getRandomString(), LABELS_PARAM_GREEN_COLOR);
        user.apiFiltersSteps().createFilterForFolderOrLabel(address, subject,
            FILTERS_ADD_PARAM_MOVE_LABEL, label.getLid(), FILTERS_ADD_PARAM_CLICKER_MOVEL, containsAttachments
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS);
    }

    @Test
    @Title("Тест на создание фильтра для кастомной метки")
    @TestCaseId("1318")
    public void testCreateNewSimpleFilterForCustomMark() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FILTERS)
            .shouldSee(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().shouldSeeSelectedConditionInFilter(address, subject)
            .shouldSeeSelectedActionInFilter(String.format(MARK_ACTION, label.getName()))
            .clicksOnFilter(0)
            .shouldSeeCorrectDataInFilterConfigurationPage(containsAttachments, address, subject);
    }
}
