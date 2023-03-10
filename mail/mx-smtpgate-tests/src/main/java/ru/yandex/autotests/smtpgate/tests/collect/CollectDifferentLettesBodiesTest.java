package ru.yandex.autotests.smtpgate.tests.collect;

import com.jayway.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.tests.utils.SshConnectionRule;
import ru.yandex.autotests.innerpochta.util.LogToFileUtils;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.autotests.smtpgate.tests.append.AppendDifferentLettesBodiesTest;
import ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static org.cthul.matchers.object.ContainsPattern.containsPattern;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.util.InnerpochtaProjectProperties.chooseUser;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasResponseBody;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasStatusCode;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi.smtpgateApi;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.COLLECT_RESP_PATTERN;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateProperties.smtpgateProps;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

/**
 * User: alex89
 * Date: 04.02.2016
 * https://wiki.yandex-team.ru/pochta/mproto/smtp-gate/
 */

@Stories("Positive")
@Feature("COLLECT")
@Aqua.Test
@Title("CollectDifferentLettesBodiesTest. ?????????????? ???????????????? ?????????????????????? ???????????? ?????????? COLLECT")
@Description("??????????????????, ?????? ???????? ???????????? ???????????????????? ??????????????????")
@RunWith(Parameterized.class)
public class CollectDifferentLettesBodiesTest {
    private static final User RECEIVER =
            chooseUser(new User("smmtpgate-test4@ya.ru", "testqa12345678"),
                    new User("mxtest-34@mail.yandex-team.ru", "OqKEt7R9sr1v"));
    private static final SmtpgateApi SMTP_SIMPLE_REQUEST =
            smtpgateApi().queryParam("fid", "1").queryParam("date", "1454436917");
    private Response response;
    private TestMessage msg;
    private String eml;

    @Parameterized.Parameter(0)
    public String emlFileName;

    @Rule
    public SshConnectionRule sshConnectionRule = new SshConnectionRule(smtpgateProps().getHost());
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        for (String fileName : asCharSource(getResource("emls.list"), UTF_8).readLines()) {
            data.add(new Object[]{fileName});
        }
        return data;
    }

    @BeforeClass
    public static void cleanUser() {
        inMailbox(RECEIVER).clearDefaultFolder();
    }


    @Before
    public void executeCollect() throws IOException, MessagingException {
        File emlFile = LogToFileUtils.getLogFile();
        elliptics().indefinitely().path(AppendDifferentLettesBodiesTest.class)
                .name(emlFileName).get().asFile(emlFile);
        msg = new TestMessage(emlFile);
        eml = elliptics().indefinitely().path(AppendDifferentLettesBodiesTest.class)
                .name(emlFileName).get().asString();

        response = SMTP_SIMPLE_REQUEST.letter(eml).email(RECEIVER.getLogin()).collect("445712417");
    }


    @Test
    public void shouldSeeLetterBodyDelivery() throws Exception {
        assertThat("???????????????? ?????????? Collect-??????????????!", response, allOf(hasStatusCode(OK_200),
                hasResponseBody(containsPattern(COLLECT_RESP_PATTERN))));

        String msgSubject = msg.getSubject().replace("Re: ", "");
        String expectedEmlText = eml.substring(eml.indexOf("\n\n") + 2) + "\r\n";

        assertThat("?? ???????????????????? Collect-?????????????? ???????? ???????????? ?????????????????????? ??????????????????????!",
                IOUtils.toString(inMailbox(RECEIVER).getMessageWithSubject(msgSubject).getInputStream(), UTF_8),
                equalTo(expectedEmlText));
    }
}