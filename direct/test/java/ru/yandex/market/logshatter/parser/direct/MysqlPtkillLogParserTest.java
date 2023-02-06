package ru.yandex.market.logshatter.parser.direct;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.SimpleDateFormat;

public class MysqlPtkillLogParserTest {

    @Test
    public void testParse() throws Exception {
        String line = "{\"timestamp\":\"2021-06-16T18:05:47\", \"query_type\":\"KILL QUERY\", \"query_id\":7307489, \"query_command\":\"Query\", " 
	    + " \"query_time_second\": 43,  \"query_info\": \"SELECT /* reqid:1037975779019365290:direct.web:searchBanners:operator=665718361 */" 
            + " t.*, GROUP_CONCAT(t.bid SEPARATOR ',') AS bids FROM (select u.phone as user_phone, u.uid, u.login, u.FIO, u.ClientID\",  \"shard_name\": \"ppcdata8\"}";

        MysqlPtkillLogParser parser = new MysqlPtkillLogParser();
        LogParserChecker checker = new LogParserChecker(parser);
        checker.setHost("ppcback01f.yandex.ru");

        SimpleDateFormat dateTimeFormat = parser.dateFormat;

        checker.check(line,
            dateTimeFormat.parse("2021-06-16T18:05:47"),
            "ppcback01f.yandex.ru",
            7307489L,
            43L,
            "SELECT /* reqid:1037975779019365290:direct.web:searchBanners:operator=665718361 */ t.*, "
                + "GROUP_CONCAT(t.bid SEPARATOR ',') AS bids FROM (select u.phone as user_phone, u.uid, u.login, u.FIO, u.ClientID",
            "reqid:1037975779019365290:direct.web:searchBanners:operator=665718361",
            "ppcdata8"
        );
    }


}
