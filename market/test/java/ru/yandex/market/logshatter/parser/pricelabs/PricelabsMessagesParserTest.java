package ru.yandex.market.logshatter.parser.pricelabs;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.trace.Environment;
import ru.yandex.market.logshatter.parser.trace.RequestType;

public class PricelabsMessagesParserTest {
    private static final String CONF_PATH = "market/infra/market-health/config-cs-logshatter/src/conf.d/";

    @Test
    public void testApi2Errors() throws Exception {
        final String line = "tskv\tdate=2020-02-25T16:38:36.605+03:00\t" +
            "request_id=1582637815954/e7da6409597dfc3955bbd292278458a6\tthread=qtp1801106162-20\t" +
            "class=ru.yandex.Sample\t" +
            "message=Exception when processing request\terror_class=java.util.concurrent.CompletionException\t" +
            "error_message=ru.yandex.market.pricelabs.misc.PricelabsHttpExecutionException: " +
            "pricelabs-tms2.tst.vs.market.yandex.net: POST /api/v1/shared/scheduleOffers, " +
            "response=1582637815954/e7da6409597dfc3955bbd292278458a6\t" +
            "cause_error_message=<html><body><h1>504 Gateway Time-out</h1>\\nThe server didn't respond in time." +
            "\\n</body></html>\\n";
        final LogParserChecker checker = checker("pricelabs_api2_errors.json");
        checker.check(line,
            new Date(1582637916000L),
            "1582637815954/e7da6409597dfc3955bbd292278458a6",
            -1L,
            -1L,
            -1,
            "qtp1801106162-20",
            "ru.yandex.Sample",
            "Exception when processing request",
            "java.util.concurrent.CompletionException",
            "ru.yandex.market.pricelabs.misc.PricelabsHttpExecutionException: " +
                "pricelabs-tms2.tst.vs.market.yandex.net: POST /api/v1/shared/scheduleOffers, " +
                "response=1582637815954/e7da6409597dfc3955bbd292278458a6",
            "<html><body><h1>504 Gateway Time-out</h1>\\nThe server didn't respond in time.\\n</body></html>\\n",
            "hostname.test",
            "UNKNOWN");
    }

    @Test
    public void testApi2HttpTms() throws Exception {
        final String line = "tskv\tdate=2020-02-21T15:10:27.339+03:00\ttype=OUT\t" +
            "request_id=1582287026978/ddbf2dfd5be850c6d75b03f503170b96\tsource_module=market_pricelabs_api2\t" +
            "\ttarget_host=pricelabs-tms2.tst.vs.market.yandex.net\thttp_method=POST\t" +
            "request_method=/api/v1/shared/notifyChange\t" +
            "query_params=entity=SHOP&entityIdList=10731&shard=shard2\t" +
            "time_millis=350\thttp_code=200\tkv.activeTime=349\tkv.waitingTime=0\tkv.inputRows=0\t" +
            "kv.outputRows=1\tkv.responseId=1582637815954/e7da6409597dfc3955bbd292278458a6\tresponse_size_bytes=14";
        final LogParserChecker checker = checker("pricelabs_api2_http_tms.json");
        checker.check(line,
            new Date(1582287026000L),
            1582287026978L,
            "ddbf2dfd5be850c6d75b03f503170b96",
            new Integer[0],
            1582287026989L,
            1582287027339L,
            350,
            RequestType.OUT,
            "",
            "hostname.test",
            "market_pricelabs_api2",
            "hostname.test",
            "",
            "pricelabs-tms2.tst.vs.market.yandex.net",
            Environment.UNKNOWN,
            "/api/v1/shared/notifyChange",
            200,
            1,
            "",
            "",
            "POST",
            "entity=SHOP&entityIdList=10731&shard=shard2",
            "",
            "",
            new Object[0],
            new Object[0],
            new Object[0],
            new Object[0],
            new Integer[0],
            "",
            "",
            14,
            0L,
            0L,
            0,
            349L,
            0L,
            0,
            1,
            "1582637815954/e7da6409597dfc3955bbd292278458a6",
            (byte) 0);
    }


