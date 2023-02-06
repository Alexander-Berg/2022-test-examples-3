package ru.yandex.market.abo.mm.imap;

import java.util.Arrays;

import javax.mail.Folder;
import javax.mail.Message;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.util.mail.ImapMailConnector;

/**
 * @author artemmz
 * @date 15/05/19.
 */
@Disabled
class ImapMailConnectorTest {

    private static final String LOGIN = "pasha.sinichka@yandex.ru";
    /**
     * get from abo.mail.password datasource props.
     */
    private static final String PASSWORD = "";

    @Test
    void load() throws Exception {
        ImapMailConnector mailConnector = new ImapMailConnector("imap.yandex.ru", LOGIN, PASSWORD);
        mailConnector.connect(store -> {
            try (Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_WRITE);
                int messageCount = inbox.getMessageCount();
                System.out.println("messageCount = " + messageCount);
                if (messageCount > 0) {
                    Message[] messages = inbox.getMessages(1, Math.min(10, messageCount));
                    for (Message msg : messages) {
                        System.out.println(Arrays.toString(msg.getHeader("Message-Id")));
                    }
                }
            }
        });
    }
}