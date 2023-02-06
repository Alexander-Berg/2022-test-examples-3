package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class modDirectCategory extends MailProvider {
    public modDirectCategory() throws IOException {
    }

    @Test
    public void CategoryAge1132Mail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/moderationQueue/modMskAge.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("ModMSKAge", messageId);
    }

    @Test
    public void CategoryDissent1133Mail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/moderationQueue/modMskDissent.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("ModMSKDissent", messageId);
    }

    @Test
    public void CategoryDoc1135Mail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/moderationQueue/modMskDoc.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("ModMSKDoc", messageId);
    }

    @Test
    public void CategoryOther1136Mail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/moderationQueue/modMskOther.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("ModMSKOther", messageId);
    }
}
