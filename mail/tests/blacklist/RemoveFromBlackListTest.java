package ru.yandex.autotests.innerpochta.yfurita.tests.blacklist;

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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.JSON;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.getRandomEmail;

/**
 * User: stassiak
 * Date: 19.02.13
 */
@Feature("Yfurita.Blacklist")
@Aqua.Test(title = "Тестирование удаленич из черный список",
        description = "Тестирование удаления из черного списка")
@Title("RemoveFromBlackListTest.Тестирование удаления из черного списка")
@Description("Тестирование удаления из черного списка")
public class RemoveFromBlackListTest {
    private static FilterUser fUser;

    @Credentials(loginGroup = "RemoveFromBlackListTest")
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
        fUser.clearBlacklist();
    }

    @Before
    public void getEmailList() throws Exception {
        for (int i = 0; i < 2; i++) {
            fUser.addToBlacklist(getRandomEmail());
            fUser.addToBlacklist(randomAlphanumeric(15) + "@яндекстест.рф");
            fUser.addToBlacklist(randomAlphanumeric(15) + "@яндекс123тест.рф");
        }
    }

    @Test
    public void testAllEmailsWereRemoved() throws Exception {
        fUser.clearBlacklist();
        assertThat("Не все адреса были удалены из Черного списка!", fUser.getBlacklist(), hasSize(0));
    }

    @Test
    public void jsonTestAllEmailsWereRemoved() throws Exception {
        fUser.clearBlacklist(JSON);
        assertThat("Не все адреса были удалены из Черного списка!", fUser.getBlacklist(JSON), hasSize(0));
    }
}