package ru.yandex.crm.tests.delivery.accounts._1_4_Metrica;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class metricaAppEmail extends MailProvider {
    public metricaAppEmail() throws IOException {
    }

    @Test
    public void metricaAppEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_4_Metrika/metricaAppEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("metricaAppEmail", messageId);
    }
}
