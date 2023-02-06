package ru.yandex.edu;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.core.MailProvider;
import ru.yandex.core.RawMailSender;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nasyrov on 10.03.2016.
 */
public class MailSenderTests {

    private String newMessageId() {
        return "<" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date()) + "@tester.crm.yandex.ru>";
    }
    public static String newClientLogin() {
        return "<" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + "-login>";
    }
    public static String newXuid() {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
    }

    @Test
    public void main() throws IOException {

        String raw = "to: dev@dev.ru\r\n" +
                "from: tester@dev.ru\n" +
                "subject: test\n" +
                "\n" +
                "body\n";

        MailProvider.mailSender().sendMail(raw, newMessageId(), newClientLogin(), newXuid());
    }


    @Test
    public void sendFileTest() throws IOException {
        MailProvider.mailSender().sendFile("./data/support/3.eml", newMessageId(), newClientLogin(), newXuid());
    }
}
