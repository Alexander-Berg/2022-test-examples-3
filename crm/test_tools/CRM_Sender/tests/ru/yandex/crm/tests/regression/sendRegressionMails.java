package ru.yandex.crm.tests.regression;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 25.12.2019.
 */
public class sendRegressionMails extends MailProvider {
    public sendRegressionMails() throws IOException {
    }

    @Test
    public void ticketWithoutClient() throws IOException {
        String messageId = mailSender().sendFile("./data/support/regression/ticketWithoutClient.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("ticketWithoutClient", messageId);
    }

    @Test
    public void ticketWithClient() throws IOException {
        String messageId = mailSender().sendFile("./data/support/regression/ticketWithClient.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("ticketWithClient", messageId);
    }

    @Test
    public void personalMailWithoutClient() throws IOException {
        String messageId = mailSender().sendFile("./data/support/regression/personalMailWithoutClient.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("personalMailWithoutClient", messageId);
    }

    @Test
    public void personalMailWithClient() throws IOException {
        String messageId = mailSender().sendFile("./data/support/regression/personalMailWithClient.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("personalMailWithClient", messageId);
    }



}


