package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 24.03.2016.
 */



public class grkMedia extends MailProvider {
    public grkMedia() throws IOException {
    }

    @Test
    public void GrkMediaMSK() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMediaOfficeMsk.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMediaMsk", messageId);
    }

    @Test
    public void GrkMediaSPB() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMediaOfficeSpb.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMediaSpb", messageId);
    }

    @Test
    public void GrkMedialEKT() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMediaOfficeEkt.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMediaEkt", messageId);
    }

    @Test
    public void GrkMediaNSB() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMediaOfficeNsb.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMediaNsb", messageId);
    }

    @Test
    public void GrkMediaKZN() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMediaOfficeKzn.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkMediaKzn", messageId);
    }

    @Test
    public void GrkMediaRND() throws IOException {
        String messageId = mailSender().sendFile("./data/support/grk/grkMediaOfficeRnd.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("grkntMediaRnd", messageId);
    }
}

