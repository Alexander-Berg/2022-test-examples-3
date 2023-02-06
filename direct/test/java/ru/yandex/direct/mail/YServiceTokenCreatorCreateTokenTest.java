package ru.yandex.direct.mail;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class YServiceTokenCreatorCreateTokenTest {
    private final MimeMessage message;
    private final YServiceToken expectedToken;
    private final YServiceTokenCreator tokenCreator;

    public YServiceTokenCreatorCreateTokenTest(String serviceName, String serviceSalt, MimeMessage message, YServiceToken expectedToken) {
        this.message = message;
        this.expectedToken = expectedToken;
        this.tokenCreator = new YServiceTokenCreator(serviceName, serviceSalt, "");
    }

    @Test
    public void createToken() throws MessagingException, UnsupportedEncodingException {
        YServiceToken actualToken = tokenCreator.createToken(message);
        assertEquals(actualToken.getDate(), expectedToken.getDate());
        assertEquals(actualToken.getTokenValue(), expectedToken.getTokenValue());
    }

    private static MimeMessage m(String to, EmailAddress from, String subject, String dateHeader) throws MessagingException {
        MimeMessage result = new MimeMessage((Session) null);
        result.setFrom(from.toInternetAddress());
        result.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        result.setSubject(subject);
        result.setHeader(MailUtil.RFC822_DATE_HEADER, dateHeader);
        return result;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testData() throws MessagingException {
        return Arrays.asList(
                new Object[]{
                        "yadirect", "1234567890abcdef",
                        m("user@ya.ru", new EmailAddress("user@void.ru", null), "Task 12", "Sun, 22 Jan 2017 20:51:44 +0300"),
                        new YServiceToken("Sun, 22 Jan 2017 20:51:44 +0300", "eWFkaXJlY3QgNzQ2NjYwN2ExOTkwMWQ0NmQ3ZTc0YWMzODg3YzMxMjc=")
                },
                new Object[]{
                        "yadirect", "1234567890abcdef",
                        m("user@ya.ru", new EmailAddress("user@void.ru", "UserName"), "Task 12", "Sun, 22 Jan 2017 20:51:44 +0300"),
                        new YServiceToken("Sun, 22 Jan 2017 20:51:44 +0300", "eWFkaXJlY3QgOWM5YTdiMmNlMDQ0YjBmYzMxMzJmZDAxZGIyYzM3OTY=")
                },
                new Object[]{
                        "yaru", "76336c7a96b13f0258054d239e70fec8",
                        m("user@ya.ru", new EmailAddress("devnull@yandex.ru", "alsun (Я.ру)"), "Штука для снятия стрессов", "Fri, 27 Mar 2009 01:14:46 +0300"),
                        new YServiceToken("Fri, 27 Mar 2009 01:14:46 +0300", "eWFydSBiOWQ2YTc3ZmJmODkyMDk5NGJiNzZmNWZlMmM2ZDZhYg==")
                }
        );
    }
}
