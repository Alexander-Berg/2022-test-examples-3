package ru.yandex.market.pvz.tms.executor.sqs.handler;

import java.time.Instant;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.logistics.les.tpl.Courier;
import ru.yandex.market.logistics.les.tpl.Item;
import ru.yandex.market.logistics.les.tpl.TplCourierReceivedItemsForPvzDelivery;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderAdditionalInfoRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderAdditionalInfo;
import ru.yandex.market.pvz.core.domain.order.model.place.OrderPlace;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.tms.executor.sqs.ProcessIncomingSqsEventsExecutor;
import ru.yandex.market.pvz.tms.other.SqsMessageListener;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.LES_DISABLED;
import static ru.yandex.market.pvz.core.domain.sqs.handler.CourierReceivedItemsSqsEventHandler.ORDER_ACCEPTED_BY_COURIER_EVENT;

@TransactionlessEmbeddedDbTest
@Import({SqsMessageListener.class, ProcessIncomingSqsEventsExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CourierReceivedItemsSqsEventHandlerTest {

    private final static String COURIER_ID = RandomStringUtils.randomAlphanumeric(10);

    private final TestOrderFactory orderFactory;
    private final SqsMessageListener sqsMessageListener;
    private final OrderAdditionalInfoRepository orderAdditionalInfoRepository;
    private final ProcessIncomingSqsEventsExecutor processIncomingSqsEventsExecutor;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final ConfigurationProvider configurationProvider;

    @Test
    void whenReceiveCourierMessageThenSaveCourierId() {
        Order order = orderFactory.createOrder();
        List<Entry<String, String>> externalIdPlaceCode = StreamEx.of(order.getPlaces())
                .mapToEntry(place -> place.getOrder().getExternalId(), OrderPlace::getBarcode)
                .toList();

        OrderAdditionalInfo orderAdditionalInfo = orderAdditionalInfoRepository.findByOrderId(order.getId());
        assertThat(orderAdditionalInfo.getCourierId(configurationProvider)).isNull();
        assertThat(orderAdditionalInfo.isAcceptedByCourier(configurationProvider)).isFalse();

        Event event = buildEvent(externalIdPlaceCode);
        sqsMessageListener.processEvent(event);
        processIncomingSqsEventsExecutor.doRealJob(null);

        orderAdditionalInfo = orderAdditionalInfoRepository.findById(order.getId()).get();
        assertThat(orderAdditionalInfo.getCourierId(configurationProvider)).isEqualTo(COURIER_ID);
        assertThat(orderAdditionalInfo.isAcceptedByCourier(configurationProvider)).isTrue();
    }

    @Test
    void whenDidntReceiveCourierMessageButLesDisabledReturnEmptyString() {
        configurationGlobalCommandService.setValue(LES_DISABLED, true);
        Order order = orderFactory.createOrder();
        OrderAdditionalInfo orderAdditionalInfo = orderAdditionalInfoRepository.findByOrderId(order.getId());
        assertThat(orderAdditionalInfo.isAcceptedByCourier(configurationProvider)).isTrue();
        assertThat(orderAdditionalInfo.getCourierId(configurationProvider)).isEmpty();
    }

    private Event buildEvent(List<Entry<String, String>> externalIdPlaceCode) {
        Courier courier = new Courier(COURIER_ID, "Курьер Евдоким");
        Set<Item> items = externalIdPlaceCode.stream()
                .map(idCode -> new Item("", idCode.getKey(), idCode.getValue()))
                .collect(Collectors.toSet());
        EventPayload eventPayload = new TplCourierReceivedItemsForPvzDelivery("pickupPointLmsId", courier, items);
        return new Event("source", "event_id", Instant.now().toEpochMilli(), ORDER_ACCEPTED_BY_COURIER_EVENT,
                eventPayload, "description");
    }
}
