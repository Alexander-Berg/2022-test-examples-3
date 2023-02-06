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
import ru.yandex.autotests.innerpochta.wmi.adapter.WmiAdapterUser;
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
@Aqua.Test(title = "Тест SMTP. Тест STARTTLS.",
        description = "Тест SMTP. Тест STARTTLS.")
@Title("SmtpTLSTest.Тест STARTTLS.")
@Description("Тест SMTP. Тест STARTTLS.")
public class SmtpTLSTest {
    private static final String host = mxTestProps().getMxServer();
    private static final int port = mxTestProps().getMxPort();
    private static User sender;
    private static User receiver;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule aqualogRule = new LogConfigRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @BeforeClass
    public static void setReceiverAndSender() throws Exception {
        receiver = accountRule.getReceiverUser();
        sender = accountRule.getSenderUser();
    }

    @Test
    public void testStarTls() throws IOException, MessagingException,  InterruptedException {
        String subject = randomAlphanumeric(10);
        TestMessage msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setSubject(subject);
        msg.setRecipient(receiver.getEmail());
        msg.setText("Test send");
        msg.saveChanges();
        WmiAdapterUser mailBoxApi = new WmiAdapterUser(receiver);
        sendByNwsmtp(msg, host, port, sender);
        mailBoxApi.shouldSeeLetterWithSubject(subject);
    }
}
