/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.yandex.autotests.smtpgate.tests.append;

import com.google.common.base.Charsets;
import com.jayway.restassured.response.Response;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static org.cthul.matchers.object.ContainsPattern.containsPattern;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasResponseBody;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasStatusCode;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi.smtpgateApi;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.RESP_PATTERN;

/**
 * localhost:2000/
 * append?src_email=yapoptest@yandex.ru&fid=1&user_flags=Foo&user_flags=Bar&system_flags=Seen&date=1454436917
 */

@Stories("Negative")
@Feature("APPEND")
@Aqua.Test
@Title("AppendNegativeTest.Передача некорректного набора параметров")
@Description("Проверяем сообщения об ошибке и коды ответа при некорректном наборе параметров")
public class AppendNegativeTest {
    private static final String NO_FID_ERROR_MSG = "'/append' failed, invalid parameters: missing argument: \"fid\"";
    private static final String NO_DATE_ERROR_MSG = "'/append' failed, invalid parameters: missing argument: \"date\"";
    private static final String NO_SRC_EMAIL_ERROR_MSG =
            "'/append' failed, invalid parameters: missing argument: \"src_email\"";
    private static final String BAD_SERVER_RESPONSE_ERROR_MSG =
            "'/append' failed, smtp error: Bad reply code from server";
    private static final String BAD_RCPT_RESPONSE_ERROR_MSG = "'/append' failed, smtp error: Bad recipient";
    private static String emlText;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @BeforeClass
    public static void readMsg() throws IOException {
        emlText = asCharSource(getResource("simple-letter.eml"), Charsets.UTF_8).read();
    }

    @Test
    public void shouldSeeNoFidErrorMsg() {
        Response response =
                smtpgateApi().letter(emlText).srcEmail("pleskav@ya.ru").append();
        assertThat("Неверный результат заведомо некорректного append-запроса!",
                response,
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasResponseBody(equalTo(NO_FID_ERROR_MSG))));
    }

    @Test
    public void shouldSeeNoDateErrorMsg() {
        Response response = smtpgateApi().letter(emlText).queryParam("fid", "1").append();
        assertThat("Неверный результат заведомо некорректного append-запроса!",
                response,
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasResponseBody(equalTo(NO_DATE_ERROR_MSG))));
    }

    @Test
    public void shouldSeeNoSrcEmailErrorMsg() {
        Response response = smtpgateApi().letter(emlText).queryParam("fid", "1")
                .queryParam("date", "1454436917").append();
        assertThat("Неверный результат заведомо некорректного append-запроса!",
                response,
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasResponseBody(equalTo(NO_SRC_EMAIL_ERROR_MSG))));
    }

    @Test
    public void shouldSeeBadServerResponseErrorMsg() {
        Response response = smtpgateApi().letter(emlText)
                .srcEmail("pleskav")
                .queryParam("fid", "1")
                .queryParam("date", "1454436917").append();
        assertThat("Неверный результат заведомо некорректного append-запроса!",
                response,
                allOf(hasStatusCode(INTERNAL_SERVER_ERROR_500),
                        hasResponseBody(equalTo(BAD_SERVER_RESPONSE_ERROR_MSG))));
    }

    @Test
    public void shouldSeeBadServerResponseErrorMsgInCaseOfIncorrectSrcEmail() {
        Response response = smtpgateApi().letter(emlText)
                .srcEmail("pleskav@ya.ru,mx-test-user6@ya.ru")
                .queryParam("fid", "1")
                .queryParam("date", "1454436917").append();
        assertThat("Неверный результат заведомо некорректного append-запроса!",
                response,
                allOf(hasStatusCode(INTERNAL_SERVER_ERROR_500),
                        hasResponseBody(equalTo(BAD_RCPT_RESPONSE_ERROR_MSG))));
    }

    @Test
    public void shouldSeeOkServerResponseInCaseOfAppendWithoutBody() {
        Response response = smtpgateApi().letter(emlText)
                .srcEmail("mx-test-user6@ya.ru")
                .queryParam("fid", "1")
                .queryParam("date", "1454436917").appendWithoutBody();
        assertThat("Неверный результат заведомо некорректного append-запроса!",
                response,
                allOf(hasStatusCode(INTERNAL_SERVER_ERROR_500),
                        hasResponseBody(equalTo(BAD_SERVER_RESPONSE_ERROR_MSG))));
    }
}
