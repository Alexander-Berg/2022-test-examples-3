package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 23.03.2016.
 */

public class queueCity extends MailProvider {
    public queueCity() throws IOException {
    }

    @Test
    public void QueueMSKMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/QueueMSK.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("QueueMSK", messageId);
    }

    @Test
    public void QueueSPBMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/QueueSPB.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("QueueSPB", messageId);
    }

    @Test
    public void QueueEKTMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/QueueEKT.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("QueueEKT", messageId);
    }

    @Test
    public void QueueNSBMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/QueueNSB.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("QueueNSB", messageId);
    }

    @Test
    public void QueueKZNMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/QueueKZN.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("QueueKZN", messageId);
    }

    @Test
    public void QueueRNDMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/QueueRND.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("QueueRND", messageId);
    }

    @Test
    public void QueueNVGMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/QueueNVG.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("QueueNVG", messageId);
    }
}