package ru.yandex.autotests.innerpochta.imap.utils;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.wmicommon.Util;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 27.02.14
 * Time: 17:42
 */
public class MessageUtils {

    private MessageUtils() {
    }

    public static String getMessage(TestMessage message) throws IOException, MessagingException {
        OutputStream stream = new ByteArrayOutputStream();
        message.writeTo(stream);
        stream.close();
        return stream.toString();
    }

    public static String getRandomMessage() throws Exception {
        TestMessage testMessage = new TestMessage();
        testMessage.setSentDate(new Date(System.currentTimeMillis()));
        testMessage.setRecipient(Util.getRandomAddress());
        testMessage.setFrom(Util.getRandomAddress());
        testMessage.setSubject("Random subject " + getRandomSubject());
        testMessage.setText("Timemark: " + System.currentTimeMillis() + "  Random message text" + getRandomBody());
        testMessage.saveChanges();
        return getMessage(testMessage);
    }

    public static String getRandomMessage(String subject) throws Exception {
        TestMessage testMessage = new TestMessage();
        testMessage.setSentDate(new Date(System.currentTimeMillis()));
        testMessage.setRecipient(Util.getRandomAddress());
        testMessage.setFrom(Util.getRandomAddress());
        testMessage.setSubject(subject);
        testMessage.setText("Timemark: " + System.currentTimeMillis() + "  Random message text" + getRandomBody());
        testMessage.saveChanges();
        return getMessage(testMessage);
    }

    public static TestMessage getFilledMessageWithAttachFromEML(String pathToEML) throws Exception {
        TestMessage message = new TestMessage(new File(pathToEML));
        message.setSubject("attach mail " + randomAlphanumeric(15));
        message.setFrom(new InternetAddress(randomAlphanumeric(10) + "@" +
                randomAlphanumeric(7) + ".ru"));
        message.saveChanges();
        return message;
    }

    public static TestMessage getFilledMessageWithAttachFromEML(URI pathToEML) throws Exception {
        TestMessage message = new TestMessage(new File(pathToEML));
//        message.setSubject("attach mail " + randomAlphanumeric(15));
//        message.setFrom(new InternetAddress(randomAlphanumeric(10) + "@" +
//                randomAlphanumeric(7) + ".ru"));
        //     message.setRecipient(RandomStringUtils.randomAlphanumeric(10) + "@" +
        //            RandomStringUtils.randomAlphanumeric(7) + ".ru");
        message.saveChanges();
        return message;
    }

    public static TestMessage getMessageFromEML(URI pathToEML) throws Exception {
        TestMessage message = new TestMessage(new File(pathToEML));
        message.saveChanges();
        return message;
    }

    public static Message getRandomFilledMessage(Message.RecipientType recipientType, String recipient) {
        TestMessage tm = null;
        try {
            tm = new TestMessage();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            tm.setText("Timemark: " + System.currentTimeMillis() + "  Random message text" + getRandomBody());
            tm.setSubject("Random subject " + getRandomSubject());
            tm.setFrom(randomAlphanumeric(10).toString() + "@" + randomAlphanumeric(3) + ".ru");
            if (recipient != null) {
                //       tm.setRecipients(recipientType, recipient);
            }
            tm.setSentDate(new Date(System.currentTimeMillis()));
            tm.saveChanges();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return tm;
    }

    private static String getRandomSentence(int numOfWords, int wordLen) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numOfWords; i++) {
            sb.append(randomAlphanumeric(wordLen));
            sb.append("  ");
        }
        sb.append(randomAlphanumeric(wordLen));
        return sb.toString();
    }

    private static String getRandomSubject() {
        return getRandomSentence(5, 7);
    }

    private static String getRandomBody() {
        return getRandomSentence(100, 30);
    }
}

