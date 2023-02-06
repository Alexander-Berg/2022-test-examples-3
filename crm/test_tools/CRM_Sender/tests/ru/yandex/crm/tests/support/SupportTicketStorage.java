package ru.yandex.crm.tests.support;

import ru.yandex.core.DbProvider;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by nasyrov on 12.03.2016.
 */
public class SupportTicketStorage {

    private static final String SqlSelectTicket = "select\n" +
            "  t.id\n" +
            "  ,t.ticket_num ticketnum\n" +
            "  ,t.ticket_dt ticketdt\n" +
            "  ,t.queue_id queueId\n" +
            "  ,t.category_id categoryId\n" +
            "from\n" +
            "  crm.sp_ticket t\n" +
            "  ,crm.mail_raw m\n" +
            "  ,crm.sp_ticket_timeline tm\n" +
            "where \n" +
            "  t.id = tm.ticket_id\n" +
            "  and m.mail_id = tm.mail_id\n" +
            "  and m.message_id = :pMessageId";

    public static TicketRow getTicketByMessageId(String messageId) throws SQLException {
        //System.out.println(messageId);
        return DbProvider.db().open()
                .createQuery(SqlSelectTicket)
                .addParameter("pMessageId", messageId)
                .executeAndFetch(TicketRow.class)
                .get(0);
    }
}
