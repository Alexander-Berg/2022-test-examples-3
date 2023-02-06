package ru.yandex.market.logistics.logistics4shops.logbroker.lom.processor;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderItemInstance;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderItemInstances;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderItemsInstancesUpdatedPayload;
import ru.yandex.market.logistics.logistics4shops.logbroker.LomEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.utils.LogisticEventUtil;
import ru.yandex.market.logistics.logistics4shops.utils.LomEventFactory;
import ru.yandex.market.logistics.logistics4shops.utils.ProtobufAssertionsUtils;

@DisplayName("Обработка события изменения экземпляров товаров")
@ParametersAreNonnullByDefault
class OrderItemsInstancesUpdatedProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private LomEventMessageHandler lomEventMessageHandler;

    @Autowired
    private LogisticEventUtil logisticEventUtil;

    @Test
    @DisplayName("Успешная обработка события добавления cis")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/instancesupdated/db/after/event_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCisAdded() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/instancesupdated/diff/cis_added.json",
                "logbroker/lom/event/instancesupdated/snapshot/cis_added.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(
            softly.assertThat(eventPayload.getOrderItemsInstancesUpdatedPayload())
        )
            .isEqualTo(defaultOrderItemsInstancesUpdatedPayload(
                OrderItemInstance.newBuilder().setCis("cis").build())
            );
    }

    @Test
    @DisplayName("Успешная обработка события добавления cis и cis_full")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/instancesupdated/db/after/event_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCisAndCisFull() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/instancesupdated/diff/cis_and_cis_full_added.json",
                "logbroker/lom/event/instancesupdated/snapshot/cis_and_cis_full_added.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(
            softly.assertThat(eventPayload.getOrderItemsInstancesUpdatedPayload())
        )
            .isEqualTo(defaultOrderItemsInstancesUpdatedPayload(
                OrderItemInstance.newBuilder().setCis("cis").setCisFull("cis_full").build())
            );
    }

    @Test
    @DisplayName("Успешная обработка события добавления cis_full")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/instancesupdated/db/after/event_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCisFull() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/instancesupdated/diff/cis_full_added.json",
                "logbroker/lom/event/instancesupdated/snapshot/cis_full_added.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(
            softly.assertThat(eventPayload.getOrderItemsInstancesUpdatedPayload())
        )
            .isEqualTo(defaultOrderItemsInstancesUpdatedPayload(
                OrderItemInstance.newBuilder().setCisFull("cis_full").build())
            );
    }

    @Test
    @DisplayName("Успешная обработка события обновления экземпляров - несколько товаров")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/instancesupdated/db/after/event_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successProcessingMultipleItems() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/instancesupdated/diff/cis_full_added.json",
                "logbroker/lom/event/instancesupdated/snapshot/multiple_items_and_instances.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(
            softly.assertThat(eventPayload.getOrderItemsInstancesUpdatedPayload())
        )
            .ignoringCollectionOrder()
            .isEqualTo(defaultOrderItemsInstancesUpdatedPayload(
                List.of(
                    OrderItemInstances.newBuilder()
                        .setSsku("item0")
                        .addAllInstances(List.of(
                            OrderItemInstance.newBuilder().setCis("cis_0_1").build(),
                            OrderItemInstance.newBuilder().setCis("cis_0_2").build()
                        ))
                        .build(),
                    OrderItemInstances.newBuilder()
                        .setSsku("item1")
                        .addAllInstances(List.of(OrderItemInstance.newBuilder().setCisFull("cis_full_1").build()))
                        .build(),
                    OrderItemInstances.newBuilder()
                        .setSsku("item2")
                        .addAllInstances(List.of(
                            OrderItemInstance.newBuilder()
                                .setCis("cis_2_0")
                                .setCisFull("cis_full_2_0")
                                .build(),
                            OrderItemInstance.newBuilder()
                                .setCis("cis_2_1")
                                .setCisFull("cis_full_2_1")
                                .build())
                        )
                        .build()
                    ))
            );
    }

    @Test
    @DisplayName("Обработка события изменения экземпляров - заказ не FaaS")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderIsNotFaaS() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/instancesupdated/diff/cis_full_added.json",
                "logbroker/lom/event/common/snapshot/not_faas_order.json"
            )
        ));
    }

    @Nonnull
    private OrderItemsInstancesUpdatedPayload defaultOrderItemsInstancesUpdatedPayload(OrderItemInstance instance) {
        return defaultOrderItemsInstancesUpdatedPayload(
            List.of(OrderItemInstances.newBuilder().setSsku("item0").addAllInstances(List.of(instance)).build())
        );
    }

    @Nonnull
    private OrderItemsInstancesUpdatedPayload defaultOrderItemsInstancesUpdatedPayload(
        List<OrderItemInstances> instances
    ) {
        return OrderItemsInstancesUpdatedPayload.newBuilder()
            .setOrderId(2L)
            .setShopId(101L)
            .addAllItemInstances(instances)
            .build();
    }
}
