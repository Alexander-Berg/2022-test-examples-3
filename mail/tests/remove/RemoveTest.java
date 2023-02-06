package ru.yandex.autotests.innerpochta.yfurita.tests.remove;

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

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.prepareTestFoldersAndLabels;

/**
 * User: stassiak
 * Date: 14.02.13
 * Привязан к конкретному логину
 * У тестового пользователя должны быть
 * Метки "test1", "test2", "11,22". Папка "test1".
 * fUser.createFolder("test1");
 * fUser.createLabel("test1");
 * fUser.createLabel("test2");
 * fUser.createLabel("11,22");
 */
@Feature("Yfurita.Remove")
@Aqua.Test(title = "Тестирование удаления фильтров",
        description = "Тестирование удаления фильтров")
@Title("RemoveTest.Тестирование удаления фильтров")
@Description("Тестирование удаления фильтров")
public class RemoveTest {
    private static FilterUser fUser;
    private static ArrayList<Object[]> testCases;

    @Credentials(loginGroup = "BulkRemoveTest")
    public static User testUser;

    @Rule
    public LogConfigRule logRule = new LogConfigRule();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();

    @BeforeClass
    public static void init() throws Exception {
        fUser = new FilterUser(testUser);
        prepareTestFoldersAndLabels(fUser);
        testCases = (ArrayList<Object[]>) FilterSettings.dataForCreateTest(fUser);
    }

    @Before
    public void createFilters() throws Exception {
        fUser.removeAllFilters();
        for (Object[] obj : testCases) {
            HashMap<String, String> params = (HashMap<String, String>) obj[0];
            fUser.createFilter(params);
        }
        assumeThat(fUser.getAllFilters(), hasSize(equalTo(testCases.size())));
    }

    @Test
    @Title("Проверяем работу  удаления фильров по одному XML")
    @Description("Проверяем работу  удаления фильров  по одному XML")
    public void checkThatAllFiltersWereDeleted() throws Exception {
        for (String filterId : fUser.getAllFilters()) {
            fUser.removeFilter(filterId);
        }
        assertThat(fUser.getAllFilters(), hasSize(equalTo(0)));
    }

    @Test
    @Title("Проверяем работу  удаления фильров по одному JSON")
    @Description("Проверяем работу  удаления фильров  по одному JSON")
    public void jsonCheckThatAllFiltersWereDeleted() throws Exception {
        for (String filterId : fUser.getAllFilters()) {
            fUser.removeFilter(filterId, YFuritaUtils.JSON);
        }
        assertThat(fUser.getAllFilters(), hasSize(equalTo(0)));
    }
}