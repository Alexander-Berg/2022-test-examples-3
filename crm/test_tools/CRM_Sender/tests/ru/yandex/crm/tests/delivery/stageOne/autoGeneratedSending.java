package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

public class autoGeneratedSending extends MailProvider {
    public autoGeneratedSending() throws IOException {
    }

    @Test
    public void notMangrAlias_testmail_1() throws IOException {
        String messageId = mailSender().sendFile("./data/support/autoGen/testmail_1.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("notMangrAlias_testmail_1", messageId);
    }



}