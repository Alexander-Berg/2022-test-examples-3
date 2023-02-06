package ru.yandex.autotests.innerpochta.yfurita.tests.blacklist;

import org.hamcrest.Matcher;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

import java.util.Collection;
import java.util.HashMap;

import static com.jayway.restassured.RestAssured.expect;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.JSON;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.XML;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;

/**
 * User: stassiak
 * Date: 19.02.13
 */
@Feature("Yfurita.Blacklist")
@Aqua.Test(title = "Тестирование добавления в черный список повторно",
        description = "Добавляем в чёрный список пользователя повторно, проверяем сообщение об ошибке")
@Title("AddToBlacklistTwiceTest.Тестирование добавления в черный список повторно")
@Description("Добавляем в чёрный список пользователя повторно, проверяем сообщение об ошибке")
@RunWith(value = Parameterized.class)
public class AddToBlacklistTwiceTest {
    private static final String BL_ADD_COMMAND = "/api/blacklist_add.";
    private static final Matcher<String> ERRROR_MESSAGE_MATCHER =
            anyOf(equalTo("Failed to add blacklist entry: already exists"), equalTo("Already exists"));
    private static FilterUser fUser;
    private String email1 = randomAlphanumeric(15) + "@" + randomAlphanumeric(5) + "." + randomAlphanumeric(3);
    private String email2 = randomAlphanumeric(15) + "@" + randomAlphanumeric(5) + "." + randomAlphanumeric(3);

    @Credentials(loginGroup = "AddToBlacklistTwiceTest")
    public static User testUser;

    @Parameterized.Parameter(0)
    public String format;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return singletonList(new Object[]{JSON});
    }

    @BeforeClass
    public static void initUser() throws Exception {
        fUser = new FilterUser(testUser);
        fUser.clearBlacklist();
    }

    @Before
    public void addSomeEmailsToBlackList() throws Exception {
        fUser.addToBlacklist(email1);
        fUser.addToBlacklist(email2);
    }

    @Test
    public void testAllEmailsAdded() throws Exception {
        assumeThat("Не добавились два пользователя в черный список!",
                fUser.getBlacklist(), containsInAnyOrder(asList(email1, email2).toArray()));
        HashMap<String, String> params = new HashMap<>();
        params.put("uid", fUser.getUid());
        params.put("email", email1);

        expect().statusCode(500).and().body("report", ERRROR_MESSAGE_MATCHER)
                .given().parameters(params)
                .get(yfuritaProps().getYfuritaUrl() + BL_ADD_COMMAND + format);
        assertThat(fUser.getBlacklist(), containsInAnyOrder(asList(email1, email2).toArray()));
    }

    @After
    public void clearBlackList() throws Exception {
        fUser.clearBlacklist();
    }
}
