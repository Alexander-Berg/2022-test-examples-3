package ru.yandex.crm.tests.delivery.accounts._1_4_Metrica;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class metricaAppWiki extends MailProvider {
    public metricaAppWiki() throws IOException {
    }

    @Test
    public void metricaAppWki() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_4_Metrika/metricaAppWki.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("metricaAppWki", messageId);
    }
}
