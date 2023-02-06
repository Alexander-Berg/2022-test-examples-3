package ru.yandex.autotests.innerpochta.tests.pq;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqMatchers.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailishMessagesPqTable.mailishMessagesTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;

/**
 * User: alex89
 * Date: 05.05.15
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка записи данных в таблицу mailish.messages постгреса",
        description = "Проверяем, что корректно записались данные " +
                "fid,imap_id,imap_time,errors таблицы mailish.messages постгреса")
@Title("MailishMessagesTableSimpleTest. Поверка записи данных в таблицу mailish.messages постгреса")
@Description("Проверяем, что корректно записались данные " +
        "fid,imap_id,imap_time,errors таблицы mailish.messages постгреса")
@RunWith(Parameterized.class)
public class MailishMessagesTableSimpleTest {
    private static final String ZERO_ERRORS = "0";
    private static final String RECEIVER_UID = "523087229";
    private static int imapId = new Random().nextInt(1000000);
    private static int imapId2 = new Random().nextInt(100000);
    private static int imapId3 = new Random().nextInt(10000);
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage testMsg;
    private String sessionLog;
    private String mid;
    private static User receiver;

    @Parameterized.Parameter(0)
    public XYandexHintValue hintValue;
    @Parameterized.Parameter(1)
    public String expectedImapId;
    @Parameterized.Parameter(2)
    public String expectedFid;
    @Parameterized.Parameter(3)
    public String expectedImapTime;
    @Parameterized.Parameter(4)
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
        data.add(new Object[]{createHintValue().addLid("73").addExternalImapId(format("%d", imapId))
                .addRcvDate(1458143954).addFid("9"),
                format("%d", imapId), "9", "2016-03-16 18:59:14+03",
                "Проверка записи при задании fid и received_date"});
        data.add(new Object[]{createHintValue().addExternalImapId(format("%d", imapId2))
                .addRcvDate(1499999000).addFid("7"),
                format("%d", imapId2), "7", "2017-07-14 05:23:20+03", "Проверка работы lid-параметра X-Yandex-Hint"});
        data.add(new Object[]{createHintValue().addExternalImapId(format("%d", imapId3))
                .addRcvDate(1499999999).addFid("9"),
                format("%d", imapId3), "9", "2017-07-14 05:39:59+03",
                "Проверка использования одного и того же external_imap_id, но другого fid-а папки"});
        return data;
    }

    @BeforeClass
    public static void setReceiver() throws Exception {
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        testMsg = new TestMessage();
        testMsg.setSubject("MailishMessagesTableSimpleTest " + randomAlphanumeric(10));
        testMsg.setText("GOOD_TEXT_LETTER");
        testMsg.setRecipient(receiver.getEmail());
        testMsg.setHeader(X_YANDEX_HINT, hintValue.encode());
        testMsg.setHeader("X-Yandex-Mailish", RECEIVER_UID);
        testMsg.setSentDate(new Date());
        testMsg.saveChanges();

        log.info(format("Отправили письмо с темой %s и хинтовыми парметрами %s", testMsg.getSubject(), hintValue));
        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
        mid = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
    }

    @Test
    public void shouldSeeFieldsInMailishMessagesPqTable() {
        assertThat(format("Записались неверные данные в таблицу mailish.messages для случая: '%s'", caseComment),
                mailishMessagesTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                allOf(hasMailishFid(equalTo(expectedFid)),
                        hasMailishImapId(equalTo(expectedImapId)),
                        hasMailishImapTime(equalTo(expectedImapTime)),
                        hasMailishErrors(equalTo(ZERO_ERRORS))));
    }
}
