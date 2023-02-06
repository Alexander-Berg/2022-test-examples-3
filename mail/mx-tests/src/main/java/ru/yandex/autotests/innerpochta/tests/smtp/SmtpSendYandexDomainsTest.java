package ru.yandex.autotests.innerpochta.tests.smtp;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNwsmtp;

@Stories("NWSMTP")
@Feature("SMTP-тесты")
@Aqua.Test(title = "Тест SMTP. Отправка c яндексовых доменов. Отправка на яндексовые домены.",
        description = "Тест SMTP. Отправка с яндексовые доменов. Отправка на яндексовые домены.")
@Title("SmtpYandexDomainsTest. Отправка c яндексовых доменов. Отправка на яндексовые домены.")
@Description("Тест SMTP.Отправка c яндексовых доменов. Отправка на яндексовые домены.")
@RunWith(Parameterized.class)
public class SmtpSendYandexDomainsTest {
    private String host = mxTestProps().getMxServer();
    private int port = mxTestProps().getMxPort();
    private String subject = randomAlphanumeric(10);
    private static User sender;

    @Parameterized.Parameter(0)
    public User receiver;

    @ClassRule
    public static AccountRule accountRule;
    @Rule
    public LogConfigRule aqualogRule = new LogConfigRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        accountRule = new AccountRule().with(SmtpSendYandexDomainsTest.class);
        User receiver = accountRule.getReceiverUser();
        Collection<Object[]> data = new ArrayList<Object[]>();
        if (mxTestProps().isCorpServer()) {
            data.add(new Object[]{receiver});
        } else {
            for (String receiverDomain : asList("yandex.ru", "ya.ru", "yandex.com", "yandex.com.tr", "yandex.by", "yandex.kz")) {
                data.add(new Object[]{new User(receiver.getLogin(), receiver.getPassword(), receiverDomain, false)});
            }
        }
        return data;
    }

    @BeforeClass
    public static void setSender() throws Exception {
        sender = accountRule.getSenderUser();
    }

    @Test
    public void testSendToYandexDomains() throws IOException, MessagingException,  InterruptedException {
        // Добавить проверку марсшрута
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
