package ru.yandex.crm.tests.support;

import ru.yandex.core.DbProvider;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by agroroza on 27.06.2017.
 */
public class SupportRequestStorage {

    private static final String SqlSelectRequest = "select\n" +
            "  r.id\n" +
            "  ,r.request_num requestnum\n" +
            "  ,r.request_dt requestdt\n" +
            "  ,r.queue_id queueId\n" +
            "  ,r.category_id categoryId\n" +
            "from\n" +
            "  crm.req_request r\n" +
            "  ,crm.mail_raw m\n" +
            "  ,crm.req_timeline tm\n" +
            "where \n" +
            "  r.id = tm.request_id\n" +
            "  and m.mail_id = tm.mail_id\n" +
            "  and m.message_id = :pMessageId";

    public static RequestRow getRequestByMessageId(String messageId) throws SQLException {
        //System.out.println(messageId);
        return DbProvider.db().open()
                .createQuery(SqlSelectRequest)
                .addParameter("pMessageId", messageId)
                .executeAndFetch(RequestRow.class)
                .get(0);
    }
}
