package ru.yandex.crm.tests.delivery.stageTwo;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.core.MailProvider;
import ru.yandex.crm.tests.support.SupportTicketStorage;
import ru.yandex.crm.tests.support.TicketRow;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by agroroza on 11.03.2016.
 */
public class GrkMassmailTests extends MailProvider {
    public GrkMassmailTests() throws IOException {
    }

    @Test
    public void grkMassmailMskToCheck() throws SQLException {
        String messageId = getStageMessageId("grkMassmailMskTo");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(3102, ticket.queueId);
    }

    @Test
    public void grkMassmailSpbToCheck() throws SQLException {
        String messageId = getStageMessageId("grkMassmailSpbTo");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(3103, ticket.queueId);
    }

    @Test
    public void grkMassmailEktToCheck() throws SQLException {
        String messageId = getStageMessageId("grkMassmailEktTo");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(3105, ticket.queueId);
    }

    @Test
    public void grkMassmailNsbToCheck() throws SQLException {
        String messageId = getStageMessageId("grkMassmailNsbTo");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(3107, ticket.queueId);
    }

    @Test
    public void grkMassmailKznToCheck() throws SQLException {
        String messageId = getStageMessageId("grkMassmailKznTo");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(3104, ticket.queueId);
    }

    @Test
    public void grkMassmailRndToCheck() throws SQLException {
        String messageId = getStageMessageId("grkMassmailRndTo");

        TicketRow ticket = SupportTicketStorage.getTicketByMessageId(messageId);
        Assert.assertNotNull(ticket);
        Assert.assertEquals(3106, ticket.queueId);
    }




}
