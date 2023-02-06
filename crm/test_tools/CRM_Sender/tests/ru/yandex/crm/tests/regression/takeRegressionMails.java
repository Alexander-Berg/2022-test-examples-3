package ru.yandex.crm.tests.regression;

import org.junit.Assert;
import org.junit.Test;
import org.sql2o.Sql2o;
import ru.yandex.core.MailProvider;
import ru.yandex.crm.tests.support.SupportTicketStorage;
import ru.yandex.crm.tests.support.SupportTicketTake;
import ru.yandex.crm.tests.support.TicketRow;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by agroroza on 25.12.2019.
 */
public class takeRegressionMails extends MailProvider {
    public takeRegressionMails() throws IOException {
    }


    @Test
    public void takeTicketWithoutClient() throws SQLException {
        String messageId = getStageMessageId("ticketWithoutClient");
        Sql2o ticket = SupportTicketTake.takeTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
    }

    @Test
    public void takeTicketWithClient() throws SQLException {
        String messageId = getStageMessageId("ticketWithClient");
        Sql2o ticket = SupportTicketTake.takeTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
    }

}



