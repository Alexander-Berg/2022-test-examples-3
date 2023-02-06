package ru.yandex.crm.tests.delivery.stageTwo;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.core.MailProvider;
import ru.yandex.crm.tests.support.SupportTicketStorage;
import ru.yandex.crm.tests.support.TicketRow;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class DirectUnknownTests extends MailProvider {
    public DirectUnknownTests() throws IOException {
    }

    @Test
    public void DirectUnknownCheck() throws SQLException {
        String messageId = getStageMessageId("directUnknown");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(1100, ticket.queueId);
    }

}
