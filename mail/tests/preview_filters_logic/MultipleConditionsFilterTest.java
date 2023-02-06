package ru.yandex.autotests.innerpochta.yfurita.tests.preview_filters_logic;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.Credentials;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.UserInitializationRule;
import ru.yandex.autotests.innerpochta.yfurita.util.YFuritaPreviewResponse;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.expect;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;

/**
 * Created by alex89 on 20.07.17.
 * * Date: 11.09.13
 */
@Aqua.Test(title = "Тестирование preview фильтров с множеством условий",
        description = "Тестирование preview фильтров с множеством условий")
@Title("MultipleConditionsFilterTest.Тестирование preview фильтров с множеством условий")
@Description("Тестирование preview фильтров с множеством условий")
@Feature("Yfurita.Preview")
@RunWith(Parameterized.class)
public class MultipleConditionsFilterTest {
    private static final String FOLDER_WITH_TEST_LETTERS = "multi-cond-test";
    private static final long INDEX_ADDITIONAL_TIMEOUT = 30000;
    private static String filterId;
    private static FilterUser fUser;
    private static HashMap<String, TestMessage> fileNamesAndTestMsgs = new HashMap<String, TestMessage>();
    private static HashMap<String, String> fileNamesAndMids = new HashMap<String, String>();
    private Logger log = LogManager.getLogger(this.getClass());

    @Credentials(loginGroup = "MultipleConditionsFilterTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @Parameterized.Parameter(0)
    public String paramsString;
    @Parameterized.Parameter(1)
    public List<String> expectedTestLettersNames;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return asList(
                new Object[]{"?field1=subject&logic=1&field3=cat&field2=3&field1=subject&field3=cat1&field2=3",
                        asList("1.eml")},
                new Object[]{"?field1=subject&logic=1&field3=cat&field2=3&field1=subject&field3=cat1&field2=1",
                        asList("1.eml")},
                new Object[]{"?field1=subject&logic=0&field3=cat&field2=3&field1=from&field3=tripadvisor.com&field2=3",
                        asList("1.eml", "2.eml", "3.eml", "4.eml", "5.eml", "7.eml")},
                new Object[]{"?field1=subject&logic=1&field3=cat&field2=3&field1=Peterburg&field3=Peterburg&field2=3",
                        asList("1.eml", "2.eml", "3.eml")},
                new Object[]{"?field1=subject&logic=1&field3=cat&field2=3&field1=Peterburg&field3=Peterburg&field2=2",
                        asList("4.eml", "5.eml")},
                new Object[]{"?field1=subject&logic=1&field3=cat&field2=3&field1=Peterburg&field3=Peterburg&field2=2" +
                        "&field1=from&field2=1&field3=pleskav@ya.ru",
                        asList("5.eml")},
                new Object[]{"?field1=from&field2=3&field3=&field1=subject&field2=3&field3=yantester&logic=1",
                        asList("8.eml")}
        );
    }

    @BeforeClass
    public static void initFilterUserAndParams() throws Exception {
        fUser = new FilterUser(testUser);
        fUser.removeAllFilters();
        fUser.clearAll();

        fileNamesAndTestMsgs = sendAllTestMsgsFromResourceFolder(FOLDER_WITH_TEST_LETTERS, testUser);
        fileNamesAndMids = getMidsTableFromMailBox(fUser, fileNamesAndTestMsgs);

        Thread.sleep(INDEX_ADDITIONAL_TIMEOUT);
    }

    @Before
    public void createFilter() throws Exception {
        log.info(fileNamesAndMids);
        paramsString = paramsString.concat(new StringBuilder()
                .append("&clicker=delete")
                .append("&name=").append(randomAlphabetic(10))
                .append("&order=1")
                .append("&noconfirm=1")
                .append("&db=").append(fUser.getMdb())
                .append("&uid=").append(fUser.getUid())
                .append("&user=").append(fUser.getSuid()).toString());

        //todo вернуть RA реализацию, когда починится способность рестажуред не перемешивать параметры.
        // А для фуриты важен порядок элементов!
//        filterId = expect().statusCode(OK_200).and().body("session", notNullValue())
//                .given().log().all()
//                .get(yfuritaProps().getYfuritaUrl()+ "/api/edit.json" + paramsString).jsonPath()
//                .getString("id").trim();

        filterId = createFilterWithHttpClient(paramsString, log);
        log.info(filterId);

    }

    @Test
    @Title("Проверяем работу фильров с множеством условий через preview запрос с указанием id-фильтра")
    @Issues({@Issue("DARIA-34276")})
    @Description("Проверяем работу фильров с множеством условий через preview запрос с указанием id-фильтра")
    public void shouldSeeCorrectPreviewForExistedFilter() throws Exception {
        YFuritaPreviewResponse response =
                new YFuritaPreviewResponse(fUser.previewFilter(filterId));
        assertThat("Некоректный вывод preview по id-фильтра",
                response,
                equalTo(getPreviewResponseEtalon(expectedTestLettersNames, fileNamesAndMids)));
    }

    @Test
    @Title("Проверяем работу фильров с множеством условий через preview запрос с указанием этих условий")
    @Issues({@Issue("DARIA-34276")})
    @Description("Проверяем работу фильров с множеством условий через preview запрос с указанием этих условий")
    public void shouldSeeCorrectPreviewForConditions() throws Exception {
        YFuritaPreviewResponse response =
//                new YFuritaPreviewResponse(fUser.previewFilter(params));
                new YFuritaPreviewResponse(previewFilterWithHttpClient(paramsString, log));
        assertThat("Некоректный вывод preview по условиям",
                response,
                equalTo(getPreviewResponseEtalon(expectedTestLettersNames, fileNamesAndMids)));
    }

    @AfterClass
    public static void disableAllFilters() throws Exception {
        fUser.disableAllFilters();
    }


}
