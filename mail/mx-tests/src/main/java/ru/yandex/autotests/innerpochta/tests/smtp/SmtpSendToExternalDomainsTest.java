package ru.yandex.autotests.innerpochta.tests.smtp;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
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

@Stories("NWSMTP")
@Feature("SMTP-тесты")
@Aqua.Test(title = "Тест SMTP. Отправка на внешние домены.",
        description = "Тест SMTP. Отправка на внешние домены.")
@Title("SmtpSendToExternalDomainsTest.Тест SMTP. Отправка на внешние домены.")
@Description("Тест SMTP. Отправка на внешние домены.")
public class SmtpSendToExternalDomainsTest {
    private String host = mxTestProps().getMxServer();
    private int port = mxTestProps().getMxPort();
    private String subject = randomAlphanumeric(10);
    private static User receiver;
    private static User sender;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);
    @Rule
    public LogConfigRule aqualogRule = new LogConfigRule();

    @BeforeClass
    public static void setReceiverAndSender() throws Exception {
        receiver = accountRule.getReceiverUser();
        sender = accountRule.getSenderUser();
    }

    @Test
    public void testSendToExternalDomains() throws IOException, MessagingException,  InterruptedException {
        // Добавить проверку маршрута
        // Добавить проверку попадания письма в делетед бокс, как придет новое апи
        TestMessage msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setSubject(subject);
        msg.setRecipient(receiver.getEmail());
        msg.setText("Test send");
        msg.saveChanges();
        sendByNwsmtp(msg, host, port, sender);
    }
}
