package ru.yandex.travel.orders.management.metrics;


import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.WellKnownWorkflowEntityType;
import ru.yandex.travel.orders.repository.GenericOrderRepository;
import ru.yandex.travel.orders.repository.HotelOrderRepository;
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState;
import ru.yandex.travel.orders.workflow.order.generic.proto.EOrderState;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "metrics.enabled=true",
                "metrics.scheduler-enabled=false",
        }
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class GaugeServiceTest {
    @Autowired
    private GaugeService gaugeService;
    @Autowired
    private GenericOrderRepository genericOrderRepository;
    @Autowired
    private HotelOrderRepository hotelOrderRepository;

    @Test
    @Transactional
    public void testGetUpdateOrdersMap() {
        var gaugeMap = gaugeService.getUpdateOrdersMap("orders");
        assertThat(gaugeMap.size()).isEqualTo(0);

        for (var i = 0; i < 3; i++) {
            createOrder(EOrderState.OS_CONFIRMED, EDisplayOrderType.DT_SUBURBAN);
            createOrder(EOrderState.OS_CONFIRMED, EDisplayOrderType.DT_SUBURBAN);
            createOrder(EOrderState.OS_CONFIRMED, EDisplayOrderType.DT_TRAIN);
        }
        for (var i = 0; i < 5; i++) {
            createOrder(EHotelOrderState.OS_CONFIRMED);
            createOrder(EHotelOrderState.OS_CANCELLED);
        }

        gaugeMap = gaugeService.getUpdateOrdersMap("orders");
        assertThat(gaugeMap.size()).isEqualTo(4);
        assertThat(gaugeMap.get(createGenericKey(EOrderState.OS_CONFIRMED, EDisplayOrderType.DT_SUBURBAN))).isEqualTo(6);
        assertThat(gaugeMap.get(createGenericKey(EOrderState.OS_CONFIRMED, EDisplayOrderType.DT_TRAIN))).isEqualTo(3);
        assertThat(gaugeMap.get(createHotelKey(EHotelOrderState.OS_CONFIRMED))).isEqualTo(5);
        assertThat(gaugeMap.get(createHotelKey(EHotelOrderState.OS_CANCELLED))).isEqualTo(5);

        for (var i = 0; i < 3; i++) {
            createOrder(EOrderState.OS_RESERVED, EDisplayOrderType.DT_SUBURBAN);
            createOrder(EOrderState.OS_CANCELLED, EDisplayOrderType.DT_SUBURBAN);
            createOrder(EOrderState.OS_CONFIRMED, EDisplayOrderType.DT_TRAIN);
        }

        gaugeMap = gaugeService.getUpdateOrdersMap("orders");
        assertThat(gaugeMap.size()).isEqualTo(6);
        assertThat(gaugeMap.get(createGenericKey(EOrderState.OS_CONFIRMED, EDisplayOrderType.DT_SUBURBAN))).isEqualTo(6);
        assertThat(gaugeMap.get(createGenericKey(EOrderState.OS_RESERVED, EDisplayOrderType.DT_SUBURBAN))).isEqualTo(3);
        assertThat(gaugeMap.get(createGenericKey(EOrderState.OS_CANCELLED, EDisplayOrderType.DT_SUBURBAN))).isEqualTo(3);
        assertThat(gaugeMap.get(createGenericKey(EOrderState.OS_CONFIRMED, EDisplayOrderType.DT_TRAIN))).isEqualTo(6);
        assertThat(gaugeMap.get(createHotelKey(EHotelOrderState.OS_CONFIRMED))).isEqualTo(5);
        assertThat(gaugeMap.get(createHotelKey(EHotelOrderState.OS_CANCELLED))).isEqualTo(5);
    }

    private GenericOrder createOrder(EOrderState state, EDisplayOrderType displayType) {
        GenericOrder order = new GenericOrder();
        order.setId(UUID.randomUUID());
        order.setState(state);
        order.setDisplayType(displayType);
        genericOrderRepository.saveAndFlush(order);
        return order;
    }

    private HotelOrder createOrder(EHotelOrderState state) {
        HotelOrder order = new HotelOrder();
        order.setId(UUID.randomUUID());
        order.setState(state);
        order.setDisplayType(EDisplayOrderType.DT_HOTEL);
        hotelOrderRepository.saveAndFlush(order);
        return order;
    }

    private GaugeService.GaugeKey createKey(WellKnownWorkflowEntityType entityType, String state,
                                            EDisplayOrderType displayType) {
        Set<GaugeService.GaugeTag> tags = Set.of(
                new GaugeService.GaugeTag("type", entityType.getDiscriminatorValue()),
                new GaugeService.GaugeTag("state", state),
                new GaugeService.GaugeTag("displayType", displayType.toString())
        );
        return new GaugeService.GaugeKey("orders", tags);
    }

    private GaugeService.GaugeKey createGenericKey(EOrderState state, EDisplayOrderType displayOrderType) {
        return createKey(WellKnownWorkflowEntityType.GENERIC_ORDER, state.toString(), displayOrderType);
    }

    private GaugeService.GaugeKey createHotelKey(EHotelOrderState state) {
        return createKey(WellKnownWorkflowEntityType.HOTEL_ORDER, state.toString(), EDisplayOrderType.DT_HOTEL);
    }
}
