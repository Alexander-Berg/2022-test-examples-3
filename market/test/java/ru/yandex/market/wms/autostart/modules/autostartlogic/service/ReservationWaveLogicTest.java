package ru.yandex.market.wms.autostart.modules.autostartlogic.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.ReservationWaveLogic;
import ru.yandex.market.wms.autostart.autostartlogic.nonsort.NonSortLocationReserver;
import ru.yandex.market.wms.autostart.autostartlogic.nonsort.NonSortService;
import ru.yandex.market.wms.autostart.autostartlogic.service.CreationAutoStartService;
import ru.yandex.market.wms.autostart.autostartlogic.service.WaveReserver;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.ReservationSettingsCreator;
import ru.yandex.market.wms.autostart.model.AosWave;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.PickingOrderBatch;
import ru.yandex.market.wms.common.spring.dao.entity.SubBatch;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.WaveType;
import ru.yandex.market.wms.common.spring.service.OrderStatusNotificationService;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReservationWaveLogicTest {

    private ReservationWaveLogic reservationWaveLogic;
    private ServicebusClient servicebusClient;
    private NonSortService nonSortService;
    private OrderStatusNotificationService orderStatusNotificationService;

    @BeforeEach
    public void setUp() {
        CreationAutoStartService creationAutoStartService = mock(CreationAutoStartService.class);
        nonSortService = mock(NonSortService.class);
        when(nonSortService.getAosReserver(null)).thenReturn(new NonSortLocationReserver());
        servicebusClient = mock(ServicebusClient.class);
        orderStatusNotificationService = mock(OrderStatusNotificationService.class);
        ReservationSettingsCreator settingsCreator = mock(ReservationSettingsCreator.class);
        WaveReserver waveReserver = mock(WaveReserver.class);
        reservationWaveLogic = new ReservationWaveLogic(
                settingsCreator,
                creationAutoStartService,
                waveReserver
        );
    }

    @Test
    public void notifyOrderStatusChangedFailedTest() {
        List<Order> orders = buildOrdersList();
        List<PickingOrderBatch> pickingOrderBatches = buildPickingOrderBatches(orders);
        AosWave aosWave = AosWave.builder()
                .waveType(WaveType.ALL)
                .linkedToDsType(LinkedToDsType.NO_LINK_TO_DS)
                .stationToCarriers(Collections.emptyList())
                .batches(pickingOrderBatches)
                .build();
        // That RuntimeException should be caught in reservationWaveLogic.reserve(aosWave)
        when(servicebusClient.pushOrderStatus(any())).thenThrow(new RuntimeException("Failed to push order status"));
        reservationWaveLogic.reserve(aosWave);
    }

    private List<PickingOrderBatch> buildPickingOrderBatches(List<Order> orders) {
        return List.of(
                PickingOrderBatch.builder()
                        .subBatches(List.of(
                                SubBatch.<OrderWithDetails>builder()
                                        .orders(orders.stream()
                                                .map(o -> OrderWithDetails.builder().order(o).build())
                                                .collect(Collectors.toList()))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<Order> buildOrdersList() {
        return List.of(
                Order.builder().orderKey("KEY1").externalOrderKey("EKEY1").build(),
                Order.builder().orderKey("KEY2").externalOrderKey("EKEY2").build(),
                Order.builder().orderKey("KEY3").externalOrderKey("EKEY3").build()
        );
    }

}
