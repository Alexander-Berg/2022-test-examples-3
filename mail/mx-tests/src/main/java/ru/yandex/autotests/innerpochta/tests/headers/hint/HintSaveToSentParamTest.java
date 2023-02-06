package ru.yandex.autotests.innerpochta.tests.headers.hint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.utils.HintData;
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
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.PG_FOLDER_OUTBOX;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование save_to_sent",
        description = "Проверяем, что при выставлении save_to_sent=1 письмо сохраняется в отправленных")
@Title("HintCopyToInboxParamTest.Тестирование save_to_sent")
@Description("Проверяем, что при выставлении save_to_sent=1 письмо сохраняется в отправленных")
public class HintSaveToSentParamTest {
    private Logger log = LogManager.getLogger(this.getClass());
    private HintData.XYandexHintValue hintValue = createHintValue();
    private TestMessage msg;
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
    public static void initReceiver() throws Exception {
        receiver = accountRule.getReceiverUser();
        inMailbox(receiver).clearAll();
    }

    @Before
    public void prepareTestMessage() throws Exception {
        msg = new TestMessage();
        msg.setFrom("yantester@ya.ru");
        msg.setRecipient(receiver.getEmail());
        msg.setText("Test message");
        msg.saveChanges();
    }

    @Test
    public void testXYandexHintSaveToSentValidParam() throws IOException, MessagingException {
        hintValue = hintValue.addSaveToSent("1").addEmail(receiver.getEmail());
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setSubject(randomAlphanumeric(10));
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));
        log.info("Письмо должно сохраниться в отправленных");
        inMailbox(receiver).inFolder(PG_FOLDER_OUTBOX).shouldSeeLetterWithSubject(msg.getSubject());
    }

    @Test
    public void testXYandexHintSaveToSentDefaultParam() throws IOException, MessagingException {
        hintValue = hintValue.addSaveToSent("0").addEmail(receiver.getEmail());
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setSubject(randomAlphanumeric(10));
        msg.saveChanges();
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));
        log.info("Письмо не должно сохраниться в отправленных");
        inMailbox(receiver).inFolder(PG_FOLDER_OUTBOX).shouldNotSeeLetterWithSubject(msg.getSubject());
    }
}
