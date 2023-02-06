package ru.yandex.autotests.smtpgate.tests.append;

import com.google.common.base.Charsets;
import com.jayway.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.LogToFileUtils;
import ru.yandex.autotests.innerpochta.tests.utils.SshConnectionRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import javax.mail.Header;
import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.Thread.currentThread;
import static java.util.regex.Pattern.compile;
import static org.cthul.matchers.object.ContainsPattern.containsPattern;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasResponseBody;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasStatusCode;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi.smtpgateApi;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.getHintInformationByAppendResponse;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

/**
 * User: alex89
 * Date: 04.02.2016
 * todo: доделать
 */

//@Stories("FASTSRV")
//@Feature("PQ")
//@Aqua.Test
//@Title("AppendDifferentLettesBodiesTest. Поверка добавления меток к письму в таблицу mail.box и mail.labels постгреса")
//@Description("Проверяем, что к письму в таблицу mail.box записались нужные lids меток," +
//        " и метки эти верно предствлены (name,type,color) в таблице mail.labels")
//@RunWith(Parameterized.class)
public class AppendDifferentLettesHeadersTest {
    private static final Pattern RESP_PATTERN =
            compile("\\{\"mid\":\"(?<mid>[0-9]+)\"\\,\"imap_id\":\"(?<imapId>[0-9]+)\"\\}");
    private static final Pattern SESSION_ID_PATTERN =
            compile("fastsrv: (?<sessionId>[0-9A-Za-z]+): message stored in");
    private static final Pattern HINT_PATTERN = compile("X-Yandex-Hint: (?<hint>.*)");
    private static final User RECEIVER = new User("smmtpgate-test@ya.ru", "testqa12345678");
    private static Logger log = LogManager.getLogger(AppendHintParamsTransmissionTest.class);

    @Parameterized.Parameter(0)
    public String emlFileName;

    private Response response;
    private TestMessage msg;


    @Rule
    public SshConnectionRule sshConnectionRule = new SshConnectionRule("mxback-qa.cmail.yandex.net");
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        File list = new File(currentThread().getContextClassLoader().getResource("emls.list").getFile());
        Collection<Object[]> data = new LinkedList<Object[]>();
        for (String fName : (List<String>) FileUtils.readLines(list)) {
            data.add(new Object[]{fName});
        }
        return data;
    }

    @BeforeClass
    public static void cleanUser() {
        inMailbox(RECEIVER).clearDefaultFolder();
    }




    @Before
    public void executeAppend() throws IOException, MessagingException {
        File emlf = LogToFileUtils.getLogFile();
        System.out.println(elliptics().indefinitely().path("ru/yandex/autotests/innerpochta/tests/WrongEncodingsTest/")
                .name(emlFileName).get().fullpath());
        elliptics().indefinitely().path("ru/yandex/autotests/innerpochta/tests/WrongEncodingsTest/")
                .name(emlFileName).get().asFile(emlf);
        log.info("Работаем с письмом: " + emlFileName);
        msg = new TestMessage(emlf);

           String textOfMsg="";
        for (Enumeration e = msg.getAllHeaders(); e.hasMoreElements();) {
            Header h = (Header) e.nextElement();
            textOfMsg = textOfMsg+h.getName() + ": " + h.getValue()+"\n";
        }
        textOfMsg = textOfMsg+"\n\n"+
        IOUtils.toString(msg.getInputStream(), Charsets.UTF_8);


        System.out.println(textOfMsg);
        textOfMsg = elliptics().indefinitely().path("ru/yandex/autotests/innerpochta/tests/WrongEncodingsTest/")
                .name(emlFileName).get().asString();
        log.info("Выполняем append");
        response = smtpgateApi().queryParam("fid", "1").queryParam("date", "1454436917").letter(textOfMsg)
                .queryParam("src_email", "smmtpgate-test@ya.ru").append();

        System.out.println("===============================");
        System.out.println(emlFileName);
        System.out.println("===============================");

        System.out.println(IOUtils.toString( inMailbox(RECEIVER).getMessageWithSubject(msg.getSubject().replace("Re: ", "")).getInputStream()));


        assertThat("Us!",
                IOUtils.toString( inMailbox(RECEIVER).getMessageWithSubject(msg.getSubject().replace("Re: ","")).getInputStream(), Charsets.UTF_8),
                equalTo(textOfMsg.substring(textOfMsg.indexOf("\n\n")+2)+"\r\n"));
    }



    @Test
    public void shouldSeeHintTransmissionToFastsrvInMaillogDebug() throws Exception {
        assertThat("Неверный ответ append-запроса!",
                response, allOf(hasStatusCode(OK_200), hasResponseBody(containsPattern(RESP_PATTERN))));
        assertThat("Append-запрос неверно пробросил параметры в X-Yandex-Hint (fastsrv)!",
                getHintInformationByAppendResponse(response.asString(), RECEIVER.getLogin(), sshConnectionRule),
                equalTo("received_date=1454436917 fid=1 skip_loop_prevention=1 notify=0 filters=0 mixed=0 imap=1"));
    }

}
