package ru.yandex.market.logshatter.parser.mbi;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

public class TmsTraceLogParserTest {

    LogParserChecker checker = new LogParserChecker(new TmsTraceLogParser());

    @Test
    public void parse() throws Exception {

        String line1 = "tskv\tdate=2020-10-07T00:14:21.262+03:00\ttype=OUT\trequest_id=1602016200670" +
            "/5c3cc2d426c909ed242333b4f9fc27a6/207917\ttarget_module=mbi-delivery-calculator-indexer\ttarget_host" +
            "=delicalc.indexer.mbi.vs.market.yandex.net:30002\tprotocol=http\thttp_method=POST\trequest_method" +
            "=/getShopDeliveryCost\tquery_params=/getShopDeliveryCost\tretry_num=1\ttime_millis=3\thttp_code=200";
        String line2 = "tskv\tdate=2020-10-07T16:17:00.158+03:00\ttype=OUT\trequest_id=1602076620152" +
            "/3dd3268f9a70df97fb7d029a6fed9d5d/1\trequest_method=getOrderInfoExecutor job, group " +
            "DEFAULT\ttime_millis=0";
        String line3 = "tskv\tdate=2020-10-07T16:06:42.824+03:00\ttype=OUT\trequest_id=1602075600196" +
            "/6d4eb6b675433a40d27c84c87ddce33c/5893\ttarget_module=datacamp-stroller\ttarget_host=datacamp.blue.vs" +
            ".market.yandex.net\tprotocol=http\thttp_method=POST\trequest_method=/shops/779527/offers/search" +
            "\tquery_params=/shops/779527/offers/search?original=0\tretry_num=1\ttime_millis=19\thttp_code=200";

        checker.check(
            line1,
            1602018861,
            checker.getHost(), Environment.UNKNOWN,
            "1602016200670/5c3cc2d426c909ed242333b4f9fc27a6/207917", // request id
            "mbi-delivery-calculator-indexer", // target module
            "delicalc.indexer.mbi.vs.market.yandex.net:30002", // target host
            "/getShopDeliveryCost", // request method
            200, // http code
            1, // retry num
            3, // time millis
            "", // error code
            "http", // protocol
            "POST", // http method
            "OUT", // type
            "/getShopDeliveryCost" // query params
        );

        checker.check(
            line2,
            1602076620,
            checker.getHost(), Environment.UNKNOWN,
            "1602076620152/3dd3268f9a70df97fb7d029a6fed9d5d/1", // request id
            "", // target module
            "", // target host
            "getOrderInfoExecutor job, group DEFAULT", // request method
            0, // http code
            0, // retry num
            0, // time millis
            "", // error code
            "", // protocol
            "", // http method
            "OUT", // type
            "" // query params
        );

        checker.check(
            line3,
            1602076002,
            checker.getHost(), Environment.UNKNOWN,
            "1602075600196/6d4eb6b675433a40d27c84c87ddce33c/5893", // request id
            "datacamp-stroller", // target module
            "datacamp.blue.vs.market.yandex.net", // target host
            "/shops/779527/offers/search", // request method
            200, // http code
            1, // retry num
            19, // time millis
            "", // error code
            "http", // protocol
            "POST", // http method
            "OUT", // type
            "/shops/779527/offers/search?original=0" // query params
        );
    }
}
