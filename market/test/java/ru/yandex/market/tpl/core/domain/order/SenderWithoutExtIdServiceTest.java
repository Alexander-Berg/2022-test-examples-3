package ru.yandex.market.tpl.core.domain.order;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.tpl.api.model.locker.LockerExtradition;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.locker.LockerDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SenderWithoutExtIdServiceTest {

    @InjectMocks
    private SenderWithoutExtIdService senderWithoutExtIdService;

    @Mock
    private SenderWithoutExtIdManager senderWithoutExtIdManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldUpdateLockerTask() {
        var externalOrderId = "LO-external-order-id";
        String orderBarcode = "order-barcode";
        String yandexId = "yandex-id";

        OrderPlaceBarcode orderPlaceBarcode = mock(OrderPlaceBarcode.class);
        when(orderPlaceBarcode.getBarcode()).thenReturn(orderBarcode);

        OrderPlace orderPlace = mock(OrderPlace.class);
        when(orderPlace.getBarcode()).thenReturn(orderPlaceBarcode);

        var order = mock(Order.class);
        when(order.getExternalOrderId()).thenReturn(externalOrderId);
        when(order.getPlaces()).thenReturn(Set.of(orderPlace));
        when(order.getSender()).thenReturn(OrderSender.builder().yandexId(yandexId).build());

        var ordersMap = new HashMap<String, Order>();
        ordersMap.put(externalOrderId, order);

        var lockerDto = new LockerDto();
        lockerDto.setPartnerSubType(PartnerSubType.LOCKER);

        OrderDto orderDto = new OrderDto();
        orderDto.setExternalOrderId(externalOrderId);

        LockerExtradition lockerExtradition = new LockerExtradition();
        lockerExtradition.setExternalOrderId(externalOrderId);

        var lockerDeliveryTaskDto = new LockerDeliveryTaskDto();
        lockerDeliveryTaskDto.setLocker(lockerDto);
        lockerDeliveryTaskDto.setOrders(List.of(orderDto));
        lockerDeliveryTaskDto.setExtraditionOrders(List.of(lockerExtradition));

        when(senderWithoutExtIdManager.existsByYandexId(yandexId)).thenReturn(true);

        senderWithoutExtIdService.updateLockerTask(ordersMap, lockerDeliveryTaskDto);
        assertEquals(orderBarcode, lockerDeliveryTaskDto.getOrders().get(0).getExternalOrderId());
        assertEquals(orderBarcode, lockerDeliveryTaskDto.getExtraditionOrders().get(0).getExternalOrderId());
    }
}
