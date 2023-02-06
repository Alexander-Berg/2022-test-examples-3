package ru.yandex.market.logshatter.parser.direct;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static java.util.Collections.singletonList;

public class MySqlSlowLogParserNewTest {
    private final LogParserChecker checker = new LogParserChecker(new MySqlSlowLogParserNew());
    @Test
    public void testParseSleep() throws Exception {
        String line = "{\"clusterId\":\"mdbtd5ut53qg8v2misp9\",\"clusterName\":\"ppcdata14-testing-2020\"," +
            "\"clusterVersion\":1,\"recordIdTimestamp\":1637096431000,\"recordIdPositionInTimestamp\":0," +
            "\"queryNormalForm\":\"select ?, sleep(?)\",\"queryService\":\"\",\"queryMethod\":\"\"," +
            "\"queryRequestId\":0,\"timestamp\":\"2021-11-16T21:00:31.454612Z\",\"queryText\":\"select SLEEP(61), '/*" +
            " pt-kill-me */';\",\"userName\":\"direct-test\",\"userSecondName\":\"direct-test\"," +
            "\"userHost\":\"ppcdev1.da.yandex.ru\",\"userIp\":\"2a02:6b8:c03:35a:0:1358:b2aa:41a\"," +
            "\"connectionId\":3696838,\"schema\":\"ppc\",\"lastErrorNumber\":0,\"killedCode\":0," +
            "\"queryTimeInSeconds\":29.486171,\"lockTimeInSeconds\":0.0,\"rowsSentCount\":1,\"rowsExaminedCount\":0," +
            "\"rowsAffectedCount\":0,\"bytesSentCount\":116,\"tempTablesCount\":0,\"tempTablesOnDiskCount\":0," +
            "\"tempTablesSizesInBytes\":0,\"transactionId\":0,\"hasQcHit\":false,\"hasFullScan\":false," +
            "\"hasFullJoin\":false,\"hasTempTables\":false,\"hasTempTablesOnDisk\":false,\"hasFileSort\":false," +
            "\"hasFileSortOnDisk\":false,\"mergePassesCount\":0,\"innoDbIoReadOperationsCount\":0," +
            "\"innoDbIoReadBytesCount\":0,\"innoDbReadWaitInSeconds\":0.0,\"innoDbRecordsLockWaitInSeconds\":0.0," +
            "\"innoQbQueueWaitInSeconds\":0.0,\"innoDbPagesCountDistinct\":0,\"rawRecordText\":\"test\"}";

        Date date = new Date(1637096431000L);
        Object[] row = new Object[]{
            "mdbtd5ut53qg8v2misp9",
            "ppcdata14-testing-2020",
            1,
            0,
            "select ?, sleep(?)",
            "",
            "",
            0L,
            "select SLEEP(61), '/* pt-kill-me */';",
            "direct-test",
            "direct-test",
            "ppcdev1.da.yandex.ru",
            "2a02:6b8:c03:35a:0:1358:b2aa:41a",
            3696838L,
            "ppc",
            0,
            0,
            29.486171D,
            0.0D,
            1L,
            0L,
            0L,
            116L,
            0, 0,
            0L, 0L,
            false, false, false, false, false, false, false,
            0,
            0, 0L, 0.0D, 0.0D, 0.0D, 0,
            "test"
        };

        checker.check(line,
            singletonList(date),
            singletonList(row)
        );
    }

