package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class LogApiParserTest {
    LogApiParser parser = new LogApiParser();
    LogParserChecker checker = new LogParserChecker(parser);
    SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

    @Test
    public void testSimpleLog() throws Exception {
        String line = "<134>1 2018-10-03T00:00:00+03:00 vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net "
            + "PPCLOG.ppclog_api.log 82655 - - 2018-10-03 00:00:00 {\"proc_id\":82655,\"reqid\":3736949679848410928,"
            + "\"cluid\":\"639325925\",\"log_type\":\"api\",\"interface\":\"json\",\"ip\":\"188.225.72.117\",\"bid\":0,"
            + "\"response_headers\":{},\"api_version\":\"5\",\"uid\":\"68764119\","
            + "\"host\":\"vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net\",\"cid\":[\"34563122\",\"34563123\","
            + "\"34575140\",\"34575408\",\"34575634\",\"34575869\",\"34946255\",\"34984042\",\"35017139\",\"35590683\","
            + "\"35590717\",\"35599585\",\"36698001\",\"36698251\",\"36698536\",\"36698691\",\"36698764\",\"36698843\"],"
            + "\"warning_object_count\":0,\"units\":26,\"units_stats\":[26,1097627,\"1100000\"],"
            + "\"units_spending_user_client_id\":\"41505547\",\"param\":{\"SelectionCriteria\":{},"
            + "\"FieldNames\":[\"Id\",\"Name\",\"Funds\",\"Statistics\",\"Status\",\"StatusClarification\","
            + "\"StatusPayment\",\"State\",\"RepresentedBy\"]},\"sleeptime\":null,\"error_object_count\":0,"
            + "\"cmd\":\"campaigns.get\",\"http_status\":0,\"error_detail\":\"\",\"logtime\":\"20181003000000\","
            + "\"application_id\":\"b2c15e83810348ba9c8fedafdc108153\",\"runtime\":0.580021142959595}";

        checker.check(
            line,
            dateTimeFormat.parse("2018-10-03 00:00:00"),
            new ArrayList<>(Arrays.asList(34563122L, 34563123L, 34575140L, 34575408L, 34575634L, 34575869L,
                34946255L, 34984042L, 35017139L, 35590683L, 35590717L, 35599585L, 36698001L, 36698251L, 36698536L, 36698691L,
                36698764L, 36698843L)),
            new ArrayList<>(Arrays.asList(0L)),
            "188.225.72.117",
            "campaigns.get",
            0.580021142959595f,
            "{\"SelectionCriteria\":{},\"FieldNames\":[\"Id\",\"Name\",\"Funds\",\"Statistics\",\"Status\",\"StatusClarification\",\"StatusPayment\",\"State\",\"RepresentedBy\"]}",
            0L,
            new ArrayList<>(Arrays.asList(639325925L)),
            3736949679848410928L,
            68764119L,
            "vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net",
            82655L,
            0f,
            "",
            26L,
            "[26,1097627,\"1100000\"]",
            (short) 5,
            "json",
            "b2c15e83810348ba9c8fedafdc108153",
            "",
            new ArrayList<>(Arrays.asList()),
            "file:/tmp/hostname.test/access.log",
            41505547L,
            0L,
            0L
        );
    }

    @Test
    public void testCmdGetBalanceLog() throws Exception {
        String line = "<134>1 2018-10-03T00:23:14+03:00 vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net "
            + "PPCLOG.ppclog_api.log 162315 - - 2018-10-03 00:23:14 {\"ip\":\"85.143.166.249\","
            + "\"reqid\":3738482614731496510,\"api_version\":104,\"interface\":\"json\",\"cid\":[],"
            + "\"units_stats\":\"--- []\\n\",\"application_id\":\"f5ff8f217d0d44b59d6e6e1d335e13f5\","
            + "\"cluid\":\"\",\"units\":0,\"http_status\":0,\"sleeptime\":null,\"log_type\":\"api\","
            + "\"proc_id\":162315,\"host\":\"vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net\","
            + "\"error_detail\":\"\",\"logtime\":\"20181003002314\",\"bid\":0,\"runtime\":0.175924062728882,"
            + "\"uid\":\"290296631\",\"cmd\":\"GetBalance\",\"param\":[\"28479990\",\"31631365\",\"32382735\",\"35828305\",\"35828311\"]}";

        checker.check(
            line,
            dateTimeFormat.parse("2018-10-03 00:23:14"),
            new ArrayList<>(Arrays.asList(28479990L, 31631365L, 32382735L, 35828305L, 35828311L)),
            new ArrayList<>(Arrays.asList(0L)),
            "85.143.166.249",
            "GetBalance",
            0.175924062728882f,
            "[\"28479990\",\"31631365\",\"32382735\",\"35828305\",\"35828311\"]",
            0L,
            new ArrayList<>(),
            3738482614731496510L,
            290296631L,
            "vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net",
            162315L,
            0f,
            "",
            0L,
            "--- []\n",
            (short) 104,
            "json",
            "f5ff8f217d0d44b59d6e6e1d335e13f5",
            "",
            new ArrayList<>(),
            "file:/tmp/hostname.test/access.log",
            0L,
            0L,
            0L
        );
    }

    @Test
    public void testCmdTransferMoneyLog() throws Exception {
        String line = "<134>1 2018-10-03T01:02:04+03:00 vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net "
            + "PPCLOG.ppclog_api.log 305830 - - 2018-10-03 01:02:04 {\"ip\":\"178.33.63.99\","
            + "\"reqid\":3741044692388071810,\"api_version\":\"4\",\"interface\":\"soap\",\"units_stats\":\"--- []\\n\","
            + "\"cid\":[],\"application_id\":\"f568746515414acb8975bed7addfa420\",\"units\":0,\"cluid\":\"\","
            + "\"http_status\":353,\"log_type\":\"api\",\"sleeptime\":null,\"proc_id\":305830,"
            + "\"host\":\"vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net\","
            + "\"error_detail\":\"The remaining balance must not be less than 1 000.00 rub. or should be zero.\","
            + "\"logtime\":\"20181003010204\",\"bid\":0,\"runtime\":0.330499172210693,\"cmd\":\"TransferMoney\","
            + "\"uid\":\"214814993\",\"param\":{\"FromCampaigns\":[{\"Sum\":\"49\",\"CampaignID\":\"6999619\"}],"
            + "\"ToCampaigns\":[{\"Sum\":\"49\",\"CampaignID\":\"16008656\"}]}}";

        checker.check(
            line,
            dateTimeFormat.parse("2018-10-03 01:02:04"),
            new ArrayList<>(Arrays.asList(6999619L, 16008656L)),
            new ArrayList<>(Arrays.asList(0L)),
            "178.33.63.99",
            "TransferMoney",
            0.330499172210693f,
            "{\"FromCampaigns\":[{\"Sum\":\"49\",\"CampaignID\":\"6999619\"}],\"ToCampaigns\":[{\"Sum\":\"49\",\"CampaignID\":\"16008656\"}]}",
            353L,
            new ArrayList<>(),
            3741044692388071810L,
            214814993L,
            "vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net",
            305830L,
            0f,
            "The remaining balance must not be less than 1 000.00 rub. or should be zero.",
            0L,
            "--- []\n",
            (short) 4,
            "soap",
            "f568746515414acb8975bed7addfa420",
            "",
            new ArrayList<>(),
            "file:/tmp/hostname.test/access.log",
            0L,
            0L,
            0L
        );
    }

    @Test
    public void testCampaignIDLog() throws Exception {
        String line = "<134>1 2018-10-03T00:12:40+03:00 vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net "
            + "PPCLOG.ppclog_api.log 162001 - - 2018-10-03 00:12:40 {\"cmd\":\"GetBannersStat\",\"uid\":\"240019127\","
            + "\"bid\":[0, 1],\"runtime\":1.4172248840332,\"param\":{\"OrderBy\":[\"clDate\",\"clBanner\"],"
            + "\"EndDate\":\"2018-10-03\",\"Currency\":\"RUB\",\"CampaignID\":\"23099778\","
            + "\"GroupByColumns\":[\"clDate\",\"clPhrase\",\"clAveragePosition\"],\"StartDate\":\"2018-10-03\","
            + "\"IncludeVAT\":\"No\"},\"error_detail\":\"\",\"logtime\":\"20181003001240\",\"units\":0,"
            + "\"cluid\":\"376357235\",\"http_status\":0,\"application_id\":\"f5ff8f217d0d44b59d6e6e1d335e13f5\","
            + "\"host\":\"vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net\",\"log_type\":\"api\","
            + "\"sleeptime\":null,\"proc_id\":162001,\"reqid\":3737784188604061680,\"ip\":\"185.76.144.253\","
            + "\"interface\":\"json\",\"cid\":[],\"units_stats\":\"--- []\\n\",\"api_version\":104}";

        checker.check(
            line,
            dateTimeFormat.parse("2018-10-03 00:12:40"),
            new ArrayList<>(Arrays.asList(23099778L)),
            new ArrayList<>(Arrays.asList(0L, 1L)),
            "185.76.144.253",
            "GetBannersStat",
            1.4172248840332f,
            "{\"OrderBy\":[\"clDate\",\"clBanner\"],\"EndDate\":\"2018-10-03\",\"Currency\":\"RUB\",\"CampaignID\":\"23099778\",\"GroupByColumns\":[\"clDate\",\"clPhrase\",\"clAveragePosition\"],\"StartDate\":\"2018-10-03\",\"IncludeVAT\":\"No\"}",
            0L,
            new ArrayList<>(Arrays.asList(376357235L)),
            3737784188604061680L,
            240019127L,
            "vla1-5413-vla-ppc-direct-soap-3-17685.gencfg-c.yandex.net",
            162001L,
            0f,
            "",
            0L,
            "--- []\n",
            (short) 104,
            "json",
            "f5ff8f217d0d44b59d6e6e1d335e13f5",
            "",
            new ArrayList<>(),
            "file:/tmp/hostname.test/access.log",
            0L,
            0L,
            0L
        );
    }

    @Test
    public void testJavaLog() throws Exception {
        String line = "2018-10-03 00:01:06 {\"api_version\":5,\"cmd\":\"keywordbids.get\","
            + "\"application_id\":\"3dd9c5d110aa41c1b552063bc7314fcd\",\"reqid\":1966608114396199130,"
            + "\"warning_object_count\":0,\"error_object_count\":0,\"uid\":13371679,\"cluid\":562629613,"
            + "\"http_status\":0,\"error_detail\":null,\"units\":15,\"cid\":[32103746],\"response_ids\":null,"
            + "\"param\":\"{\\\"SelectionCriteria\\\":{\\\"CampaignIds\\\":[32103746],\\\"KeywordIds\\\":[]},"
            + "\\\"FieldNames\\\":[\\\"KeywordId\\\",\\\"AdGroupId\\\",\\\"CampaignId\\\",\\\"ServingStatus\\\","
            + "\\\"StrategyPriority\\\"],\\\"SearchFieldNames\\\":[\\\"Bid\\\",\\\"AuctionBids\\\"],\\\"Page\\\":"
            + "{\\\"Limit\\\":10000,\\\"Offset\\\":0}}\",\"response\":null,\"ip\":\"34.248.149.181\",\"interface\":"
            + "\"json\",\"units_spending_user_client_id\":36707094,\"units_stats\":\"[15,95539,120000]\",\"runtime\":0.863768931,"
            + "\"log_type\":\"api\",\"sleeptime\":0,\"host\":\"vla1-5115-vla-ppc-direct-java-api5-24470.gencfg-c.yandex.net\","
            + "\"proc_id\":0,\"logtime\":\"2018-10-03 00:01:06\"}";

        checker.check(
            line,
            dateTimeFormat.parse("2018-10-03 00:01:06"),
            new ArrayList<>(Arrays.asList(32103746L)),
            new ArrayList<>(),
            "34.248.149.181",
            "keywordbids.get",
            0.863768931f,
            "\"{\\\"SelectionCriteria\\\":{\\\"CampaignIds\\\":[32103746],\\\"KeywordIds\\\":[]},\\\"FieldNames\\\":"
                + "[\\\"KeywordId\\\",\\\"AdGroupId\\\",\\\"CampaignId\\\",\\\"ServingStatus\\\",\\\"StrategyPriority\\\"],\\\"SearchFieldNames\\\":[\\\"Bid\\\",\\\"AuctionBids\\\"],\\\"Page\\\":{\\\"Limit\\\":10000,\\\"Offset\\\":0}}\"",
            0L,
            new ArrayList<>(Arrays.asList(562629613L)),
            1966608114396199130L,
            13371679L,
            "vla1-5115-vla-ppc-direct-java-api5-24470.gencfg-c.yandex.net",
            0L,
            0f,
            "",
            15L,
            "[15,95539,120000]",
            (short) 5,
            "json",
            "3dd9c5d110aa41c1b552063bc7314fcd",
            "",
            new ArrayList<>(),
            "file:/tmp/hostname.test/access.log",
            36707094L,
            0L,
            0L
        );
    }
}
