package ru.yandex.autotests.innerpochta.tests.pdd;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

@Stories("NWSMTP")
@Feature("ПДД")
@Aqua.Test(title = "Тестирование доставки писем на 'пдд-адрес по умолчанию'",
        description = "Отправляем письма на несуществующий пдд-адрес в домене и смотрим, " +
                "что они сложились на 'пдд-адрес по умолчанию'.")
@Title("PddDeliveryOnDefaultLoginTest.Тестирование доставки писем на 'пдд-адрес по умолчанию'")
@Description("Отправляем письма на несуществующие пдд-адреса в домене и смотрим, " +
        "что они сложились на 'пдд-адрес по умолчанию'.")
public class PddDeliveryOnDefaultLoginTest {
    private final static String server = mxTestProps().getMxServer();
    private final static Integer port = mxTestProps().getMxPort();
    private Logger log = LogManager.getLogger(this.getClass());
    private static User sender;
    private static User defaultReceiver;
    private static User receiver;
    private static User fakeReceiver;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule aquaLogRule = new LogConfigRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @BeforeClass
    public static void setReceiversAndSender() throws Exception {
        sender = accountRule.getSenderUser();
        List<User> receivers = accountRule.getReceiverUsers();
        defaultReceiver = receivers.get(0);
        receiver = receivers.get(1);
        fakeReceiver = receivers.get(2);
    }

    @Before
    public void prepareTestMessage() throws MessagingException, FileNotFoundException {
        assumeThat("Этот тест предназначен для mxfront-сервера", server, containsString("mxfront"));
    }

    @Test
    public void shouldSeeDeliveryToPddDefaultMailBox() throws InterruptedException, MessagingException, IOException {
        TestMessage msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(fakeReceiver.getEmail());
        msg.setSubject(randomAlphanumeric(10));
        msg.setText("Test send");
        msg.saveChanges();

        log.info("Отправляем письма на несуществующие пдд-адреса в домене");
        sendByNwsmtp(msg, server, port, sender);
        log.info("Проверяем, что письмо сложилось в 'пдд-адрес по умолчанию'");
        inMailbox(defaultReceiver.getEmail(), defaultReceiver.getPassword()).shouldSeeLetterWithSubject(msg.getSubject());
        log.info("Проверяем, что письмо не сложилось остальным");
        inMailbox(receiver.getEmail(), receiver.getPassword()).shouldNotSeeLetterWithSubject(msg.getSubject());
    }

    @Test
    public void shouldSeeDeliveryToPddMailBox() throws InterruptedException, MessagingException, IOException {
        TestMessage msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(randomAlphanumeric(10));
        msg.setText("Test send");
        msg.saveChanges();

        log.info("Отправляем письма на пдд-адрес");
        sendByNwsmtp(msg, server, port, sender);
        log.info("Проверяем, что письмо не сложилось в 'пдд-адрес по умолчанию'");
        inMailbox(defaultReceiver.getEmail(), defaultReceiver.getPassword()).shouldNotSeeLetterWithSubject(msg.getSubject());
        log.info("Проверяем, что письмо сложилось ресипиенту");
        inMailbox(receiver.getEmail(), receiver.getPassword()).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
