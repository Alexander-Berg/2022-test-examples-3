package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 12.12.2017.
 */
public class wrongAddress extends MailProvider {
    public wrongAddress() throws IOException {
    }

    @Test
    public void wrongAddress() throws IOException {
        String messageId = mailSender().sendFile("./data/support/wrongAddress.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("wrongAddress", messageId);
    }
}
