package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 17.03.2016.
 */
public class ticketNotThread extends MailProvider {
    public ticketNotThread() throws IOException {
    }

    @Test
    public void TicketNotThreadSend() throws IOException {
        String messageId = mailSender().sendFile("./data/support/TicketNotThreadFirst.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("TicketNotThreadFirstMail", messageId);

        String messageId2 = mailSender().sendFile("./data/support/TicketNotThreadSecond.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("TicketNotThreadSecondMail", messageId2);
    }

}
