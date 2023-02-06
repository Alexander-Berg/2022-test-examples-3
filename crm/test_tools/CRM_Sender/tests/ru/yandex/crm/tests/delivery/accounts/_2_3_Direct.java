package ru.yandex.crm.tests.delivery.accounts;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 17.05.2017.
 */
public class _2_3_Direct extends MailProvider {
    public _2_3_Direct() throws IOException {
    }

    @Test
    public void appEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_3_Direct/appEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("appEmail", messageId);
    }

    @Test
    public void commanderHeaderContact() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_3_Direct/commanderHeaderContact.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("commanderHeaderContact", messageId);
    }
    @Test
    public void commanderEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_3_Direct/commanderEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("commanderEmail", messageId);
    }

    @Test
    public void commander_engEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_3_Direct/commander_engEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("commander_engEmail", messageId);
    }
}
