package ru.yandex.autotests.innerpochta.utils;

import com.sun.mail.smtp.SMTPTransport;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;

public class LMTPTransport extends SMTPTransport {
    LMTPTransport(Session session) throws NoSuchProviderException {
        super(session, session.getTransport("smtp").getURLName());
    }

    public static LMTPTransport getLMTPTransport(Session session) throws NoSuchProviderException {
        return new LMTPTransport(session);
    }

    protected void helo(String domain) throws MessagingException {
        this.issueCommand("LHLO testhost", 250);
    }
}
