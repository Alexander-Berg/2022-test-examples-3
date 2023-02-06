package ru.yandex.autotests.innerpochta.tests.pq;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
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
import java.io.IOException;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
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
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 05.05.15
 * https://st.yandex-team.ru/MPROTO-2349#1447438326000
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка снятия метки remindme_threadabout:mark с письма",
        description = "Проверяем логику снятия метки remindme_threadabout:mark с письма MPROTO-2349")
@Title("RemindPqTest.Поверка снятия метки remindme_threadabout:mark с письма")
@Description("Проверяем логику снятия метки remindme_threadabout:mark с письма MPROTO-2349")
public class RemindPqTest {
    private static final long RCV_DATE_1 = 1450040514;
    private static final long RCV_DATE_2 = 1450040519;
    private static final long RCV_DATE_3 = 1450040520;
    private static final long RCV_DATE_4 = 1450040521;
    private static final long RCV_DATE_5 = 1450040550;
    private static Logger log = LogManager.getLogger(RemindPqTest.class);
    private static String receiverUid = "349302976";
    private TestMessage testMsg;
    private String sessionLog;
    private String midOfFirstLetter;
    private String midOfSecondLetter;
    private String midOfThirdLetter;
    private static User receiver;

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
        testMsg = new TestMessage();
        testMsg.setSubject("RemindPqTest " + randomAlphanumeric(20));
        testMsg.setHeader(X_YANDEX_HINT, createHintValue().addRcvDate(RCV_DATE_1).encode());
        testMsg.setText("first");
        testMsg.setRecipient(receiver.getEmail());
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfFirstLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);

        testMsg.setText("second");
        testMsg.setHeader(X_YANDEX_HINT, createHintValue().addRcvDate(RCV_DATE_3).addLabel(REMIND_SYS_LABEL).encode());
        testMsg.saveChanges();
        serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfSecondLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
    }

    @Test
    public void shouldSeeThatRemindLabelCanBeAdded() throws MessagingException, IOException {
        assertThat("Записались неверные данные в таблицу mail.box для первого письма",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFirstLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
        assertThat("Записались неверные данные в mail.box для 2го письма, что с меткой remindme_threadabout:mark",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfSecondLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLabelWithProperties(REMIND_SYS_LABEL, SYSTEM_TYPE, isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
    }

    @Test
    public void shouldSeeThatRemindLabelCanBeDestroyedOnLetter() throws MessagingException, IOException {
        testMsg.setText("third");
        testMsg.setHeader(X_YANDEX_HINT, createHintValue().addFid("7").addRcvDate(RCV_DATE_4).encode());
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfThirdLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);

        assertThat("Записались неверные данные в таблицу mail.box для первого письма",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFirstLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
        assertThat("Записались неверные данные в mail.box для 2го письма, что было с меткой remindme_threadabout:mark",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfSecondLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
        assertThat("Записались неверные данные в таблицу mail.box для 3го письма",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfThirdLetter),
                allOf(hasFid(equalTo("7")),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
    }

    @Test
    public void shouldSeeThatRemindLabelCanNotBeDestroyedByOldLetter() throws MessagingException, IOException {
        testMsg.setText("third");
        testMsg.setHeader(X_YANDEX_HINT, createHintValue().addFid("7").addRcvDate(RCV_DATE_2).encode());
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfThirdLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);

        assertThat("Записались неверные данные в таблицу mail.box для первого письма",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFirstLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
        assertThat("Записались неверные данные в mail.box для 2го письма, что  с меткой remindme_threadabout:mark",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfSecondLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLabelWithProperties(REMIND_SYS_LABEL, SYSTEM_TYPE, isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
        assertThat("Записались неверные данные в таблицу mail.box для 3го письма",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfThirdLetter),
                allOf(hasFid(equalTo("7")),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
    }

    @Test
    public void shouldSeeThatRemindLabelIsSaveIfAnswerInServiceFolder() throws MessagingException, IOException {
        testMsg.setText("third");
        testMsg.setHeader(X_YANDEX_HINT, createHintValue().addFid(SENT_FOLDER_FID).addRcvDate(RCV_DATE_5).encode());
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfThirdLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);

        assertThat("Записались неверные данные в таблицу mail.box для первого письма",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFirstLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
        assertThat("Записались неверные данные в mail.box для 2го письма, что с меткой remindme_threadabout:mark",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfSecondLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLabelWithProperties(REMIND_SYS_LABEL, SYSTEM_TYPE, isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
        assertThat("Записались неверные данные в таблицу mail.box для 3го письма",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfThirdLetter),
                allOf(hasFid(equalTo(SENT_FOLDER_FID)),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo("f")),
                        hasRecent(equalTo("t")),
                        hasDeleted(equalTo("f"))));
    }
}
