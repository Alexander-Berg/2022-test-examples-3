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

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.*;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 06.08.13
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование copy_to_inbox",
        description = "Проверяем, что при выставлении copy_to_inbox=1 письмо кладется во входящие и не минует фильтры")
@Title("HintCopyToInboxParamTest.Тестирование copy_to_inbox")
@Description("Проверяем, что при выставлении copy_to_inbox=1 письмо кладется во входящие и не минует фильтры")
public class HintCopyToInboxParamTest {
    private static final String LABEL_BY_FILTER = "отфильтровано";
    private static final String TEXT_FOR_FILTER = "Раздражитель фильтров.";

    private Logger log = LogManager.getLogger(this.getClass());
    private XYandexHintValue hintValue = createHintValue();
    private TestMessage msg;
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
    public void prepareTestMessage() throws Exception {
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setText(TEXT_FOR_FILTER);
        msg.saveChanges();
    }

    @Test
    public void testXYandexHintCopyToInboxValidParam() throws IOException, MessagingException {
        hintValue = hintValue.addCopyToInbox("1").addFolder(PG_FOLDER_SPAM).addEmail(receiver.getEmail());
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setSubject("Письмо должно быть откопировано во входящие и должно пройти все фильтры."
                + randomAlphanumeric(10));
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL_BY_FILTER);
        inMailbox(receiver).inFolder(PG_FOLDER_SPAM).shouldSeeLetterWithSubject(msg.getSubject());
    }

    @Test
    public void testXYandexHintCopyToInboxDefaultParam() throws IOException, MessagingException {
        hintValue = hintValue.addCopyToInbox("0").addFolder(PG_FOLDER_SPAM);
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setSubject("Письмо не должно быть откопировано во входящие." + randomAlphanumeric(10));
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldNotSeeLetterWithSubject(msg.getSubject());
        inMailbox(receiver).inFolder(PG_FOLDER_SPAM).shouldSeeLetterWithSubject(msg.getSubject());
    }

    @Test
    public void testXYandexHintCopyToInboxFakeParam() throws IOException, MessagingException {
        hintValue = hintValue.addCopyToInbox("аааа").addFolder(PG_FOLDER_SPAM);
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setSubject("Письмо не должно быть откопировано во входящие." + randomAlphanumeric(10));
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldNotSeeLetterWithSubject(msg.getSubject());
        inMailbox(receiver).inFolder(PG_FOLDER_SPAM).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
