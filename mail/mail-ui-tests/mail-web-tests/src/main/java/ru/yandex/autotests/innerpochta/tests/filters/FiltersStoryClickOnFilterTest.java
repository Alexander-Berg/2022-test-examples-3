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
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на клик по фильтру")
@Stories(FeaturesConst.GENERAL)
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
public class FiltersStoryClickOnFilterTest extends BaseTest {

    private static final String SAME_ADDRESS = "Нельзя создать правило с пересылкой на тот же адрес, на " +
        "котором создается правило.";
    private static final String EMPTY_FIELD = "Поле не заполнено.";
    private static final String WRONG_ADDRESS = "Некорректный адрес электронной почты.";
    private static final String MESSAGE = "Укажите действие, которое нужно выполнить";
    private static final String FILTER_REFACTOR_URL = "#setup/filters-create/id=";

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

    @Before
    public void logIn() throws IOException {
        folder = user.apiFoldersSteps().createNewFolder(Utils.getRandomString());
        user.apiFiltersSteps().createFilterForFolderOrLabel(Utils.getRandomName(), Utils.getRandomName(),
            FILTERS_ADD_PARAM_MOVE_FOLDER, folder.getFid(), FILTERS_ADD_PARAM_CLICKER_MOVE, true
        );
        user.apiFiltersSteps().createFilterForFolderOrLabel(Utils.getRandomName(), Utils.getRandomName(),
            FILTERS_ADD_PARAM_MOVE_FOLDER, folder.getFid(), FILTERS_ADD_PARAM_CLICKER_MOVE, true
        );
        user.apiFiltersSteps().createFilterForFolderOrLabel(Utils.getRandomName(), Utils.getRandomName(),
            FILTERS_ADD_PARAM_MOVE_FOLDER, folder.getFid(), FILTERS_ADD_PARAM_CLICKER_MOVE, true
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS);
    }

    @Test
    @Title("Клик по фильтру")
    @TestCaseId("1297")
    public void testClickOnFilter() {
        user.filtersSteps().clicksOnFilter(0);
        user.defaultSteps().shouldBeOnUrl(containsString(FILTER_REFACTOR_URL))
            .clicksOn(onSettingsPage().blockSettingsNav().filtersSetupLink())
            .shouldSee(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().clicksOnFilter(1);
        user.defaultSteps().shouldBeOnUrl(containsString(FILTER_REFACTOR_URL))
            .clicksOn(onSettingsPage().blockSettingsNav().filtersSetupLink())
            .shouldSee(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().clicksOnFilter(2);
        user.defaultSteps().shouldBeOnUrl(containsString(FILTER_REFACTOR_URL));
    }


    /**
     * Для истории:
     * Раньше был кейс в другой последовательности и ловил странный баг:
     * http://jing.yandex-team.ru/files/lanwen/2013-11-20_0051.swf?w=971&h=749
     */
    @Test
    @Title("Тест на подсказки/оповещения при создании фильтра")
    @TestCaseId("1298")
    public void testFilterCreationPageAlerts() {
        user.filtersSteps().clicksOnCreateNewFilter()
            .confirmsPassword(lock.firstAcc().getPassword())
            .chooseToForwardToAddress((lock.firstAcc().getSelfEmail()) + "\n");

        user.defaultSteps().clicksOn(onFiltersCreationPage().setupFiltersCreate().submitFilterButton())
            .shouldSee(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().emptyFieldNotification())
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().emptyFieldNotification(), EMPTY_FIELD)
            .shouldSee(onFiltersCreationPage().setupFiltersCreate()
                .blockPasswordProtectedActions().sameAddressNotification())
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate()
                .blockPasswordProtectedActions().sameAddressNotification(), SAME_ADDRESS)

            .inputsTextInElement(onFiltersCreationPage().setupFiltersCreate()
                .blockPasswordProtectedActions().forwardToInbox(), "qwerty\n")
            .clicksOn(onFiltersCreationPage().setupFiltersCreate().submitFilterButton())
            .shouldSee(onFiltersCreationPage().setupFiltersCreate()
                .blockPasswordProtectedActions().wrongAddressNotification())
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate()
                .blockPasswordProtectedActions().wrongAddressNotification(), WRONG_ADDRESS)
            .deselects(onFiltersCreationPage().setupFiltersCreate()
                .blockPasswordProtectedActions().forwardToCheckBox());
        user.messagesSteps().clicksOnElementAndChecksStatusLine(onFiltersCreationPage()
            .setupFiltersCreate().submitFilterButton(), MESSAGE);
    }
}
