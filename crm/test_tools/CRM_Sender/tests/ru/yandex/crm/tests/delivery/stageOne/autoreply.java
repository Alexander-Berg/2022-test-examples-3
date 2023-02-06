package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class autoreply extends MailProvider {
    public autoreply() throws IOException {
    }

    @Test
    public void AutoreplyPLAINMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/simpleMessages/AutoreplyPLAIN.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("AutoreplyPlain", messageId);
    }

    @Test
    public void AutoreplyHTMLMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/simpleMessages/AutoreplyHTML.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("AutoreplyHTML", messageId);
    }

}


