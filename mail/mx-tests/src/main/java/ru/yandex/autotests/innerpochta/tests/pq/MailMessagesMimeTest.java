package ru.yandex.autotests.innerpochta.tests.pq;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.yaml.snakeyaml.Yaml;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static javax.mail.Session.getDefaultInstance;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqMatchers.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailMessagesPqTable.mailMessagesTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

/**
 * User: alex89
 * Date: 05.05.17
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Сверка разницы с продакшн записи mime в таблицу mail.messages постгреса",
        description = "Проверяем, что корректно записались данные mime в таблицу mail.messages постгреса [MPROTO-3714]")
@Title("MailMessagesMimeTest. Сверка разницы с продакшн записи mime в таблицу mail.messages постгреса")
@Description("Проверяем, что корректно записались данные mime в таблицу mail.messages постгреса [MPROTO-3714] ")
@RunWith(Parameterized.class)
public class MailMessagesMimeTest {
    private static Logger log = LogManager.getLogger(MailMessagesMimeTest.class);
    public static final Pattern OFFSET_DIGITS_PATTERN_1 = compile(",([0-9]+),");
    public static final Pattern OFFSET_DIGITS_PATTERN_2 = compile(",([0-9]+)\\)");
    private static final Pattern LMTP_RESPONSE_VALIDATION_PATTERN = compile("250 2.0.0 Ok; [_A-Za-z0-9-]+ ([0-9]+).*");
    private static final String PATTERN_FOR_REPLACE_MSG_ID = "MSG_ID";
    public static final String PRODUCTION_MX_SERVER = "notsolitesrv.canary.notsolitesrv.mail.stable.qloud-d.yandex.net";
    public static final int PRODUCTION_MX_PORT = 1234;
    private static final String RECEIVER_UID = "508776282";
    private TestMessage testMsg;
    private TestMessage testMsg2;
    private String sessionLog;
    private String mid;
    private String midProd;
    private String expectedStid;
    private static User receiver;

    @Parameterized.Parameter(0)
    public String emlFileName;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        Map<String, List<String>> testCasesMap =
                new Yaml().loadAs(asCharSource(getResource("pq_data/pq_mime.yml"), UTF_8).read(), Map.class);
        for (String emlName : testCasesMap.keySet()) {
            data.add(new Object[]{emlName}); //оставил ямл-файл -  в нем можно глянуть примеры старых майм записей
        }
        return data;
    }

    @BeforeClass
    public static void setReceiver() throws Exception {
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void sendTestMsgViaQA() throws Exception {
        String emlForQa = elliptics().indefinitely().path(MailMessagesMimeTest.class)
                .name(emlFileName).get().asString()
                .replace(PATTERN_FOR_REPLACE_MSG_ID, randomAlphanumeric(20));

        testMsg = new TestMessage(new MimeMessage(getDefaultInstance(new Properties()),
                toInputStream(emlForQa, "UTF-8")));
        testMsg.setRecipient(receiver.getEmail());

        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);

        mid = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
        expectedStid = extractedParamFromLogByPattern(STID_LOG_STRING_PATTERN, sessionLog);
    }

    @Before
    public void sendTestMsgViaProd() throws Exception {
        String emlForProd = elliptics().indefinitely().path(MailMessagesMimeTest.class)
                .name(emlFileName).get().asString()
                .replace(PATTERN_FOR_REPLACE_MSG_ID, randomAlphanumeric(20));

        testMsg2 = new TestMessage(new MimeMessage(getDefaultInstance(new Properties()),
                toInputStream(emlForProd, "UTF-8")));
        testMsg2.setRecipient(receiver.getEmail());

        String serverResponseProd = sendbyNsls(testMsg2, PRODUCTION_MX_SERVER, PRODUCTION_MX_PORT);
        log.info(serverResponseProd);

        midProd = extractedParamFromLogByPattern(LMTP_RESPONSE_VALIDATION_PATTERN, serverResponseProd);
    }

    @Test
    public void shouldSeeStidAttachesFirstlineSubjectHdrDateInMailMessagesPqTable()
            throws MessagingException {
        String expectedMime = mailMessagesTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, midProd).getMime();

        assertThat(format("Записались неверные данные в таблицу mail.messages для письма %s", emlFileName),
                mailMessagesTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                allOf(hasStid(equalTo(expectedStid)),
                        hasSubject(equalTo(testMsg.getSubject())),
                        hasMime(equalTo(expectedMime)),
                        hasHdrMsgId(equalTo(testMsg.getMessageID()))));
    }
}
