package ru.yandex.crm.tests.support;

import org.sql2o.Sql2o;
import ru.yandex.core.DbProvider;

import java.sql.SQLException;

/**
 * Created by agroroza on 27.02.2020
 */
public class SupportTicketLast {

    private static final String SqlTakeTicket = " select max(id) as id from crm.sp_ticket\n" ;



    public static TicketLastRow getTicketLast() throws SQLException {
        return DbProvider.db().open()
                .createQuery(SqlTakeTicket)
                .executeAndFetch(TicketLastRow.class)
                .get(0);
    }
}
