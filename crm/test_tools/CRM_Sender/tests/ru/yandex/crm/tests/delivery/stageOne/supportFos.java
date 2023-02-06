package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 24.03.2016.
 */



public class supportFos extends MailProvider {
    public supportFos() throws IOException {
    }

    @Test
    public void SupportIntCur() throws IOException {
        String messageId = mailSender().sendFile("./data/support/SupportInterfaceCurr.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("SupportIntCur", messageId);
    }

}
