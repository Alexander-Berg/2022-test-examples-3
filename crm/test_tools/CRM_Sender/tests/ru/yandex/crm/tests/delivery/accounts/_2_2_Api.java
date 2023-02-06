package ru.yandex.crm.tests.delivery.accounts;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 17.05.2017.
 */
public class _2_2_Api extends MailProvider {
    public _2_2_Api() throws IOException {
    }

    @Test
    public void audienceEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/audienceEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("audienceEmail", messageId);
    }

    @Test
    public void audienceHeaderDocpage() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/audienceHeaderDocpage.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("audienceHeaderDocpage", messageId);
    }

    @Test
    public void content_marketEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/content_marketEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("content_marketEmail", messageId);
    }

    @Test
    public void content_marketHeaderDocpage() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/content_marketHeaderDocpage.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("content_marketHeaderDocpage", messageId);
    }

    @Test
    public void directHeaderIssue() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/directHeaderIssue.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directHeaderIssue", messageId);
    }

    @Test
    public void directEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/directEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directEmail", messageId);
    }

    @Test
    public void direct_newsEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/direct_newsEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("direct_newsEmail", messageId);
    }

    @Test
    public void displayHeaderTroubleshooting() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/displayHeaderTroubleshooting.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("displayHeaderTroubleshooting", messageId);
    }

    @Test
    public void displayEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/displayEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("displayEmail", messageId);
    }

    @Test
    public void direct_engHeaderIssue() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/direct_engHeaderIssue.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("direct_engHeaderIssue", messageId);
    }

    @Test
    public void direct_engEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/direct_engEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("direct_engEmail", messageId);
    }

    @Test
    public void metricaHeaderIndex() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/metricaHeaderIndex.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("metricaHeaderIndex", messageId);
    }

    @Test
    public void metricaEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/metricaEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("metricaEmail", messageId);
    }

    @Test
    public void partner_marketEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/partner_marketEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("partner_marketEmail", messageId);
    }

    @Test
    public void partner_marketHeaderGeneral() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/partner_marketHeaderGeneral.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("partner_marketHeaderGeneral", messageId);
    }

    @Test
    public void apps_directEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_2_Api/apps_directEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("apps_directEmail", messageId);
    }
}
