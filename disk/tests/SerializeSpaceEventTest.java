package ru.yandex.chemodan.app.eventloader.serializer.tests;

import java.math.BigInteger;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.TestProduct;
import ru.yandex.chemodan.eventlog.events.space.BasicSpaceEvent;
import ru.yandex.chemodan.eventlog.events.space.ProductSpaceEvent;
import ru.yandex.chemodan.eventlog.events.space.SpaceEventType;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class SerializeSpaceEventTest extends AbstractSerializeEventTest {
    @Test
    public void testPromoEnlarge() {
        new ExpectedJson()
                .withEventType(EventType.SPACE_PROMO_ENLARGE)
                .with("old_limit", "1024")
                .with("new_limit", "2048")
                .withProduct(TestProduct.FREE_TURKEY_PANORAMA.product)
                .serializeAndCheck(
                        new ProductSpaceEvent(
                                SpaceEventType.ENLARGE,
                                METADATA,
                                BigInteger.valueOf(1024),
                                BigInteger.valueOf(2048),
                                TestProduct.FREE_TURKEY_PANORAMA.product
                        )
                );
    }

    @Test
    public void testPromoReduce() {
        new ExpectedJson()
                .withEventType(EventType.SPACE_PROMO_REDUCE)
                .with("old_limit", "2048")
                .with("new_limit", "1024")
                .withProduct(TestProduct.FREE_PASSPORT_SPLIT.product)
                .serializeAndCheck(
                        new ProductSpaceEvent(
                                SpaceEventType.REDUCE,
                                METADATA,
                                BigInteger.valueOf(2048),
                                BigInteger.valueOf(1024),
                                TestProduct.FREE_PASSPORT_SPLIT.product
                        )
                );
    }

    @Test
    public void testProductReduce() {
        new ExpectedJson()
                .withEventType(EventType.SPACE_PRODUCT_REDUCE)
                .with("old_limit", "2048")
                .with("new_limit", "1024")
                .withProduct(TestProduct.PAID_10GB_1M_2014.product)
                .serializeAndCheck(
                        new ProductSpaceEvent(
                                SpaceEventType.REDUCE,
                                METADATA,
                                BigInteger.valueOf(2048),
                                BigInteger.valueOf(1024),
                                TestProduct.PAID_10GB_1M_2014.product
                        )
                );
    }

    @Test
    public void testBasicSpaceEvent() {
        new ExpectedJson()
                .withEventType(EventType.SPACE_PROMO_REDUCE)
                .with("old_limit", "2048")
                .with("new_limit", "1024")
                .withProductId("reason")
                .serializeAndCheck(
                        new BasicSpaceEvent(
                                SpaceEventType.REDUCE,
                                METADATA,
                                BigInteger.valueOf(2048),
                                BigInteger.valueOf(1024),
                                Option.of("reason")
                        )
                );
    }
}
