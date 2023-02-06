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

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.LinkedList;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqMatchers.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailBoxPqTable.mailBoxTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;

/**
 * User: alex89
 * Date: 05.05.15
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка записи данных в таблицу mail.box постгреса",
        description = "Проверяем, что корректно записались данные " +
                "fid,tid,lids,seen,recent,deleted таблицы mail.box постгреса")
@Title("MailBoxTableSimpleTest. Поверка записи данных в таблицу mail.box постгреса")
@Description("Проверяем, что корректно записались данные " +
        "fid,tid,lids,seen,recent,deleted  таблицы mail.box постгреса")
@RunWith(Parameterized.class)
public class MailBoxTableSimpleTest {
    private static final String RECEIVER_UID = "337798739";
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage testMsg;
    private String sessionLog;
    private String mid;
    private String expectedTid;
    private static User receiver;

    @Parameterized.Parameter(0)
    public String subject;
    @Parameterized.Parameter(1)
    public XYandexHintValue hintValue;
    @Parameterized.Parameter(2)
    public String expectedFid;
    @Parameterized.Parameter(3)
    public String expectedLids;
    @Parameterized.Parameter(4)
    public String expectedSeenFlag;
    @Parameterized.Parameter(5)
    public String expectedRecentFlag;
    @Parameterized.Parameter(6)
    public String expectedDeletedFlag;
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
        String randomPrefix = randomAlphanumeric(10);
        data.add(new Object[]{randomPrefix + "first", createHintValue().addLid("73"),
                "1", "{73}", FALSE, TRUE, FALSE, "Проверка работы lid-параметра X-Yandex-Hint"});
        data.add(new Object[]{randomPrefix + "zero", createHintValue().addFid("2"),
                "2", "{}", FALSE, TRUE, FALSE, "Проверка работы fid-параметра X-Yandex-Hint"});
        data.add(new Object[]{randomPrefix + "zero", createHintValue().addFid("3"),
                "3", "{}", FALSE, TRUE, FALSE, "Проверка работы fid-параметра X-Yandex-Hint"});
        data.add(new Object[]{randomPrefix + "core-by-imap", createHintValue().addFid("3").addImap("1")
                .addSkipLoopPrevention("1").addMid("0").addFilters("0").addNotify("0").addRcvDate(1458143954),
                "3", "{}", FALSE, TRUE, FALSE,
                "Проверка работы fid-параметра в сочетании с другими параметрами хинта X-Yandex-Hint"});
        data.add(new Object[]{randomPrefix + "zero", createHintValue().addFid("4"),
                "4", "{}", FALSE, TRUE, FALSE, "Проверка работы fid-параметра X-Yandex-Hint"});
        data.add(new Object[]{randomPrefix + "_save_to_sent", createHintValue().addSaveToSent("1"),
                "1", "{}", FALSE, TRUE, FALSE, "Проверка работы save_to_sent-параметра X-Yandex-Hint [MPROTO-3923]"});
        data.add(new Object[]{randomPrefix + "to_Deleted", createHintValue().addFid("5"),
                "5", "{}", FALSE, TRUE, FALSE, "Проверка работы fid-параметра X-Yandex-Hint"});
        data.add(new Object[]{randomPrefix + "DELETED", createHintValue().addMixed(128),
                "1", "{}", FALSE, TRUE, TRUE, "Проверка работы mixed-параметра X-Yandex-Hint  MPROTO-1770"});
        data.add(new Object[]{randomPrefix + "RECENT", createHintValue().addMixed(32),
                "1", "{}", FALSE, TRUE, FALSE, "Проверка работы mixed-параметра X-Yandex-Hint  MPROTO-1770"});
        data.add(new Object[]{randomPrefix + "SEEN", createHintValue().addMixed(2048),
                "1", "{}", TRUE, TRUE, FALSE, "Проверка работы mixed-параметра X-Yandex-Hint MPROTO-1770"});
        data.add(new Object[]{randomPrefix + "draft", createHintValue().addMixed(64), "1", "{5}",
                FALSE, TRUE, FALSE, "Проверка работы mixed-параметра X-Yandex-Hint (draft) MPROTO-1770"});
        data.add(new Object[]{randomPrefix + "forwarded", createHintValue().addMixed(512), "1", "{4}",
                FALSE, TRUE, FALSE, "Проверка работы mixed-параметра X-Yandex-Hint (forwarded) MPROTO-1770"});
        data.add(new Object[]{randomPrefix + "answered", createHintValue().addMixed(1024), "1", "{3}",
                FALSE, TRUE, FALSE, "Проверка работы mixed-параметра X-Yandex-Hint (answered) MPROTO-1770"});
        data.add(new Object[]{randomPrefix + "0IncorrectLid", createHintValue().addLid("12341245235434863464646"),
                "1", "{}", FALSE, TRUE, FALSE, "Проверка реакции на некорректный lid"});
        data.add(new Object[]{randomPrefix + "1IncorrectLid", createHintValue()
                .addFid("4").addLid("12341245235434863464646"),
                "4", "{}", FALSE, TRUE, FALSE, "Проверка реакции на некорректный lid"});
        data.add(new Object[]{randomPrefix + "3IncorrectLid", createHintValue()
                .addFid("4").addLid("2490000000042707409"),
                "4", "{}", FALSE, TRUE, FALSE, "Проверка реакции на некорректный lid"});
        data.add(new Object[]{randomPrefix + "3IncorrectFid", createHintValue().addFid("400"),
                "1", "{}", FALSE, TRUE, FALSE, "Проверка реакции на некорректный fid"});
        return data;
    }

    @BeforeClass
    public static void setReceiver() throws Exception {
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        testMsg = new TestMessage();
        testMsg.setSubject(subject);
        testMsg.setText("GOOD_TEXT_LETTER");
        testMsg.setRecipient(receiver.getEmail());
        testMsg.setHeader(X_YANDEX_HINT, hintValue.encode());
        if (subject.contains("TO_CC_FILTER")) {
            testMsg.addRecipient(MimeMessage.RecipientType.CC, new InternetAddress("yantester@ya.ru"));
        }
        testMsg.saveChanges();
        log.info(format("Отправили письмо с темой %s и хинтовыми парметрами %s", subject, hintValue));
        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
//        assertThat("Hе прошла покладка в ПГ!", sessionLog, anyOf(containsString("message stored in pq backed"),
//                containsString("message stored in db=pg backed")));
        mid = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
        expectedTid = mid;
        if (anyOf(equalTo("3"), equalTo("2")).matches(expectedFid)) { //если имеем дело с покладкой в Удаленные, Спам
            expectedTid = null;
        }
    }

    @Test
    public void shouldSeeFidLidsTidSeenRecentDeletedFieldsInMailBoxPqTable() {
        assertThat(format("Записались неверные данные в таблицу mail.box для случая: '%s'", caseComment),
                mailBoxTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                allOf(hasFid(equalTo(expectedFid)),
                        hasLids(equalTo(expectedLids)),
                        hasTid(equalTo(expectedTid)),
                        hasSeen(equalTo(expectedSeenFlag)),
                        hasRecent(equalTo(expectedRecentFlag)),
                        hasDeleted(equalTo(expectedDeletedFlag))));
    }
}
