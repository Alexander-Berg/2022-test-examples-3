package ru.yandex.travel.orders.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.commons.proto.EOrderType;
import ru.yandex.travel.orders.entities.AeroflotOrder;
import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.Order;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState;
import ru.yandex.travel.orders.workflow.order.aeroflot.proto.EAeroflotOrderState;
import ru.yandex.travel.orders.workflow.order.generic.proto.EOrderState;
import ru.yandex.travel.orders.workflow.train.proto.ETrainOrderState;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class AeroflotOrderRepositoryTest {
    @Autowired
    private AeroflotOrderRepository aeroflotOrderRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    public void testFindAllAeroflotOrders() {
        var now = LocalDateTime.now();
        var servicedAt = now.plusSeconds(10);
        var expiresAt = Instant.now();
        var item = createOrder(EOrderType.OT_AVIA_AEROFLOT, EAeroflotOrderState.OS_CONFIRMED, servicedAt, expiresAt);
        createOrder(EOrderType.OT_AVIA_AEROFLOT, EAeroflotOrderState.OS_CANCELLED, servicedAt, expiresAt);
        createOrder(EOrderType.OT_HOTEL_EXPEDIA, EHotelOrderState.OS_CONFIRMED, servicedAt, expiresAt);
        createOrder(EOrderType.OT_TRAIN, ETrainOrderState.OS_CONFIRMED, servicedAt, expiresAt);

        assertThat(aeroflotOrderRepository.findAllActiveOrders(now)).isEqualTo(List.of(item));
    }

    private Order createOrder(EOrderType orderType, Enum<?> state, LocalDateTime servicedAt, Instant expiresAt) {
        Order order;
        switch (orderType) {
            case OT_HOTEL_EXPEDIA:
                order = new HotelOrder();
                ((HotelOrder) order).setState((EHotelOrderState) state);
                break;
            case OT_AVIA_AEROFLOT:
                order = new AeroflotOrder();
                ((AeroflotOrder) order).setState((EAeroflotOrderState) state);
                break;
            case OT_TRAIN:
                order = new TrainOrder();
                ((TrainOrder) order).setState((ETrainOrderState) state);
                break;
            default:
                order = new GenericOrder();
                ((GenericOrder) order).setState((EOrderState) state);
                break;
        }
        order.setId(UUID.randomUUID());
        order.setExpiresAt(expiresAt);
        order.setServicedAt(servicedAt);
        order = orderRepository.saveAndFlush(order);
        return order;
    }
}
