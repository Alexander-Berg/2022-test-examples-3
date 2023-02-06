package ru.yandex.autotests.innerpochta.tests.pq;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.innerpochta.utils.HintData;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqMatchers.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailBoxPqTable.mailBoxTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailMessagesPqTable.mailMessagesTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 05.05.15
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка записи данных в mail.messages и mail.box при отправке письма самому себе",
        description = "Проверяем, что корректно записались данные пг-таблиц mail.messages и mail.box " +
                "при отправке письма самому себе")
@Title("MessageToMyselfPqTest.Поверка записи данных в mail.messages и mail.box при отправке письма самому себе")
@Description("Проверяем, что корректно записались данные пг-таблиц mail.messages и mail.box " +
        "при отправке письма самому себе")
@RunWith(Parameterized.class)
public class MessageToMyselfPqTest {
    private static final int MIXED_VALUE = 2049;
    private static final long RECEIVED_DATE_VALUE = 1480995884;

    private static final String EXPECTED_FIRSTLINE = "plesk";
    private static final String EXPECTED_ATTACHES = "{\"(1.2,text/plain,\\\"new  11.txt\\\",636)\"}";
    private static final String EXPECTED_HDR_DATE = "2015-10-28 13:46:50+03";
    private static final org.hamcrest.Matcher<String> EXPECTED_MULCA_SHARED_ATTRIBUTE =
            anyOf(equalTo("{mulca-shared,postmaster}"),
                    equalTo("{mulca-shared}"),
                    equalTo("{postmaster,mulca-shared}"));
    private static final String EXPECTED_RECIPIENTS = "{\"(from,\\\"\\\",pg-test-sent-folder-user@yandex.ru)\"," +
            "\"(to,\\\"\\\",pg-test-sent-folder-user@yandex.ru)\"," +
            "\"(reply-to,\\\"\\\",pg-test-sent-folder-user@yandex.ru)\"}";

    private static Logger log = LogManager.getLogger(MessageToMyselfPqTest.class);
    private static String receiverUid = "340997321";
    private TestMessage testMsg;
    private String sessionLog;
    private String midOfFirstLetter;
    private String midOfSecondLetter;
    private String expectedStid;
    private static User receiver;

    @Parameterized.Parameter(0)
    public HintData.XYandexHintValue hintValue;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{createHintValue().addRcvDate(RECEIVED_DATE_VALUE).addLabel("symbol:seen_label")
                .addCopyToInbox("1").addHost("mail.yandex.ru").addSaveToSent("1")
                .addSkipLoopPrevention("1")
                .addNotify("0").addFilters("0")});

        return data;
    }

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");

    @BeforeClass
    public static void setReceiver() throws Exception {
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        testMsg = new TestMessage(
                new File(this.getClass().getClassLoader().getResource("simple-attach.eml").getFile()));
        testMsg.setSubject("MessageToMyselfPqTest " + randomAlphanumeric(20));
        testMsg.setFrom(receiver.getEmail());
        testMsg.setRecipient(receiver.getEmail());
        testMsg.setHeader(X_YANDEX_HINT, hintValue.addEmail(receiver.getEmail()).encode());
        testMsg.saveChanges();

        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
        assertThat("Hе прошла покладка в ПГ!", sessionLog, anyOf(containsString("message stored in pq backed"),
                containsString("message stored in db=pg backed")));
        assertThat("Должны положить в ПГ два mid-а!", extractedAllMidsFromLog(sessionLog),
                IsCollectionWithSize.hasSize(2));
        midOfFirstLetter = extractedAllMidsFromLog(sessionLog).get(0);
        midOfSecondLetter = extractedAllMidsFromLog(sessionLog).get(1);
        expectedStid = extractedParamFromLogByPattern(STID_LOG_STRING_PATTERN, sessionLog);
    }

    @Test
    public void shouldSeeDataInMailMessagesAndMailBoxPqTablesForFirstLetter() throws MessagingException {
        assertThat("Записались неверные данные в таблицу mail.messages для письма, " +
                        "предназначенного для покладки в Отправленные",
                mailMessagesTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFirstLetter),
                allOf(hasStid(equalTo(expectedStid)),
                        hasAttributes(EXPECTED_MULCA_SHARED_ATTRIBUTE),
                        hasAttaches(equalTo(EXPECTED_ATTACHES)),
                        hasFirstLine(equalTo(EXPECTED_FIRSTLINE.trim())),
                        hasSubject(equalTo(testMsg.getSubject())),
                        hasHdrDate(equalTo(EXPECTED_HDR_DATE)), //MPROTO-1768
                        hasRecipients(equalTo(EXPECTED_RECIPIENTS)), //MPROTO-1768
                        hasHdrMsgId(equalTo(testMsg.getMessageID()))));

        assertThat("Записались неверные данные в таблицу mail.box для письма, " +
                        "предназначенного для покладки в Отправленные",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFirstLetter),
                allOf(hasFid(equalTo(SENT_FOLDER_FID)),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("t")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
    }

    @Test
    public void shouldSeeDataInMailMessagesAndMailBoxPqTablesForSecondLetter() throws MessagingException {
        assertThat("Записались неверные данные в таблицу mail.messages для письма, " +
                        "предназначенного для покладки во Входящие",
                mailMessagesTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfSecondLetter),
                allOf(hasStid(equalTo(expectedStid)),
                        hasAttributes(EXPECTED_MULCA_SHARED_ATTRIBUTE),
                        hasAttaches(equalTo(EXPECTED_ATTACHES)),
                        hasFirstLine(equalTo(EXPECTED_FIRSTLINE.trim())),
                        hasSubject(equalTo(testMsg.getSubject())),
                        hasHdrDate(equalTo(EXPECTED_HDR_DATE)), //MPROTO-1768
                        hasRecipients(equalTo(EXPECTED_RECIPIENTS)), //MPROTO-1768
                        hasHdrMsgId(equalTo(testMsg.getMessageID()))));

        assertThat("Записались неверные данные в таблицу mail.box для письма, " +
                        "предназначенного для покладки во Входящие",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfSecondLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
    }

    private static List<String> extractedAllMidsFromLog(String sessionLogDebug) {
        List<String> allMids = new ArrayList<String>();
        Matcher logParamMatcher = MID_LOG_STRING_PATTERN.matcher(sessionLogDebug);
        while (logParamMatcher.find()) {
            allMids.add(logParamMatcher.group(1));
        }
        return allMids;
    }
}
