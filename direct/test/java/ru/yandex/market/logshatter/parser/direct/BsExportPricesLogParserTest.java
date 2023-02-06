package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class BsExportPricesLogParserTest {


    @Test
    public void test1() throws Exception {
        String line = "[{\"cid\":\"39794141\",\"data\":{\"AutoBudget\":0,\"AutoBudgetNetCPCOptimize\":0,\"BannerGroupEID\":\"3622507418\"}," +
			"\"debug_info\":{\"host\":\"vla1-5389-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net\"," +
			"\"par_norm_nick\":\"std_9\",\"reqid\":4375093524151236921,\"shard\":\"15\", \"send_time\": \"2018-09-28 06:09:16\"},\"full_export_flag\":0," +
			"\"iter_id\":\"402776804\",\"level\":\"PRICE\",\"pid\":\"3622507418\",\"uuid\":\"E62AE886-1818-11E9-8E08-F669C243540F\"}," +
            "{\"cid\":\"39794141\",\"data\":{\"AutoBudget\":1,\"AutoBudgetNetCPCOptimize\":1,\"BannerGroupEID\":\"3622507418\"}," +
            "\"debug_info\":{\"host\":\"vla1-5389-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net\"," +
            "\"par_norm_nick\":\"std_9\",\"reqid\":4375093524151236921,\"shard\":\"15\", \"send_time\": \"2018-09-28 06:09:16\"},\"full_export_flag\":0," +
            "\"iter_id\":\"402776804\",\"level\":\"PRICE\",\"pid\":\"3622507418\",\"uuid\":\"E62AE886-1818-11E9-8E08-F669C243540F\"}]";

        BsExportPricesLogParser parser = new BsExportPricesLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

        checker.setFile("/tmp/test-prices-log");
        checker.setHost("ppcdiscord.yandex.ru");

        checker.check(line,
            dateTimeFormat.parse("2018-09-28 06:09:16"),
            39794141L,
            218L,
            "std_9",
            "request",
            3622507418L,
            "file:ppcdiscord.yandex.ru/tmp/test-prices-log",
            "uuid=E62AE886-1818-11E9-8E08-F669C243540F",
            "[{\"AutoBudget\":0,\"AutoBudgetNetCPCOptimize\":0,\"BannerGroupEID\":\"3622507418\"}, {\"AutoBudget\":1,\"AutoBudgetNetCPCOptimize\":1,\"BannerGroupEID\":\"3622507418\"}]"
        );
    }

    @Test
    public void test2() throws Exception {
        String line = "[{\"cid\":\"39794141\",\"data\":{\"AutoBudget\":0,\"AutoBudgetNetCPCOptimize\":0,\"BannerGroupEID\":\"3622507418\"}," +
            "\"debug_info\":{\"host\":\"vla1-5389-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net\"," +
            "\"par_norm_nick\":\"std_9\",\"reqid\":4375093524151236921,\"shard\":\"15\", \"send_time\": \"2018-09-28 06:09:16\"},\"full_export_flag\":0," +
            "\"iter_id\":\"402776804\",\"level\":\"PRICE\",\"pid\":\"3622507418\",\"uuid\":\"E62AE886-1818-11E9-8E08-F669C243540F\"}," +
            "{\"cid\":\"39794142\",\"data\":{\"AutoBudget\":1,\"AutoBudgetNetCPCOptimize\":1,\"BannerGroupEID\":\"3622507419\"}," +
            "\"debug_info\":{\"host\":\"vla1-5389-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net\"," +
            "\"par_norm_nick\":\"std_9\",\"reqid\":4375093524151236921,\"shard\":\"15\", \"send_time\": \"2018-09-28 06:09:16\"},\"full_export_flag\":0," +
            "\"iter_id\":\"402776804\",\"level\":\"PRICE\",\"pid\":\"3622507419\",\"uuid\":\"E62AE886-1818-11E9-8E08-F669C243541F\"}]";

        BsExportPricesLogParser parser = new BsExportPricesLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

        checker.setFile("/tmp/test-prices-log");
        checker.setHost("ppcdiscord.yandex.ru");

        Date date = dateTimeFormat.parse("2018-09-28 06:09:16");

        Object[] data1 = new Object[]{
            39794141L,
            218L,
            "std_9",
            "request",
            3622507418L,
            "file:ppcdiscord.yandex.ru/tmp/test-prices-log",
            "uuid=E62AE886-1818-11E9-8E08-F669C243540F",
            "[{\"AutoBudget\":0,\"AutoBudgetNetCPCOptimize\":0,\"BannerGroupEID\":\"3622507418\"}]"
        };

        Object[] data2 = new Object[]{
            39794142L,
            218L,
            "std_9",
            "request",
            3622507419L,
            "file:ppcdiscord.yandex.ru/tmp/test-prices-log",
            "uuid=E62AE886-1818-11E9-8E08-F669C243541F",
            "[{\"AutoBudget\":1,\"AutoBudgetNetCPCOptimize\":1,\"BannerGroupEID\":\"3622507419\"}]"
        };

        checker.check(line,
            Arrays.asList(date, date),
            Arrays.asList(data1, data2)
        );
    }

    @Test
    public void test3() throws Exception {
        String line = "[{\"cid\":\"39794141\",\"data\":{\"AutoBudget\":0,\"AutoBudgetNetCPCOptimize\":0,\"BannerGroupEID\":\"3622507418\"}, " +
            "\"debug_info\":{\"host\":\"vla1-5389-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net\", "+
            "\"par_norm_nick\":\"std_9\",\"reqid\":4375093524151236921,\"shard\":\"15\", \"send_time\": \"2018-09-28 06:09:16\"},\"full_export_flag\":0, " +
            "\"iter_id\":\"402776804\",\"level\":\"PRICE\",\"pid\":\"3622507418\",\"uuid\":\"E62AE886-1818-11E9-8E08-F669C243540F\"}," +
            "{\"cid\":\"39794141\",\"data\":{\"AutoBudget\":1,\"AutoBudgetNetCPCOptimize\":1,\"BannerGroupEID\":\"3622507418\"}, " +
            "\"debug_info\":{\"host\":\"vla1-5389-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net\", " +
            "\"par_norm_nick\":\"std_9\",\"reqid\":4375093524151236921,\"shard\":\"15\", \"send_time\": \"2018-09-28 06:09:16\"},\"full_export_flag\":0, " +
            "\"iter_id\":\"402776804\",\"level\":\"PRICE\",\"pid\":\"3622507418\",\"uuid\":\"E62AE886-1818-11E9-8E08-F669C243540F\"}]";

        BsExportPricesLogParser parser = new BsExportPricesLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

        checker.setFile("/tmp/test-prices-log");
        checker.setHost("ppcdiscord.yandex.ru");

        checker.check(line,
            dateTimeFormat.parse("2018-09-28 06:09:16"),
            39794141L,
            218L,
            "std_9",
            "request",
            3622507418L,
            "file:ppcdiscord.yandex.ru/tmp/test-prices-log",
            "uuid=E62AE886-1818-11E9-8E08-F669C243540F",
            "[{\"AutoBudget\":0,\"AutoBudgetNetCPCOptimize\":0,\"BannerGroupEID\":\"3622507418\"}, {\"AutoBudget\":1,\"AutoBudgetNetCPCOptimize\":1,\"BannerGroupEID\":\"3622507418\"}]"
        );
    }

    @Test
    public void test4() throws Exception {
        String line = "[{\"cid\":\"39794141\",\"data\":{\"AutoBudget\":0,\"AutoBudgetNetCPCOptimize\":0,\"BannerGroupEID\":\"3622507418\"}," +
            "\"debug_info\":{\"host\":\"vla1-5389-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net\"," +
            "\"par_norm_nick\":\"std_9\",\"reqid\":4375093524151236921,\"shard\":\"15\", \"send_time\": \"2018-09-28 06:09:16\"},\"full_export_flag\":0," +
            "\"iter_id\":\"402776804\",\"level\":\"PRICE\",\"pid\":\"3622507418\",\"uuid\":\"E62AE886-1818-11E9-8E08-F669C243540F\"}," +
            "{\"cid\":\"39794141\",\"data\":{\"AutoBudget\":1,\"AutoBudgetNetCPCOptimize\":1,\"BannerGroupEID\":\"3622507419\"}," +
            "\"debug_info\":{\"host\":\"vla1-5389-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net\"," +
            "\"par_norm_nick\":\"std_9\",\"reqid\":4375093524151236921,\"shard\":\"15\", \"send_time\": \"2018-09-28 06:09:16\"},\"full_export_flag\":0," +
            "\"iter_id\":\"402776804\",\"level\":\"PRICE\",\"pid\":\"3622507419\",\"uuid\":\"E62AE886-1818-11E9-8E08-F669C243540F\"}]";

        BsExportPricesLogParser parser = new BsExportPricesLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

        checker.setFile("/tmp/test-prices-log");
        checker.setHost("ppcdiscord.yandex.ru");

        Date date = dateTimeFormat.parse("2018-09-28 06:09:16");

        Object[] data1 = new Object[]{
            39794141L,
            218L,
            "std_9",
            "request",
            3622507418L,
            "file:ppcdiscord.yandex.ru/tmp/test-prices-log",
            "uuid=E62AE886-1818-11E9-8E08-F669C243540F",
            "[{\"AutoBudget\":0,\"AutoBudgetNetCPCOptimize\":0,\"BannerGroupEID\":\"3622507418\"}]"
        };

        Object[] data2 = new Object[]{
            39794141L,
            218L,
            "std_9",
            "request",
            3622507419L,
            "file:ppcdiscord.yandex.ru/tmp/test-prices-log",
            "uuid=E62AE886-1818-11E9-8E08-F669C243540F",
            "[{\"AutoBudget\":1,\"AutoBudgetNetCPCOptimize\":1,\"BannerGroupEID\":\"3622507419\"}]"
        };

        checker.check(line,
            Arrays.asList(date, date),
            Arrays.asList(data1, data2)
        );
    }
}
