package ru.yandex.market.mboc.common.services.mail;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * Stores messages instead of sending them.
 *
 * @author prediger
 */
public class JavaMailSenderMock implements JavaMailSender {
    public static final Comparator<MimeMessage> MESSAGE_COMPARATOR =
        (message1, message2) -> compareEmails(message1, message2) ? 0 : 1;

    private List<MimeMessage> mimeMessageList = new ArrayList<>();

    private static boolean compareEmails(MimeMessage message1, MimeMessage message2) {
        return emailHeadersAsSet(message1).equals(emailHeadersAsSet(message2));
    }

    public static Set<String> emailHeadersAsSet(MimeMessage message) {
        try {
            Set<String> headers = new HashSet<>();
            Enumeration<String> allHeaderLines = message.getAllHeaderLines();
            while (allHeaderLines.hasMoreElements()) {
                headers.add(allHeaderLines.nextElement());
            }
            return headers;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MimeMessage> getMimeMessages() {
        return mimeMessageList;
    }

    public MimeMessage createTestMimeMessage(String fromEmail, String fromPersonal,
                                             Collection<String> to, Collection<String> cc,
                                             String subject, String text, boolean isHtml) {
        try {
            MimeMessage message = createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED);
            helper.setTo(to.toArray(new String[0]));
            helper.setCc(cc.toArray(new String[0]));
            helper.setFrom(fromEmail, fromPersonal);
            helper.setSubject(subject);
            helper.setText(text != null ? text : "", isHtml);
            return message;
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MimeMessage createMimeMessage() {
        return new MimeMessage((Session) null);
    }

    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        mimeMessageList.add(mimeMessage);
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        throw new UnsupportedOperationException();
    }
}
