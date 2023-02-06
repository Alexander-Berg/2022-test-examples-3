package ru.yandex.autotests.innerpochta.yfurita.tests.oder;

import org.junit.*;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.*;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.prepareTestFoldersAndLabels;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.FiltersOrderMatcher.hasOrder;

/**
 * User: stassiak
 * Date: 18.02.13
 * У тестового пользователя должны быть
 * Метки "test1", "test2", "11,22". Папка "test1".
 * fUser.createFolder("test1");
 * fUser.createLabel("test1");
 * fUser.createLabel("test2");
 * fUser.createLabel("11,22");
 */
@Aqua.Test(title = "Тестирование запроса oder",
        description = "Тестирование запроса oder")
@Feature("Yfurita.Oder")
public class OrderTest {
    private static FilterUser fUser;
    private static List<String> filters;

    @Credentials(loginGroup = "OrderTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @BeforeClass
    public static void init() throws Exception {
        fUser = new FilterUser(testUser);
        prepareTestFoldersAndLabels(fUser);
    }

    @Before
    public void createFilters() throws Exception {
        fUser.removeAllFilters();
        ArrayList<Object[]> testCases = (ArrayList<Object[]>) FilterSettings.dataForCreateTest(fUser);
        for (Object[] obj : testCases) {
            HashMap<String, String> params = (HashMap<String, String>) obj[0];
            fUser.createFilter(params);
        }
        filters = new ArrayList<>(fUser.getAllFilters());
        assumeThat("Не создались все фильтры?", filters, hasSize(testCases.size()));
    }

    @Test
    public void jsonTestOderChange() throws Exception {
        Collections.shuffle(filters);
        fUser.changeOder(filters, YFuritaUtils.JSON);
        assertThat(fUser.listFilters(false).jsonPath(), hasOrder(equalTo(filters)));
    }
}
