package ru.yandex.market.pvz.tms.executor.sqs.handler;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.les.CourierOrderEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderAdditionalInfoRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderAdditionalInfo;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.tms.executor.sqs.ProcessIncomingSqsEventsExecutor;
import ru.yandex.market.pvz.tms.other.SqsMessageListener;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.LES_DISABLED;
import static ru.yandex.market.pvz.core.domain.sqs.handler.OrderStatusChangedSqsEventHandler.ORDER_ACCEPTED_BY_COURIER_EVENT;

@Deprecated(forRemoval = true)
@TransactionlessEmbeddedDbTest
@Import({SqsMessageListener.class, ProcessIncomingSqsEventsExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderStatusChangedSqsEventHandlerTest {

    private final TestOrderFactory orderFactory;
    private final SqsMessageListener sqsMessageListener;
    private final OrderAdditionalInfoRepository orderAdditionalInfoRepository;
    private final ProcessIncomingSqsEventsExecutor processIncomingSqsEventsExecutor;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final ConfigurationProvider configurationProvider;

    @Test
    void whenOrderAdditionalInfoExist() {
        Order order = orderFactory.createOrder();
        orderFactory.setOrderAcceptedByCourier(order.getExternalId(), false);
        OrderAdditionalInfo orderAdditionalInfo = orderAdditionalInfoRepository.findByOrderId(order.getId());
        assertThat(orderAdditionalInfo.isAcceptedByCourier(configurationProvider)).isFalse();

        Event event = buildEvent(order.getExternalId());
        sqsMessageListener.processEvent(event);
        processIncomingSqsEventsExecutor.doRealJob(null);

        orderAdditionalInfo = orderAdditionalInfoRepository.findById(orderAdditionalInfo.getId()).get();
        assertThat(orderAdditionalInfo.isAcceptedByCourier(configurationProvider)).isTrue();
    }

    private Event buildEvent(String externalId) {
        EventPayload eventPayload = new CourierOrderEvent(
                externalId, "origin", "requestId", true, null
        );
        return new Event(
                "source", "event_id", 123456789L, ORDER_ACCEPTED_BY_COURIER_EVENT, eventPayload, "description"
        );
    }

    @Test
    void whenOrderAdditionalInfoNotExistButLesDisabled() {
        configurationGlobalCommandService.setValue(LES_DISABLED, true);
        Order order = orderFactory.createOrder();
        orderFactory.setOrderAcceptedByCourier(order.getExternalId(), false);
        OrderAdditionalInfo orderAdditionalInfo = orderAdditionalInfoRepository.findByOrderId(order.getId());
        assertThat(orderAdditionalInfo.isAcceptedByCourier(configurationProvider)).isTrue();
    }
}
