package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static java.util.Collections.singletonList;

public class PpcPriceLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new PpcPriceLogParser());
    private SimpleDateFormat dateFormat = new SimpleDateFormat(PpcPriceLogParser.DATE_PATTERN);

    @Test
    public void testParse() throws Exception {
        String jsonLine =
            "{'log_time':'2018-09-19 00:00:01',"
                + "'uid':13371679,"
                + "'method':'bids.set',"
                + "'service':'direct.api5',"
                + "'ip':'34.248.149.181',"
                + "'reqid':3980559843943465881,"
                + "'log_hostname':'vla1-5112-vla-ppc-direct-java-api5-24470.gencfg-c.yandex.net',"
                + "'log_type':'ppclog_price'"
                + ",'data':["
                + "{'cid':30800530,"
                + "'pid':2994864176,"
                + "'bid':0,"
                + "'id':11330566665,"
                + "'price_ctx':0.3,"
                + "'price':7.6,"
                + "'type':'update',"
                + "'currency':'RUB'}]"
                + "}";

        Date date = dateFormat.parse("2018-09-19 00:00:01");
        Object[] ppcPriceLog = new Object[]{
            "direct.api5",
            "bids.set",
            3980559843943465881L,
            13371679,
            "vla1-5112-vla-ppc-direct-java-api5-24470.gencfg-c.yandex.net",
            "34.248.149.181",
            30800530L,
            2994864176L,
            0L,
            11330566665L,
            "update",
            "RUB",
            7.6F,
            0.3F
        };

        checker.check(jsonLine,
            singletonList(date),
            singletonList(ppcPriceLog)
        );
    }

    @Test
    public void testParse2() throws Exception {
        String jsonLine =
            "{'log_time':'2018-09-19 00:00:01',"
                + "'uid':13371679,"
                + "'method':'bids.set',"
                + "'service':'direct.api5',"
                + "'ip':null,"
                + "'reqid':3980559843943465881,"
                + "'log_hostname':'vla1-5112-vla-ppc-direct-java-api5-24470.gencfg-c.yandex.net',"
                + "'log_type':'ppclog_price'"
                + ",'data':["
                + "{'cid':30800530,"
                + "'pid':2994864176,"
                + "'bid':0,"
                + "'id':11330566665,"
                + "'price_ctx':null,"
                + "'price':null,"
                + "'type':'update',"
                + "'currency':'RUB'}]"
                + "}";

        Date date = dateFormat.parse("2018-09-19 00:00:01");
        Object[] ppcPriceLog = new Object[]{
            "direct.api5",
            "bids.set",
            3980559843943465881L,
            13371679,
            "vla1-5112-vla-ppc-direct-java-api5-24470.gencfg-c.yandex.net",
            "",
            30800530L,
            2994864176L,
            0L,
            11330566665L,
            "update",
            "RUB",
            0.0F,
            0.0F
        };

        checker.check(jsonLine,
            singletonList(date),
            singletonList(ppcPriceLog)
        );
    }

    @Test
    public void testParse3() throws Exception {
        String jsonLine =
            "{'log_time':'2019-01-24 15:59:59'," +
                "'uid':551152748," +
                "'method':'bids.set'," +
                "'service':'direct.api5'," +
                "'ip':'18.202.79.101'," +
                "'reqid':4609729875179891493," +
                "'log_hostname':'sas2-0231-sas-ppc-direct-java-api5-21909.gencfg-c.yandex.net'," +
                "'log_type':'ppclog_price'," +
                "'data':[{'cid':39522205,'pid':3606867442,'bid':0,'id':15229459939,'price_ctx':0.0,'price':37.0,'type':'update'," +
                "'currency':'RUB'}]}";

        Date date = dateFormat.parse("2019-01-24 15:59:59");
        Object[] ppcPriceLog = new Object[]{
            "direct.api5",
            "bids.set",
            4609729875179891493L,
            551152748,
            "sas2-0231-sas-ppc-direct-java-api5-21909.gencfg-c.yandex.net",
            "18.202.79.101",
            39522205L,
            3606867442L,
            0L,
            15229459939L,
            "update",
            "RUB",
            37.0F,
            0.0F
        };

        checker.check(jsonLine,
            singletonList(date),
            singletonList(ppcPriceLog)
        );
    }

    @Test
    public void multiLineParsetest() throws Exception {
        String jsonLine =
            "{'log_time':'2018-09-19 00:00:01',"
                + "'uid':13371679,"
                + "'method':'bids.set',"
                + "'service':'direct.api5',"
                + "'ip':'34.248.149.181',"
                + "'reqid':3980559843943465881,"
                + "'log_hostname':'vla1-5112-vla-ppc-direct-java-api5-24470.gencfg-c.yandex.net',"
                + "'log_type':'ppclog_price'"
                + ",'data':["
                + "{'cid':30800530,"
                + "'pid':2994864176,"
                + "'bid':0,"
                + "'id':11330566665,"
                + "'price_ctx':0.3,"
                + "'price':7.6,"
                + "'type':'update',"
                + "'currency':'RUB'"
                + "},"
                + "{'cid':30800530,"
                + "'pid':3348439226,"
                + "'bid':0,"
                + "'id':13386465237,"
                + "'price_ctx':0.0,"
                + "'price':7.4,"
                + "'type':'update',"
                + "'currency':"
                + "'RUB'"
                + "}]}";

        Date date = dateFormat.parse("2018-09-19 00:00:01");

        Object[] ppcPriceLog1 = new Object[]{
            "direct.api5",
            "bids.set",
            3980559843943465881L,
            13371679,
            "vla1-5112-vla-ppc-direct-java-api5-24470.gencfg-c.yandex.net",
            "34.248.149.181",
            30800530L,
            2994864176L,
            0L,
            11330566665L,
            "update",
            "RUB",
            7.6F,
            0.3F
        };

        Object[] ppcPriceLog2 = new Object[]{
            "direct.api5",
            "bids.set",
            3980559843943465881L,
            13371679,
            "vla1-5112-vla-ppc-direct-java-api5-24470.gencfg-c.yandex.net",
            "34.248.149.181",
            30800530L,
            3348439226L,
            0L,
            13386465237L,
            "update",
            "RUB",
            7.4F,
            0.0F
        };

        checker.check(jsonLine,
            Arrays.asList(date, date),
            Arrays.asList(ppcPriceLog1, ppcPriceLog2)
        );
    }
}
