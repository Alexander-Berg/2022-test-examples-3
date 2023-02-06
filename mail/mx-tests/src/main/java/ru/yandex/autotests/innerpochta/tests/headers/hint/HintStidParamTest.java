package ru.yandex.autotests.innerpochta.tests.headers.hint;

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
import java.util.List;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assume.assumeFalse;
import static ru.yandex.autotests.innerpochta.utils.HintData.*;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 06.08.13
 * Используется для тестрования БАЗ
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование source_stid",
        description = "Отправляем первое письмо на ящик №1.Узанем его stid. Отправляем второе письмо на ящик №2 " +
                "с X-Yandex-Hint с парметром source_stid, который равен stid-у первого письма. " +
                "Проверяем, что во втором ящике положилось письмо из первого ящика.")
@Title("HintStidParamTest.Тестирование source_stid")
@Description("Отправляем первое письмо на ящик №1.Узанем его stid. Отправляем второе письмо на ящик №2 " +
        "с X-Yandex-Hint с парметром source_stid, который равен stid-у первого письма. " +
        "Проверяем, что во втором ящике положилось письмо из первого ящика.")
public class HintStidParamTest {
    private static final String BODY_MD5_HASH_OF_MSG_WITH_CAT_TEXT = "9c9798428b1972ac174a1dba29bffddc";
    private static final String FINAL_HEADERS_LEN_VALUE = "536";
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;
    private String firstLetterSubject = "Letter1>>>" + randomAlphanumeric(20);
    private String secondLetterSubject = "Letter2>>>" + randomAlphanumeric(20);
    private String firstLetterStid;
    private String sessionLog2;
    private static User sender;
    private static User receiver1;
    private static User receiver2;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @BeforeClass
    public static void setReceiverAndSender() throws Exception {
        List<User> receivers = accountRule.getReceiverUsers();
        sender = accountRule.getSenderUser();
        receiver1 = receivers.get(0);
        receiver2 = receivers.get(1);
    }

    @Before
    public void sendFirstTestMessageAndGetStid() throws Exception, InterruptedException  {
        inMailbox(receiver1).clearAll();
        inMailbox(receiver2).clearAll();
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver1.getEmail());
        msg.setSubject(firstLetterSubject);
        msg.setText("Dog");
        msg.setHeader("Date", "Wed, 5 Apr 2017 02:49:11 -0700");
        msg.setMessageID("<" + randomAlphanumeric(20) + ">");
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        String sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
        firstLetterStid = getStidFromLog(sessionLog);
        log.info("STID первого письма:" + firstLetterStid);
        XYandexHintValue hintValue = createHintValue().addSourceStid(firstLetterStid);
        log.info("Отправим письмо на второй ящик и подставим STID первого письма из первого ящика:" + hintValue);
        msg.setHeader(X_YANDEX_HINT, hintValue.addFinalHeadersLen(FINAL_HEADERS_LEN_VALUE).
                addBodyMd5(BODY_MD5_HASH_OF_MSG_WITH_CAT_TEXT).encode());
        msg.setSubject(secondLetterSubject);
        msg.setText("Cat");
        msg.setRecipient(receiver2.getEmail());
        msg.saveChanges();
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
        sessionLog2 = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog2);
    }

    @Test
    public void shouldSeeThatXYandexHintStidParamCanReplaceStidsInLog() throws IOException, MessagingException {
        assertThat("Добавление параметров неверно представлено в логе!",
                sessionLog2, containsString(STID_LOG_SIGN + firstLetterStid));
    }

    @Test
    public void shouldSeeThatXYandexHintStidParamCanReplaceMessages() throws IOException, MessagingException {
        log.info("Проверим, что письмо подменилось. (В качестве признака выберем текст первого письма).");
        inMailbox(receiver2).shouldSeeLetterWithSubjectAndContent(secondLetterSubject, equalTo("Dog"));
    }

    private static String getStidFromLog(String log) {
        Matcher serverFormat = STID_LOG_PATTERN.matcher(log);
        assertThat("Не удалось извлечь stid из лога первого письма!", serverFormat.find(), is(true));
        return serverFormat.group(1);
    }
}