    @Test
    public void testParseLogMillisNanosDateFormat() throws Exception {
        String line = "{\"clusterId\":\"mdb6gq7f2cavi8qlkk6j\",\"clusterName\":\"ppcdata13-testing-2020\"," +
            "\"clusterVersion\":1,\"recordIdTimestamp\":1638320565000,\"recordIdPositionInTimestamp\":2," +
            "\"queryNormalForm\":\"select bs_auction_stat.phraseid, bs_auction_stat.pid from bs_auction_stat left " +
            "outer join bids on (bids.pid = bs_auction_stat.pid and bids.phraseid = bs_auction_stat.phraseid) where " +
            "(bids.id is null and bs_auction_stat.stattime < ?) limit ?\",\"queryService\":\"direct.jobs\"," +
            "\"queryMethod\":\"bsclearidhistory.BsClearIdHistoryJob\",\"queryRequestId\":\"4161395112758053418\"," +
            "\"timestamp\":1638320565.764542000,\"queryText\":\"select /* reqid:4161395112758053418:direct" +
            ".jobs:bsclearidhistory.BsClearIdHistoryJob */ `bs_auction_stat`.`pid`, `bs_auction_stat`.`PhraseID` from" +
            " `bs_auction_stat` left outer join `bids` on (`bids`.`pid` = `bs_auction_stat`.`pid` and `bids`" +
            ".`PhraseID` = `bs_auction_stat`.`PhraseID`) where (`bids`.`id` is null and `bs_auction_stat`.`stattime` " +
            "< '2021-11-30 03:59:06.423862') limit 500000;\",\"userName\":\"direct-test\"," +
            "\"userSecondName\":\"direct-test\",\"userHost\":\"\",\"userIp\":\"2a02:6b8:c08:1c8a:0:1358:6ffe:0\"," +
            "\"connectionId\":5781405,\"schema\":\"ppc\",\"lastErrorNumber\":0,\"killedCode\":0," +
            "\"queryTimeInSeconds\":219.338534,\"lockTimeInSeconds\":5.38E-4,\"rowsSentCount\":0," +
            "\"rowsExaminedCount\":28976569,\"rowsAffectedCount\":0,\"bytesSentCount\":156,\"tempTablesCount\":0," +
            "\"tempTablesOnDiskCount\":0,\"tempTablesSizesInBytes\":0,\"transactionId\":0,\"hasQcHit\":false," +
            "\"hasFullScan\":true,\"hasFullJoin\":false,\"hasTempTables\":false,\"hasTempTablesOnDisk\":false," +
            "\"hasFileSort\":false,\"hasFileSortOnDisk\":false,\"mergePassesCount\":0," +
            "\"innoDbIoReadOperationsCount\":632890,\"innoDbIoReadBytesCount\":10369269760," +
            "\"innoDbReadWaitInSeconds\":120.674057,\"innoDbRecordsLockWaitInSeconds\":0.0," +
            "\"innoQbQueueWaitInSeconds\":0.0,\"innoDbPagesCountDistinct\":8191}";

        Date date = new Date(1638320565000L);
        Object[] row = new Object[]{
            "mdb6gq7f2cavi8qlkk6j",
            "ppcdata13-testing-2020",
            1,
            2,
            "select bs_auction_stat.phraseid, bs_auction_stat.pid from bs_auction_stat left outer join bids on (bids.pid = bs_auction_stat.pid and bids.phraseid = bs_auction_stat.phraseid) where (bids.id is null and bs_auction_stat.stattime < ?) limit ?",
            "direct.jobs",
            "bsclearidhistory.BsClearIdHistoryJob",
            4161395112758053418L,
            "select /* reqid:4161395112758053418:direct.jobs:bsclearidhistory.BsClearIdHistoryJob */ `bs_auction_stat`.`pid`, `bs_auction_stat`.`PhraseID` from `bs_auction_stat` left outer join `bids` on (`bids`.`pid` = `bs_auction_stat`.`pid` and `bids`.`PhraseID` = `bs_auction_stat`.`PhraseID`) where (`bids`.`id` is null and `bs_auction_stat`.`stattime` < '2021-11-30 03:59:06.423862') limit 500000;",
            "direct-test",
            "direct-test",
            "",
            "2a02:6b8:c08:1c8a:0:1358:6ffe:0",
            5781405L,
            "ppc",
            0,
            0,
            219.338534D,
            0.000538D,
            0L,
            28976569L,
            0L,
            156L,
            0, 0,
            0L, 0L,
            false, true, false, false, false, false, false,
            0,
            632890, 10369269760L, 120.674057D, 0.0D, 0.0D, 8191,
            ""
        };

        checker.check(line,
            singletonList(date),
            singletonList(row)
        );
    }

