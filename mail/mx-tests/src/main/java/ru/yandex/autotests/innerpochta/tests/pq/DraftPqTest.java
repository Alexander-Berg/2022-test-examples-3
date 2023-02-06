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
import java.io.File;
import java.io.IOException;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AnyOf.anyOf;
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
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 05.05.15
 * MPROTO-1974
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка сохранения черновиков в ПГ",
        description = "Проверяем, в mail.messages и mail.box перезапись информации о письме-черновике")
@Title("DraftPqTest. Поверка сохранения черновиков в ПГ")
@Description("Проверяем, в mail.messages и mail.box перезапись информации о письме-черновике")
public class DraftPqTest {
    private static final String EXPECTED_FIRSTLINE_OF_FIRST_DRAFT = "plesk";
    private static final String EXPECTED_FIRSTLINE_OF_SECOND_DRAFT = "second";
    private static final String EXPECTED_ATTACHES = "{\"(1.2,text/plain,\\\"new  11.txt\\\",636)\"}";
    private static final String EXPECTED_HDR_DATE = "2015-10-28 13:46:50+03";
    private static final String receiverUid = "350043404";
    private static String expectedRecipients;
    private static Logger log = LogManager.getLogger(DraftPqTest.class);
    private TestMessage testMsg;
    private String sessionLog;
    private String midOfFirstLetter;
    private String midOfSecondLetter;
    private String stidOfFirstLetter;
    private String stidOfSecondLetter;
    private static User sender;
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
    public static void setReceiverAndSender() throws Exception {
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();
        expectedRecipients = "{\"(from,\\\"\\\","     + sender.getEmail()   + ")\"," +
                              "\"(to,\\\"\\\","       + receiver.getEmail() + ")\"," +
                              "\"(reply-to,\\\"\\\"," + sender.getEmail()   + ")\"}";
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        testMsg = new TestMessage(
                new File(this.getClass().getClassLoader().getResource("simple-attach.eml").getFile()));
        testMsg.setSubject("DraftPqTest " + randomAlphanumeric(20));
        testMsg.setHeader(X_YANDEX_HINT, createHintValue().addLabel("symbol:draft_label")
                .addFid(DRAFT_FOLDER_FID).encode());
        testMsg.setRecipient(receiver.getEmail());
        testMsg.setFrom(sender.getEmail());
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfFirstLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
        stidOfFirstLetter = extractedParamFromLogByPattern(STID_LOG_STRING_PATTERN, sessionLog);
    }

    @Test
    public void shouldSeeThatDraftsHaveCommonMidButDifferentStid() throws MessagingException, IOException {
        testMsg.setText(EXPECTED_FIRSTLINE_OF_SECOND_DRAFT);
        testMsg.setHeader(X_YANDEX_HINT, createHintValue().addFid(DRAFT_FOLDER_FID).addLabel("symbol:draft_label")
                .addMid(midOfFirstLetter).encode());
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfSecondLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
        stidOfSecondLetter = extractedParamFromLogByPattern(STID_LOG_STRING_PATTERN, sessionLog);

        assertThat("Mid второго письма должен совпадать с mid первого [MPROTO-1974]", midOfSecondLetter,
                equalTo(midOfFirstLetter));
        assertThat("stid второго письма должен совпадать сo stid первого [MPROTO-1974]", stidOfSecondLetter,
                not(equalTo(stidOfFirstLetter)));

    }

    @Test
    public void shouldSeePropertiesOfDraftLetterCanBeRewritten() throws MessagingException, IOException {
        assertThat("Записались неверные данные в таблицу mail.messages для первой редакции черновика [MPROTO-1974]",
                mailMessagesTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFirstLetter),
                allOf(hasStid(equalTo(stidOfFirstLetter)),
                        hasAttributes(anyOf(equalTo("{}"), equalTo("{postmaster}"))),
                        hasAttaches(equalTo(EXPECTED_ATTACHES)),
                        hasFirstLine(equalTo(EXPECTED_FIRSTLINE_OF_FIRST_DRAFT)),
                        hasSubject(equalTo(testMsg.getSubject())),
                        hasHdrDate(equalTo(EXPECTED_HDR_DATE)),
                        hasRecipients(equalTo(expectedRecipients)),
                        hasHdrMsgId(equalTo(testMsg.getMessageID()))));

        assertThat("Записались неверные данные в таблицу mail.box для первой редакции черновика [MPROTO-1974]",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFirstLetter),
                allOf(hasFid(equalTo(DRAFT_FOLDER_FID)),
                        hasLabelWithProperties("draft", SYSTEM_TYPE, isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo(FALSE)),
                        hasRecent(equalTo(TRUE)),
                        hasDeleted(equalTo(FALSE))));

        testMsg.setText(EXPECTED_FIRSTLINE_OF_SECOND_DRAFT);
        testMsg.setHeader(X_YANDEX_HINT, createHintValue().addLabel("symbol:draft_label").addFid(DRAFT_FOLDER_FID)
                .addMid(midOfFirstLetter).encode());
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfSecondLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
        stidOfSecondLetter = extractedParamFromLogByPattern(STID_LOG_STRING_PATTERN, sessionLog);

        assertThat("Записались неверные данные в таблицу mail.messages для второй редакции черновика [MPROTO-1974]",
                mailMessagesTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfSecondLetter),
                allOf(hasStid(equalTo(stidOfSecondLetter)),
                        hasAttributes(anyOf(equalTo("{}"), equalTo("{postmaster}"))),
                        hasAttaches(equalTo("{}")),
                        hasFirstLine(equalTo(EXPECTED_FIRSTLINE_OF_SECOND_DRAFT)),
                        hasSubject(equalTo(testMsg.getSubject())),
                        hasHdrDate(equalTo(EXPECTED_HDR_DATE)),
                        hasRecipients(equalTo(expectedRecipients)),
                        hasHdrMsgId(equalTo(testMsg.getMessageID()))));

        assertThat("Записались неверные данные в таблицу mail.box для второй редакции черновика [MPROTO-1974]",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfSecondLetter),
                allOf(hasFid(equalTo(DRAFT_FOLDER_FID)),
                        hasLabelWithProperties("draft", SYSTEM_TYPE, isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo(FALSE)),
                        hasRecent(equalTo(TRUE)),
                        hasDeleted(equalTo(FALSE))));
    }
}
