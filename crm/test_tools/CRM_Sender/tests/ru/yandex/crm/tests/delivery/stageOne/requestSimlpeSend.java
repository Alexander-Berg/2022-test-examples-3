package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class requestSimlpeSend extends MailProvider {
    public requestSimlpeSend() throws IOException {
    }

    @Test
    public void requestSimlpe() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_7_Direct/appEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("requestSimple", messageId);
    }
}