    @Test
    public void testTms2Errors() throws Exception {
        final String line = "tskv\tdate=2020-02-25T19:32:10.878+03:00\ttask_id=7350767\tjob_id=3268\tjob_type=5" +
            "\tthread=Processor-14\tmessage=Error when executing task: Service Unavailable\\r\\n" +
            "\terror_class=ru.yandex.misc.thread" +
            ".ExecutionRuntimeException\terror_message=ru.yandex.market.pricelabs.misc" +
            ".PricelabsHttpExecutionException: report.tst.vs.market.yandex.net: GET /yandsearch, " +
            "response=1582648330804/f36dae8ab7bfe3db5280e43bc428aa7a\tcause_error_message=Service " +
            "Unavailable\\r\\n";
        final LogParserChecker checker = checker("pricelabs_tms2_errors.json");
        checker.check(line,
            new Date(1582648330000L),
            "",
            7350767L,
            3268L,
            5,
            "Processor-14", "",
            "Error when executing task: Service Unavailable\\r\\n",
            "ru.yandex.misc.thread.ExecutionRuntimeException",
            "ru.yandex.market.pricelabs.misc.PricelabsHttpExecutionException: report.tst.vs.market.yandex.net: " +
                "GET /yandsearch, response=1582648330804/f36dae8ab7bfe3db5280e43bc428aa7a",
            "Service Unavailable\\r\\n",
            "hostname.test",
            "UNKNOWN");
    }

    @Test
    public void testTms2SetPartnerApiBids() throws Exception {
        final String line = "tskv\tdate=2020-02-25T21:14:24.113+03:00\ttask_id=7381520\tjob_id=3268\tjob_type=5" +
            "\top=set\tshop_id=931\t" +
            "feed_id=1726\toffer_id=728\tbid=0.18\tis_dont_up_to_min=false";
        final LogParserChecker checker = checker("pricelabs_tms2_set_partner_api.json");
        checker.check(line,
            new Date(1582654464000L),
            7381520L,
            3268L,
            5,
            "set",
            931,
            1726,
            "728",
            0.18d,
            "false",
            -1.d,
            "",

            "", // error
            "", // strategy_type
            "", // is_strategy_reach
            -1, // model_id
            -1L, // app_strategy_id
            -1.0, // min_bid
            -1, // current_pos_all
            -1, // ms_model_count

            -1.0, // ms_bid_1..12
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,

            -1.0, // old_bid
            "", // old_is_dont_up_to_min
            -1L, // old_app_strategy_id
            -1, // old_is_hide_ttl_hours
            "", // old_is_hide_on_market
            "", // old_strategy_type,
            "", // old_is_strategy_reach

            -1,
            "",
            "hostname.test",
            "UNKNOWN",
            -1,
            "",
            -1.0);
    }

    @Test
    public void testTms2SetPartnerApiBids2() throws Exception {
        final String line = "tskv\tdate=2020-03-12T16:05:25.419+03:00\top=set\tshop_id=10268904\tfeed_id=200353610" +
            "\toffer_id=139293\tbid=1.0\tis_dont_up_to_min=false\terror=OFFER_NOT_FOUND\tstrategy_type=MAIN" +
            "\tis_strategy_reach=true\tmodel_id=1780891895\tmin_bid=0.0\tapp_strategy_id=100000023301\tms_bid_1=0" +
            ".0\tms_bid_2=0.0\tms_bid_3=0.0\tms_bid_4=0.0\tms_bid_5=0.0\tms_bid_6=0.0\tms_bid_7=0.0\tms_bid_8=0" +
            ".0\tms_bid_9=0.0\tms_bid_10=0.0\tms_bid_11=0.0\tms_bid_12=0" +
            ".0\told_bid=0.0\told_is_dont_up_to_min=false\told_app_strategy_id=100000023301" +
            "\told_is_hide_ttl_hours=0" +
            "\told_is_hide_on_market=false\told_strategy_type=MAIN\told_is_strategy_reach=false" +
            "\tmin_price_feed_id=2\tmin_price_offer_id=123\tmin_price_bid=4.3";

        final LogParserChecker checker = checker("pricelabs_tms2_set_partner_api.json");
        checker.check(line,
            new Date(1584018325000L),
            -1L,
            -1L,
            -1,
            "set",
            10268904,
            200353610,
            "139293",
            1.0d,
            "false",
            -1.d,
            "",


            "OFFER_NOT_FOUND", // error
            "MAIN", // strategy_type
            "true", // is_strategy_reach
            1780891895, // model_id
            100000023301L, // app_strategy_id
            0.0, // min_bid
            -1, // current_pos_all
            -1, // ms_model_count

            0.0, // ms_bid_1..12
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,

            0.0, // old_bid
            "false", // old_is_dont_up_to_min
            100000023301L, // old_app_strategy_id
            0, // old_is_hide_ttl_hours
            "false", // old_is_hide_on_market
            "MAIN", // old_strategy_type,
            "false", // old_is_strategy_reach

            -1,
            "",
            "hostname.test",
            "UNKNOWN",
            2,
            "123",
            4.3);
    }

