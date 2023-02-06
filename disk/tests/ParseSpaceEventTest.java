package ru.yandex.chemodan.eventlog.log.tests;

import java.math.BigInteger;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.TestProduct;
import ru.yandex.chemodan.eventlog.events.space.BasicSpaceEvent;
import ru.yandex.chemodan.eventlog.events.space.ProductSpaceEvent;
import ru.yandex.chemodan.eventlog.events.space.SpaceEventType;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class ParseSpaceEventTest extends AbstractParseEventTest {
    private static final String BIG_INTEGER_STR1 = String.valueOf(Long.MAX_VALUE) + "999";

    private static final String BIG_INTEGER_STR2 = String.valueOf(Long.MAX_VALUE) + "000";

    @Test
    public void testEnlarge() {
        assertParseEquals(UID, "space-enlarge",
                "reason=1tb_1y_2015\told_limit=1024\tnew_limit=2048\t" + TestProduct.PAID_1TB_1Y_2015,
                new ProductSpaceEvent(
                        SpaceEventType.ENLARGE,
                        EVENT_METADATA,
                        BigInteger.valueOf(1024),
                        BigInteger.valueOf(2048),
                        TestProduct.PAID_1TB_1Y_2015.product
                )
        );
    }

    @Test
    public void testReduce() {
        assertParseEquals(UID, "space-reduce",
                "reason=yandex_ege\told_limit=2048\tnew_limit=1024\t" + TestProduct.FREE_YANDEX_EGE,
                new ProductSpaceEvent(
                        SpaceEventType.REDUCE,
                        EVENT_METADATA,
                        BigInteger.valueOf(2048),
                        BigInteger.valueOf(1024),
                        TestProduct.FREE_YANDEX_EGE.product
                )
        );
    }

    @Test
    public void testReduceLarge() {
        assertParseEquals(UID, "space-reduce",
                TestProduct.FREE_YANDEX_EGE +
                        "\treason=yandex_ege" +
                        "\told_limit=" + BIG_INTEGER_STR1 +
                        "\tnew_limit=" + BIG_INTEGER_STR2,
                new ProductSpaceEvent(
                        SpaceEventType.REDUCE,
                        EVENT_METADATA,
                        new BigInteger(BIG_INTEGER_STR1),
                        new BigInteger(BIG_INTEGER_STR2),
                        TestProduct.FREE_YANDEX_EGE.product
                )
        );
    }

    @Test
    public void testBasicSpaceEvent() {
        assertParseEquals(UID, "space-enlarge",
                "reason=non_product_reason\told_limit=1024\tnew_limit=2048",
                new BasicSpaceEvent(SpaceEventType.ENLARGE, EVENT_METADATA,
                        BigInteger.valueOf(1024),
                        BigInteger.valueOf(2048),
                        Option.of("non_product_reason"))
        );
    }

    @Test
    public void testForProductWithoutPeriod() {
        assertParseEquals(UID, "space-reduce",
                TestProduct.FREE_PASSPORT_SPLIT +
                        "\treason=passport_split" +
                        "\told_limit=1024" +
                        "\tnew_limit=2048",
                new ProductSpaceEvent(
                        SpaceEventType.REDUCE,
                        EVENT_METADATA,
                        new BigInteger("1024"),
                        new BigInteger("2048"),
                        TestProduct.FREE_PASSPORT_SPLIT.product
                )
        );
    }
}
