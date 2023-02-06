package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 17.03.2016.
 */
public class threadNum extends MailProvider {
    public threadNum() throws IOException {
    }

    @Test
    public void ThreadNumSend() throws IOException {
        String messageId = mailSender().sendFile("./data/support/threadNum.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("threadNum", messageId);
    }

}
