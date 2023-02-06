package ru.yandex.direct.mail;


import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(JUnitParamsRunner.class)
public class SmtpMailSenderSubtypesTest {

    private static final String BOUNCE_ADDRESS = "devnull@ya.ru";
    private static final String Y_SERVICE_NAME = "yadirect";
    private static final String Y_SERVICE_SALT = "1234567890ABCDEF";
    private static final String HMAC_SALT = "FEDCBA0987654321";

    private static final String FROM_ADDRESS = "from@ya.ru";
    private static final String FROM_ADDRESS_PERSONAL_PART = "User From";

    private static final String TO_ADDRESS = "to@ya.ru";
    private static final String TO_ADDRESS_PERSONAL_PART = "User To";

    private static final String SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Hello message";


    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    public static Object[][] parametersForCorrectContentType() {
        return new Object[][]{
                {MailMessage.EmailContentType.TEXT, "text/plain; charset=utf-8"},
                {MailMessage.EmailContentType.HTML, "text/html; charset=utf-8"},
        };
    }

    @Test
    @Parameters
    public void correctContentType(MailMessage.EmailContentType contentType, String expectedContentType)
            throws Exception {
        MimeMessage receivedMessage = sendMessageWithContentType(contentType);
        String[] actualContentType = receivedMessage.getHeader("Content-Type");
        assertThat(actualContentType, beanDiffer(new String[]{expectedContentType}));
    }

    private MimeMessage sendMessageWithContentType(MailMessage.EmailContentType contentType) {
        YServiceTokenCreator tokenCreator = new YServiceTokenCreator(Y_SERVICE_NAME, Y_SERVICE_SALT, HMAC_SALT);
        SmtpMailSender sender = new SmtpMailSender(BOUNCE_ADDRESS, ServerSetupTest.SMTP.getBindAddress(),
                ServerSetupTest.SMTP.getPort(), tokenCreator);

        MailMessage message = new MailMessage(
                new EmailAddress(FROM_ADDRESS, FROM_ADDRESS_PERSONAL_PART),
                new EmailAddress(TO_ADDRESS, TO_ADDRESS_PERSONAL_PART),
                SUBJECT, MESSAGE_BODY, contentType, null);

        sender.send(message);
        return greenMail.getReceivedMessages()[0];
    }

}
