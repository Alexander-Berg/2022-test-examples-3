package ru.yandex.crm.tests.delivery.accounts;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 17.05.2017.
 */
public class _2_8_DocSelf extends MailProvider {
    public _2_8_DocSelf() throws IOException {
    }


    @Test
    public void selfHeaderVerification() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_8_DocSelf/selfHeaderVerification.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("selfHeaderVerification.eml", messageId);
    }

}
