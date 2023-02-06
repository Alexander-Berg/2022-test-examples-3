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

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;

/**
 * User: stassiak
 * Date: 19.02.13
 */
@Feature("Yfurita.Enable")
@Aqua.Test(title = "Тестирование выключения фильтров",
        description = "Тестирование выключения фильтров")
@Title("DisableTest.Тестирование выключения фильтров")
@Description("Тестирование выключения фильтров")
public class DisableTest {
    private static FilterUser fUser;
    private String filterId;

    @Credentials(loginGroup = "DisableTest")
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
        filterSettings.setLogic(LOGIC_OR);
        filterSettings.setClicker(CLIKER_REPLY);
        filterSettings.setAutoAnswer(randomAlphanumeric(7));
        filterSettings.setStop(STOP_YES);

        filterId = fUser.createFilter(filterSettings.getParams());
    }

    @Test
    public void testFilterDisable() throws Exception {
        fUser.disableFilter(filterId);
        assertThat(fUser.getFilter(filterId, false).get("enabled"), equalTo(false));
    }

    @Test
    public void jsonTestFilterDisable() throws Exception {
        fUser.disableFilter(filterId, YFuritaUtils.JSON);
        assertThat(fUser.getFilter(filterId, false).get("enabled"), equalTo(false));
    }

    @After
    public void deleteFilter() throws Exception {
        fUser.removeFilter(filterId);
    }
}
