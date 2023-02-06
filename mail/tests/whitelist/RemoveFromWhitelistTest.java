package ru.yandex.autotests.innerpochta.yfurita.tests.whitelist;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.Credentials;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.UserInitializationRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.JSON;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.getRandomEmail;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;

/**
 * User: stassiak
 * Date: 19.02.13
 */
@Feature("Yfurita.Whitelist")
@Aqua.Test(title = "Тестирование удаления из белого списка",
        description = "Тестирование удаления из белого списка")
@Title("RemoveFromWhitelistTest.Тестирование удаления из белого списка")
@Description("Тестирование удаления из белого списка")
public class RemoveFromWhitelistTest {
    private static FilterUser fUser;
    @Credentials(loginGroup = "RemoveFromWhitelistTest")
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
    }

    @Before
    public void getEmailList() throws Exception {
        fUser.clearWhitelist();
        for (int i = 0; i < 2; i++) {
            fUser.addToWhitelist(getRandomEmail());
            fUser.addToWhitelist(randomAlphanumeric(15) + "@белыйсписок.рф");
            fUser.addToWhitelist(randomAlphanumeric(15) + "@белыйсписок12.рф");
        }
        assumeThat(fUser.getWhitelist(), hasSize(equalTo(6)));
    }

    @Test
    public void testAllEmailsAdded() throws Exception {
        fUser.clearWhitelist();
        assertThat(fUser.getWhitelist(), hasSize(0));
    }

    @Test
    public void jsonTestAllEmailsAdded() throws Exception {
        for (String email : fUser.getWhitelist()) {
            fUser.removeFromWhitelist(email, JSON);
        }
        assertThat(fUser.getWhitelist(JSON), hasSize(0));
    }
}
