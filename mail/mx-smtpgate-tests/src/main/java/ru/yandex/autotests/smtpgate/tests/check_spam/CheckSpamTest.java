package ru.yandex.autotests.smtpgate.tests.check_spam;

import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.eclipse.jetty.http.HttpStatus.NOT_ACCEPTABLE_406;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasResponseBody;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasStatusCode;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi.smtpgateApi;

/**
 * User: alex89
 * Date: 04.02.2016
 * https://wiki.yandex-team.ru/pochta/mproto/smtp-gate/
 */

@Stories("Positive")
@Feature("CHECK_SPAM")
@Aqua.Test
@Title("CheckSpamTest. Поверка работы ручки check_spam")
@Description("Проверяем на неспамовых и спамовых письмах работу check_spam")
@RunWith(Parameterized.class)
public class CheckSpamTest {
    private Response response;

    @Parameterized.Parameter(0)
    public String emlText;
    @Parameterized.Parameter(1)
    public String expectedResponse;
    @Parameterized.Parameter(2)
    public int expectedResponseCode;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{"\n\rgood", "{\"spam_status\":\"ham\"}\n", OK_200});
        data.add(new Object[]{"\n\rqhYjHuANTI-UBE-TEST-EMAIL*C.34XXJS*C4JDBQADN1" +
                ".NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X", "{\"spam_status\":\"spam\"}\n",
                NOT_ACCEPTABLE_406});
        data.add(new Object[]{"\n\rgood", "{\"spam_status\":\"ham\"}\n", OK_200});
        data.add(new Object[]{"\n\rXJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STRONG-ANTI-UBE-TEST-EMAIL*C.34X",
                "{\"spam_status\":\"spam\"}\n", NOT_ACCEPTABLE_406});
        data.add(new Object[]{"\n\rgood", "{\"spam_status\":\"ham\"}\n", OK_200});
        return data;
    }


    @Before
    public void executeAppend() throws IOException, MessagingException {
        response = smtpgateApi().subject("subject-" + randomAlphanumeric(10))
                .from("yantester@yandex.ru").to("strongspamtest@yandex.ru")
                .addHeaders("1")
                .clientIp("93.158.191.22").requestId(randomAlphanumeric(5) + "-" + randomAlphanumeric(5))
                .soType("out").source("calendar")
//                .uid("12345")
//                .karma("100")
//                .karma_status("3100")
                .letter(emlText).checkSpam();
    }


    //curl -X POST -v --upload-file mspam.txt
    // 'http://mxback-qa.cmail.yandex.net:2000/check_spam?from=yantester@yandex.ru&to=strongspamtest@yandex.ru
    // &subject=test&request_id=NlPDTZjq7Z-nGYWtfng&client_ip=93.158.191.22&so_type=in'

    @Test
    public void shouldSeeSpamDetection() throws Exception {
        assertThat("Неверный ответ append-запроса!",
                response, allOf(hasStatusCode(expectedResponseCode), hasResponseBody(equalTo(expectedResponse))));
    }
}