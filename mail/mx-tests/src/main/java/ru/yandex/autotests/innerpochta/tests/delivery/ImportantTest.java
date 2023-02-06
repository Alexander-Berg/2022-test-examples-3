package ru.yandex.autotests.innerpochta.tests.delivery;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.RetryRule;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.io.IOException;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNwsmtp;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

@Stories("NWSMTP")
@Feature("Базовые проверки доставки")
@Aqua.Test(title = "Важные письма",
        description = "Проверяем отправку важных сообщений")
@Title("ImportantTest.Важные письма")
@Description("Проверяем отправку важных сообщений")
public class ImportantTest {
    private static Logger log = LogManager.getLogger(MsgDublicatesTest.class);
    private static String server = mxTestProps().getMxServer();
    private static Integer port = mxTestProps().getMxPort();
    private static String importantMessage = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-IMPORTANT-ANTI-UBE-TEST-EMAIL*C.34X";
    private static TestMessage message;
    public static User receiver;
    public static User sender;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule aquaLogRule = new LogConfigRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @BeforeClass
    static public void prepareMessage() throws Exception {
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();
        message = new TestMessage();
        message.setFrom(sender.getEmail());
        message.setRecipient(receiver.getEmail());
        message.setSubject(randomAlphanumeric(10));
        message.setText(importantMessage);
        message.saveChanges();
        log.info("Отправляем письмо. Проверяем, что оно пометилось как важное");
        sendByNwsmtp(message, server, port, sender);
    }

    @Test
    public void shouldSeeMessageInInboxWithImportantLabel() throws IOException, MessagingException, InterruptedException {
        inMailbox(receiver).inFolder("Inbox").shouldSeeLetterWithSubjectAndLabel(message.getSubject(), "system_hamon");
    }
}
