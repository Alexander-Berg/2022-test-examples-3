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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jayway.restassured.RestAssured.expect;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.BLACK_LIST;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.WHITE_LIST;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.getRandomEmail;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * User: alex89
 * Date: 17.03.14
 */
@Feature("Yfurita.BlackAndWhitelist")
@Aqua.Test(title = "Тестирование вывода запроса /api/blackwhitelist",
        description = "Производим манипуляции с белым и чёрным списком, смотрим правильность /api/blackwhitelist")
@Title("BlackAndWhiteListTest.Тестирование вывода запроса /api/blackwhitelist")
@Description("Производим манипуляции с белым и чёрным списком, смотрим правильность /api/blackwhitelist")
public class BlackAndWhiteListTest {
    private static final String BL_ADD_COMMAND = "/api/blacklist_add.json";
    private static final String ERRROR_MESSAGE = "Already exists";
    private static FilterUser fUser;
    private List<String> whiteEmails = new ArrayList<>();
    private List<String> blackEmails = new ArrayList<>();

    @Credentials(loginGroup = "BlackAndWhiteListTest")
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
        fUser.clearBlacklist();
        fUser.clearWhitelist();

        for (String email : asList(getRandomEmail(), getRandomEmail(), randomAlphanumeric(15) + "@белый.рф")) {
            fUser.addToWhitelist(email);
            whiteEmails.add(email);
        }
        for (String email : asList(getRandomEmail(), getRandomEmail(), randomAlphanumeric(15) + "@чёрныш123.укр")) {
            fUser.addToBlacklist(email);
            blackEmails.add(email);
        }
    }

    @Test
    public void testBlackAndWhiteList() throws Exception {
        assertThat("Проблемы с Белым списком!",
                fUser.getBlackAndWhitelist().get(WHITE_LIST), hasSameItemsAsList(whiteEmails));
        assertThat("Проблемы с Чёрным списком!",
                fUser.getBlackAndWhitelist().get(BLACK_LIST), hasSameItemsAsList(blackEmails));
    }

    @Test
    public void testBlackAndWhiteListWhenBlackListIsClean() throws Exception {
        fUser.clearBlacklist();
        assertThat("Проблемы с Белым списком!",
                fUser.getBlackAndWhitelist().get(WHITE_LIST), hasSameItemsAsList(whiteEmails));
        assertThat("Проблемы с Чёрным списком!", fUser.getBlackAndWhitelist().get(BLACK_LIST), hasSize(0));
    }

    @Test
    public void testBlackAndWhiteListWhenWhiteListIsClean() throws Exception {
        fUser.clearWhitelist();
        assertThat("Проблемы с Белым списком!", fUser.getBlackAndWhitelist().get(WHITE_LIST), hasSize(0));
        assertThat("Проблемы с Чёрным списком!",
                fUser.getBlackAndWhitelist().get(BLACK_LIST), hasSameItemsAsList(blackEmails));
    }

    @Test
    public void testBlackAndWhiteListsAreClean() throws Exception {
        fUser.clearWhitelist();
        fUser.clearBlacklist();
        assertThat("Проблемы с Белым списком!", fUser.getBlackAndWhitelist().get(WHITE_LIST), hasSize(0));
        assertThat("Проблемы с Чёрным списком!", fUser.getBlackAndWhitelist().get(BLACK_LIST), hasSize(0));
    }

    @Test
    public void testBlackAndWhiteListsCanNotHaveCommonEmail() throws Exception {
        String commonEmail = getRandomEmail();
        fUser.addToWhitelist(commonEmail);
        whiteEmails.add(commonEmail);

        HashMap<String, String> params = new HashMap<>();
        params.put("uid", fUser.getUid());
        params.put("email", commonEmail);


        expect().statusCode(500).and().body("report", equalTo(ERRROR_MESSAGE))
                .given().parameters(params)
                .get(yfuritaProps().getYfuritaUrl() + BL_ADD_COMMAND);

        assertThat("Проблемы с Белым списком!",
                fUser.getBlackAndWhitelist().get(WHITE_LIST), hasSameItemsAsList(whiteEmails));
        assertThat("Проблемы с Чёрным списком!",
                fUser.getBlackAndWhitelist().get(BLACK_LIST), hasSameItemsAsList(blackEmails));
    }
}
