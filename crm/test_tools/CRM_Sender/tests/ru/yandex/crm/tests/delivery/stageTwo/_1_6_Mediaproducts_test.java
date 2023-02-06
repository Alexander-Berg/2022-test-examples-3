package ru.yandex.crm.tests.delivery.stageTwo;

import org.junit.*;
import ru.yandex.core.MailProvider;
import ru.yandex.crm.tests.support.SupportTicketStorage;
import ru.yandex.crm.tests.support.TicketRow;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by agroroza on 27.05.2017.
 */
public class _1_6_Mediaproducts_test extends MailProvider {
    public _1_6_Mediaproducts_test() throws IOException {
    }

    @Test
    public void DirectUnknownCheck() throws SQLException {
        String messageId = getStageMessageId("directUnknown");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(1100, ticket.queueId);
    }
}
