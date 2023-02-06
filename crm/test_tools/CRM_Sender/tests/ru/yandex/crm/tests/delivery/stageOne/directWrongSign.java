package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 11.03.2016.
 */
public class directWrongSign extends MailProvider {
    public directWrongSign() throws IOException {
    }

    @Test
    public void directWrongSign() throws IOException {
        String messageId = mailSender().sendFile("./data/support/directWrongSign.eml", newMessageId(), newClientLogin(), newXuid());
        putStageMessageId("directWrongSign", messageId);
    }
}
