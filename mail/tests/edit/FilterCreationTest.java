package ru.yandex.autotests.innerpochta.yfurita.tests.edit;

import com.jayway.restassured.response.Response;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.prepareTestFoldersAndLabels;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.FilterSettingsMatcher.hasSettings;

/**
 * User: stassiak
 * Date: 05.02.13
 * <p/>
 * У тестового пользователя должны быть
 * Метки "test1", "test2", "11,22". Папка "test1".
 */
@Feature("Yfurita.Edit")
@Aqua.Test(title = "Тестирование создания фильтров",
        description = "Тестирование запроса edit без параметра id")
@Title("FilterCreationTest.Тестирование создания фильтров")
@Description("Тестирование запроса edit без параметра id")
@RunWith(Parameterized.class)
public class FilterCreationTest {
    private static FilterUser fUser;
    private String filterId;

    @Credentials(loginGroup = "FilterCreationTest")
    public static User testUser;

    @Parameterized.Parameter(0)
    public HashMap<String, String> params;
    @Parameterized.Parameter(1)
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
        userInitializationRule.initUsers(FilterCreationTest.class.getFields());
        fUser = new FilterUser(testUser);
        prepareTestFoldersAndLabels(fUser);
        ArrayList<Object[]> dataForCreate = (ArrayList<Object[]>) FilterSettings.dataForCreateTest(fUser);
        ArrayList<Object[]> testCases = new ArrayList<>();

        for (int i = 0; i < dataForCreate.size(); i++) {
            HashMap<String, String> parameters = (HashMap<String, String>) dataForCreate.get(i)[0];
            testCases.add(new Object[]{new HashMap<>(parameters), YFuritaUtils.JSON});
        }
        return testCases;
    }

    @Before
    public void createFilter() throws Exception {
        System.out.println("++++++++++++++++++++");
        System.out.println(params);
        System.out.println("======================");
        filterId = fUser.createFilter(params, format).trim();

        params.remove("db");
        params.remove("oder");
        params.remove("user");
        params.remove("uid");

        if (params.get("attachment").equals("")) {
            params.remove("attachment");
        }
        params.remove("lang");
        params.remove("confirm_domain");
        params.remove("auth_domain");
        params.remove("from");
    }

    @Test
    public void testFilterWasCreatedCorrectly() throws Exception {
        Response response = fUser.listFilter(filterId, true, format);
        assertThat(response.jsonPath(), hasSettings(equalTo(params)));
    }

    @After
    public void deleteFilter() throws Exception {
        fUser.removeFilter(filterId, format);
    }
}
