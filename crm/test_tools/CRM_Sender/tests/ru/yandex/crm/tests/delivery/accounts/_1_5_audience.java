package ru.yandex.crm.tests.delivery.accounts;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class _1_5_audience extends MailProvider {
    public _1_5_audience() throws IOException {
    }

    @Test
    public void audience() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_5_API/audience.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("audience", messageId);
    }

    @Test
    public void contentMarket() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_5_API/contentMarket.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("contentMarket", messageId);
    }

    @Test
    public void display() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_5_API/display.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("display", messageId);
    }

    @Test
    public void directEng() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_5_API/directEng.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directEng", messageId);
    }

    @Test
    public void direct() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_5_API/direct.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("direct", messageId);
    }

    @Test
    public void metrica() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_5_API/metrica.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("metrica", messageId);
    }

    @Test
    public void partnerMarket() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_5_API/partnerMarket.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("partnerMarket", messageId);
    }

    @Test
    public void directUnits() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_5_API/directUnits.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directUnits", messageId);
    }

}
