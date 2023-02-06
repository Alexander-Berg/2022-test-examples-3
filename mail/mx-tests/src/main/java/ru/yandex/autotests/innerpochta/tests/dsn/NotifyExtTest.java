package ru.yandex.autotests.innerpochta.tests.dsn;

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

import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNwsmtp;

@Feature("DSN")
@Stories("NWSMTP")
@Aqua.Test(title = "Тестирование расширения NOTIFY",
        description = "Тестирование расширения NOTIFY")
@Title("NotifyExtTest. Тестирование расширения NOTIFY")
@Description("Проверка расширения NOTIFY")
public class NotifyExtTest {
    private static final String DSN_200_SUBJECT = "Письмо успешно доставлено";
    private static final String DSN_500_SUBJECT = "Недоставленное сообщение";
    private static final String fakeUser = "nosuchuser-nosuchuser-yet-user2@ya.ru";
    private static final String server = mxTestProps().getMxServer();
    private static final int port = mxTestProps().getMxPort();
    private static final int DSN_EXTRA_TIMEOUT = 30000;
    private static User sender;
    private static User receiver;

    @Rule
    public LogConfigRule aquaLogRule = new LogConfigRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);
    @ClassRule
    public static AccountRule accountRule = new AccountRule();

    @BeforeClass
    public static void setReceiverAndSender() throws Exception {
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void clearMailboxes() {
        inMailbox(sender).clearDefaultFolder();
        inMailbox(receiver).clearDefaultFolder();
    }

    @Test
    public void testNeverValue() throws IOException, MessagingException, InterruptedException {
        TestMessage message = new TestMessage();
        message.setFrom(sender.getEmail());
        message.setRecipient(receiver.getEmail());
        message.setSubject(randomAlphanumeric(10));
        message.setText("Test send");
        message.saveChanges();

        sendByNwsmtp(message, server, port, sender.getEmail(), sender.getPassword(), "NEVER");

        Thread.sleep(DSN_EXTRA_TIMEOUT);
        inMailbox(receiver).shouldSeeLetterWithSubject(message.getSubject());
        inMailbox(sender).shouldNotSeeLetterWithSubject(DSN_200_SUBJECT);
    }

    @Test
    public void testSuccessValue() throws IOException, MessagingException, InterruptedException {
        TestMessage message = new TestMessage();
        message.setFrom(sender.getEmail());
        message.setRecipient(receiver.getEmail());
        message.setSubject(randomAlphanumeric(10));
        message.setText("Test send");
        message.saveChanges();

        sendByNwsmtp(message, server, port, sender.getEmail(), sender.getPassword(), "SUCCESS");

        Thread.sleep(DSN_EXTRA_TIMEOUT);
        inMailbox(receiver).shouldSeeLetterWithSubject(message.getSubject());
        inMailbox(sender).shouldSeeLetterWithSubject(DSN_200_SUBJECT);
    }

    @Test
    public void testFailureValue() throws IOException, MessagingException, InterruptedException {
        TestMessage message = new TestMessage();
        message.setFrom(sender.getEmail());
        message.setRecipient(fakeUser);
        message.setSubject(randomAlphanumeric(10));
        message.setText("Test send");
        message.saveChanges();

        sendByNwsmtp(message, server, port, sender.getEmail(), sender.getPassword(), "FAILURE");

        Thread.sleep(DSN_EXTRA_TIMEOUT);
        inMailbox(sender).shouldSeeLetterWithSubject(DSN_500_SUBJECT);

    }

    @Test
    public void testSuccessFailureValue() throws IOException, MessagingException, InterruptedException {
        TestMessage message = new TestMessage();
        message.setFrom(sender.getEmail());
        message.setRecipient(fakeUser);
        message.setHeader("CC", receiver.getEmail());
        message.setSubject(randomAlphanumeric(10));
        message.setText("Test send");
        message.saveChanges();

        sendByNwsmtp(message, server, port, sender.getEmail(), sender.getPassword(), "SUCCESS,FAILURE");

        Thread.sleep(DSN_EXTRA_TIMEOUT);
        inMailbox(receiver).shouldSeeLetterWithSubject(message.getSubject());
        inMailbox(sender).shouldSeeLetterWithSubject(DSN_200_SUBJECT);
        inMailbox(sender).shouldSeeLetterWithSubject(DSN_500_SUBJECT);
    }
}
