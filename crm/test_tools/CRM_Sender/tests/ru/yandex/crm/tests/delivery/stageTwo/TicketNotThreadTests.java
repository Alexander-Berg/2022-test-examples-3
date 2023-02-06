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
public class TicketNotThreadTests extends MailProvider {
    public TicketNotThreadTests() throws IOException {
    }

    @Test
    public void TicketNotThreadCheck() throws SQLException {
        String messageIdFirstMail = getStageMessageId("TicketNotThreadFirstMail");
        String messageIdSecondMail = getStageMessageId("TicketNotThreadSecondMail");

        TicketRow ticketFirst = SupportTicketStorage.getTicketByMessageId(messageIdFirstMail);
        TicketRow ticketSecond = SupportTicketStorage.getTicketByMessageId(messageIdSecondMail);
        Assert.assertNotNull(ticketFirst);
        Assert.assertNotNull(ticketSecond);
        Assert.assertNotEquals(ticketFirst.ticketNum, ticketSecond.ticketNum);
    }

}
