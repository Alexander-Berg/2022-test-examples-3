package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class ekbd extends MailProvider {
    public ekbd() throws IOException {
    }

    @Test
    public void and() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/and.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void and1() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/and1.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void and_and1() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/and_and1.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void body() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/body.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void bred() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/bred.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void Del1() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/Del1.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void ekbstudent() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/ekbstudent.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void ekbstudent_bred() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/ekbstudent_bred.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void header() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/header.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void past() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/past.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void sub_no() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/sub_no.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void tor() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/tor.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }

    @Test
    public void vsem() throws IOException {
        String messageId = mailSender().sendFile("./data/support/ekbd/vsem.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("xTest", messageId);
    }
}
