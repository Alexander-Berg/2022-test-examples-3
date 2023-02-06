package ru.yandex.crm.tests.fastScripts;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.core.MailProvider;
import ru.yandex.crm.tests.support.SupportTicketLast;
import ru.yandex.crm.tests.support.SupportTicketStorage;
import ru.yandex.crm.tests.support.TicketLastRow;
import ru.yandex.crm.tests.support.TicketRow;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by agroroza on 17.03.2016.
 */
public class lastTicketId extends MailProvider {
    public lastTicketId() throws IOException {
    }

    @Test
    public void showLastTicketId() throws SQLException {
        TicketLastRow ticketLast = SupportTicketLast.getTicketLast();
        Assert.assertNotNull(ticketLast);
        System.out.println("Last ticket id is " +ticketLast.id);
    }

}