package ru.yandex.market.logshatter.parser.direct;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class BsExportDataResponseLogParserTest {

    @Test
    public void testParse() throws Exception {
        String line2 = "{\"log_time\":\"2019-01-28 00:03:15\",\"data\":{\"Archive\":\"0\",\"UpdateTime\":\"20190128000253\"," +
            "\"ID\":\"17961734\",\"EID\":\"31237610\",\"Stop\":\"1\"},\"par_norm_nick\":\"std_3\",\"cid\":\"31237610\"," +
            "\"reqid\":5628480185114786391,\"level\":\"ORDER\",\"host\":\"man1-9606-man-ppc-direct-scripts-1-32223.gencfg-c.yandex.net\"," +
            "\"backend_host\":\"bssoap03e\",\"uuid\":\"DA43C944-2276-11E9-8495-0712256E4268\",\"shard\":\"11\"}";

        String data = "{\"Archive\":\"0\",\"UpdateTime\":\"20190128000253\"," +
            "\"ID\":\"17961734\",\"EID\":\"31237610\",\"Stop\":\"1\"}";

        BsExportDataResponseLogParser parser = new BsExportDataResponseLogParser();
        LogParserChecker checker = new LogParserChecker(parser);
        checker.setFile("/tmp/test-response-log");
        checker.setHost("ppcdiscord.yandex.ru");

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

        checker.check(line2,
            dateTimeFormat.parse("2019-01-28 00:03:15"),
            31237610L,
            212L,
            "std_3",
            "response",
            0L,
            0L,
            0L,
            0L,
            "file:ppcdiscord.yandex.ru/tmp/test-response-log",
            "uuid=DA43C944-2276-11E9-8495-0712256E4268",
            0L,
            data
        );
    }


    @Test
    public void testParse2() throws Exception {
        String line2 = "{\"cid\":\"32377888\",\"par_norm_nick\":\"std_3\"," +
            "\"data\":{\"EID\":\"3650944676\",\"PHRASE\":{\"P_1029592409_15536608670\":{\"ID\":\"1029592409\",\"EID\":\"15536608670\"}," +
            "\"P_1242845425_15536670471\":{\"EID\":\"15536670471\",\"ID\":\"1242845425\"}},\"ID\":\"1844221378\"," +

            "\"BANNER\":{\"B_6694962649_6883381927\":{\"ID\":\"6694962649\",\"EID\":\"6883381927\",\"Stop\":\"0\"}}}," +
            "\"log_time\":\"2019-01-28 00:03:15\",\"shard\":\"11\",\"uuid\":\"DA43C944-2276-11E9-8495-0712256E4268\"," +
            "\"backend_host\":\"bssoap03e\",\"host\":\"man1-9606-man-ppc-direct-scripts-1-32223.gencfg-c.yandex.net\"," +
            "\"level\":\"CONTEXT\",\"reqid\":5628480185114786391,\"pid\":\"3650944676\"}";

        BsExportDataResponseLogParser parser = new BsExportDataResponseLogParser();
        LogParserChecker checker = new LogParserChecker(parser);
        checker.setFile("/tmp/test-response-log");
        checker.setHost("ppcdiscord.yandex.ru");

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;
        Date date = dateTimeFormat.parse("2019-01-28 00:03:15");

        String dataBidResult = "{\"ID\":\"6694962649\",\"EID\":\"6883381927\",\"Stop\":\"0\"}";
        String dataPidResult = "{\"EID\":\"3650944676\",\"PHRASE\":{\"P_1029592409_15536608670\":{\"ID\":\"1029592409\",\"EID\":\"15536608670\"}," +
            "\"P_1242845425_15536670471\":{\"EID\":\"15536670471\",\"ID\":\"1242845425\"}},\"ID\":\"1844221378\"}";

        Object[] dataBid = new Object[]{
            32377888L,
            212L,
            "std_3",
            "response",
            3650944676L,
            6883381927L,
            0L,
            0L,
            "file:ppcdiscord.yandex.ru/tmp/test-response-log",
            "uuid=DA43C944-2276-11E9-8495-0712256E4268",
            0L,
            dataBidResult
        };

        Object[] dataPid = new Object[]{
            32377888L,
            212L,
            "std_3",
            "response",
            3650944676L,
            0L,
            0L,
            0L,
            "file:ppcdiscord.yandex.ru/tmp/test-response-log",
            "uuid=DA43C944-2276-11E9-8495-0712256E4268",
            0L,
            dataPidResult
        };

        checker.check(line2,
            Arrays.asList(date, date),
            Arrays.asList(dataBid, dataPid)
        );
    }
}