    @Test
    public void testTms2SetPartnerApiBids3() throws Exception {
        final String line = "tskv\tdate=2020-03-12T16:05:25.419+03:00\top=set\tshop_id=10268904\tfeed_id=200353610" +
            "\toffer_id=101087\tbid=0.99\tis_dont_up_to_min=true\t" +
            "\tstrategy_type=MAIN\tis_strategy_reach=false\tmodel_id=13909305\tmin_bid=0.98" +
            "\tapp_strategy_id=100000024401\tms_bid_1=0.98\tms_bid_2=0.98\tms_bid_3=0.98\tms_bid_4=0.98\tms_bid_5=0" +
            ".98\tms_bid_6=0.98\tms_bid_7=0.98\tms_bid_8=0.98\tms_bid_9=0.98\tms_bid_10=0.98\tms_bid_11=0" +
            ".0\tms_bid_12=0.0\told_bid=0.99\told_is_dont_up_to_min=true\told_app_strategy_id=100000024401" +
            "\told_is_hide_ttl_hours=0" +
            "\told_is_hide_on_market=false\tactual_bid=0" +
            ".94\tactual_is_dont_up_to_min=false\tcurrent_pos_all=4\tms_model_count=2";

        final LogParserChecker checker = checker("pricelabs_tms2_set_partner_api.json");
        checker.check(line,
            new Date(1584018325000L),
            -1L,
            -1L,
            -1,
            "set",
            10268904,
            200353610,
            "101087",
            0.99d,
            "true",
            0.94d,
            "false",

            "", // error
            "MAIN", // strategy_type
            "false", // is_strategy_reach
            13909305, // model_id
            100000024401L, // app_strategy_id
            0.98, // min_bid
            4, // current_pos_all
            2, // ms_model_count

            0.98, // ms_bid_1..12
            0.98,
            0.98,
            0.98,
            0.98,
            0.98,
            0.98,
            0.98,
            0.98,
            0.98,
            0.0,
            0.0,

            0.99d, // old_bid
            "true", // old_is_dont_up_to_min
            100000024401L, // old_app_strategy_id
            0, // old_is_hide_ttl_hours
            "false", // old_is_hide_on_market
            "", // old_strategy_type,
            "", // old_is_strategy_reach

            -1,
            "",
            "hostname.test",
            "UNKNOWN",
            -1,
            "",
            -1.0);
    }

    @Test
    public void testTms2SetPartnerApiHidings() throws Exception {
        final String line = "tskv\tdate=2020-02-25T21:14:24.113+03:00\ttask_id=7381520\tjob_id=3268\tjob_type=5" +
            "\top=hide\tshop_id=931\t" +
            "feed_id=1726\toffer_id=728\tis_hide_ttl_hours=72\tcomment=pricelabs";
        final LogParserChecker checker = checker("pricelabs_tms2_set_partner_api.json");
        checker.check(line,
            new Date(1582654464000L),
            7381520L,
            3268L,
            5,
            "hide",
            931,
            1726,
            "728",
            -1.0d,
            "",
            -1.0d,
            "",

            "", // error
            "", // strategy_type
            "", // is_strategy_reach
            -1, // model_id
            -1L, // app_strategy_id
            -1.0, // min_bid
            -1, // current_pos_all
            -1, // ms_model_count

            -1.0, // ms_bid_1..12
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1.0,

            -1.0, // old_bid
            "", // old_is_dont_up_to_min
            -1L, // old_app_strategy_id
            -1, // old_is_hide_ttl_hours
            "", // old_is_hide_on_market
            "", // old_strategy_type,
            "", // old_is_strategy_reach

            72,
            "pricelabs",
            "hostname.test",
            "UNKNOWN",
            -1,
            "",
            -1.0);
    }

