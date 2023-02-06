package ru.yandex.autotests.smtpgate.tests.send_mail;

import com.google.common.base.Charsets;
import com.jayway.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.tests.utils.SshConnectionRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.cthul.matchers.object.ContainsPattern.containsPattern;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasResponseBody;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasStatusCode;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi.smtpgateApi;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.SEND_SYSTEM_MAIL_RESP_PATTERN;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.getHintInformationBySendMailResponse;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.prepareHint;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateProperties.smtpgateProps;

/**
 * User: alex89
 * Date: 04.02.2016
 * <p>
 * https://wiki.yandex-team.ru/BitovyeFlagiISMIXED/
 * https://wiki.yandex-team.ru/pochta/mproto/smtp-gate/
 */
@Stories("Positive")
@Feature("SEND_MAIL")
@Aqua.Test
@Title("SendMailHintParamsTransmissionTest. Передача параметров в X-Yandex-Hint")
@Description("Проверяем, что параметры send_mail запроса верно преобразуются в параметры X-Yandex-Hint")
@RunWith(Parameterized.class)
public class SendMailHintParamsTransmissionTest {
    private static final User RECEIVER = new User("smmtpgate-sendmail-test@ya.ru", "testqa12345678");
    private static final User SENDER = new User("smmtpgate-sendmail-test2@ya.ru", "testqa12345678");
    private static String emlText;
    private String subject = randomAlphanumeric(10);
    private Response response;

    @Parameterized.Parameter(0)
    public SmtpgateApi request;
    @Parameterized.Parameter(1)
    public Matcher expectedHintMather;
    @Parameterized.Parameter(2)
    public int expectedNumberOfHints;

