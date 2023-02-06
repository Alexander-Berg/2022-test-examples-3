package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 11.03.2016.
 */
public class directEnglish extends MailProvider {
    public directEnglish() throws IOException {
    }

    @Test
    public void DirectEnglishMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/service-direct-english.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directEnglish", messageId);
    }
}
