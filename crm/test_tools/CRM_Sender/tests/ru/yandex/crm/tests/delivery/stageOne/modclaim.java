package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class modclaim extends MailProvider {
    public modclaim() throws IOException {
    }

    @Test
    public void modclaim() throws IOException {
        String messageId = mailSender().sendFile("./data/support/modclaimYa.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("modclaimYa", messageId);
    }

    @Test
    public void noModclaim() throws IOException {
        String messageId = mailSender().sendFile("./data/support/modclaimNoYa.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("modclaimNoYa", messageId);
    }
}
