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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqMatchers.hasReceivedDate;
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
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка записи received_date в таблицу mail.box постгреса",
        description = "Проверяем, что корректно записали received_date в таблицу mail.box постгреса " +
                "при прокидывании параметра received_date X-Yandex-Hint")
@Title("MailBoxTableReceivedDateTest. Поверка записи received_date в таблицу mail.box постгреса")
@Description("Проверяем, что корректно записали received_date в таблицу mail.box постгреса " +
        "при прокидывании параметра received_date X-Yandex-Hint")
@RunWith(Parameterized.class)
public class MailBoxTableReceivedDateTest {
    private static final String RECEIVER_UID = "326197893";
    private Logger log = LogManager.getLogger(this.getClass());
    private String mid;
    private static Random rnd = new Random();
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX");//2013-08-18 15:29:23+03
    private static long randomDateReceived = (long) rnd.nextInt(1500000000);
    private static User receiver;

    @Parameterized.Parameter(0)
    public XYandexHintValue hintValue;
    @Parameterized.Parameter(1)
    public String expectedReceivedDate;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    @Rule
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{createHintValue().addRcvDate(randomDateReceived),
                sdf.format(new Date(randomDateReceived * SECONDS.toMillis(1)))});
        data.add(new Object[]{createHintValue().addRcvDate(1391686558),
                sdf.format(new Date(1391686558 * SECONDS.toMillis(1)))});
        data.add(new Object[]{createHintValue().addRcvDate(1440498025),
                sdf.format(new Date(1440498025 * SECONDS.toMillis(1)))});
        return data;
    }

    @BeforeClass
    public static void setReceiver() throws Exception {
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        TestMessage testMsg = new TestMessage();
        testMsg.setSubject(randomAlphanumeric(7));
        testMsg.setText(randomAlphanumeric(7));
        testMsg.setRecipient(receiver.getEmail());
        testMsg.setHeader(X_YANDEX_HINT, hintValue.encode());
        testMsg.saveChanges();
        log.info(format("Отправили письмо с хинтовыми парметрами %s", hintValue));
        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        String sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
//        assertThat("Hе прошла покладка в ПГ!", sessionLog, anyOf(containsString("message stored in pq backed"),
//                containsString("message stored in db=pg backed")));
        mid = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
    }

    @Test
    public void shouldSeeReceivedDateInMailBoxPqTable() {
        assertThat(format("Записались неверный received_date в таблицу mail.box для X_YANDEX_HINT: '%s'", hintValue),
                mailBoxTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                hasReceivedDate(equalTo(expectedReceivedDate)));
    }
}
