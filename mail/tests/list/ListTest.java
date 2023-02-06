package ru.yandex.autotests.innerpochta.yfurita.tests.list;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.*;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.prepareTestFoldersAndLabels;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.FilterSettingsListMatcher.hasSettingsList;

/**
 * User: stassiak
 * Date: 12.02.13
 * * У тестового пользователя должны быть
 * Метки "test1", "test2", "11,22". Папка "test1".
 * fUser.createFolder("test1");
 * fUser.createLabel("test1");
 * fUser.createLabel("test2");
 * fUser.createLabel("11,22");
 */
@Feature("Yfurita.List")
@Aqua.Test(title = "Тестирование запроса list",
        description = "Тестирование запроса list")
@Title("ListTest.Тестирование запроса list")
@Description("Тестирование запроса list")
public class ListTest {
    private static FilterUser fUser;
    private static HashMap<String, HashMap<String, String>> filtersSettingsList
            = new HashMap<String, HashMap<String, String>>();

    @Credentials(loginGroup = "ListTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @BeforeClass
    public static void initUser() throws Exception {
        fUser = new FilterUser(testUser);
        prepareTestFoldersAndLabels(fUser);
        fUser.removeAllFilters();

        ArrayList<Object[]> testCases = (ArrayList<Object[]>) FilterSettings.dataForCreateTest(fUser);
        for (Object[] obj : testCases) {
            HashMap<String, String> params = (HashMap<String, String>) obj[0];
            filtersSettingsList.put(fUser.createFilter(params), params);
            params.remove("db");
            params.remove("uid");
            params.remove("oder");
            params.remove("user");
            if (params.get("attachment").equals("")) {
                params.remove("attachment");
            }
            params.remove("lang");
            params.remove("confirm_domain");
            params.remove("auth_domain");
            params.remove("from");
        }
    }

    @Test
    public void jsonTestList() throws Exception {
        assertThat(fUser.listFilters(false, YFuritaUtils.JSON).jsonPath(),
                hasSettingsList(equalTo(FilterSettings.getShortFiltersSettingsList(filtersSettingsList))));
    }

    @Test
    public void jsonTestDetailedList() throws Exception {
        assertThat(fUser.listFilters(true, YFuritaUtils.JSON).jsonPath(),
                hasSettingsList(equalTo(filtersSettingsList)));
    }

    @AfterClass
    public static void deleteFilters() throws Exception {
        for (String filterId : filtersSettingsList.keySet()) {
            fUser.removeFilter(filterId);
        }
    }
}
