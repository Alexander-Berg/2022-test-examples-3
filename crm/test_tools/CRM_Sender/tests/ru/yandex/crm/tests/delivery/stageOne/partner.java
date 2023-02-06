package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class partner extends MailProvider {
    public partner() throws IOException {
    }

    @Test
    public void partner() throws IOException {
        String messageId = mailSender().sendFile("./data/support/partner.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("partner", messageId);
    }

    @Test
    public void partnerAuto() throws IOException {
        String messageId = mailSender().sendFile("./data/support/partnerAuto.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("partnerAuto", messageId);
    }

}
