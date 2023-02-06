package ru.yandex.market.logshatter.parser.antifraud.orders;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.EnvironmentMapper;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 04.10.2019
 */
public class CheckouterAntifraudLogParserTest {
    private LogParserChecker checker = new LogParserChecker(new CheckouterAntifraudLogParser());

    {
        checker.setParam(EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX + checker.getOrigin(), "DEVELOPMENT");
    }

    @Test
    public void testRecordWithAllFields() throws Exception {
        checker.check(
            "tskv\tdatetime=[2019-10-04T00:07:59 +0300]\ttimestamp=1570136879\t" +
                "antifraudActions=[ORDER_ITEM_CHANGE]\tdetectorName=AntifraudItemLimitRuleDetector\torderId=null\t" +
                "buyer=[uid=730911013, uuid=123456678901235467890, email=test@test.com, phone=9123456789]\t" +
                "delivery=[outletId=123, outletCode=outletCode123]\t" +
                "items=" +
                "[item_id=null, feed_id=475690, offer_id=510785.ПДО240М, ssku=ПДО240М, " +
                "msku=100716268035, price=2250]x1 " +
                "[item_id=null, feed_id=475690, offer_id=610541.01-1-730, ssku=01-1-730, " +
                "msku=100749709804, price=69]x1 " +
                "[item_id=null, feed_id=475690, offer_id=538217.A/KCKS, ssku=A/KCKS, " +
                "msku=100470358899, price=445]x1 " +
                "[item_id=null, feed_id=475690, offer_id=610541.01-1-725, ssku=01-1-725, " +
                "msku=100749709799, price=52]x1 " +
                "[item_id=null, feed_id=475690, offer_id=493303.000149.18976, ssku=000149.18976, " +
                "msku=100605814884, price=346]x1 " +
                "[item_id=null, feed_id=475690, offer_id=493303.000333.993115, ssku=000333.993115, " +
                "msku=100614911072, price=1139]x1\treason=Reason",
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse("2019-10-04T00:07:59 +0300"),
            checker.getHost(),
            Environment.DEVELOPMENT,
            new String[]{"ORDER_ITEM_CHANGE"},
            "AntifraudItemLimitRuleDetector",
            -1L,
            730911013L,
            "123456678901235467890",
            "test@test.com",
            "9123456789",
            123L,
            "outletCode123",
//            new String[]{
//                "item_id=null@@feed_id=475690@@offer_id=510785.ПДО240М@@ssku=ПДО240М@@msku=100716268035@@" +
//                    "price=2250@@count=1",
//                "item_id=null@@feed_id=475690@@offer_id=610541.01-1-730@@ssku=01-1-730@@msku=100749709804@@" +
//                    "price=69@@count=1",
//                "item_id=null@@feed_id=475690@@offer_id=538217.A/KCKS@@ssku=A/KCKS@@msku=100470358899@@" +
//                    "price=445@@count=1",
//                "item_id=null@@feed_id=475690@@offer_id=610541.01-1-725@@ssku=01-1-725@@msku=100749709799@@" +
//                    "price=52@@count=1",
//                "item_id=null@@feed_id=475690@@offer_id=493303.000149.18976@@ssku=000149.18976@@msku=100605814884@@" +
//                    "price=346@@count=1",
//                "item_id=null@@feed_id=475690@@offer_id=493303.000333.993115@@ssku=000333
//                .993115@@msku=100614911072@@" +
//                    "price=1139@@count=1"}
            new String[0],
            "[item_id=null, feed_id=475690, offer_id=510785.ПДО240М, ssku=ПДО240М, " +
                "msku=100716268035, price=2250]x1 " +
                "[item_id=null, feed_id=475690, offer_id=610541.01-1-730, ssku=01-1-730, " +
                "msku=100749709804, price=69]x1 " +
                "[item_id=null, feed_id=475690, offer_id=538217.A/KCKS, ssku=A/KCKS, " +
                "msku=100470358899, price=445]x1 " +
                "[item_id=null, feed_id=475690, offer_id=610541.01-1-725, ssku=01-1-725, " +
                "msku=100749709799, price=52]x1 " +
                "[item_id=null, feed_id=475690, offer_id=493303.000149.18976, ssku=000149.18976, " +
                "msku=100605814884, price=346]x1 " +
                "[item_id=null, feed_id=475690, offer_id=493303.000333.993115, ssku=000333.993115, " +
                "msku=100614911072, price=1139]x1",
            "Reason"
        );
    }

    @Test
    public void testRecordWithOutOptionalFields() throws Exception {
        checker.check("tskv\tdatetime=[2019-10-04T00:07:47 +0300]\ttimestamp=1570136867\t" +
                "antifraudActions=[ORDER_ITEM_CHANGE]\tdetectorName=AntifraudItemLimitRuleDetector\torderId=null\t" +
                "buyer=[uid=579902430, uuid=null, email=null, phone=null]\t" +
                "delivery=[outletId=null, outletCode=null]\t" +
                "items=[item_id=null, feed_id=475690, offer_id=537372.000235.T039439, ssku=000235.T039439, " +
                "msku=100235865655, price=24714]x1\treason=null",
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse("2019-10-04T00:07:47 +0300"),
            checker.getHost(),
            Environment.DEVELOPMENT,
            new String[]{"ORDER_ITEM_CHANGE"},
            "AntifraudItemLimitRuleDetector",
            -1L,
            579902430L,
            "",
            "",
            "",
            -1L,
            "",
//            new String[]{"item_id=null@@feed_id=475690@@offer_id=537372.000235.T039439@@ssku=000235.T039439@@" +
//                "msku=100235865655@@price=24714@@count=1"}
            new String[0],
            "[item_id=null, feed_id=475690, offer_id=537372.000235.T039439, ssku=000235.T039439, " +
                "msku=100235865655, price=24714]x1",
            ""
        );
    }
}