    @Test
    public void testTms2LogbrokerPurchasePriceIn() throws Exception {
        final String line = "tskv\tdate=2020-02-25T21:14:24.113+03:00\tmsg_url=\\\"https://karkam16.ru/test/price" +
            ".csv\\\"\tmsg_lastaccess=1589490258\tmsg_zoractx=\\\"\\\\t\\\\002&\\\\177\\\\345\\\\251\\\\037P@\\\\030" +
            "\\\\200\\\\200\\\\200\\\\0048\\\\317\\\\344\\\\366\\\\365\\\\005X\\\\360\\\\235\\\\253\\\\365\\\\005" +
            "\\\\200\\\\001\\\\360\\\\235\\\\253\\\\365\\\\005\\\\210\\\\001\\\\002\\\\230\\\\001\\\\214\\\\236" +
            "\\\\253\\\\365\\\\005\\\\270\\\\001\\\\215\\\\001\\\\212\\\\002\\\\002\\\\bL\\\\212\\\\002\\\\002\\\\b" +
            "\\\\035\\\\312\\\\002\\\\021336806@production\\\\362\\\\002&\\\\n\\\\021336806@production" +
            "\\\\\\\"\\\\021336806@production\\\"\tmsg_httpcode=206\tmsg_mimetype=0\tmsg_mdskeys=\\\"2050452" +
            "/WmivkGciOL41HoavFPpoSpEXPHVhqGrX|\\\"\tmsg_numberofparts=1\tmsg_zorastatus=1\tmsg_fetchstatus=0" +
            "\tmsg_context=\\\"336806@production\\\"\tmsg_crc32=1119966922\tmsg_prodcontext=\\\"336806@production" +
            "\\\"\tstatus=OK\tinfo=\tprices_count=1577";
        final LogParserChecker checker = checker("pricelabs_tms2_logbroker_purchase_price_in.json");
        checker.check(line,
            new Date(1582654464000L),
            "\\\"https://karkam16.ru/test/price.csv\\\"",
            "",
            1589490258,
            "",
            "\\\"\\\\t\\\\002&\\\\177\\\\345\\\\251\\\\037P@\\\\030\\\\200\\\\200\\\\200\\\\0048\\\\317\\\\344\\\\366" +
                "\\\\365\\\\005X\\\\360\\\\235\\\\253\\\\365\\\\005\\\\200\\\\001\\\\360\\\\235\\\\253\\\\365\\\\005" +
                "\\\\210\\\\001\\\\002\\\\230\\\\001\\\\214\\\\236\\\\253\\\\365\\\\005\\\\270\\\\001\\\\215\\\\001" +
                "\\\\212\\\\002\\\\002\\\\bL\\\\212\\\\002\\\\002\\\\b\\\\035\\\\312\\\\002\\\\021336806@production" +
                "\\\\362\\\\002&\\\\n\\\\021336806@production\\\\\\\"\\\\021336806@production\\\"",
            206,
            0,
            "\\\"2050452/WmivkGciOL41HoavFPpoSpEXPHVhqGrX|\\\"",
            1,
            "",
            1,
            0,
            "\\\"336806@production\\\"",
            1119966922,
            "\\\"336806@production\\\"",
            "",
            "",
            "OK",
            "",
            1577,
            "hostname.test",
            "UNKNOWN");
    }

    static LogParserChecker checker(String configFile) throws IOException, ConfigValidationException,
        ConfigurationException {
        final JsonObject configObject = ConfigurationService.getConfigObject(FileUtils.readFileToString(
            new File(Paths.getSourcePath(CONF_PATH + configFile)))
        );
        final LogParserProvider parserProvider = ConfigurationService.getParserProvider(configObject);
        Map<String, String> params = ConfigurationService.readParams(configObject);
        return new LogParserChecker(parserProvider.createParser(params));
    }

}
