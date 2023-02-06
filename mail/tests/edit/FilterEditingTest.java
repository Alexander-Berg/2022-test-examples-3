package ru.yandex.autotests.innerpochta.yfurita.tests.edit;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.tests.preview.LengthTest;
import ru.yandex.autotests.innerpochta.yfurita.util.*;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.prepareTestFoldersAndLabels;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.FilterSettingsMatcher.hasSettings;

/**
 * User: stassiak
 * Date: 12.02.13
 * У тестового пользователя должны быть
 * Метки "test1", "test2", "11,22". Папка "test1".
 * fUser.createFolder("test1");
 * fUser.createLabel("test1");
 * fUser.createLabel("test2");
 * fUser.createLabel("11,22");
 */

@Feature("Yfurita.Edit")
@Aqua.Test(title = "Тестирование редактирования фильтров",
        description = "Тестирование запроса edit c параметром id")
@Title("FilterEditingTest.Тестирование редактирования фильтров")
@Description("Тестирование запроса edit c параметром id")
@RunWith(Parameterized.class)
public class FilterEditingTest {
    private static FilterUser fUser;
    private String filterId;

    @Credentials(loginGroup = "FilterEditingTest")
    public static User testUser;

    @Parameterized.Parameter(0)
    public HashMap<String, String> createParams;
    @Parameterized.Parameter(1)
    public HashMap<String, String> editParams;
    @Parameterized.Parameter(2)
    public String format;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule;
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        userInitializationRule = new UserInitializationRule();
        userInitializationRule.initUsers(FilterEditingTest.class.getFields());

        fUser = new FilterUser(testUser);
        prepareTestFoldersAndLabels(fUser);
        fUser.removeAllFilters();

        ArrayList<Object[]> dataForEdit = (ArrayList<Object[]>) FilterSettings.dataForEditTest(fUser);
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();

        for (int i = 0; i < dataForEdit.size(); i++) {
            HashMap<String, String> editParameters = (HashMap<String, String>) dataForEdit.get(i)[1];
            HashMap<String, String> createParameters = (HashMap<String, String>) dataForEdit.get(i)[0];
            testCases.add(new Object[]{new HashMap<>(createParameters),
                    new HashMap<>(editParameters), YFuritaUtils.JSON});
        }
        return testCases;
    }

    @Before
    public void createFilter() throws Exception {
        filterId = fUser.createFilter(createParams);
        filterId = fUser.editFilter(editParams, filterId, format);

        editParams.remove("db");
        editParams.remove("uid");
        editParams.remove("oder");
        editParams.remove("user");
        editParams.remove("id");
        if (editParams.get("attachment").equals("")) {
            editParams.remove("attachment");
        }
        editParams.remove("lang");
        editParams.remove("confirm_domain");
        editParams.remove("auth_domain");
        editParams.remove("from");
    }

    @Test
    public void testFilterWasCreatedCorrectly() throws Exception {
        JsonPath listResp = fUser.listFilter(filterId, true, YFuritaUtils.JSON).jsonPath();
        assertThat(listResp, hasSettings(equalTo(editParams)));
    }

    @After
    public void deleteFilter() throws Exception {
        fUser.removeFilter(filterId);
    }
}
