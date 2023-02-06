package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class market extends MailProvider {
    public market() throws IOException {
    }

    @Test
    public void marketClientCampaign() throws IOException {
        String messageId = mailSender().sendFile("./data/support/market/marketClientCampaign.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("marketClientCampaign", messageId);
    }

    @Test
    public void marketClientCampaign11() throws IOException {
        String messageId = mailSender().sendFile("./data/support/market/marketClientCampaign11.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("marketClientCampaign11", messageId);
    }

    @Test
    public void marketInMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/market/marketInMail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("marketInMail", messageId);
    }

    @Test
    public void marketServiceQuality() throws IOException {
        String messageId = mailSender().sendFile("./data/support/market/marketServiceQuality.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("marketServiceQuality", messageId);
    }

    @Test
    public void marketInMailEng() throws IOException {
        String messageId = mailSender().sendFile("./data/support/market/marketInMailEng.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("marketInMailEng", messageId);
    }

    @Test
    public void marketPartnerAll() throws IOException {
        String messageId = mailSender().sendFile("./data/support/market/marketPartnerAll.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("marketPartnerAll", messageId);
    }

    @Test
    public void marketPartnerYa() throws IOException {
        String messageId = mailSender().sendFile("./data/support/market/marketPartnerYa.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("marketPartnerYa", messageId);
    }

    @Test
    public void marketAccessLostRu() throws IOException {
        String messageId = mailSender().sendFile("./data/support/market/marketAccessLostRu.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("marketAccessLost", messageId);
    }

    @Test
    public void marketAccessLostUa() throws IOException {
        String messageId = mailSender().sendFile("./data/support/market/marketAccessLostUa.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("marketAccessLostUa", messageId);
    }
}


