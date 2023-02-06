package ru.yandex.market.logistics.logistics4shops.logbroker.processor;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.google.common.collect.ComparisonChain;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.logbroker.OrderEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.utils.ProtobufAssertionsUtils;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderItemsRemovedPayload;

@DatabaseSetup("/logbroker/orderEvent/prepare.xml")
abstract class AbstractMbiOrderEventProcessorTest extends AbstractIntegrationTest {

    @Autowired
    protected OrderEventMessageHandler orderEventProtoMessageHandler;

    protected void assertEvent(OrderEvent expected, OrderEvent actual) {
        ProtobufAssertionsUtils.prepareProtobufAssertion(softly.assertThat(actual))
            .withComparatorForType(
                (o1, o2) -> ComparisonChain.start()
                    .compare(o1.getOrderLineId(), o2.getOrderLineId())
                    .compare(o1.getRemovedCount(), o2.getRemovedCount())
                    .result(),
                OrderItemsRemovedPayload.ItemsRemovedEntry.class
            )
            .isEqualTo(expected);
    }
}
