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
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
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
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 * User: alex89
 * Date: 05.05.15
 * DARIA-51780
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка выставления непрочитанности письмам из mute-треда",
        description = "Проверяем, что письмам в mail.box выставляется флаг seen=t и mute-метка," +
                "если в треде уже есть письмо с меткой mute")
@Title("MutePqTest.Поверка выставления прочитанности письмам из mute-треда")
@Description("Проверяем, что письмам в mail.box выставляется флаг seen=t и mute-метка," +
        "если в треде уже есть письмо с меткой mute")
public class MutePqTest {
    private static Logger log = LogManager.getLogger(MutePqTest.class);
    private static String receiverUid = "349511584";
    private TestMessage testMsg;
    private String sessionLog;
    private String midOfFirstLetter;
    private String midOfSecondLetter;
    private String midOfThirdLetter;
    private String midOfFourthLetter;
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
        testMsg.setSubject("MutePqTest " + randomAlphanumeric(20));
        testMsg.setText("first");
        testMsg.setRecipient(receiver.getEmail());
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfFirstLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);

        testMsg.setText("second");
        testMsg.setHeader(X_YANDEX_HINT, createHintValue().
                addLabel("symbol:mute_label").addFid(SENT_FOLDER_FID).encode());
        testMsg.saveChanges();
        serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfSecondLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
    }

    @Test
    public void shouldSeeThatMuteLabelCanBeAdded() throws MessagingException, IOException {
        assertThat("Записались неверные данные в таблицу mail.box для первого письма",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFirstLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLids(equalTo("{}")),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo(FALSE)),
                        hasRecent(equalTo(TRUE)),
                        hasDeleted(equalTo(FALSE))));

        assertThat("Записались неверные данные в таблицу mail.box для второго письма, что с меткой mute [DARIA-51780]",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfSecondLetter),
                allOf(hasFid(equalTo(SENT_FOLDER_FID)),
                        hasLabelWithProperties("mute", equalTo("system"), isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo(TRUE)),
                        hasRecent(equalTo(TRUE)),
                        hasDeleted(equalTo(FALSE))));
    }


    @Test
    public void shouldSeeThatMuteLabelMakesNewLettersSeen() throws MessagingException, IOException {
        testMsg.setText("third");
        testMsg.removeHeader(X_YANDEX_HINT);
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfThirdLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);

        testMsg.setText("fourth");
        testMsg.saveChanges();
        serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfFourthLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);

        assertThat("Записались неверные данные в mail.box для 3го письма,которое прилетело в тред с mute[DARIA-51780]",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfThirdLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLabelWithProperties("mute", equalTo("system"), isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo(TRUE)),
                        hasRecent(equalTo(TRUE)),
                        hasDeleted(equalTo(FALSE))));
        assertThat("Записались неверные данные в mail.box для 4го письма,которое прилетело в тред с mute[DARIA-51780]",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFourthLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLabelWithProperties("mute", equalTo("system"), isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo(TRUE)),
                        hasRecent(equalTo(TRUE)),
                        hasDeleted(equalTo(FALSE))));
    }

    @Test
    public void shouldSeeThatMuteLabelMakesNewLettersSeenAfterMuteLetterRemoving()
            throws MessagingException, IOException {
        testMsg.setText("third");
        testMsg.removeHeader(X_YANDEX_HINT);
        testMsg.saveChanges();
        String serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfThirdLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);

        clearSentFolder();

        testMsg.setText("fourth");
        testMsg.saveChanges();
        serverResponse = sendByNsls(testMsg);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), getMessageIdByServerResponse(serverResponse));
        log.info(sessionLog);
        midOfFourthLetter = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);

        assertThat("Записались неверные данные в mail.box для 3го письма,которое прилетело в тред с mute[DARIA-51780]",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfThirdLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLabelWithProperties("mute", equalTo("system"), isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo(TRUE)),
                        hasRecent(equalTo(TRUE)),
                        hasDeleted(equalTo(FALSE))));

        assertThat("Записались неверные данные в mail.box для 4го письма,которое прилетело в тред с mute[DARIA-51780]",
                mailBoxTableInfoFromPq(sshAuthRule.conn(), receiverUid, midOfFourthLetter),
                allOf(hasFid(equalTo(DEFAULT_FOLDER_FID)),
                        hasLabelWithProperties("mute", equalTo("system"), isEmptyOrNullString()),
                        hasTid(equalTo(midOfFirstLetter)),
                        hasSeen(equalTo(TRUE)),
                        hasRecent(equalTo(TRUE)),
                        hasDeleted(equalTo(FALSE))));
    }

    private void clearSentFolder() {
        CleanMessagesMopsRule.with(inMailbox(receiver).getAuth()).outbox().call();
    }
}
