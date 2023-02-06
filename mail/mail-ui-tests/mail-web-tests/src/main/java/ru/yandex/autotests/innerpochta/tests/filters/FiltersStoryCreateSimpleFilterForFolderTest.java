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
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 07.09.12
 * Time: 15:47
 */

@Aqua.Test
@Title("Тест на создание простого фильтра для пустой папки")
@RunWith(Parameterized.class)
@Stories(FeaturesConst.SIMPLE_FILTERS)
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
public class FiltersStoryCreateSimpleFilterForFolderTest extends BaseTest {

    private static final String MOVE_ACTION_PATTERN = "— переместить письмо в папку «folder»";
    private static final String FOLDER_NAME = "folder";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private Folder folder;

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
            {Utils.getRandomName(), Utils.getRandomName(), true},
            {Utils.getRandomName(), Utils.getRandomName(), false},
            {Utils.getRandomName(), "", true},
            {"", Utils.getRandomName(), true},
            {Utils.getRandomName(), "", false},
            {"", Utils.getRandomName(), false},
        };
        return Arrays.asList(data);
    }

    @Before
    public void logIn() throws IOException {
        folder = user.apiFoldersSteps().createNewFolder(FOLDER_NAME);
        user.apiFiltersSteps().createFilterForFolderOrLabel(address, subject, FILTERS_ADD_PARAM_MOVE_FOLDER,
            folder.getFid(), FILTERS_ADD_PARAM_CLICKER_MOVE, containsAttachments
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS);
    }

    @Test
    @Title("Тест на создание фильтра для пустой папки")
    @Description("Проверяется, что условие и выбранное действие отображаются верно")
    @TestCaseId("1317")
    public void testCreateNewSimpleFilterForCustomFolder() {
        user.defaultSteps().shouldSee(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().shouldSeeSelectedConditionInFilter(address, subject)
            .shouldSeeSelectedActionInFilter(MOVE_ACTION_PATTERN)
            .clicksOnFilter(0)
            .shouldSeeCorrectDataInFilterConfigurationPage(containsAttachments, address, subject);
    }
}
