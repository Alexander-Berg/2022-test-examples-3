package ru.yandex.autotests.smtpgate.tests.save;

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

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.cthul.matchers.object.ContainsPattern.containsPattern;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasResponseBody;
import static ru.yandex.autotests.smtpgate.tests.matchers.SmtpgateMatchers.hasStatusCode;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateApi.smtpgateApi;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.*;
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
@Feature("SAVE")
@Aqua.Test
@Title("SaveHintParamsTransmissionTest. Передача параметров в X-Yandex-Hint")
@Description("Проверяем, что параметры save запроса верно преобразуются в параметры X-Yandex-Hint")
@RunWith(Parameterized.class)
public class SaveHintParamsTransmissionTest {
    private static final User RECEIVER = new User("smmtpgate-save-test@ya.ru", "testqa12345678");
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
        data.add(new Object[]{
                smtpgateApi().requestId(randomAlphanumeric(10)+"-"+ randomAlphanumeric(10))
                        .queryParam("fid", "1").queryParam("date", "1454436917"),
                prepareHint("fid=1 filters=0 notify=0 received_date=1454436917 skip_loop_prevention=1 skip_meta_msg=1 sync_dlv=1")});
        data.add(new Object[]{
                smtpgateApi().requestId(randomAlphanumeric(10)+"-"+ randomAlphanumeric(10))
                        .queryParam("fid", "1").queryParam("date", "1454436917").queryParam("lid", "10")
                        .queryParam("lid", "11"),
                prepareHint("fid=1 filters=0 lid=10 lid=11 notify=0 received_date=1454436917 skip_loop_prevention=1 skip_meta_msg=1 sync_dlv=1")});

        data.add(new Object[]{
                smtpgateApi().requestId(randomAlphanumeric(10)+"-"+ randomAlphanumeric(10))
                        .queryParam("fid", "6").queryParam("date", "1454436917")
                        .queryParam("label", "symbol:draft_label")
                        .queryParam("label", "symbol:seen_label")
                        .queryParam("old_mid", "157907461934678023")
                        .queryParam("detect_spam","1"),
                prepareHint("fid=6 filters=0 label=symbol:draft_label label=symbol:seen_label" +
                        " mid=157907461934678023 notify=0 received_date=1454436917" +
                        " skip_loop_prevention=1 skip_meta_msg=1 sync_dlv=1")});
        return data;
    }

    @BeforeClass
    public static void readMsg() throws IOException {
        emlText = asCharSource(getResource("simple-letter2.eml"), Charsets.UTF_8).read();
    }

    @Before
    public void executeAppend() {
        response = request
                .forMailish()
                .letter(emlText
                        .replace("<11446029210@verstka8-qa.yandex.ru>", "<"+randomAlphanumeric(10)+">"))
                .srcEmail(RECEIVER.getLogin()).save();
    }


    @Test
    public void shouldSeeHintTransmissionToFastsrvInMaillogDebug() throws Exception {
        assertThat("Неверный ответ save-запроса![MPROTO-3874]",
                response, allOf(hasStatusCode(OK_200), hasResponseBody(containsPattern(SAVE_RESP_PATTERN))));

        List<String> extractredHints = getHintInformationBySaveResponse(response.asString(),
                RECEIVER.getLogin(), sshConnectionRule);
        assertThat("Save-запрос неверно пробросил параметры в X-Yandex-Hint (NSLS)!\n" +
                        "Не видно в логе нужного количества X-Yandex-Hint-заголовков.",
                extractredHints, hasSize(equalTo(1)));
        assertThat("Save-запрос неверно пробросил параметры в X-Yandex-Hint (NSLS)![MPROTO-3874]",
                extractredHints.get(0),equalTo(expectedHint));
    }
}
