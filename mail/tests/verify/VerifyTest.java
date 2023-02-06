package ru.yandex.autotests.innerpochta.yfurita.tests.verify;

import com.jayway.restassured.response.Response;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.Credentials;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.UserInitializationRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jayway.restassured.RestAssured.expect;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.matchers.MessageContentMatcher.readMessageContent;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;

/**
 * User: stassiak
 * Date: 19.02.13
 */
@Feature("Yfurita.Verify")
@Aqua.Test(title = "Тестирование запроса verify",
        description = "Тестирование запроса verify")
@Title("VerifyTest.Тестирование запроса verify")
@Description("Тестирование запроса verify")
public class VerifyTest {
    private static final Pattern E_STRING_PATTERN = compile("e=([A-Za-z%_0-9]+)>");
    private static final String INCORRECT_E_STRING =
            "9f2AC/8ho9kw8XUQlg/2ToEsAiucAtT7dAVBAtmPm19YAgv6Ze6u2Xy8MwnP152Re9uBy7UiCxjwMsXSjfuYwLxOOtWB96fX";
    private static final String CONFIRM_MAIL_SUBJ = "Подтверждение адреса для получения уведомлений";
    private static String confirmEmail;
    private static FilterUser fUser;
    private static FilterUser toUser;
    private String filterId;
    private String eString;

    @Credentials(loginGroup = "VerifyTest1")
    public static User testUser;

    @Credentials(loginGroup = "VerifyTest2")
    public static User testUser2;

    @Rule
    public LogConfigRule logRule = new LogConfigRule();
    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();

    @BeforeClass
    public static void createFilters() throws Exception {
        confirmEmail = testUser2.getLogin();
        fUser = new FilterUser(testUser);
        toUser = new FilterUser(testUser2);
        fUser.removeAllFilters();
        fUser.clearAll();
    }

    @Before
    public void createFilter() throws Exception {
        toUser.clearAll();
        FilterSettings filterSettings = new FilterSettings();
        setFilterSettings(filterSettings);

        filterId = fUser.createFilter(filterSettings.getParams());
//        assertThat(fUser.getFilter(filterId, false).getAttribute("enabled"), equalTo("0"));
        //   assertThat(fUser.getFilter(filterId, false).getAttribute("enabled"), equalTo("1"));

        eString = getEString();
    }

    @Test
    public void testThatFilterMarkedAsEnabledAndVerifiedAfterVerifyRequestWasSent() throws Exception {
        Response resp = fUser.verify(eString);
        assertThat(resp.jsonPath().getString("fwd_from").trim(), equalTo(testUser.getLogin()));
        assertThat(resp.jsonPath().getString("fwd_to").trim(), equalTo(confirmEmail));
        assertThat(fUser.getFilter(filterId, false).get("enabled"), equalTo(true));
        assertThat(fUser.listFilter(filterId, false).jsonPath().get("rules[0].actions[0].verified"), equalTo(true));
    }

    @Test
    public void testThatReturnCorrectFromAndToForVerifyFromRequest() throws Exception {
        assertThat(fUser.verify(eString, testUser.getLogin()).jsonPath().getString("fwd_from").trim(),
                equalTo(testUser.getLogin()));
        assertThat(fUser.verify(eString, testUser.getLogin()).jsonPath().getString("fwd_to").trim(),
                equalTo(confirmEmail));
    }

    @Test
    public void testThatReturnCorrectErrorCodeForVerifyRequestWithIncorrectEString() throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("user", fUser.getSuid());
        params.put("db", fUser.getMdb());
        params.put("e", INCORRECT_E_STRING);

        expect().log().all().statusCode(400)
                .body("status", equalTo("error"))
                .body("report", equalTo("No enough parameters"))
                .given().log().all().parameters(params).get(yfuritaProps().getYfuritaUrl() + "/api/verify.json");
    }

    @Test
    public void testThatRequestWithRandomFromReturnsCorrectFwdFromAndFwdTo() throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("user", fUser.getSuid());
        params.put("db", fUser.getMdb());
        params.put("e", eString);
        params.put("from", randomAlphanumeric(15));

        expect().log().all().statusCode(200)
                .body("fwd_from", equalTo(testUser.getLogin()))
                .body("fwd_to", equalTo(confirmEmail))
                .given().log().all().parameters(params).get(yfuritaProps().getYfuritaUrl() + "/api/verify.json");
    }

    @Test
    public void testThatRequestWithNullFromReturnsCorrectFwdFromAndFwdTo() throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("user", fUser.getSuid());
        params.put("db", fUser.getMdb());
        params.put("e", eString);

        expect().log().all().statusCode(200)
                .body("fwd_from", equalTo(testUser.getLogin()))
                .body("fwd_to", equalTo(confirmEmail))
                .given().log().all().parameters(params).get(yfuritaProps().getYfuritaUrl() + "/api/verify.json?from");
    }

    @Test
    public void testThatRequestWithEmptyFromReturnsCorrectFwdFromAndFwdTo() throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("user", fUser.getSuid());
        params.put("db", fUser.getMdb());
        params.put("e", eString);
        params.put("from", "");

        expect().log().all().statusCode(200)
                .body("fwd_from", equalTo(testUser.getLogin()))
                .body("fwd_to", equalTo(confirmEmail))
                .given().log().all().parameters(params).get(yfuritaProps().getYfuritaUrl() + "/api/verify.json");
    }

    @After
    public void removeFilter() throws Exception {
        fUser.removeFilter(filterId);
    }

    private void setFilterSettings(FilterSettings filterSettings) {
        filterSettings.setLetter(FilterSettings.LETTER_NOSPAM);
        filterSettings.setClicker(FilterSettings.CLIKER_NOTIFY);
        filterSettings.setNotifyAddress(confirmEmail);
        filterSettings.setLogic(FilterSettings.LOGIC_OR);
        filterSettings.setStop(FilterSettings.STOP_YES);
        filterSettings.setFromConfirm(testUser.getLogin());
        filterSettings.setConfirmLang(FilterSettings.LANG_RU);
        filterSettings.setConfirmDomain("mail.yandex.ru");
        filterSettings.setAuthDomain("yandex.ru");
    }

    public String getEString() throws Exception {
        toUser.shouldSeeLetterWithSubject(CONFIRM_MAIL_SUBJ, 120);
        String content = readMessageContent(toUser.getMessageWithSubject(CONFIRM_MAIL_SUBJ));
        Matcher eParamMatcher = E_STRING_PATTERN.matcher(content);
        if (eParamMatcher.find()) {
            return URLDecoder.decode(eParamMatcher.group(1), "UTF-8");
        }
        return "";
    }
}