    @Rule
    public SshConnectionRule sshConnectionRule = new SshConnectionRule(smtpgateProps().getHost());
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new LinkedList<Object[]>();
        // "ipfrom" should be "10.0.0.1" after MAILDLV-1918
        data.add(new Object[]{
                smtpgateApi().requestId(randomAlphanumeric(10) + "-requestId")
                        .queryParam("from_uid", "12345678")
                        .queryParam("from_email", SENDER.getLogin())
                        .queryParam("sender_email", SENDER.getLogin())
                        .to(RECEIVER.getLogin())
                        .queryParam("ip", "10.0.0.1").queryParam("source", "web")
                        .queryParam("host", "unconfigured.yandex.ru")
                        .queryParam("phone", "+79626937266")
                        .queryParam("lid", "11").queryParam("sender_label", "forsender")
                        .queryParam("common_label", "common"),
                allOf(hasItem(containsPattern(compile(prepareHint("ipfrom=::1 label=SystMetkaSO:people label=SystMetkaSO:trust_5 label=common session_id=[a-zA-Z0-9-]+")))),
                        hasItem(containsPattern(compile(prepareHint("email=smmtpgate-sendmail-test2@ya.ru " +
                                "filters=0 host=unconfigured.yandex.ru label=forsender lid=11 notify=0 " +
                                "phone=\\+79626937266 received_date=[0-9]+ save_to_sent=0 " +
                                "skip_loop_prevention=1"))))), 2
        });
        data.add(new Object[]{
                smtpgateApi().requestId(randomAlphanumeric(10) + "-requestId")
                        .queryParam("from_uid", "12345678")
                        .queryParam("from_email", SENDER.getLogin())
                        .queryParam("sender_email", SENDER.getLogin())
                        .from(SENDER.getLogin()).to(SENDER.getLogin()).to(RECEIVER.getLogin())
                        .queryParam("ip", "10.0.0.1").queryParam("source", "web")
                        .queryParam("host", "unconfigured.yandex.ru")
                        .queryParam("phone", "+79626937266")
                        .queryParam("lid", "11").queryParam("sender_label", "forsender")
                        .queryParam("common_label", "common"),
                allOf(hasItem(containsPattern(compile(prepareHint("ipfrom=::1 label=SystMetkaSO:people label=SystMetkaSO:trust_5 label=common session_id=[a-zA-Z0-9-]+")))),
                        hasItem(containsPattern(compile(prepareHint("email=smmtpgate-sendmail-test2@ya.ru " +
                                "filters=0 host=unconfigured.yandex.ru label=forsender lid=11 notify=0 " +
                                "phone=\\+79626937266 received_date=[0-9]+ save_to_sent=0 " +
                                "skip_loop_prevention=1"))))), 2
        });
        data.add(new Object[]{
                smtpgateApi().requestId(randomAlphanumeric(10) + "-requestId")
                        .queryParam("from_uid", "12345678")
                        .queryParam("from_email", SENDER.getLogin())
                        .queryParam("sender_email", SENDER.getLogin())
                        .from(SENDER.getLogin()).to(SENDER.getLogin()).to(RECEIVER.getLogin())
                        .queryParam("ip", "10.0.0.1").queryParam("source", "web")
                        .queryParam("host", "unconfigured.yandex.ru")
                        .queryParam("phone", "+79626937266")
                        .queryParam("lid", "11").queryParam("sender_label", "forsender")
                        .queryParam("common_label", "common")
                        .queryParam("save_to_sent", "1"),
                allOf(hasItem(containsPattern(compile(prepareHint("ipfrom=::1 label=SystMetkaSO:people label=SystMetkaSO:trust_5 label=common session_id=[a-zA-Z0-9-]+")))),
                        hasItem(containsPattern(compile(prepareHint("email=smmtpgate-sendmail-test2@ya.ru " +
                                "filters=0 host=unconfigured.yandex.ru label=forsender lid=11 notify=0 " +
                                "phone=\\+79626937266 received_date=[0-9]+ save_to_sent=1 " +
                                "skip_loop_prevention=1"))))), 2
        });
        data.add(new Object[]{
                smtpgateApi().requestId(randomAlphanumeric(10) + "-requestId")
                        .queryParam("from_uid", "12345678")
                        .queryParam("from_email", SENDER.getLogin())
                        .queryParam("sender_email", SENDER.getLogin())
                        .from(SENDER.getLogin()).to(SENDER.getLogin()).to(RECEIVER.getLogin())
                        .queryParam("ip", "10.0.0.1").queryParam("source", "web")
                        .queryParam("host", "unconfigured.yandex.ru")
                        .queryParam("phone", "+79626937266")
                        .queryParam("lid", "11").queryParam("sender_label", "forsender")
                        .queryParam("common_label", "common")
                        .queryParam("save_to_sent", "1").queryParam("notify", "1"),
                allOf(hasItem(containsPattern(compile(prepareHint("ipfrom=::1 label=SystMetkaSO:people label=SystMetkaSO:trust_5 label=common session_id=[a-zA-Z0-9-]+")))),
                        hasItem(containsPattern(compile(prepareHint("email=smmtpgate-sendmail-test2@ya.ru " +
                                "filters=0 host=unconfigured.yandex.ru label=forsender lid=11 notify=0 " +
                                "phone=\\+79626937266 received_date=[0-9]+ save_to_sent=1 " +
                                "skip_loop_prevention=1"))))), 2
        });
        return data;
    }

    @BeforeClass
    public static void readMsg() throws IOException {
        emlText = asCharSource(getResource("simple-letter2.eml"), Charsets.UTF_8).read();
    }

    @Before
    public void executeAppend() {
        response = request
                .letter(emlText
                        .replace("SIMPLE_LETTER", subject)
                        .replace("<11446029210@verstka8-qa.yandex.ru>", "<" + randomAlphanumeric(10) + ">"))
                .sendMail();
    }


    @Test
    public void shouldSeeHintTransmissionToFastsrvInMaillogDebug() throws Exception {
        assertThat("Неверный ответ send_mail-запроса!", response,
                allOf(hasStatusCode(OK_200), hasResponseBody(containsPattern(SEND_SYSTEM_MAIL_RESP_PATTERN))));
        inMailbox(RECEIVER).shouldSeeLetterWithSubject(subject);
        List<String> extractedHints = getHintInformationBySendMailResponse(response.asString(),
                sshConnectionRule);
        System.out.println(extractedHints);
        assertThat("SendMail-запрос неверно пробросил параметры в X-Yandex-Hint (NSLS)!\n" +
                        "Не видно в логе нужного количества X-Yandex-Hint-заголовков.",
                extractedHints, hasSize(equalTo(expectedNumberOfHints)));
        assertThat("SendMail-запрос неверно пробросил параметры в X-Yandex-Hint (NSLS)!",
                extractedHints, expectedHintMather);
    }
}
