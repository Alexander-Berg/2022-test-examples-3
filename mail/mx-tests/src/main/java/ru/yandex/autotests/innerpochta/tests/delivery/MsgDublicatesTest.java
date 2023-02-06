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
import static ru.yandex.autotests.innerpochta.tests.headers.HeadersData.HeaderNames.*;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNwsmtp;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;

@Stories("NWSMTP")
@Feature("Базовые проверки доставки")
@Aqua.Test(title = "Тестирование дубликатов писем",
        description = "Проверяем, что дубликаты письма не доставляются")
@Title("MsgDublicatesTest.Тестирование дубликатов писем")
@Description("Проверяем, что дубликаты письма не доставляются.")
public class MsgDublicatesTest {
    private static Logger log = LogManager.getLogger(MsgDublicatesTest.class);
    private static TestMessage originalMessage;
    private static String server = mxTestProps().getMxServer();
    private static Integer port = mxTestProps().getMxPort();
    private static User sender;
    private static User receiver;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule aquaLogRule = new LogConfigRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @BeforeClass
    public static void setReceiverAndSender() throws Exception {
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void sendOriginalMessage() throws Exception {
        originalMessage = new TestMessage(true, randomAlphanumeric(10));
        originalMessage.setHeader(FROM.getName(), sender.getEmail());
        originalMessage.setSubject(randomAlphanumeric(10));
        originalMessage.setRecipient(receiver.getEmail());
        originalMessage.setHeader(DATE.getName(), "Mon, 23 Jan 2012 19:01:06 +0400");
        originalMessage.setText("Test message");
        originalMessage.saveChanges();

        log.info("Отправляем оригинальное письмо");
        sendByNwsmtp(originalMessage, server, port, sender);
        inMailbox(receiver).shouldSeeLetterWithSubject(originalMessage.getSubject());
    }

    @Test
    public void shouldNoSeeDuplicate() throws IOException, MessagingException, InterruptedException {
        log.info("Отправляем дубликат письма. Не должны увидеть дубликата");
        sendByNwsmtp(originalMessage, server, port, sender);
        inMailbox(receiver).shouldSeeLettersWithSubject(originalMessage.getSubject(), 1);
    }

    @Test
    public void shouldSееMessageWithDifferentSubjectHeader() throws IOException, MessagingException, InterruptedException {
        TestMessage message = new TestMessage(originalMessage);
        message.setSubject(randomAlphanumeric(10));
        message.saveChanges();

        log.info("Отправляем письмо с отличной темой. Должны увидеть новое письмо");
        sendByNwsmtp(message, server, port, sender);
        inMailbox(receiver).shouldSeeLettersWithSubject(message.getSubject(), 1);
    }

    @Test
    public void shouldSееMessageWithDifferentFromHeader() throws IOException, MessagingException, InterruptedException {
        TestMessage message = new TestMessage(originalMessage);
        message.setHeader(DATE.getName(), "Mon, 24 Jan 2012 19:01:06 +0400");
        message.saveChanges();

        log.info("Отправляем письмо с отличным хидером Date. Должны увидеть новое письмо");
        sendByNwsmtp(message, server, port, sender);
        inMailbox(receiver).shouldSeeLettersWithSubject(message.getSubject(), 2);
    }

    @Test
    public void shouldSееMessageWithDifferentDateHeader() throws IOException, MessagingException, InterruptedException {
        TestMessage message = new TestMessage(originalMessage);
        message.setHeader(FROM.getName(), receiver.getEmail());
        message.saveChanges();

        log.info("Отправляем письмо с отличным хидером From. Должны увидеть новое письмо");
        sendByNwsmtp(message, server, port, sender);
        inMailbox(receiver).shouldSeeLettersWithSubject(message.getSubject(), 2);
    }

    @Test
    public void shouldSееMessageWithDifferentFolder() throws IOException, MessagingException, InterruptedException {
        TestMessage message = new TestMessage(originalMessage);
        message.setHeader(X_YANDEX_HINT.getName(), createHintValue().addFolder("duplicate").encode());
        message.saveChanges();

        log.info("Отправляем письмо в другую папку. Должны увидеть новое письмо");
        sendByNwsmtp(message, server, port, sender);
        inMailbox(receiver).inFolder("duplicate").shouldSeeLettersWithSubject(message.getSubject(), 1);
    }
}
