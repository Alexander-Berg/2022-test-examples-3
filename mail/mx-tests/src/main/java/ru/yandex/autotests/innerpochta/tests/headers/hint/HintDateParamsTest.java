package ru.yandex.autotests.innerpochta.tests.headers.hint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.MxUtils;
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.getMessageIdByServerResponse;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNsls;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 12.08.13
 * * Используется для тестрования БАЗ
 * todo: выяснить, почему не записываются Хидеры?
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование hdr_date, received_date",
        description = "Проверяем, что они правильно фиксируются в логе")
@Title("HintDateParamsTest.Тестирование hdr_date, received_date")
@Description("Проверяем, что они правильно фиксируются в логе")
public class HintDateParamsTest {
    private static final String HDR_DATE = "hdr_date=%s";
    private static final String RECEIVED_DATE = "received_date=%s";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //2013-08-18 15:29:23
    private Logger log = LogManager.getLogger(this.getClass());
    private XYandexHintValue hintValue;
    private TestMessage msg;
    private String messageId;
    private Random rnd = new Random();
    private long dateReceived = (long) rnd.nextInt(1500000000);
    private long dateOfSending = (long) rnd.nextInt(1500000000);

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
    public static void setReceiverAndSenderAndClearMailboxes() throws Exception {
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();
        inMailbox(receiver).clearAll();
    }

    @Before
    public void sendTestMessage() throws Exception {
        hintValue = createHintValue().addHdrDate(dateOfSending).addRcvDate(dateReceived);
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(hintValue + ">>>" + randomAlphanumeric(20));
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.saveChanges();
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
    }

    @Test
    public void shouldSeeThatXYandexHintDateParamsAreVisibleInLog() throws IOException, MessagingException, InterruptedException  {
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue + " и метками: ");
        String sessionLog = MxUtils.getInfoFromNsls(sshAuthRule.ssh().conn(), messageId);
        String expectedHdrDateInLog = format(HDR_DATE, sdf.format(new Date(dateOfSending * SECONDS.toMillis(1))));
        String expectedRcvDateInLog = format(RECEIVED_DATE, sdf.format(new Date(dateReceived * SECONDS.toMillis(1))));

        assertThat("Добавление временных параметров неверно представлено в логе",
                sessionLog, allOf(containsString(expectedRcvDateInLog), containsString(expectedHdrDateInLog)));
        inMailbox(receiver).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
