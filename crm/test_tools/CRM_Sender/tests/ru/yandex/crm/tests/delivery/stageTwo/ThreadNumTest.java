package ru.yandex.crm.tests.delivery.stageTwo;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.core.MailProvider;
import ru.yandex.crm.tests.support.SupportTicketStorage;
import ru.yandex.crm.tests.support.TicketRow;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by agroroza on 17.03.2016.
 */
public class ThreadNumTest extends MailProvider {
    public ThreadNumTest() throws IOException {
    }

    @Test
    public void ThreadNumCheck() throws SQLException {
        String ThreadNumMail = getStageMessageId("threadNum");

        TicketRow ticketFirst = SupportTicketStorage.getTicketByMessageId(ThreadNumMail);
        Assert.assertNotNull(ticketFirst);
        Assert.assertEquals(1266320, ticketFirst.ticketNum);
    }

}