package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 11.03.2016.
 */
public class issueDublicate extends MailProvider {
    public issueDublicate() throws IOException {
    }

    @Test
    public void issueDublicate1() throws IOException {
        String messageId = mailSender().sendFile("./data/support/issueDublicate.eml", newMessageId(), newClientLogin(), newXuid());
        putStageMessageId("issueDublicate1", messageId);
    }

    @Test
    public void issueDublicate2() throws IOException {
        String messageId = mailSender().sendFile("./data/support/issueDublicate.eml", newMessageId(), newClientLogin(), newXuid());
        putStageMessageId("issueDublicate2", messageId);
    }

    @Test
    public void issueDublicate3() throws IOException {
        String messageId = mailSender().sendFile("./data/support/issueDublicate.eml", newMessageId(), newClientLogin(), newXuid());
        putStageMessageId("issueDublicate3", messageId);
    }

    @Test
    public void issueDublicate4() throws IOException {
        String messageId = mailSender().sendFile("./data/support/issueDublicate.eml", newMessageId(), newClientLogin(), newXuid());
        putStageMessageId("issueDublicate4", messageId);
    }
}