    @Test
    public void testParseOsiDateFormat() throws Exception {
        String line = "{\"clusterId\":\"mdbmp5hdachjk7g2rtfa\",\"clusterName\":\"ppcdata12-testing-2020\"," +
            "\"clusterVersion\":1,\"recordIdTimestamp\":1638184943000,\"recordIdPositionInTimestamp\":0," +
            "\"queryNormalForm\":\"select c.cid, sum(if(b.statusbssynced = ?, ?, ?)) banners_num, sum(if(p" +
            ".statusbssynced = ?, ?, ?)) contexts_num from bs_export_candidates bec join campaigns c on c.cid = bec" +
            ".cid left join phrases p on p.cid = c.cid left join banners b on b.pid = p.pid where c.statusbssynced = " +
            "? or b.statusbssynced = ? or p.statusbssynced = ? and bec.cid in (...) group by c.cid order by null\"," +
            "\"queryService\":\"direct.script\",\"queryMethod\":\"bsExportMaster\"," +
            "\"queryRequestId\":2645806162065311082,\"timestamp\":\"2021-11-29T11:22:23.088756Z\"," +
            "\"queryText\":\"SELECT /* reqid:2645806162065311082:direct.script:bsExportMaster */\\n                c" +
            ".cid,\\n                SUM(\\n                    IF(b.statusBsSynced = 'Sending', 1, 0)\\n            " +
            "    ) banners_num\\n              , SUM(\\n                    IF(p.statusBsSynced = 'Sending', 1, 0)\\n" +
            "                ) contexts_num\\n            FROM\\n                bs_export_candidates bec\\n         " +
            "       JOIN campaigns c ON c.cid = bec.cid\\n                LEFT JOIN phrases p ON p.cid = c.cid\\n    " +
            "            LEFT JOIN banners b ON b.pid = p.pid\\n            WHERE\\n                c.statusBsSynced " +
            "= 'Sending' OR b.statusBsSynced = 'Sending' OR p.statusBsSynced = 'Sending'\\n         AND `bec`.`cid` " +
            "IN ('290416588')\\n            GROUP BY c.cid\\n            ORDER BY null;\"," +
            "\"userName\":\"direct-test\",\"userSecondName\":\"direct-test\"," +
            "\"userHost\":\"direct-testing-perl-intapi-2.man.yp-c.yandex.net\"," +
            "\"userIp\":\"2a02:6b8:c25:16d4:0:1358:9b3f:0\",\"connectionId\":5164461,\"schema\":\"ppc\"," +
            "\"lastErrorNumber\":0,\"killedCode\":0,\"queryTimeInSeconds\":35.950879,\"lockTimeInSeconds\":1.85E-4," +
            "\"rowsSentCount\":1434,\"rowsExaminedCount\":3056998,\"rowsAffectedCount\":0,\"bytesSentCount\":27797," +
            "\"tempTablesCount\":1,\"tempTablesOnDiskCount\":0,\"tempTablesSizesInBytes\":0,\"transactionId\":0," +
            "\"hasQcHit\":false,\"hasFullScan\":true,\"hasFullJoin\":false,\"hasTempTables\":true," +
            "\"hasTempTablesOnDisk\":false,\"hasFileSort\":false,\"hasFileSortOnDisk\":false,\"mergePassesCount\":0," +
            "\"innoDbIoReadOperationsCount\":172150,\"innoDbIoReadBytesCount\":2820505600," +
            "\"innoDbReadWaitInSeconds\":23.382996,\"innoDbRecordsLockWaitInSeconds\":0.0," +
            "\"innoQbQueueWaitInSeconds\":0.0,\"innoDbPagesCountDistinct\":8191,\"rawRecordText\":\"\\\\t\\\\n\\\\t\\\\n\"}";

        Date date = new Date(1638184943000L);
        Object[] row = new Object[]{
            "mdbmp5hdachjk7g2rtfa",
            "ppcdata12-testing-2020",
            1,
            0,
            "select c.cid, sum(if(b.statusbssynced = ?, ?, ?)) banners_num, sum(if(p.statusbssynced = ?, ?, ?)) contexts_num from bs_export_candidates bec join campaigns c on c.cid = bec.cid left join phrases p on p.cid = c.cid left join banners b on b.pid = p.pid where c.statusbssynced = ? or b.statusbssynced = ? or p.statusbssynced = ? and bec.cid in (...) group by c.cid order by null",
            "direct.script",
            "bsExportMaster",
            2645806162065311082L,
            "SELECT /* reqid:2645806162065311082:direct.script:bsExportMaster */\n                c.cid,\n         " +
                "       SUM(\n                    IF(b.statusBsSynced = 'Sending', 1, 0)\n                ) " +
                "banners_num\n              , SUM(\n                    IF(p.statusBsSynced = 'Sending', 1, 0)\n  " +
                "              ) contexts_num\n            FROM\n                bs_export_candidates bec\n       " +
                "         JOIN campaigns c ON c.cid = bec.cid\n                LEFT JOIN phrases p ON p.cid = c" +
                ".cid\n                LEFT JOIN banners b ON b.pid = p.pid\n            WHERE\n                c" +
                ".statusBsSynced = 'Sending' OR b.statusBsSynced = 'Sending' OR p.statusBsSynced = 'Sending'\n      " +
                "   AND `bec`.`cid` IN ('290416588')\n            GROUP BY c.cid\n            ORDER BY null;",
            "direct-test",
            "direct-test",
            "direct-testing-perl-intapi-2.man.yp-c.yandex.net",
            "2a02:6b8:c25:16d4:0:1358:9b3f:0",
            5164461L,
            "ppc",
            0,
            0,
            35.950879D,
            0.000185D,
            1434L,
            3056998L,
            0L,
            27797L,
            1, 0,
            0L, 0L,
            false, true, false, true, false, false, false,
            0,
            172150, 2820505600L, 23.382996D, 0.0D, 0.0D, 8191,
            "\\t\\n\\t\\n"
        };

        checker.check(line,
            singletonList(date),
            singletonList(row)
        );
    }
}
