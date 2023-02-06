package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.Test;
import java.util.Arrays;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class BsExportDataRequestLogParserTest {
    @Test
    public void testParse() throws Exception {
        String line = "[{'cid': '15239851'," +
            "'data': {'AgencyID': '417938'," +
                "'AttributionType': 1," +
                "'AutoOptimization': 1," +
                "'ClientID': '1616282'," +
                "'ContentType': 'text'," +
                "'ContextPriceCoef': '100'," +
                "'CurrencyConvertDate': '20160623'," +
                "'CurrencyISOCode': 643," +
                "'Description': '15239851: general_poisk_exp'," +
                "'EID': '15239851'," +
                "'GroupOrder': 0," +
                "'GroupOrderID': '3158315'," +
                "'ID': '6938101'," +
                "'IndependentBids': 0," +
                "'ManagerUID': 0," +
                "'MaxCPC': 0," +
                "'OrderType': 1," +
                "'QueueSetTime': '20181226182616'," +
                "'SUM': '2717.330000'," +
                "'SUMCur': '1122073.281500'," +
                "'Start_time': '20151110000000'," +
                "'Stop': 0," +
                "'UpdateInfo': 0," +
                "'isExtendedRelevanceMatchEnabled': 0," +
                "'isVirtual': 0}," +
            "'debug_info': {'host': 'vla1-5385-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net'," +
                "'par_norm_nick': 'heavy_4'," +
                "'reqid': 2568384893738646080," +
                "'send_time': '2019-01-16 17:13:02'," +
                "'shard': '4'}," +
            "'full_export_flag': 0," +
            "'iter_id': '395410733'," +
            "'level': 'ORDER'," +
            "'uuid': 'F9D0CD98-0926-11E9-8FA2-7138C2D82987'}]";

        String data = "{\"AgencyID\":\"417938\",\"AttributionType\":1,\"AutoOptimization\":1,\"ClientID\":\"1616282\"," +
            "\"ContentType\":\"text\",\"ContextPriceCoef\":\"100\",\"CurrencyConvertDate\":\"20160623\",\"CurrencyISOCode\":643," +
            "\"Description\":\"15239851: general_poisk_exp\",\"EID\":\"15239851\",\"GroupOrder\":0,\"GroupOrderID\":\"3158315\"," +
            "\"ID\":\"6938101\",\"IndependentBids\":0,\"ManagerUID\":0,\"MaxCPC\":0,\"OrderType\":1,\"QueueSetTime\":\"20181226182616\"," +
            "\"SUM\":\"2717.330000\",\"SUMCur\":\"1122073.281500\",\"Start_time\":\"20151110000000\",\"Stop\":0,\"UpdateInfo\":0," +
            "\"isExtendedRelevanceMatchEnabled\":0,\"isVirtual\":0}";

        BsExportDataRequestLogParser parser = new BsExportDataRequestLogParser();
        LogParserChecker checker = new LogParserChecker(parser);
        checker.setHost("ppcdiscord.yandex.ru");

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

        checker.check(line,
            dateTimeFormat.parse("2019-01-16 17:13:02"),
            15239851L,
            33L,
            "heavy_4",
            "request",
            0L,
            0L,
            0L,
            0L,
            "file:ppcdiscord.yandex.ru",
            "uuid=F9D0CD98-0926-11E9-8FA2-7138C2D82987",
            0L,
            data
            );
    }

    @Test
    public void testParse2() throws Exception {
        String line = "[{'cid': '15239851'," +
            "'data': {" +
                "'BANNER': {'B_6663707641_6817852983_0': {'Age': 1," +
                    "'Archive': 0," +
                    "'EID': '6817852983'," +
                    "'Flags': ''," +
                    "'ID': '6663707641'," +
                    "'ParentExportID': '1400487443'," +
                    "'Stop': 0," +
                    "'UpdateInfo': 0}}," +
                "'EID': '1029753000'," +
                "'ID': '1770503641'," +
                "'Type': 'base'," +
                "'UpdateInfo': 0}," +
            "'debug_info': {'host': 'vla1-5385-vla-ppc-direct-scripts-2-32286.gencfg-c.yandex.net'," +
                "'par_norm_nick': 'heavy_4'," +
                "'reqid': 2568384893738646080," +
                "'send_time': '2019-01-16 17:13:04'," +
                "'shard': '4'}," +
            "'full_export_flag': 0," +
            "'iter_id': '395410733'," +
            "'level': 'CONTEXT'," +
            "'pid': '1029753000'," +
            "'uuid': 'F9D0CD98-0926-11E9-8FA2-7138C2D82987'}]";

        String dataBid = "{\"Age\":1,\"Archive\":0,\"EID\":\"6817852983\",\"Flags\":\"\",\"ID\":\"6663707641\"," +
            "\"ParentExportID\":\"1400487443\",\"Stop\":0,\"UpdateInfo\":0}";
        String dataCid = "{\"EID\":\"1029753000\",\"ID\":\"1770503641\",\"Type\":\"base\",\"UpdateInfo\":0}";

        BsExportDataRequestLogParser parser = new BsExportDataRequestLogParser();
        LogParserChecker checker = new LogParserChecker(parser);
        checker.setHost("ppcdiscord.yandex.ru");

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

        Date date = dateTimeFormat.parse("2019-01-16 17:13:04");

        Object[] data1 = new Object[]{
            15239851L,
            33L,
            "heavy_4",
            "request",
            1029753000L,
            6817852983L,
            0L,
            0L,
            "file:ppcdiscord.yandex.ru",
            "uuid=F9D0CD98-0926-11E9-8FA2-7138C2D82987",
            0L,
            dataBid
        };

        Object[] data2 = new Object[]{
            15239851L,
            33L,
            "heavy_4",
            "request",
            1029753000L,
            0L,
            0L,
            0L,
            "file:ppcdiscord.yandex.ru",
            "uuid=F9D0CD98-0926-11E9-8FA2-7138C2D82987",
            0L,
            dataCid
        };

        checker.check(line,
            Arrays.asList(date, date),
            Arrays.asList(data1, data2)
        );
    }
}
