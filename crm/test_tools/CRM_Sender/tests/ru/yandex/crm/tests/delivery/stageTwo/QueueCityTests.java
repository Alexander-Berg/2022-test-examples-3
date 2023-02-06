package ru.yandex.crm.tests.delivery.stageTwo;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.core.MailProvider;
import ru.yandex.crm.tests.support.SupportTicketStorage;
import ru.yandex.crm.tests.support.TicketRow;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by agroroza on 23.03.2016.
 */

public class QueueCityTests extends MailProvider {
    public QueueCityTests() throws IOException {
    }

    @Test
    public void QueueMSKCheck() throws SQLException {
        String messageId = getStageMessageId("QueueMSK");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(1101, ticket.queueId);
    }

    @Test
    public void QueueSPBCheck() throws SQLException {
        String messageId = getStageMessageId("QueueSPB");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(1102, ticket.queueId);
    }

    @Test
    public void QueueEKTCheck() throws SQLException {
        String messageId = getStageMessageId("QueueEKT");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(1104, ticket.queueId);
    }

    @Test
    public void QueueNSBCheck() throws SQLException {
        String messageId = getStageMessageId("QueueNSB");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(1106, ticket.queueId);
    }

    @Test
    public void QueueKZNCheck() throws SQLException {
        String messageId = getStageMessageId("QueueKZN");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(1103, ticket.queueId);
    }

    @Test
    public void QueueRNDCheck() throws SQLException {
        String messageId = getStageMessageId("QueueRND");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(1105, ticket.queueId);
    }

    @Test
    public void QueueNVGCheck() throws SQLException {
        String messageId = getStageMessageId("QueueNVG");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(1107, ticket.queueId);
    }

}