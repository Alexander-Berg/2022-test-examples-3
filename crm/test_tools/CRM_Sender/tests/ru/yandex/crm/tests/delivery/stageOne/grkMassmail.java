package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 24.03.2016.
 */



public class grkMassmail extends MailProvider {
    public grkMassmail() throws IOException {
    }

    @Test
    public void GrkMassmailMSK() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMassmailMskTo.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMassmailMskTo", messageId);
    }

    @Test
    public void GrkMassmailSPB() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMassmailSpbTo.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMassmailSpbTo", messageId);
    }

    @Test
    public void GrkMassmailEKT() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMassmailEktTo.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMassmailEktTo", messageId);
    }

    @Test
    public void GrkMassmailNSB() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMassmailNsbTo.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMassmailNsbTo", messageId);
    }

    @Test
    public void GrkMassmailKZN() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMassmailKznTo.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMassmailKznTo", messageId);
    }

    @Test
    public void GrkMassmailRND() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMassmailRndTo.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMassmailRndTo", messageId);
    }
}

