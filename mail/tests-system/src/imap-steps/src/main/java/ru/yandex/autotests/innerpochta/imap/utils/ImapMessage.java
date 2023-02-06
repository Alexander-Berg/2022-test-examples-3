package ru.yandex.autotests.innerpochta.imap.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import javax.mail.MessagingException;

import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;

import static ru.yandex.autotests.innerpochta.imap.utils.Utils.getRandomName;

/**
 * Created by kurau on 18.03.16.
 */
public class ImapMessage {

    private TestMessage testMessage;

    private Date sendDate = new Date(System.currentTimeMillis());

    private String subject = "Random subject " + getRandomName();

    private String text = "Random message text\n" + getRandomName();

    private String from = getRandomAddress();

    private String bcc = "";

    private String cc = "";

    private ImapMessage() throws FileNotFoundException, MessagingException {
        testMessage = new TestMessage();
    }

    public static ImapMessage imapMessage() throws FileNotFoundException, MessagingException {
        return new ImapMessage();
    }

    public static String getMessage(TestMessage message) throws IOException, MessagingException {
        OutputStream stream = new ByteArrayOutputStream();
        message.writeTo(stream);
        stream.close();
        return stream.toString();
    }

    public static String getRandomAddress() {
        return getRandomName() + "@yandex.ru";
    }

    public ImapMessage withSendDate(Date date) {
        this.sendDate = date;
        return this;
    }

    public ImapMessage withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public ImapMessage withText(String text) {
        this.text = text;
        return this;
    }

    public ImapMessage withFrom(String from) {
        this.from = from;
        return this;
    }

    public ImapMessage withCc(String cc) {
        this.cc = cc;
        return this;
    }

    public ImapMessage withBcc(String bcc) {
        this.bcc = bcc;
        return this;
    }

    public String construct() throws Exception {
        testMessage.setSentDate(sendDate);
        testMessage.setRecipient(getRandomAddress());
        testMessage.setFrom(from);
        testMessage.setSubject(subject);
        testMessage.setText("Timemark: " + System.currentTimeMillis() + "\n" + text);
        if (!"".equals(bcc)) {
            testMessage.addHeader("Bcc", bcc);
        }
        if (!"".equals(cc)) {
            testMessage.addHeader("Cc", cc);
        }
        testMessage.saveChanges();
        return getMessage(testMessage);
    }


}
