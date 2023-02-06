package ru.yandex.autotests.innerpochta.tests.pq;

import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.io.FileNotFoundException;

import static java.lang.String.format;
import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.yandex.autotests.innerpochta.tests.headers.HeadersData.HeaderNames.X_YANDEX_SPAM;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;

/**
 * User: alex89
 * Date: 19.06.2015
 * todo:
 */
public class PqTestMsgs {                                  //fastsrv-pg-test-user1@ya.ru
    public static final String RECEIVER_UID = "315581191";
    private static final User RECEIVER = new User("pq-test-user3@ya.ru", "testqa");
    private static final User SIDE_RECEIVER = new User("pleskav2@ya.ru", "testqa12345678");
    public static final String FIRSTLINE = "firstline";

    public static TestMessage getMixedPostmasterMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("postmaster letter by mixed=16" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipient(RECEIVER.getLogin());
        msg.setHeader(X_YANDEX_HINT, createHintValue().addMixed(16).encode());
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getRealPostmasterMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("real postmaster letter" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setFrom("good@kruzkastakan.ee");
        msg.setRecipient(RECEIVER.getLogin());
        msg.saveChanges();
        return msg;
    }


    public static TestMessage getRealPostmasterSpamMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("real spam postmaster letter" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setHeader(X_YANDEX_SPAM.getName(), "4");
        msg.setFrom("good@kruzkastakan.ee");
        msg.setRecipient(RECEIVER.getLogin());
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getMixedPostmasterSpamMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("real spam mixed postmaster letter" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setHeader(X_YANDEX_SPAM.getName(), "4");
        msg.setHeader(X_YANDEX_HINT, createHintValue().addMixed(16).encode());
        msg.setFrom("good@kruzkastakan.ee");
        msg.setRecipient(RECEIVER.getLogin());
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getSpamMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        //msg.setHeader("X-Spam-Flag", "YES");
        msg.setHeader(X_YANDEX_SPAM.getName(), "4");
        msg.setSubject("spam letter by header" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipient(RECEIVER.getLogin());
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getMixedSpamMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setHeader(X_YANDEX_HINT, createHintValue().addMixed(4).encode());
        msg.setSubject("spam letter by mixed=4" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipient(RECEIVER.getLogin());
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getSharedMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("shared letter" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipient(RECEIVER.getLogin());
        msg.setRecipient(BCC, new InternetAddress(SIDE_RECEIVER.getLogin()));
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getSharedMsg2() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("shared letter" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipients(TO, format("%s, %s", RECEIVER.getLogin(), SIDE_RECEIVER.getLogin()));
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getSharedMsg3() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("shared letter" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipient(RECEIVER.getLogin());
        msg.setRecipient(CC, new InternetAddress(SIDE_RECEIVER.getLogin()));
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getSharedMsg4() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("shared letter" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipient(CC, new InternetAddress(RECEIVER.getLogin()));
        msg.setRecipient(BCC, new InternetAddress(SIDE_RECEIVER.getLogin()));
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getAppendMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("append letter by mixed=65536" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipient(RECEIVER.getLogin());
        msg.setHeader(X_YANDEX_HINT, createHintValue().addMixed(65536).encode());
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getRealAppendMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("append letter by mixed=65536" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipient(RECEIVER.getLogin());
        msg.setHeader(X_YANDEX_HINT, createHintValue().addRcvDate(1449048926).addMixed(0)
                .addSkipLoopPrevention("1").addFid("1").addNotify("0").addFilters("0").addImap("1").encode());
        msg.saveChanges();
        return msg;
    }

    public static TestMessage getMixedSmsMsg() throws MessagingException, FileNotFoundException {
        TestMessage msg = new TestMessage();
        msg.setSubject("sms letter by mixed=8192" + randomAlphabetic(10));
        msg.setText(FIRSTLINE);
        msg.setRecipient(RECEIVER.getLogin());
        msg.setHeader(X_YANDEX_HINT, createHintValue().addMixed(8192).encode());
        msg.saveChanges();
        return msg;
    }
}
