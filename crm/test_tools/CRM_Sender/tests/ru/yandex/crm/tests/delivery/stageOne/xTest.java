package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 11.03.2016.
 */
public class xTest extends MailProvider {
    public xTest() throws IOException {
    }

    @Test
    public void xTest() throws IOException {
        String messageId = mailSender().sendFile("./data/support/xTest.eml", newMessageId(), newClientLogin(), newXuid());
        putStageMessageId("xTest", messageId);
    }
}
