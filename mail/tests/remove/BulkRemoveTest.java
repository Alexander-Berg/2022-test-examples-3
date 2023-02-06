package ru.yandex.autotests.innerpochta.yfurita.tests.remove;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.*;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.prepareTestFoldersAndLabels;

/**
 * User: petcazay
 * Date: 27.07.17
 * Привязан к конкретному логину
 * У тестового пользователя должны быть
 * Метки "test1", "test2", "11,22". Папка "test1".
 * fUser.createFolder("test1");
 * fUser.createLabel("test1");
 * fUser.createLabel("test2");
 * fUser.createLabel("11,22");
 */
@Feature("Yfurita.BulkRemove")
@Aqua.Test(title = "Тестирование массового удаления фильтров",
        description = "Тестирование массового удаления фильтров")
@Title("BulkRemoveTest.Тестирование массового удаления фильтров")
@Description("Тестирование массового удаления фильтров")
public class BulkRemoveTest {
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
    @Title("Проверяем работу массового удаления фильров XML")
    @Issues({@Issue("MAILDEV-855")})
    @Description("Проверяем работу массового удаления фильров XML")
    public void checkThatAllFiltersWereDeleted() throws Exception {
        fUser.removeFilters(fUser.getAllFilters());
        assertThat(fUser.getAllFilters(), hasSize(equalTo(0)));
    }

    @Test
    @Title("Проверяем работу массового удаления фильров JSON")
    @Issues({@Issue("MAILDEV-855")})
    @Description("Проверяем работу массового удаления фильров JSON")
    public void jsonCheckThatAllFiltersWereDeleted() throws Exception {
        fUser.removeFilters(fUser.getAllFilters(), YFuritaUtils.JSON);
        assertThat(fUser.getAllFilters(), hasSize(equalTo(0)));
    }

    @Test
    @Title("Проверяем удаление несуществующего правила при массовом удалении")
    @Issues({@Issue("MAILDEV-855")})
    @Description("Проверяем, что удаление несуществующего правила при массовом удалении не вредит общему удалению")
    public void checkThatDeletionOfNonexistentRuleIsSafe() throws Exception {
        List<String> rules = new ArrayList<>();
        rules.addAll(fUser.getAllFilters());
        rules.add("1");
        rules.addAll(rules);
        fUser.removeFilters(rules);
        assertThat(fUser.getAllFilters(), hasSize(equalTo(0)));
    }
}