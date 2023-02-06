package ru.yandex.market.wms.autostart.strategy;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.wms.autostart.service.SortStationResolver;
import ru.yandex.market.wms.autostart.settings.service.AutostartSettingsService;
import ru.yandex.market.wms.autostart.strategy.manual.waveprocessing.release.BigWithdrawalReleaseStrategy;
import ru.yandex.market.wms.autostart.strategy.manual.waveprocessing.release.DefaultReleaseStrategy;
import ru.yandex.market.wms.common.model.enums.NSqlConfigKey;
import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.model.enums.PickDetailStatus;
import ru.yandex.market.wms.common.model.enums.WaveInProcessStatus;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.AssigmentType;
import ru.yandex.market.wms.common.spring.dao.entity.Loc;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.PickDetail;
import ru.yandex.market.wms.common.spring.dao.entity.Wave;
import ru.yandex.market.wms.common.spring.dao.implementation.LocDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.PickingAssignmentsDao;
import ru.yandex.market.wms.common.spring.dao.implementation.UserActivityDao;
import ru.yandex.market.wms.common.spring.enums.WaveState;
import ru.yandex.market.wms.common.spring.service.NamedCounterService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ReleaseStrategiesTest extends BaseTest {

    @Mock
    private LocDAO locDAO;
    @Mock
    private AutostartSettingsService settings;
    @Mock
    private NamedCounterService namedCounterService;
    @Mock
    private SortStationResolver sortStationResolver;
    @Mock
    private PickingAssignmentsDao pickingAssignmentsDao;
    @Mock
    private UserActivityDao userActivityDao;
    @Mock
    private DbConfigService nSqlConfig;

    @Test
    public void checkDefaultSplittingTaskDetails() {
        setupMocks();
        var strategy = new DefaultReleaseStrategy(locDAO, settings, namedCounterService,
                sortStationResolver, pickingAssignmentsDao, userActivityDao);
        Wave wave = createDefaultWave();
        strategy.release(List.of(wave));
        assertions.assertThat(wave.getTaskDetails().get(0).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(1).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(2).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(3).getAssignmentNumber()).isEqualTo("02");
        assertions.assertThat(wave.getTaskDetails().get(4).getAssignmentNumber()).isEqualTo("02");
        assertions.assertThat(wave.getPickingAssignments().get(0).getAssignmentNumber()).isEqualTo("01");
    }

    @Test
    public void checkBigWithdrawalSplittingTaskDetails() {
        setupMocks();
        var strategy = new BigWithdrawalReleaseStrategy(locDAO, settings, namedCounterService,
                sortStationResolver, pickingAssignmentsDao, userActivityDao, nSqlConfig);
        Wave wave = createBigWithdrawalWave(OrderType.OUTBOUND_FIT);
        strategy.release(List.of(wave));
        assertions.assertThat(wave.getTaskDetails().get(0).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(1).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(2).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(3).getAssignmentNumber()).isEqualTo("02");
        assertions.assertThat(wave.getTaskDetails().get(4).getAssignmentNumber()).isEqualTo("02");
        assertions.assertThat(wave.getPickingAssignments().get(0).getAssignmentNumber()).isEqualTo("01");
    }

    private static Stream<Arguments> utilTypes() {
        return Stream.of(
                Arguments.of(OrderType.PLAN_UTILIZATION_OUTBOUND),
                Arguments.of(OrderType.MANUAL_UTILIZATION_OUTBOUND)
        );
    }

    @ParameterizedTest
    @MethodSource("utilTypes")
    public void checkBigWithdrawalSplittingTaskDetailsToOneAssignment(OrderType type) {
        setupMocks();
        var strategy = new BigWithdrawalReleaseStrategy(locDAO, settings, namedCounterService,
                sortStationResolver, pickingAssignmentsDao, userActivityDao, nSqlConfig);
        Wave wave = createBigWithdrawalWave(type);
        strategy.release(List.of(wave));
        assertions.assertThat(wave.getTaskDetails().get(0).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(1).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(2).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(3).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getTaskDetails().get(4).getAssignmentNumber()).isEqualTo("01");
        assertions.assertThat(wave.getPickingAssignments().get(0).getAssignmentNumber()).isEqualTo("01");
    }

    private void setupMocks() {
        MockitoAnnotations.initMocks(this);
        when(locDAO.findAll(anyCollection())).thenReturn(List.of(Loc.builder()
                .loc("A1")
                .logicalLocation("015743")
                .putawayzone("FLOOR")
                .build()));
        when(settings.finishPickingInConsLocForNonSort()).thenReturn(false);
        when(settings.getItemsIntoPickingOrder()).thenReturn(3);
        when(namedCounterService.getNextAssignmentNumber()).thenReturn("01", "02");
        when(sortStationResolver.getNonSortPackingConsLocation(any(), anyInt(), any(), any()))
                .thenReturn(Optional.empty());
        when(nSqlConfig.getConfigAsBoolean(eq(NSqlConfigKey.PICKING_CAN_PICK_ID), anyBoolean())).thenReturn(true);
    }

    private Wave createDefaultWave() {
        return Wave.builder()
                .waveKey("WAVE-001")
                .batchKey("01")
                .state(WaveState.ALLOCATED)
                .inProcessStatus(WaveInProcessStatus.RESERVATION_COMPLETED)
                .batchOrder(Order.builder().status(OrderStatus.ALLOCATED.getValue()).build())
                .orderDetails(List.of(OrderDetail.builder()
                        .storerKey("SK01").sku("SKU01")
                        .assigmentType(AssigmentType.SORTABLE_CONVEYABLE).build()))
                .batchOrderDetails(List.of(createOrderDetail("01")))
                .pickDetails(List.of(createPickDetail("01"),
                        createPickDetail("02"),
                        createPickDetail("03"),
                        createPickDetail("04"),
                        createPickDetail("05")))
                .build();
    }

    private Wave createBigWithdrawalWave(OrderType type) {
        return Wave.builder()
                .waveKey("WAVE-001")
                .batchKey("01")
                .state(WaveState.ALLOCATED)
                .inProcessStatus(WaveInProcessStatus.RESERVATION_COMPLETED)
                .batchOrder(Order.builder().status(OrderStatus.ALLOCATED.getValue()).build())
                .orderDetails(List.of(OrderDetail.builder()
                        .storerKey("SK01").sku("SKU01")
                        .status(OrderStatus.ALLOCATED)
                        .assigmentType(AssigmentType.SORTABLE_CONVEYABLE).build()))
                .batchOrderDetails(List.of(createOrderDetail("01")))
                .pickDetails(List.of(createPickDetail("01"),
                        createPickDetail("02"),
                        createPickDetail("03"),
                        createPickDetail("04"),
                        createPickDetail("05")))
                .realOrders(List.of(createOrder("01", type)))
                .build();
    }

    private Order createOrder(String orderKey, OrderType type) {
        return Order.builder()
                .orderKey(orderKey)
                .storerKey("SK01")
                .status(Integer.toString(OrderStatus.ALLOCATED.getCode()))
                .type(type.getCode())
                .build();
    }

    private OrderDetail createOrderDetail(String orderKey) {
        return OrderDetail.builder()
                .orderKey(orderKey)
                .storerKey("SK01")
                .sku("SKU01")
                .status(OrderStatus.ALLOCATED)
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    private PickDetail createPickDetail(String pickDetailKey) {
        return PickDetail.builder()
                .pickDetailKey(pickDetailKey)
                .storerKey("SK01")
                .sku("SKU01")
                .loc("A1")
                .fromLoc("A1")
                .status(PickDetailStatus.NORMAL)
                .build();
    }
}
