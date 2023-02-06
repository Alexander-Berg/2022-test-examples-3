package ru.yandex.crm.tests.support;

import org.sql2o.Sql2o;
import ru.yandex.core.DbProvider;

import java.sql.SQLException;

/**
 * Created by nasyrov on 12.03.2016.
 */
public class SupportTicketTake {

    private static final String SqlTakeTicket = "update\n" +
            "  crm.sp_ticket t\n" +
            "  set\n" +
            "  t.owner_id = 1499\n" +
            "  ,t.state_id = 3\n" +
            "where \n" +
            "  id =\n" +
            "  (select t.id\n" +
            "from\n" +
            "  crm.sp_ticket t\n" +
            "  ,crm.mail_raw m\n" +
            "  ,crm.sp_ticket_timeline tm\n" +
            "where \n" +
            "  t.id = tm.ticket_id\n" +
            "  and m.mail_id = tm.mail_id\n" +
            "  and m.message_id = :pMessageId)" ;



    public static Sql2o takeTicketByMessageId(String messageId) throws SQLException {
        System.out.println(messageId);
        return DbProvider.db().beginTransaction()
                .createQuery(SqlTakeTicket)
                .addParameter("pMessageId", messageId)
                .executeUpdate()
                .commit();
    }
}
