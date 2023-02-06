package ru.yandex.autotests.smtpgate.tests.append;

import com.google.common.base.Charsets;
import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.utils.SshConnectionRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.cthul.matchers.object.ContainsPattern.containsPattern;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.util.InnerpochtaProjectProperties.innerpochtaProps;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasResponseBody;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasStatusCode;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi.smtpgateApi;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.RESP_PATTERN;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.getHintInformationByAppendResponse;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.prepareHint;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateProperties.smtpgateProps;

/**
 * User: alex89
 * Date: 04.02.2016
 * smmtpgate-test testqa12345678
 * <p>
 * https://wiki.yandex-team.ru/BitovyeFlagiISMIXED/
 * https://wiki.yandex-team.ru/pochta/mproto/smtp-gate/
 */
@Stories("Positive")
@Feature("APPEND")
@Aqua.Test
@Title("AppendHintParamsTransmissionTest. Передача параметров в X-Yandex-Hint")
@Description("Проверяем, что параметры append запроса верно преобразуются в параметры X-Yandex-Hint")
@RunWith(Parameterized.class)
public class AppendHintParamsTransmissionTest {
    private static final String RECEIVER_LOGIN =
            innerpochtaProps().isCorpServer() ? "mxtest-35@yandex-team.ru" : "smmtpgate-test@ya.ru";
    private static String emlText;
    private Response response;

    @Parameterized.Parameter(0)
    public SmtpgateApi request;
    @Parameterized.Parameter(1)
    public String expectedHint;

    @Rule
    public SshConnectionRule sshConnectionRule = new SshConnectionRule(smtpgateProps().getHost());
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1").queryParam("date", "1454436917"),
                prepareHint("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 imap=1")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "3").queryParam("date", "1454438917"),
                prepareHint("received_date=1454438917 fid=3 skip_loop_prevention=1 notify=0 filters=0 imap=1")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Recent"), // Recent - игнорируется
                prepareHint("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 imap=1")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Draft"),
                prepareHint("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 mixed=64 imap=1")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Deleted"),
                prepareHint("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 mixed=128 imap=1")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Answered"),
                prepareHint("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 mixed=1024 imap=1")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Seen"),
                prepareHint("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 mixed=2048 imap=1")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("user_flags", "$Forwarded"),
                prepareHint("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 mixed=512 imap=1 " +
                        "imaplabel=$Forwarded")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("user_flags", "Forwarded"),
                prepareHint("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 imap=1 " +
                        "imaplabel=Forwarded")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("user_flags", "Bob").queryParam("system_flags", "Seen")
                .queryParam("system_flags", "Answered"),
                prepareHint("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 mixed=3072 imap=1 " +
                        "imaplabel=Bob")});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436920").queryParam("user_flags", "Bob").queryParam("system_flags", "Seen")
                .queryParam("system_flags", "Answered")
                .queryParam("user_flags", "Eva1234567891011121314151617181920Brown")
                .queryParam("system_flags", "Deleted"),
                prepareHint("received_date=1454436920 fid=1 skip_loop_prevention=1 notify=0 filters=0 mixed=3200 imap=1 " +
                        "imaplabel=Bob imaplabel=Eva1234567891011121314151617181920Brown")});
        return data;
    }

    @BeforeClass
    public static void readMsg() throws IOException {
        emlText = asCharSource(getResource("simple-letter2.eml"), Charsets.UTF_8).read();
    }

    @Before
    public void executeAppend() {
        response = request.letter(emlText.concat(randomAlphanumeric(10))).srcEmail(RECEIVER_LOGIN).append();
    }

    @Test
    public void shouldSeeResponseWithMidAndImapId() {
        assertThat("Неверный ответ append-запроса!",
                response, allOf(hasStatusCode(OK_200), hasResponseBody(containsPattern(RESP_PATTERN))));
    }

    @Test
    public void shouldSeeHintTransmissionToFastsrvInMaillogDebug() throws Exception {
        assumeThat("Неверный ответ append-запроса, нет смысла дальше продолжать данный автотест!",
                response, allOf(hasStatusCode(OK_200), hasResponseBody(containsPattern(RESP_PATTERN))));
        assertThat("Append-запрос неверно пробросил параметры в X-Yandex-Hint (NSLS)!",
                getHintInformationByAppendResponse(response.asString(), RECEIVER_LOGIN, sshConnectionRule), equalTo(expectedHint));
    }
}
