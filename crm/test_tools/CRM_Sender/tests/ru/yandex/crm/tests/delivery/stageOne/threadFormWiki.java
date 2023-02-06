package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class threadFormWiki extends MailProvider {
    public threadFormWiki() throws IOException {
    }

    @Test
    public void threadFormWiki() throws IOException {
        String messageId = mailSender().sendFile("./data/support/threadFormWiki.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("threadFormWiki", messageId);
    }
}
