package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 17.03.2016.
 */
public class ticketThread extends MailProvider {
    public ticketThread() throws IOException {
    }

    @Test
    public void TicketThreadSend() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ticketThread.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("TicketThreadFirstMail", messageId);
        String messageId2 = mailSender().sendFile("./data/support/ticketThread.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("TicketThreadSecondMail", messageId2);
    }

}
