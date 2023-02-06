package ru.yandex.autotests.innerpochta.yfurita.tests.whitelist;

import org.junit.*;
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
@Feature("Yfurita.Whitelist")
@Aqua.Test(title = "Тестирование добавления в белого списка",
        description = "Тестирование добавления в белого списка")
@Title("AddToWhiteListTest.Тестирование добавления в белый список")
@Description("Тестирование добавления в белый список")
public class AddToWhiteListTest {
    private static FilterUser fUser;
    private List<String> emails;

    @Credentials(loginGroup = "AddToWhiteListTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @BeforeClass
    public static void initTestUser() throws Exception {
        fUser = new FilterUser(testUser);
    }

    @Before
    public void getEmailList() throws Exception {
        emails = new LinkedList<>();
        fUser.clearWhitelist();
        for (int i = 0; i < 2; i++) {
            emails.add(getRandomEmail());
            emails.add(randomAlphanumeric(15) + "@белыйсписок.рф");
            emails.add(randomAlphanumeric(15) + "@белыйсписок12.рф");
        }
    }

    @Test
    public void testAllEmailsAdded() throws Exception {
        for (String email : emails) {
            fUser.addToWhitelist(email);
        }
        assertThat(fUser.getWhitelist(), containsInAnyOrder(emails.toArray(new String[emails.size()])));
    }

    @Test
    public void jsonTestAllEmailsAdded() throws Exception {
        for (String email : emails) {
            fUser.addToWhitelist(email, YFuritaUtils.JSON);
        }
        assertThat(fUser.getWhitelist(YFuritaUtils.JSON),
                containsInAnyOrder(emails.toArray(new String[emails.size()])));
    }

    @After
    public void clearBlackList() throws Exception {
        fUser.clearWhitelist();
    }
}
