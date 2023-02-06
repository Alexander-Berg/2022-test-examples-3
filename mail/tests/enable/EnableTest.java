package ru.yandex.autotests.innerpochta.yfurita.tests.enable;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.*;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;

/**
 * User: stassiak
 * Date: 19.02.13
 */
@Feature("Yfurita.Enable")
@Aqua.Test(title = "Тестирование включения фильтров",
        description = "Тестирование включения фильтров")
@Title("EnableTest.Тестирование включения фильтров")
@Description("Тестирование включения фильтров")
public class EnableTest {
    private static FilterUser fUser;
    private String filterId;

    @Credentials(loginGroup = "EnableTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @BeforeClass
    public static void createFilters() throws Exception {
        fUser = new FilterUser(testUser);
        fUser.removeAllFilters();
    }

    @Before
    public void createFilter() throws Exception {
        FilterSettings filterSettings = new FilterSettings();
        filterSettings.setLetter(LETTER_NOSPAM);
        filterSettings.setOrder(ORDER_FIRST);
        filterSettings.setStop(STOP_YES);
        filterSettings.setField3("111, 222");

        filterId = fUser.createFilter(filterSettings.getParams());
    }

    @Test
    public void testFilterIsEnabledByDefault() throws Exception {
        assertThat("Фильтр должен быть включен по-умолчанию!",
                fUser.getFilter(filterId, false).get("enabled"), equalTo(true));
    }

    @Test
    public void testFilterEnable() throws Exception {
        fUser.disableFilter(filterId);
        fUser.enableFilter(filterId);
        assertThat(fUser.getFilter(filterId, false).get("enabled"), equalTo(true));
    }

    @Test
    public void jsonTestFilterEnable() throws Exception {
        fUser.disableFilter(filterId);
        fUser.enableFilter(filterId, YFuritaUtils.JSON);
        assertThat(fUser.getFilter(filterId, false).get("enabled"), equalTo(true));
    }

    @After
    public void deleteFilter() throws Exception {
        fUser.removeFilter(filterId);
    }
}
