package ru.yandex.autotests.smtpgate.tests.send_system_mail;

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
import static java.util.regex.Pattern.compile;

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.cthul.matchers.object.ContainsPattern.containsPattern;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.cthul.matchers.object.ContainsPattern.containsPattern;
import static ru.yandex.autotests.innerpochta.tests.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.util.InnerpochtaProjectProperties.chooseUser;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasResponseBody;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasStatusCode;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi.smtpgateApi;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.SEND_SYSTEM_MAIL_RESP_PATTERN;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.getHintInformationBySendSystemMailResponse;
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
@Feature("SEND_SYSTEM_MAIL")
@Aqua.Test
@Title("SendSystemMailHintParamsTransmissionTest. Передача параметров в X-Yandex-Hint")
@Description("Проверяем, что параметры send_system_mail запроса верно преобразуются в параметры X-Yandex-Hint")
@RunWith(Parameterized.class)
public class SendSystemMailHintParamsTransmissionTest {
    private static final User RECEIVER =
            chooseUser(new User("smmtpgate-send-system-mail@ya.ru", "testqa12345678"),
                    new User("mxtest-33@mail.yandex-team.ru", "OqKEt7R9sr1v"));
    private static String emlText;
    private String subject = randomAlphanumeric(10);
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
        data.add(new Object[]{
                smtpgateApi().queryParam("label", "symbol:seen_label").queryParam("lid", "1"),
                "ipfrom=::1 label=SystMetkaSO:people label=SystMetkaSO:t_people label=SystMetkaSO:trust_4 label=symbol:seen_label lid=1 session_id=[a-zA-Z0-9-]+"});
        data.add(new Object[]{
                smtpgateApi().queryParam("lid", "2").queryParam("lid", "1"),
                "ipfrom=::1 label=SystMetkaSO:people label=SystMetkaSO:t_people label=SystMetkaSO:trust_4 lid=1 lid=2 session_id=[a-zA-Z0-9-]+"});
        data.add(new Object[]{
                smtpgateApi().queryParam("label", "dfasdaasf12323").queryParam("lid", "1"),
                "ipfrom=::1 label=SystMetkaSO:people label=SystMetkaSO:t_people label=SystMetkaSO:trust_4 label=dfasdaasf12323 lid=1 session_id=[a-zA-Z0-9-]+"});

        return data;
    }

    @BeforeClass
    public static void readMsg() throws IOException {
        emlText = asCharSource(getResource("simple-letter2.eml"), Charsets.UTF_8).read();
        inMailbox(RECEIVER).clearAll();
    }

    @Before
    public void executeAppend() {
        response = request
                .from("sfdsdsfsdfsf@ya.ru")
                .letter(emlText
                        .replace("SIMPLE_LETTER", subject)
                        .replace("<11446029210@verstka8-qa.yandex.ru>", "<" + randomAlphanumeric(10) + ">"))
                .to(RECEIVER.getLogin()).sendSystemMail();
    }


    @Test
    public void shouldSeeHintTransmissionToFastsrvInMaillogDebug() throws Exception {
        assertThat("Неверный ответ store-запроса![MPROTO-3809],[MPROTO-3812]",
                response, allOf(hasStatusCode(OK_200), hasResponseBody(containsPattern(SEND_SYSTEM_MAIL_RESP_PATTERN))));

        List<String> extractedHints = getHintInformationBySendSystemMailResponse(response.asString(),
                sshConnectionRule);
        System.out.println(extractedHints);
        assertThat("SendSystemMail-запрос неверно пробросил параметры в X-Yandex-Hint (NSLS)!\n" +
                        "Не видно в логе нужного количества X-Yandex-Hint-заголовков.",
                extractedHints, hasSize(equalTo(1)));
        assertThat("SendSystemMail-запрос неверно пробросил параметры в X-Yandex-Hint (NSLS)!",
                extractedHints,
                hasItem(containsPattern(compile(prepareHint(expectedHint)))));
        inMailbox(RECEIVER).shouldSeeLetterWithSubject(subject);
    }
}
