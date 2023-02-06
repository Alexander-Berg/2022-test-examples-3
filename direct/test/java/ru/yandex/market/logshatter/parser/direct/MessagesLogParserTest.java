package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class MessagesLogParserTest {
    MessagesLogParser parser = new MessagesLogParser();
    LogParserChecker checker = new LogParserChecker(parser);
    SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

    @Test
    public void testJavaLogWithoutNanos() throws Exception {
        String line =
            "2017-05-12:00:00:00 ppcdev-java-2.haze.yandex.net,direct.jobs/campaignlastchange.CampAggregatedLastchangeFeeder,4370699468929123824:0:4370699468929123824 [direct-job-pool_Worker-5] INFO  ru.yandex.direct.jobs.interceptors.JobLoggingInterceptor - START shard_7";

        checker.check(line,
            dateTimeFormat.parse("2017-05-12:00:00:00"),
            "direct.jobs",
            "campaignlastchange.CampAggregatedLastchangeFeeder",
            0,
            "ppcdev-java-2.haze.yandex.net",
            4370699468929123824L,
            0L,
            4370699468929123824L,
            "direct-job-pool_Worker-5",
            "INFO",
            "ru.yandex.direct.jobs.interceptors.JobLoggingInterceptor",
            "START shard_7"
        );
    }

    @Test
    public void testJavaLogWithNanos() throws Exception {
        String line =
            "2018-09-25:16:34:30.258 man1-9615-man-ppc-direct-java-api5-19625.gencfg-c.yandex.net,direct.api5/unknown,0:0:0 [main] INFO  ru.yandex.direct.common.admin.engine.AdminServlet - FrameworkServlet 'admin': initialization started";

        checker.check(line,
            dateTimeFormat.parse("2018-09-25:16:34:30.258"),
            "direct.api5",
            "unknown",
            258000000,
            "man1-9615-man-ppc-direct-java-api5-19625.gencfg-c.yandex.net",
            0L,
            0L,
            0L,
            "main",
            "INFO",
            "ru.yandex.direct.common.admin.engine.AdminServlet",
            "FrameworkServlet 'admin': initialization started"
        );
    }

    @Test
    public void testJavaLogWithLongMessage() throws Exception {
        String line =
            "2018-12-02:12:29:04.944737000 man1-9615-man-ppc-direct-java-api5-19625.gencfg-c.yandex.net,unknown/unknown,0:0:0 [jetty-worker-1-49] WARN  com.zaxxer.hikari.pool.PoolBase - ppcdict__2 - Failed to validate connection com.mysql.jdbc.JDBC4Connection@2086705f (No operations allowed after connection closed.). Possibly consider using a shorter maxLifetime value.";

        checker.check(line,
            dateTimeFormat.parse("2018-12-02:12:29:04"),
            "unknown",
            "unknown",
            944737000,
            "man1-9615-man-ppc-direct-java-api5-19625.gencfg-c.yandex.net",
            0L,
            0L,
            0L,
            "jetty-worker-1-49",
            "WARN",
            "com.zaxxer.hikari.pool.PoolBase",
            "ppcdict__2 - Failed to validate connection com.mysql.jdbc.JDBC4Connection@2086705f (No operations allowed after connection closed.). Possibly consider using a shorter maxLifetime value."
        );
    }


    @Test
    public void testJavaLogWithLongMessage1() throws Exception {
        String line =
            "2019-04-01:00:09:59.947 sas1-2258-sas-ppc-direct-java-jobs-28256.gencfg-c.yandex.net,direct.jobs/promocodes.CheckChangedCampaignsJob,3083619779941187247:0:3083619779941187247 [direct-job-pool_Worker-40] INFO  ru.yandex.direct.core.entity.promocodes.service.PromocodesAntiFraudService - Going to tear off ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo{id=6185398, code=BELHEXQ8JXC3YYJT, invoiceId=92584907, invoiceExternalId=BLR-1630102485-1, invoiceEnabledAt=2019-03-31T14:08:10} from order 7-42203672";
        checker.check(line,
            dateTimeFormat.parse("2019-04-01:00:09:59.947"),
            "direct.jobs",
            "promocodes.CheckChangedCampaignsJob",
            947000000,
            "sas1-2258-sas-ppc-direct-java-jobs-28256.gencfg-c.yandex.net",
            3083619779941187247L,
            0L,
            3083619779941187247L,
            "direct-job-pool_Worker-40",
            "INFO",
            "ru.yandex.direct.core.entity.promocodes.service.PromocodesAntiFraudService",
            "Going to tear off ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo{id=6185398, code=BELHEXQ8JXC3YYJT, invoiceId=92584907, invoiceExternalId=BLR-1630102485-1, invoiceEnabledAt=2019-03-31T14:08:10} from order 7-42203672"
        );
    }

    @Test
    public void testPerlLogWithoutNanos() throws Exception {
        String line =
            "2016-07-22:18:42:00 ppcdev4.yandex.ru,direct.script/ppcMonitorYTResourceUsage,1314708194963030138:0:1314708194963030138 message";

        checker.check(line,
            dateTimeFormat.parse("2016-07-22:18:42:00"),
            "direct.script",
            "ppcMonitorYTResourceUsage",
            0,
            "ppcdev4.yandex.ru",
            1314708194963030138L,
            0L,
            1314708194963030138L,
            "",
            "",
            "",
            "message"
        );
    }

    @Test
    public void testPerlLogWithBulk() throws Exception {
        String line =
            "2016-07-22:18:42:00 ppcdev4.yandex.ru,direct.script/ppcMonitorYTResourceUsage,1314708194963030138:0:1314708194963030138#bulk "
                + "[{\"response\":{\"error\":\"BS_TOO_MUCH_STATISTICS\",\"format\":\"default\"},"
                + "\"request\":{\"with_nds\":\"0\",\"page_size\":\"1000\","
                + "\"with_discount\":1,\"group_by_date\":\"none\"}}]";

        checker.check(line,
            dateTimeFormat.parse("2016-07-22:18:42:00"),
            "direct.script",
            "ppcMonitorYTResourceUsage",
            0,
            "ppcdev4.yandex.ru",
            1314708194963030138L,
            0L,
            1314708194963030138L,
            "",
            "",
            "",
            "{\"response\":{\"error\":\"BS_TOO_MUCH_STATISTICS\",\"format\":\"default\"},"
                + "\"request\":{\"with_nds\":\"0\",\"page_size\":\"1000\","
                + "\"with_discount\":1,\"group_by_date\":\"none\"}}"
        );
    }

    @Test
    public void testPerlLogWithPrefix() throws Exception {
        String line =
            "2018-09-26:00:50:30.020650000 iva1-5915-msk-iva-ppc-direct-f-ede-14646.gencfg-c.yandex.net,direct.web/ajaxGetUrlPhrases,3075295632914456618:0:3075295632914456618 [suggest_phrases] get_transliterations";

        checker.check(line,
            dateTimeFormat.parse("2018-09-26:00:50:30"),
            "direct.web",
            "ajaxGetUrlPhrases",
            20650000,
            "iva1-5915-msk-iva-ppc-direct-f-ede-14646.gencfg-c.yandex.net",
            3075295632914456618L,
            0L,
            3075295632914456618L,
            "suggest_phrases",
            "",
            "",
            "get_transliterations"
        );
    }
}
