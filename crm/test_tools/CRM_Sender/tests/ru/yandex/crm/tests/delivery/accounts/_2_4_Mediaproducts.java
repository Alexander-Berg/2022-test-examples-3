package ru.yandex.crm.tests.delivery.accounts;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 17.05.2017.
 */
public class _2_4_Mediaproducts extends MailProvider {
    public _2_4_Mediaproducts() throws IOException {
    }

    @Test
    public void mainbooking_alertEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_4_Mediaproducts/mainbooking_alertEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("mainbooking_alertEmail", messageId);
    }

}
