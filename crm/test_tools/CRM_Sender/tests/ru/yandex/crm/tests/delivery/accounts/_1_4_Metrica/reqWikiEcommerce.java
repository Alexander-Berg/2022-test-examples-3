package ru.yandex.crm.tests.delivery.accounts._1_4_Metrica;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class reqWikiEcommerce extends MailProvider {
    public reqWikiEcommerce() throws IOException {
    }

    @Test
    public void reqWikiEcommerce() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_4_Metrika/reqWikiEcommerce.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("reqWikiEcommerce", messageId);
    }
}
