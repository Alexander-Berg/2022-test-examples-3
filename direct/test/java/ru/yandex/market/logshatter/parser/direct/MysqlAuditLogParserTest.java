package ru.yandex.market.logshatter.parser.direct;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.SimpleDateFormat;

public class MysqlAuditLogParserTest {
    @Test
    public void testParse() throws Exception {
        String line = "{\"audit_record\":{\"name\":\"Connect\",\"record\":\"65201748_2018-07-12T13:00:57\","
            + "\"timestamp\":\"2018-12-14T09:59:34 UTC\",\"connection_id\":\"32599053\",\"status\":0,\"user\":\"root\","
            + "\"priv_user\":\"root\",\"os_login\":\"\",\"proxy_user\":\"\",\"host\":\"localhost\",\"ip\":\"\",\"db\":\"\"}}";

        MysqlAuditLogParser parser = new MysqlAuditLogParser();
        LogParserChecker checker = new LogParserChecker(parser);
        checker.setHost("ppcdata1-01f.yandex.ru");

        SimpleDateFormat dateTimeFormat = parser.dateFormat;

        checker.check(line,
            dateTimeFormat.parse("2018-12-14T09:59:34 UTC"),
            "Connect",
            "65201748_2018-07-12T13:00:57",
            "ppcdata1-01f.yandex.ru",
            32599053L,
            0L,
            "root",
            "root",
            "",
            "",
            "localhost",
            "",
            ""
        );
    }
}
