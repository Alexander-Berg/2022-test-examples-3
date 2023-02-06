package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class planning extends MailProvider {
    public planning() throws IOException {
    }

    @Test
    public void planning() throws IOException {
        String messageId = mailSender().sendFile("./data/support/planning.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("planning", messageId);
    }

}
