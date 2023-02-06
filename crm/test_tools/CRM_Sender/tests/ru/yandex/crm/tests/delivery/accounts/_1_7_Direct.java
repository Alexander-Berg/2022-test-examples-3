package ru.yandex.crm.tests.delivery.accounts;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class _1_7_Direct extends MailProvider {
    public _1_7_Direct() throws IOException {
    }

    @Test
    public void directBodyNoShowAllPhrases() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_7_Direct/directBodyNoShowAllPhrases.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directBodyNoShowAllPhrases", messageId);
    }

    @Test
    public void directEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_7_Direct/directEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directEmail", messageId);
    }

    @Test
    public void appEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_7_Direct/appEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("appEmail", messageId);
    }

    @Test
    public void commanderBodyValidacija() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_7_Direct/commanderBodyValidacija.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("commanderBodyValidacija", messageId);
    }

    @Test
    public void commanderEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_7_Direct/commanderEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("commanderEmail", messageId);
    }
}
