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
import ru.yandex.autotests.innerpochta.steps.beans.filter.Filter;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;

/**
 * Created by mabelpines on 15.03.16.
 */
@Aqua.Test
@Stories(FeaturesConst.EDIT_FILTERS)
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
@Title("Удаление пользовательской папки, связанной с фильтром")
public class RemoveCustomFolderWithFilterTest extends BaseTest {

    private Folder folder;
    private Filter filter;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        folder = user.apiFoldersSteps().createNewFolder(Utils.getRandomString());
        filter = user.apiFiltersSteps().createFilterForFolderOrLabel(Utils.getRandomString(),
            Utils.getRandomString(), FILTERS_ADD_PARAM_MOVE_FOLDER, folder.getFid(),
            FILTERS_ADD_PARAM_CLICKER_MOVE, true
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @TestCaseId("3282")
    @Title("Фильтр должен удаляться вместе с удалением пользовательской папки")
    public void shouldDeleteFilterWhenDeleteCustomFolder() {
        user.settingsSteps().clicksOnFolder(folder.getName())
            .clicksOnDeleteFolder();
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().deleteFolderPopUpOld().confirmDelete())
            .opensFragment(QuickFragments.SETTINGS_FILTERS);
        user.filtersSteps().shouldNotSeeCreatedFilter(filter.getName());
    }
}
