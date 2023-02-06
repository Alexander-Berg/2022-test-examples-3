package ru.yandex.crm.tests.delivery.stageOne;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

public class uncertainMailRoutes extends MailProvider {
    public uncertainMailRoutes() throws IOException {
    }

    @Test
    public void notMangrAlias() throws IOException {
        String messageId = mailSender().sendFile("./data/support/uncertainMailRoutes/notMangrAlias.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("notMangrAlias", messageId);
    }

    @Test
    public void aliasAndMail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/uncertainMailRoutes/aliasAndMail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("aliasAndMail", messageId);
    }

    @Test
    public void twoAlias() throws IOException {
        String messageId = mailSender().sendFile("./data/support/uncertainMailRoutes/twoAlias.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("twoAlias", messageId);
    }

    @Test
    public void mangrDismissed() throws IOException {
        String messageId = mailSender().sendFile("./data/support/uncertainMailRoutes/mangrDismissed.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("mangrDismissed", messageId);
    }

    @Test
    public void agencyClientWithMangr() throws IOException {
        String messageId = mailSender().sendFile("./data/support/uncertainMailRoutes/agencyClientWithMangr.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("agencyClientWithMangr", messageId);
    }

    @Test
    public void tooBigToBePerfect() throws IOException {
        String messageId = mailSender().sendFile("./data/support/uncertainMailRoutes/tooBigToBePerfect", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("tooBigToBePerfect", messageId);
    }


}
