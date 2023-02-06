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
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.util.Collection;
import java.util.LinkedList;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqLabelMatcher.hasLabelWithProperties;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqMatchers.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailBoxPqTable.mailBoxTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailMessagesPqTable.mailMessagesTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.tests.pq.PqTestMsgs.FIRSTLINE;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 05.05.15
 * https://wiki.yandex-team.ru/sergejjxandrikov/novoemetaxranilishhe/Метки/#simvolymetok
 * https://st.yandex-team.ru/MPROTO-2351
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка возможности передачи symbol-меток",
        description = "Проверяем, что корректно записались данные в  mail.box,mail.message при отправке " +
                "symbol-меток в Hint-е")
@Title("SymbolLabelsPqTest. Поверка возможности передачи symbol-меток")
@Description("Проверяем, что корректно записались данные в  mail.box,mail.message при отправке symbol-меток в Hint-е")
@RunWith(Parameterized.class)
public class SymbolLabelsPqTest {
    private static final String RECEIVER_UID = "595150344";
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage testMsg;
    private String sessionLog;
    private String mid;
    private String expectedTid;
    private String expectedStid;
    private static User receiver;

    @Parameterized.Parameter(0)
    public XYandexHintValue hintValue;
    @Parameterized.Parameter(1)
    public String expectedFid;
    @Parameterized.Parameter(2)
    public Matcher<MailBoxPqTable> expectedLidsMatcher;
    @Parameterized.Parameter(3)
    public String expectedSeenFlag;
    @Parameterized.Parameter(4)
    public String expectedRecentFlag;
    @Parameterized.Parameter(5)
    public String expectedDeletedFlag;
    @Parameterized.Parameter(6)
    public String expectedAttributes;
    @Parameterized.Parameter(7)
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
        data.add(new Object[]{createHintValue().addLabel("symbol:deleted"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE, "{}", "Проверка работы при несуществующем symbol"});
        data.add(new Object[]{createHintValue().addLabel("symbol:deleted_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, TRUE, "{}", "Проверка работы symbol:deleted_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:seen_label"),
                "1", hasLids(equalTo("{}")), TRUE, TRUE, FALSE, "{}", "Проверка работы symbol:seen_label"});
        data.add(new Object[]{createHintValue().addLid("FAKE_SEEN_LBL"), "1", hasLids(equalTo("{}")),
                FALSE, TRUE, FALSE, "{}", "Проверка отсутствия реакции на lid=FAKE_SEEN_LBL"});
        data.add(new Object[]{createHintValue().addLabel("FAKE_SEEN_LBL"),
                "1", hasLids(equalTo("{}")), TRUE, TRUE, FALSE, "{}",
                "Проверка работы FAKE_SEEN_LBL=symbol:seen_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:mute_label"),
                "1", hasLabelWithProperties("mute", SYSTEM_TYPE, isEmptyOrNullString()),
                TRUE, TRUE, FALSE, "{}", "Проверка выставления symbol:mute_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:pinned_label"),
                "1", hasLabelWithProperties("pinned", SYSTEM_TYPE, isEmptyOrNullString()), FALSE, TRUE, FALSE,
                "{}", "Проверка работы symbol:pinned_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:forwarded_label"),
                "1", hasLabelWithProperties("forwarded", SYSTEM_TYPE, isEmptyOrNullString()), FALSE, TRUE, FALSE,
                "{}", "Проверка работы symbol:forwarded_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:answered_label"),
                "1", hasLabelWithProperties("answered", SYSTEM_TYPE, isEmptyOrNullString()), FALSE, TRUE, FALSE,
                "{}", "Проверка работы symbol:answered_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:recent_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE, "{}", "Проверка работы symbol:recent_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:draft_label"),
                "1", hasLabelWithProperties("draft", SYSTEM_TYPE, isEmptyOrNullString()), FALSE, TRUE, FALSE,
                "{}", "Проверка работы symbol:draft_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:important_label"),
                "1", hasLabelWithProperties("priority_high", SYSTEM_TYPE, isEmptyOrNullString()), FALSE, TRUE, FALSE,
                "{}", "Проверка работы symbol:important_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:postmaster_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE,
                "{postmaster}", "Проверка работы symbol:postmaster_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:mulcaShared_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE,
                "{mulca-shared}", "Проверка работы symbol:mulcaShared_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:imap_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE, "{}", "Проверка работы symbol:imap_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:append_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE, "{append}", "Проверка работы symbol:append_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:copy_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE, "{copy}", "Проверка работы symbol:copy_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:spam_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE, "{spam}", "Проверка работы symbol:spam_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:remindNoAnswer_label"),
                "1", hasLabelWithProperties("remindme_threadabout:mark", SYSTEM_TYPE, isEmptyOrNullString()),
                FALSE, TRUE, FALSE, "{}", "Проверка работы symbol:remindNoAnswer_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:notifyNoAnswer_label"),
                "1", hasLabelWithProperties("SystMetkaWJDT:NOTIFY", SYSTEM_TYPE, isEmptyOrNullString()),
                FALSE, TRUE, FALSE, "{}", "Проверка работы symbol:notifyNoAnswer_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:remindMessage_label"),
                "1", hasLabelWithProperties("SystMetka:remindme_about_message", SYSTEM_TYPE, isEmptyOrNullString()),
                FALSE, TRUE, FALSE, "{}", "Проверка работы symbol:remindMessage_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:notifyMessage_label"),
                "1", hasLabelWithProperties("SystMetkaWJDT:NOTIFY_MESSAGE", SYSTEM_TYPE, isEmptyOrNullString()),
                FALSE, TRUE, FALSE, "{}", "Проверка работы symbol:notifyMessage_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:attached_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE, "{}", "Проверка работы symbol:attached_label"});
        data.add(new Object[]{createHintValue().addLabel("symbol:hasUserLabels_label"),
                "1", hasLids(equalTo("{}")), FALSE, TRUE, FALSE, "{}", "Проверка работы symbol:hasUserLabels_label"});
        return data;
    }

    @BeforeClass
    public static void setReceiver() throws Exception {
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        testMsg = new TestMessage();
        testMsg.setSubject("SymbolLabelsPqTest" + hintValue + randomAlphanumeric(7));
        testMsg.setText(FIRSTLINE);
        testMsg.setRecipient(receiver.getEmail());
        testMsg.setHeader(X_YANDEX_HINT, hintValue.encode());
        testMsg.saveChanges();
        log.info(format("Отправили письмо с темой %s и хинтовыми парметрами %s", testMsg.getSubject(), hintValue));
        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
        mid = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
        expectedTid = extractedParamFromLogByPattern(TID_LOG_STRING_PATTERN, sessionLog);
        expectedStid = extractedParamFromLogByPattern(STID_LOG_STRING_PATTERN, sessionLog);
    }

    @Test
    public void shouldSeeThatSymbolLabelWasAddedCorrectly() throws MessagingException {
        assertThat(format("Записались неверные данные в  mail.box для случая: '%s' [MPROTO-2351]", caseComment),
                mailBoxTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                allOf(hasFid(equalTo(expectedFid)),
                        expectedLidsMatcher,
                        hasTid(equalTo(expectedTid)),
                        hasSeen(equalTo(expectedSeenFlag)),
                        hasRecent(equalTo(expectedRecentFlag)),
                        hasDeleted(equalTo(expectedDeletedFlag))));
        assertThat(format("Записались неверные данные в  mail.messages для случая: '%s' [MPROTO-2351]", caseComment),
                mailMessagesTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                allOf(hasAttributes(equalTo(expectedAttributes)),
                        hasStid(equalTo(expectedStid)),
                        hasAttaches(equalTo("{}")),
                        hasFirstLine(equalTo(FIRSTLINE)),
                        hasSubject(equalTo(testMsg.getSubject())),
                        hasHdrMsgId(equalTo(testMsg.getMessageID()))));
    }
}
