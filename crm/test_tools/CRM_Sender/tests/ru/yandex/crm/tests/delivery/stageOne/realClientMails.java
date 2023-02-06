package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

public class realClientMails extends MailProvider {
    public realClientMails() throws IOException {
    }

    @Test
    public void from_client_real_testmail_1() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_1.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_1", messageId);
    }

    @Test
    public void from_client_real_testmail_2() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_2.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_2", messageId);
    }

    @Test
    public void from_client_real_testmail_3() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_3.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_3", messageId);
    }

    @Test
    public void from_client_real_testmail_4() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_4.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_4", messageId);
    }

    @Test
    public void from_client_real_testmail_5() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_5.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_5", messageId);
    }

    @Test
    public void from_client_real_testmail_6() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_6.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_6", messageId);
    }

    @Test
    public void from_client_real_testmail_7() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_7.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_7", messageId);
    }

    @Test
    public void from_client_real_testmail_8() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_8.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_8", messageId);
    }

    @Test
    public void from_client_real_testmail_9() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_9.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_9", messageId);
    }

    @Test
    public void from_client_real_testmail_10() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_10.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_10", messageId);
    }

    @Test
    public void from_client_real_testmail_11() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_11.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_11", messageId);
    }

    @Test
    public void from_client_real_testmail_12() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_12.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_12", messageId);
    }

    @Test
    public void from_client_real_testmail_13() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_13.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_13", messageId);
    }

    @Test
    public void from_client_real_testmail_14() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_14.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_14", messageId);
    }

    @Test
    public void from_client_real_testmail_15() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_15.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_15", messageId);
    }

    @Test
    public void from_client_real_testmail_16() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_16.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_16", messageId);
    }

    @Test
    public void from_client_real_testmail_17() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_17.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_17", messageId);
    }

    @Test
    public void from_client_real_testmail_18() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_18.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_18", messageId);
    }

    @Test
    public void from_client_real_testmail_19() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_19.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_19", messageId);
    }

    @Test
    public void from_client_real_testmail_20() throws IOException {
        String messageId = mailSender().sendFile("./data/support/realClientMails/testmail_20.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("from_client_real_testmail_20", messageId);
    }

}
