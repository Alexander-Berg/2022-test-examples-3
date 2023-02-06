package ru.yandex.autotests.smtpgate.tests.store;

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
import java.util.Random;

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static java.util.Arrays.asList;
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
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.STORE_RESP_PATTERN;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.getHintInformationByStoreResponse;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateData.prepareHint;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateProperties.smtpgateProps;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * User: alex89
 * Date: 04.02.2016
 * smmtpgate-test testqa12345678
 * <p>
 * https://wiki.yandex-team.ru/BitovyeFlagiISMIXED/
 * https://wiki.yandex-team.ru/pochta/mproto/smtp-gate/
 */
@Stories("Positive")
@Feature("STORE")
@Aqua.Test
@Title("StoreHintParamsTransmissionTest. Передача параметров в X-Yandex-Hint")
@Description("Проверяем, что параметры store запроса верно преобразуются в параметры X-Yandex-Hint")
@RunWith(Parameterized.class)
public class StoreHintParamsTransmissionTest {
    private static final User RECEIVER_MAILISH = new User("mx-test-user@mail.ru", "testqa12345678");
    private static final String RECEIVER_MAILISH_UID = "570124575";
    private static final String EXPECTED_HINT_VALUE_WITH_SO_TYPES =
            " label=SystMetkaSO:people label=SystMetkaSO:t_people label=SystMetkaSO:trust_4";
    private static String emlText;
    private Response response;
    private String externalImapIdValue;

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
                smtpgateApi().queryParam("fid", "1").queryParam("date", "1454436917").queryParam("disable_push", "1"),
                prepareHint("disable_push=1 external_imap_id=%s fid=1 filters=0 imap=1 notify=0 " +
                        "received_date=1454436917 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Recent"), // Recent - игнорируется
                prepareHint("external_imap_id=%s fid=1 filters=0 imap=1 notify=0 " +
                        "received_date=1454436917 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Draft"),
                prepareHint("external_imap_id=%s fid=1 filters=0 imap=1 mixed=64 notify=0 " +
                        "received_date=1454436917 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Deleted"),
                prepareHint("external_imap_id=%s fid=1 filters=0 imap=1 mixed=128 notify=0 " +
                        "received_date=1454436917 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Answered"),
                prepareHint("external_imap_id=%s fid=1 filters=0 imap=1 mixed=1024 notify=0 " +
                        "received_date=1454436917 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("system_flags", "Seen"),
                prepareHint("external_imap_id=%s fid=1 filters=0 imap=1 mixed=2048 notify=0 " +
                        "received_date=1454436917 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("user_flags", "$Forwarded"),
                prepareHint("external_imap_id=%s fid=1 filters=0 imap=1 imaplabel=$Forwarded mixed=512 " +
                        "notify=0 received_date=1454436917 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("user_flags", "Forwarded"),
                prepareHint("external_imap_id=%s fid=1 filters=0 imap=1 imaplabel=Forwarded " +
                        "notify=0 received_date=1454436917 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436917").queryParam("user_flags", "Bob").queryParam("system_flags", "Seen")
                .queryParam("system_flags", "Answered"),
                prepareHint("external_imap_id=%s fid=1 filters=0 imap=1 imaplabel=Bob mixed=3072 " +
                        "notify=0 received_date=1454436917 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        data.add(new Object[]{smtpgateApi().queryParam("fid", "1")
                .queryParam("date", "1454436920").queryParam("user_flags", "Bob").queryParam("system_flags", "Seen")
                .queryParam("system_flags", "Answered")
                .queryParam("user_flags", "Eva1234567891011121314151617181920Brown")
                .queryParam("system_flags", "Deleted"),
                prepareHint("external_imap_id=%s fid=1 filters=0 imap=1 imaplabel=Bob " +
                        "imaplabel=Eva1234567891011121314151617181920Brown mixed=3200 notify=0 " +
                        "received_date=1454436920 skip_loop_prevention=1 sync_dlv=1" + EXPECTED_HINT_VALUE_WITH_SO_TYPES)});
        return data;
    }

    @BeforeClass
    public static void readMsg() throws IOException {
        emlText = asCharSource(getResource("simple-letter2.eml"), Charsets.UTF_8).read();
    }

    @Before
    public void executeAppend() {
        externalImapIdValue = format("%d", new Random().nextInt(100000));
        response = request.externalImapId(externalImapIdValue)
                .requestId(randomAlphanumeric(10) + "-storetest")
                .uid(RECEIVER_MAILISH_UID)
                //.karma("100")
                //.karma_status("3100")
                .letter(emlText.concat(randomAlphanumeric(10))).srcEmail(RECEIVER_MAILISH.getLogin()).store();
    }


    @Test
    public void shouldSeeHintTransmissionToFastsrvInMaillogDebug() throws Exception {
        assertThat("Неверный ответ store-запроса![MPROTO-3809],[MPROTO-3812]",
                response, allOf(hasStatusCode(OK_200), hasResponseBody(containsPattern(STORE_RESP_PATTERN))));

        List<String> extractredHints = getHintInformationByStoreResponse(response.asString(),
                RECEIVER_MAILISH.getLogin(), sshConnectionRule);
        assertThat("Store-запрос неверно пробросил параметры в X-Yandex-Hint (NSLS)!\n" +
                        "Не видно в логе нужного количества X-Yandex-Hint-заголовков.[MPROTO-3809],[MPROTO-3812]",
                extractredHints, hasSize(equalTo(1)));
        assertThat("Store-запрос неверно пробросил параметры в X-Yandex-Hint (NSLS)![MPROTO-3809],[MPROTO-3812]",
                extractredHints,
                hasSameItemsAsList(asList(format(expectedHint, externalImapIdValue))));
    }
}
