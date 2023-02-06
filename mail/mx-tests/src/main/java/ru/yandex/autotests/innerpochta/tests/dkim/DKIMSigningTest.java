package ru.yandex.autotests.innerpochta.tests.dkim;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import java.util.Collection;
import java.util.LinkedList;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.innerpochta.matchers.MessageContentMatcher.hasSameContentWithMsg;
import static ru.yandex.autotests.innerpochta.tests.matchers.MessageHeaderMatcher.hasAuthResultsHeader;
import static ru.yandex.autotests.innerpochta.tests.matchers.MessageHeaderMatcher.hasDkimSignature;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNwsmtp;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

@Stories("NWSMTP")
@Feature("DKIM")
@Aqua.Test(title = "Тестирование подписывания писем DKIM'ом для яндексовых получателей",
        description = "Тестируем, что при отправке письма подписываются DKIM'ом.")
@Title("DKIMSigningTest. Тестирование подписывания писем DKIM'ом для яндексовых получателей")
@Description("Тестируем, что при отправке письма подписываются DKIM'ом.")
@RunWith(Parameterized.class)
public class DKIMSigningTest {
    private static String server = mxTestProps().getMxServer();
    private final static Integer port = mxTestProps().getMxPort();
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;
    private static User sender;
    private static User receiver;

    @Parameterized.Parameter(0)
    public String senderDomain;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    @Rule
    public RetryRule retryRule = new RetryRule(3);
    @ClassRule
    public static AccountRule accountRule = new AccountRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        for (String senderDomain : asList("yandex.ru", "ya.ru", "yandex.com", "yandex.by",  "yandex.kz")) {
            data.add(new Object[]{senderDomain});
        }
        return data;
    }

    @Before
    public void setReceiverAndSenderAndPrepareTestMessage() throws Exception {
        receiver = accountRule.getReceiverUser();
        User senderUser = accountRule.getSenderUser();
        sender = new User(senderUser.getLogin(), senderUser.getPassword(), senderDomain, false);
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(randomAlphanumeric(20));
        msg.setText(randomAlphanumeric(30));
        msg.saveChanges();
    }

    @Test
    public void shouldSeeDKIMSigningForYandexUsers() throws IOException, MessagingException, InterruptedException {
        log.info(String.format("Отправляем письмо с темой '%s', Ожидаем пересылки на ящик %s",
                msg.getSubject(), receiver.getEmail()));
        sendByNwsmtp(msg, server, port, sender);
        TestMessage receivedMsg = inMailbox(receiver).getMessageWithSubject(msg.getSubject());

        assertThat("Содержимое не совпадает у полученного и отправленного писем!",
                receivedMsg, hasSameContentWithMsg(msg));
        assertThat("Проблемы со статусом DKIM!", receivedMsg, hasAuthResultsHeader(
                allOf(containsString("dkim=pass"), containsString(senderDomain.toLowerCase()))));
        assertThat("Домен в DKIM пропечатался неверно!", receivedMsg,
                hasDkimSignature(containsString("d=" + senderDomain.toLowerCase())));
    }
}
