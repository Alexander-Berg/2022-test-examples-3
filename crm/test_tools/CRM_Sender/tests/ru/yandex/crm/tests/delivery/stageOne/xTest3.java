package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 11.03.2016.
 */
public class xTest3 extends MailProvider {
    public xTest3() throws IOException {
    }

    @Test
    public void xTest3() throws IOException {
        String messageId = mailSender().sendFile("./data/support/xTest3.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest3", messageId);
    }
}
