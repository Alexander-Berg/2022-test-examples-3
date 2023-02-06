package ru.yandex.autotests.innerpochta.tests.pq;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.Matcher;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
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

import java.util.Collection;
import java.util.LinkedList;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqReferencesMatcher.hasReferenceNotationWithValueAndType;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailMessageReferencesPqTable.mailMessageReferencesTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;

/**
 * User: alex89
 * Date: 05.05.15
 * private static final User RECEIVER = new User("mxqa-pg@ya.ru", "qwerty123456");
 * private static final String RECEIVER_UID = "324457098";
 * <p>
 * public static SshLocalPortForwardingRule fwd = viaRemoteHost(URI.create("https://baida2-qa.yandex.ru"))
 * .forwardTo(URI.create("jdbc://xdb-qa01h.cmail.yandex.net:6432"))
 * .onLocalPort(localPortForMocking());
 * <p>
 * Учтено MPROTO-4174
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка записи данных в таблицу mail.message_references постгреса",
        description = "Проверяем, что к письму в таблицу mail.message_references записались нужные хэши, " +
                "соответствующие содержимому заголовков References и In-Reply-To.")
@Title("MailMessageReferencesTest. Поверка записи данных в таблицу mail.message_references постгреса")
@Description("Проверяем, что к письму в таблицу mail.message_references записались нужные хэши, " +
        "соответствующие содержимому заголовков References и In-Reply-To.")
@RunWith(Parameterized.class)
public class MailMessageReferencesTest {
    private static final String RECEIVER_UID = "344625229";
    private static final String REFERENCE_TYPE = "reference";
    private static final String IN_REPLY_TO_TYPE = "in-reply-to";
    private static final String IN_REPLY_TO = "In-Reply-To";
    private static final String REF = "References";

    private static Logger log = LogManager.getLogger(MailMessageReferencesTest.class);
    private TestMessage testMsg;
    private String sessionLog;
    private String mid;
    private static User receiver;

    @Parameterized.Parameter(0)
    public String refHeaderValue;
    @Parameterized.Parameter(1)
    public String inReplyToHeaderValue;
    @Parameterized.Parameter(2)
    public Matcher expectedLidsMatcher;
    @Parameterized.Parameter(3)
    public String caseComment;

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
        data.add(new Object[]{"<aaaa@ssss.ru>\n <bbbb@ssss.ru>", "<aaaa@ssss.ru>",
                allOf(hasReferenceNotationWithValueAndType("6629733468597843020", REFERENCE_TYPE),
                        hasReferenceNotationWithValueAndType("2281081122731893304", IN_REPLY_TO_TYPE), hasSize(3)),
                "записываем два корректых reference, один из которых указан в In-Reply-To"});
        data.add(new Object[]{"<aaaa@ssss.ru>\n <bbbb@ssss.ru>\n <bbbb@ssss.ru>\n <bbbbssssru>", "<aaaa@ssss.ru>",
                allOf(hasReferenceNotationWithValueAndType("6629733468597843020", REFERENCE_TYPE),
                        hasReferenceNotationWithValueAndType("2281081122731893304", IN_REPLY_TO_TYPE), hasSize(3)),
                "записываем два корректых reference, один из которых указан в In-Reply-To," +
                        "и два некорректных reference"});
        data.add(new Object[]{"<a1@ssss.ru>\n <sss.ru>\n <b2@ssss.ru>\n <bbbbssssru>", "<aaaa@ssss.ru>",
                allOf(hasReferenceNotationWithValueAndType("9646593150664821194", REFERENCE_TYPE),
                        hasReferenceNotationWithValueAndType("5147459602253373628", REFERENCE_TYPE),
                        hasReferenceNotationWithValueAndType("2281081122731893304", IN_REPLY_TO_TYPE), hasSize(4)),
                "записываем два корректых и два некорректных reference, In-Reply-To указан, " +
                        "но с reference не пересекается"});
        data.add(new Object[]{"<aaaa@ssss.ru>\n <bbbb@ssss.ru>", null,
                allOf(hasReferenceNotationWithValueAndType("6629733468597843020", REFERENCE_TYPE),
                        hasReferenceNotationWithValueAndType("2281081122731893304", REFERENCE_TYPE), hasSize(3)),
                "записываем два корректых reference, заголовок In-Reply-To не указан"});
        data.add(new Object[]{"<aaaa@ssss.ru>\n <aaaa@ssss.ru>", null,
                allOf(hasReferenceNotationWithValueAndType("2281081122731893304", REFERENCE_TYPE), hasSize(2)),
                "записываем два корректых одинаковых reference, заголовок In-Reply-To не указан"});
        data.add(new Object[]{"<aaaa@ssss.ru>\n <aaaa@ssss.ru>", "<aaaa@ssss.ru>",
                allOf(hasReferenceNotationWithValueAndType("2281081122731893304", IN_REPLY_TO_TYPE), hasSize(2)),
                "записываем два корректых одинаковых reference, и заголовок In-Reply-To указан такой же"});
        data.add(new Object[]{null, "<aaaa@ssss.ru>",
                allOf(hasReferenceNotationWithValueAndType("2281081122731893304", IN_REPLY_TO_TYPE), hasSize(2)),
                "заголовок In-Reply-To указан, а References - нет"});
        data.add(new Object[]{null, null, hasSize(1), "заголовки In-Reply-To и References не указаны"});
        return data;
    }

    @BeforeClass
    public static void setReceiver() throws Exception {
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void prepareTestMessage() throws Exception {
        testMsg = new TestMessage();
        testMsg.setSubject("MailMessageReferencesTest" + randomAlphabetic(10));
        testMsg.setText(randomAlphabetic(10));
        testMsg.setRecipient(receiver.getEmail());
        if (refHeaderValue != null) {
            testMsg.setHeader(REF, refHeaderValue);
        }
        if (inReplyToHeaderValue != null) {
            testMsg.setHeader(IN_REPLY_TO, inReplyToHeaderValue);
        }
        testMsg.saveChanges();
        log.info(format("Отправили письмо с темой %s", testMsg.getSubject()));
        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
//        assertThat("Hе прошла покладка в ПГ!", sessionLog, anyOf(containsString("message stored in pq backed"),
//                containsString("message stored in db=pg backed")));
        mid = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
    }

    @Test
    @Ignore("MAILDLV-3396")
    public void shouldSeeCorrectSetOfValuesAndTypesInMailMessageReferenceTable() {
        assertThat(format("Записались неверные данные о references в таблицу " +
                        "mail.message_references для случая: '%s'", caseComment),
                mailMessageReferencesTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                allOf(expectedLidsMatcher));
    }
}
