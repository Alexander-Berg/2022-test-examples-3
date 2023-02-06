package ru.yandex.autotests.smtpgate.tests.save;

import com.jayway.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.tests.utils.SshConnectionRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.LinkedList;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.cthul.matchers.object.ContainsPattern.containsPattern;
import static org.eclipse.jetty.http.HttpStatus.NOT_ACCEPTABLE_406;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static ru.yandex.autotests.innerpochta.tests.utils.MxConstants.VIRUS_TEXT;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasResponseBody;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasStatusCode;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi.smtpgateApi;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.*;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateProperties.smtpgateProps;

/**
 * User: alex89
 * Date: 04.02.2016
 * https://wiki.yandex-team.ru/pochta/mproto/smtp-gate/
 */

@Stories("Positive")
@Feature("SAVE")
@Aqua.Test
@Title("SaveDetectSpamTest. Поверка работы ручки save c detect_spam=1")
@Description("Проверяем на неспамовых и спамовых письмах работу save c detect_spam=1")
@RunWith(Parameterized.class)
public class SaveDetectSpamTest {
    private static final User RECEIVER = new User("smmtpgate-save-test@ya.ru", "testqa12345678");
    private Response response;

    @Parameterized.Parameter(0)
    public String emlText;
    @Parameterized.Parameter(1)
    public Matcher<String> expectedResponse;
    @Parameterized.Parameter(2)
    public int expectedResponseCode;

    @Rule
    public SshConnectionRule sshConnectionRule = new SshConnectionRule(smtpgateProps().getHost());
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{"\ngood", containsPattern(SAVE_RESP_PATTERN), OK_200});
        data.add(new Object[]{"\nqhYjHuANTI-UBE-TEST-EMAIL*C.34XXJS*C4JDBQADN1" +
                ".NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X",
                containsPattern(SAVE_SPAM_RESP_PATTERN),
                NOT_ACCEPTABLE_406});
        data.add(new Object[]{"\ngood", containsPattern(SAVE_RESP_PATTERN), OK_200});
        data.add(new Object[]{"Content-Type: text/plain; charset=\"us-ascii\"\nMIME-Version: 1.0\n" +
                "Content-Transfer-Encoding: 7bit\n\n"
                + VIRUS_TEXT,
                containsPattern(SAVE_RESP_PATTERN), OK_200});
        data.add(new Object[]{"\ngood", containsPattern(SAVE_RESP_PATTERN), OK_200});
        data.add(new Object[]{"\nXJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STRONG-ANTI-UBE-TEST-EMAIL*C.34X",
                containsPattern(SAVE_STRONG_SPAM_RESP_PATTERN), NOT_ACCEPTABLE_406});
        data.add(new Object[]{"\ngood", containsPattern(SAVE_RESP_PATTERN), OK_200});
        return data;
    }

    @Before
    public void executeAppend() {
        response = smtpgateApi().requestId(randomAlphanumeric(10) + "-" + randomAlphanumeric(10))
                .queryParam("fid", "1").queryParam("date", "1454436917")
                .queryParam("detect_spam", "1")
                .letter(String.format("From:%s@ya.ru\nSubject:%s\n%s",
                        randomAlphanumeric(10), randomAlphanumeric(10), emlText))
                .srcEmail(RECEIVER.getLogin()).save();
    }

    @Test
    public void shouldSeeSpamDetection() throws Exception {
        assertThat("Неверный ответ save-запроса на сохранение письма с текстом: " + emlText,
                response, allOf(
                        hasStatusCode(expectedResponseCode),
                        hasResponseBody(expectedResponse)));
    }
}