package ru.yandex.crm.tests.delivery.accounts._1_4_Metrica;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class account_metrica extends MailProvider {
    public account_metrica() throws IOException {
    }

    @Test
    public void account_metrica() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_4_Metrika/account_metrica.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("account_metrica", messageId);
    }
}
