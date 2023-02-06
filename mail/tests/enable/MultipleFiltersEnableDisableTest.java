package ru.yandex.autotests.innerpochta.yfurita.tests.enable;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.tests.edit.FilterEditingTest;
import ru.yandex.autotests.innerpochta.yfurita.util.Credentials;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.UserInitializationRule;
import ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.createFilterWithHttpClient;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;

/**
 * User: alex89
 * Date: 14.01.15
 * https://st.yandex-team.ru/DARIA-29644  MPROTO-1118
 */
@Feature("Yfurita.Enable")
@Aqua.Test(title = "Тестирование включения-выключения фильтров с несколькими условиями и действиями",
        description = "Тестирование включения-выключения фильтров с несколькими условиями и действиями")
@Title("MultipleFiltersEnableDisableTest.Тестирование включения-выключения фильтров с несколькими " +
        "условиями и действиями")
@Description("Тестирование включения-выключения фильтров с несколькими условиями и действиями")
@RunWith(value = Parameterized.class)
public class MultipleFiltersEnableDisableTest {
    private static FilterUser fUser;
    private Logger log = LogManager.getLogger(this.getClass());
    private String filterId;

    @Credentials(loginGroup = "MultipleFiltersEnableDisableTest")
    public static User testUser;

    @Parameterized.Parameter(0)
    public String params;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule;
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        userInitializationRule = new UserInitializationRule();
        userInitializationRule.initUsers(MultipleFiltersEnableDisableTest.class.getFields());
        fUser = new FilterUser(testUser);
        fUser.removeAllFilters();
        String labelId = fUser.createLabel("LABEL_" + randomAlphabetic(10));
        String folderId = fUser.createFolder("FOLDER_" + randomAlphabetic(10));

        String filter1 = new StringBuilder().append("?attachment=")
                .append("&field1=from").append("&field2=4").append("&field3=yantester")
                .append("&field1=subject").append("&field2=4").append("&field3=yantester")
                .append("&logic=1")
                .append("&clicker=movel").append("&move_label=").append(labelId)
                .append("&order=1")
                .append("&noconfirm=1").append("&uid=").append(fUser.getUid()).toString();

        String filter2 = new StringBuilder().append("?attachment=")
                .append("&field1=subject").append("&field2=1").append("&field3=cat123456")
                .append("&logic=1")
                .append("&clicker=movel").append("&move_label=").append(labelId)
                .append("&clicker=movel").append("&move_label=").append("lid_read")
                .append("&clicker=move").append("&move_folder=").append(folderId)
                .append("&order=1")
                .append("&noconfirm=1").append("&uid=").append(fUser.getUid()).toString();

        String filter3 = new StringBuilder().append("?attachment=")
                .append("&field1=from").append("&field2=4").append("&field3=yantester")
                .append("&field1=subject").append("&field2=4").append("&field3=yantester")
                .append("&logic=1")
                .append("&clicker=movel").append("&move_label=").append(labelId)
                .append("&clicker=move").append("&move_folder=").append(folderId)
                .append("&order=1")
                .append("&noconfirm=1").append("&uid=").append(fUser.getUid()).toString();

        return asList(new Object[]{filter1}, new Object[]{filter2}, new Object[]{filter3});
    }

    @Before
    public void createFilterAndPrepareTestMessage() throws Exception {
        params = params.concat("&name=").concat(randomAlphabetic(10));
        log.info("Создаём фильтр с параметрами" + params);
        filterId = createFilterWithHttpClient(params, log);
        log.info(filterId);
    }

    @Test
    public void testFilterEnable() throws Exception {
        fUser.disableFilter(filterId);
        fUser.enableFilter(filterId);
        assertThat("Не удалось включить фильтр![DARIA-29644],[MPROTO-1118]",
                fUser.getFilter(filterId, false).get("enabled"), equalTo(true));
    }

    @Test
    public void jsonTestFilterEnable() throws Exception {
        fUser.disableFilter(filterId, YFuritaUtils.JSON);
        fUser.enableFilter(filterId, YFuritaUtils.JSON);
        assertThat("Не удалось включить фильтр![DARIA-29644],[MPROTO-1118]",
                fUser.getFilter(filterId, false).get("enabled"), equalTo(true));
    }


    @Test
    public void testFilterDisable() throws Exception {
        fUser.disableFilter(filterId);
        assertThat("Не удалось выключить фильтр![DARIA-29644],[MPROTO-1118]",
                fUser.getFilter(filterId, false).get("enabled"), equalTo(false));
    }

    @Test
    public void jsonTestFilterDisable() throws Exception {
        fUser.disableFilter(filterId, YFuritaUtils.JSON);
        assertThat("Не удалось выключить фильтр![DARIA-29644],[MPROTO-1118]",
                fUser.getFilter(filterId, false).get("enabled"), equalTo(false));
    }

    @Test
    public void testFilterDisableEnable() throws Exception {
        for (int i = 0; i < 3; i++) {
            fUser.disableFilter(filterId);
            fUser.enableFilter(filterId);
            fUser.disableFilter(filterId, YFuritaUtils.JSON);
            fUser.enableFilter(filterId, YFuritaUtils.JSON);
        }
        assertThat("Не удалось включить фильтр после серии включений-выключений![DARIA-29644],[MPROTO-1118]",
                fUser.getFilter(filterId, false).get("enabled"), equalTo(true));
    }
}
