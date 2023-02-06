package ru.yandex.autotests.innerpochta.yfurita.tests.blacklist;

import org.junit.*;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.Credentials;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.UserInitializationRule;
import ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.getRandomEmail;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;

/**
 * User: stassiak
 * Date: 19.02.13
 * https://st.yandex-team.ru/MPROTO-1119
 */
@Aqua.Test(title = "Тестирование добавления в черный список",
        description = "Тестирование добавления в черный список")
@Title("AddToBlacklistTest.Тестирование добавления в черный список")
@Description("Тестирование добавления в черный список")
@Feature("Yfurita.Blacklist")
public class AddToBlacklistTest {
    private static FilterUser fUser;
    private List<String> emailsForBlacklist;
    private List<String> emailsForWhitelist;

    @Credentials(loginGroup = "AddToBlacklistTest")
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
    public void getEmailList() {
        emailsForBlacklist = new LinkedList<>();
        for (int i = 0; i < 2; i++) {
            emailsForBlacklist.add(getRandomEmail());
            emailsForBlacklist.add(randomAlphanumeric(15) + "@черныйсписок.рф");
            emailsForBlacklist.add(randomAlphanumeric(15) + "@бчЁрныйсписок123.тест.рф");
        }
        emailsForWhitelist = new LinkedList<>();
        for (int i = 0; i < 2; i++) {
            emailsForWhitelist.add(getRandomEmail());
            emailsForWhitelist.add(randomAlphanumeric(15) + "@белыйсписок.рф");
            emailsForWhitelist.add(randomAlphanumeric(15) + "@белыйсписок12.рф");
        }
    }

    @Test
    public void testAllEmailsAdded() throws Exception {
        for (String email : emailsForBlacklist) {
            fUser.addToBlacklist(email);
        }
        assertThat("Не добавились все адреса в черный список!", fUser.getBlacklist(),
                containsInAnyOrder(emailsForBlacklist.toArray(new String[emailsForBlacklist.size()])));
    }

    @Test
    public void jsonTestAllEmailsAdded() throws Exception {
        for (String email : emailsForBlacklist) {
            fUser.addToBlacklist(email, YFuritaUtils.JSON);
        }
        assertThat("Не добавились все адреса в черный список!", fUser.getBlacklist(YFuritaUtils.JSON),
                containsInAnyOrder(emailsForBlacklist.toArray(new String[emailsForBlacklist.size()])));
    }

    @Test
    public void jsonTestAllEmailsAddedIfWhitelistAddition() throws Exception {
        for (String email : emailsForBlacklist) {
            fUser.addToBlacklist(email, YFuritaUtils.JSON);
        }
        for (String email : emailsForWhitelist) {
            fUser.addToWhitelist(email, YFuritaUtils.JSON);
        }
        assertThat("Не добавились все адреса в черный список!", fUser.getBlacklist(YFuritaUtils.JSON),
                containsInAnyOrder(emailsForBlacklist.toArray(new String[emailsForBlacklist.size()])));
    }

    @After
    public void clearBlackList() throws Exception {
        fUser.clearBlacklist();
    }
}