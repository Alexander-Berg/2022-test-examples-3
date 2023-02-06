package ru.yandex.autotests.innerpochta.tests.mailing_list;

import org.apache.commons.mail.EmailException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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
import java.util.List;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

@Stories("NWSMTP")
@Feature("Рассылки")
@Aqua.Test(title = "Тестирование отправки писем на рассылку",
        description = "Тестирование отправки писем на рассылку")
@Title("Рассылки. Отправка писем на рассылку")
@Description("Тестирование отправки писем на рассылку")
public class BigMailingListTest {
    private final static String server = mxTestProps().getMxServer();
    private final static Integer port = mxTestProps().getMxPort();
    private static User receiver;

    private static final String subject = randomAlphanumeric(10);
    private  Logger log = LogManager.getLogger(this.getClass());

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule aquaLogRule = new LogConfigRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @BeforeClass
    public static void setReceiversAndSenderAndPrepareTestMessage() throws EmailException, MessagingException, IOException, InterruptedException {
        User sender = accountRule.getSenderUser();
        List<User> receivers = accountRule.getReceiverUsers();
        User mailList = receivers.get(0);
        receiver = receivers.get(1);

        TestMessage msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(mailList.getEmail());
        msg.setText("Test send");
        msg.setSubject(subject);
        msg.saveChanges();

        sendByNwsmtp(msg, server, port, sender);
    }

    @Test
    public void shouldSeeMessageInInboxFolderForMlSubscribers() throws IOException, MessagingException, InterruptedException {
        log.info("Проверяем, что письмо сложилось в папку Inbox подписчикам рассылки");
        inMailbox(receiver.getEmail(), receiver.getPassword()).inFolder("Inbox").shouldSeeLetterWithSubject(subject);
    }
}
