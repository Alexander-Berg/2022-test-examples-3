package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 11.03.2016.
 */
public class directSign extends MailProvider {
    public directSign() throws IOException {
    }

    @Test
    public void directSign() throws IOException {
        String messageId = mailSender().sendFile("./data/support/directSign.eml", newMessageId(), newClientLogin(), newXuid());
        putStageMessageId("directSign", messageId);
    }
}
