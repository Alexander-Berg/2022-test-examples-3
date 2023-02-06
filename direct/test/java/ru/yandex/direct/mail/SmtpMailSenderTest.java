package ru.yandex.direct.mail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class SmtpMailSenderTest {
    private static final String BOUNCE_ADDRESS = "devnull@ya.ru";
    private static final String Y_SERVICE_NAME = "yadirect";
    private static final String Y_SERVICE_SALT = "1234567890ABCDEF";
    private static final String HMAC_SALT = "FEDCBA0987654321";

    private static final String FROM_ADDRESS = "me@ya.ru";
    private static final String FROM_ADDRESS_PERSONAL_PART = "Семен Семенович";
    private static final String FROM_ADDRESS_FULL_ENCODED =
            "=?utf-8?B?0KHQtdC80LXQvSDQodC10LzQtdC90L7QstC40Yc=?= <me@ya.ru>";

    private static final String TO_ADDRESS = "user@ya.ru";
    private static final String TO_ADDRESS_PERSONAL_PART = "Наполеон Бонапартович Третий";
    private static final String TO_ADDRESS_FULL_ENCODED = "=?utf-8?B?0J3QsNC/0L7Qu9C10L7QvSDQkdC+0L3QsNC/?=\r\n"
            + " =?utf-8?B?0LDRgNGC0L7QstC40Ycg0KLRgNC10YLQuNC5?= <user@ya.ru>";

    private static final String SUBJECT = "subject тесттесттесттесттест";
    private static final String SUBJECT_ENCODED = "=?utf-8?B?c3ViamVjdCDRgtC10YHRgtGC0LU=?=\r\n"
            + " =?utf-8?B?0YHRgtGC0LXRgdGC0YLQtdGB0YLRgtC10YHRgg==?=";
    private static final String CONTENT_TYPE = "text/plain; charset=utf-8";

    private static final String MESSAGE_BODY = "В чащах юга жил бы цитрус? Да, но фальшивый экземпляр!";
    public static final Long OPERATOR_UID = 12345678L;
    public static final Long USER_CLIENT_ID = 9876543L;


    private final SmtpMailSender sender;
    private final YServiceTokenCreator tokenCreator;
    private MimeMessage[] receivedMessages;

    public SmtpMailSenderTest() {
        tokenCreator = new YServiceTokenCreator(Y_SERVICE_NAME, Y_SERVICE_SALT, HMAC_SALT);
        sender = new SmtpMailSender(BOUNCE_ADDRESS, ServerSetupTest.SMTP.getBindAddress(),
                ServerSetupTest.SMTP.getPort(), tokenCreator);
    }

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Before
    public void before() {
        MailMessage m = new MailMessage(
                new EmailAddress(FROM_ADDRESS, FROM_ADDRESS_PERSONAL_PART),
                new EmailAddress(TO_ADDRESS, TO_ADDRESS_PERSONAL_PART),
                SUBJECT, MESSAGE_BODY, MailMessage.EmailContentType.TEXT, null);
        sender.send(m);
        receivedMessages = greenMail.getReceivedMessages();
    }

    @Test
    public void checkMessagesCount() {
        assertThat(receivedMessages.length, is(1));
    }

    @Test
    public void checkReturnPath() throws MessagingException {
        MimeMessage m = receivedMessages[0];
        //Return-Path добавит smtp-сервер
        String[] returnPath = m.getHeader("Return-Path");
        assertThat(returnPath, beanDiffer(new String[]{String.format("<%s>", BOUNCE_ADDRESS)}));
    }

    @Test
    public void checkPrecedenceField() throws MessagingException {
        MimeMessage m = receivedMessages[0];
        String[] returnPath = m.getHeader("Precedence");
        assertThat(returnPath, beanDiffer(new String[]{"bulk"}));
    }

    @Test
    public void checkYToken() throws UnsupportedEncodingException, MessagingException {
        MimeMessage m = receivedMessages[0];
        YServiceToken expectedToken = tokenCreator.createToken(m);
        String[] tokens = m.getHeader(YServiceTokenCreator.X_SERVICE_HEADER);
        assertThat(tokens, beanDiffer(new String[]{expectedToken.getTokenValue()}));
    }

    @Test
    public void checkHmacSign() throws MessagingException {
        prepareAuthorizedMail();
        //смотрим на второе сообщение, т.к. первое - это простое MailMessage, отправленное в before
        MimeMessage m = receivedMessages[1];
        String expectedHmacSign = tokenCreator.createHmacToken(OPERATOR_UID, USER_CLIENT_ID, SUBJECT);


        String[] actualOperatorUid = m.getHeader(YServiceTokenCreator.X_OPERATOR_UID_HEADER);
        String[] actualCleintID = m.getHeader(YServiceTokenCreator.X_CLIENT_ID_HEADER);
        String[] actualHmacSign = m.getHeader(YServiceTokenCreator.X_HMAC_SIGN_HEADER);

        assertThat(actualOperatorUid, beanDiffer(new String[]{OPERATOR_UID.toString()}));
        assertThat(actualCleintID, beanDiffer(new String[]{USER_CLIENT_ID.toString()}));
        assertThat(actualHmacSign, beanDiffer(new String[]{expectedHmacSign}));
    }

    @Test
    public void checkFrom() throws MessagingException {
        MimeMessage m = receivedMessages[0];
        String[] from = m.getHeader("From");
        assertThat(from, beanDiffer(new String[]{FROM_ADDRESS_FULL_ENCODED}));
    }

    @Test
    public void checkTo() throws MessagingException {
        MimeMessage m = receivedMessages[0];
        String[] to = m.getHeader("To");
        assertThat(to, beanDiffer(new String[]{TO_ADDRESS_FULL_ENCODED}));
    }

    @Test
    public void checkSubject() throws MessagingException {
        MimeMessage m = receivedMessages[0];
        String[] subject = m.getHeader("Subject");
        assertThat(subject, beanDiffer(new String[]{SUBJECT_ENCODED}));
    }

    @Test
    public void checkSubtype() throws MessagingException {
        MimeMessage m = receivedMessages[0];
        String[] contentType = m.getHeader("Content-Type");
        assertThat(contentType, beanDiffer(new String[]{CONTENT_TYPE}));
    }

    @Test
    public void checkMessageBody() throws MessagingException, IOException {
        MimeMessage m = receivedMessages[0];
        String actualMessage = (String) m.getContent();
        assertEquals(actualMessage, MESSAGE_BODY);
    }

    private void prepareAuthorizedMail() {
        var m = new AuthorizedMailMessage(
                OPERATOR_UID,
                USER_CLIENT_ID,
                new EmailAddress(FROM_ADDRESS, FROM_ADDRESS_PERSONAL_PART),
                new EmailAddress(TO_ADDRESS, TO_ADDRESS_PERSONAL_PART),
                SUBJECT, MESSAGE_BODY, MailMessage.EmailContentType.TEXT, null);
        sender.send(m);
        receivedMessages = greenMail.getReceivedMessages();
    }
}
