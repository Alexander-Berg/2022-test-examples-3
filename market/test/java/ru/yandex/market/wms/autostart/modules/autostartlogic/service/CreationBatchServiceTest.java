package ru.yandex.market.wms.autostart.modules.autostartlogic.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.autostart.autostartlogic.service.CreationBatchService;
import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.model.enums.WaveInProcessStatus;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderHistory;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.PickingOrderBatch;
import ru.yandex.market.wms.common.spring.dao.entity.SubBatch;
import ru.yandex.market.wms.common.spring.dao.entity.Wave;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderStatusHistoryDao;
import ru.yandex.market.wms.common.spring.enums.WaveState;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_01;

@ExtendWith(MockitoExtension.class)
public class CreationBatchServiceTest {

    private CreationBatchService creationBatchService;

    @BeforeEach
    public void setUp() {
        OrderDao orderDao = mock(OrderDao.class);
        OrderDetailDao orderDetailDao = mock(OrderDetailDao.class);
        OrderStatusHistoryDao orderStatusHistoryDao = mock(OrderStatusHistoryDao.class);
        creationBatchService = new CreationBatchService(
                orderDao,
                orderDetailDao,
                orderStatusHistoryDao
        );
    }

    @Test
    public void verifyBatchOrder() {
        List<Order> orders = buildOrdersList();
        PickingOrderBatch pickingOrderBatch = buildPickingOrderBatch(orders);
        ArrayList<OrderHistory> orderHistories = new ArrayList<>();
        String building = "1";
        String batchKey = "batchKey";
        Wave wave = Wave.builder()
                .waveKey("waveKey")
                .waveType(WaveType.ALL)
                .batchKey(batchKey)
                .state(WaveState.STARTED)
                .realOrders(orders)
                .inProcessStatus(WaveInProcessStatus.TASK_STARTED)
                .skuIdBatchOrderDetails(new HashMap<>())
                .building(building)
                .build();

        creationBatchService.fillBatchDetailAndHistoryDetails(wave, orderHistories, pickingOrderBatch);

        Order batchOrder = wave.getBatchOrder();
        assertThat(batchOrder).isNotNull();
        assertThat(batchOrder.getBatchKey()).isEqualTo(batchKey);
        assertThat(batchOrder.getTotalqty())
                .isEqualTo(orders.stream().map(Order::getTotalqty).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertThat(batchOrder.getDoor()).isEqualTo(DOOR_S_01);
        assertThat(batchOrder.getStatus()).isEqualTo(OrderStatus.RELEASED.getValue());
        assertThat(batchOrder.getBuilding()).isEqualTo(building);
    }

    private PickingOrderBatch buildPickingOrderBatch(List<Order> orders) {
        return PickingOrderBatch.builder()
                .subBatches(List.of(
                        SubBatch.<OrderWithDetails>builder()
                                .orders(orders.stream()
                                        .map(o -> OrderWithDetails.builder().order(o).build())
                                        .collect(Collectors.toList()))
                                .sortingStation(DOOR_S_01)
                                .build()
                ))
                .build();
    }

    private List<Order> buildOrdersList() {
        return List.of(
                Order.builder().orderKey("KEY1").externalOrderKey("EKEY1").totalqty(BigDecimal.ONE).build(),
                Order.builder().orderKey("KEY2").externalOrderKey("EKEY2").totalqty(BigDecimal.ONE).build(),
                Order.builder().orderKey("KEY3").externalOrderKey("EKEY3").totalqty(BigDecimal.ONE).build()
        );
    }

}
