package ru.yandex.market.tpl.core.domain.locker;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointReturnReason;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnQueryService;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoQueryService;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.order.MultiOrderService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.pickup.LockerUnloadOrderResult;
import ru.yandex.market.tpl.core.domain.pickup.LockerUnloadScanSummary;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CARGO_DROPOFF_DIRECT_FLOW_ENABLED;

@ExtendWith(MockitoExtension.class)
class LockerUnloadValidatorUnitTest {

    public static final long USER_SHIFT_ID = 777L;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MultiOrderService multiOrderService;
    @Mock
    private DropoffCargoRepository dropoffCargoRepository;
    @Mock
    private ClientReturnQueryService clientReturnQueryService;
    @Mock
    private DropoffCargoQueryService dropoffCargoQueryService;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @InjectMocks
    private LockerUnloadValidator lockerUnloadValidator;

    @AfterEach
    void tearDown() {
        Mockito.reset(dropoffCargoRepository, configurationProviderAdapter);
    }

    @BeforeEach
    void setUp() {
        when(configurationProviderAdapter.isBooleanEnabled(CARGO_DROPOFF_DIRECT_FLOW_ENABLED)).thenReturn(true);
    }

    @Test
    void withAllcargos() {
        //given
        List<DropoffCargo> cargos = List.of(
                        Pair.of(1L, "barcode1"),
                        Pair.of(2L, "barcode2"),
                        Pair.of(3L, "barcode3"))
                .stream()
                .map(pair -> buildCargo(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());

        Set<Long> cargoIds = cargos.stream().map(DropoffCargo::getId).collect(Collectors.toSet());
        DeliveryTask task = buildMockedDeliveryTask(cargoIds);

        when(dropoffCargoRepository.findByIdIn(cargoIds)).thenReturn(cargos);
        when(dropoffCargoQueryService.getDropoffForPickup(any(), any())).thenReturn(List.of());

        //when
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder("barcode1", PickupPointReturnReason.DIMENSIONS_EXCEEDS_LOCKER, List.of()),
                new UnloadedOrder("barcode2", PickupPointReturnReason.CELL_DID_NOT_OPEN, List.of()),
                new UnloadedOrder("barcode3", PickupPointReturnReason.OTHER, List.of())
        );
        LockerUnloadScanSummary summary = lockerUnloadValidator.getSummary(task, unloadedOrders);

        //then
        assertSummary(cargos, List.of(), summary,
                Set.of(LockerUnloadOrderResult.CELL_DID_NOT_OPEN,
                        LockerUnloadOrderResult.OTHER, LockerUnloadOrderResult.DIMENSIONS_EXCEEDS), Set.of(),
                List.of());
    }

    @Test
    void withCargosDirectAndOrders() {
        //given
        List<DropoffCargo> cargos = List.of(
                        Pair.of(1L, "barcode1"),
                        Pair.of(2L, "barcode2"),
                        Pair.of(3L, "barcode3"))
                .stream()
                .map(pair -> buildCargo(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());

        Set<Long> cargoIds = cargos.stream().map(DropoffCargo::getId).collect(Collectors.toSet());
        DeliveryTask task = buildMockedDeliveryTask(Set.of(), cargoIds);

        when(dropoffCargoRepository.findByIdIn(any())).thenReturn(List.of());
        when(dropoffCargoQueryService.getDropoffForPickup(eq(USER_SHIFT_ID), eq(Set.of("barcode4",
                cargos.get(0).getBarcode(),
                cargos.get(1).getBarcode())))).thenReturn(List.of(cargos.get(0), cargos.get(1)));

        //when
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder(cargos.get(0).getBarcode(), PickupPointReturnReason.OTHER,
                        List.of(cargos.get(0).getBarcode())),
                new UnloadedOrder(cargos.get(1).getBarcode(), PickupPointReturnReason.CELL_DID_NOT_OPEN,
                        List.of(cargos.get(1).getBarcode())),
                new UnloadedOrder("barcode4", PickupPointReturnReason.OTHER, List.of("barcode4"))
        );
        LockerUnloadScanSummary summary = lockerUnloadValidator.getSummary(task, unloadedOrders);

        //then
        assertSummary(List.of(), List.of(cargos.get(0), cargos.get(1)), summary,
                Set.of(),
                Set.of(LockerUnloadOrderResult.OK),
                List.of("barcode4"));
    }

    @Test
    void withCargosReturnAndOrders() {
        //given
        List<DropoffCargo> cargos = List.of(
                        Pair.of(1L, "barcode1"),
                        Pair.of(2L, "barcode2"),
                        Pair.of(3L, "barcode3"))
                .stream()
                .map(pair -> buildCargo(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());

        Set<Long> cargoIds = cargos.stream().map(DropoffCargo::getId).collect(Collectors.toSet());
        DeliveryTask task = buildMockedDeliveryTask(cargoIds);

        when(dropoffCargoRepository.findByIdIn(cargoIds)).thenReturn(cargos);
        when(dropoffCargoQueryService.getDropoffForPickup(any(), any())).thenReturn(List.of());

        //when
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder("barcode1", PickupPointReturnReason.DIMENSIONS_EXCEEDS_LOCKER, List.of()),
                new UnloadedOrder("barcode2", PickupPointReturnReason.CELL_DID_NOT_OPEN, List.of()),
                new UnloadedOrder("barcode4", PickupPointReturnReason.OTHER, List.of())
        );
        LockerUnloadScanSummary summary = lockerUnloadValidator.getSummary(task, unloadedOrders);

        //then
        assertSummary(List.of(cargos.get(0), cargos.get(1)), List.of(), summary,
                Set.of(LockerUnloadOrderResult.CELL_DID_NOT_OPEN,
                        LockerUnloadOrderResult.DIMENSIONS_EXCEEDS), Set.of(),
                List.of("barcode4"));
    }

    @Test
    void whenCargoEmpty() {
        //given
        List<DropoffCargo> cargos = List.of(
                        Pair.of(1L, "barcode1"),
                        Pair.of(2L, "barcode2"),
                        Pair.of(3L, "barcode3"))
                .stream()
                .map(pair -> buildCargo(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());

        Set<Long> cargoIds = cargos.stream().map(DropoffCargo::getId).collect(Collectors.toSet());
        DeliveryTask task = buildMockedDeliveryTask(cargoIds);

        when(dropoffCargoRepository.findByIdIn(cargoIds)).thenReturn(cargos);
        when(dropoffCargoQueryService.getDropoffForPickup(any(), any())).thenReturn(List.of());

        //when
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder("barcode5", PickupPointReturnReason.DIMENSIONS_EXCEEDS_LOCKER, List.of()),
                new UnloadedOrder("barcode6", PickupPointReturnReason.CELL_DID_NOT_OPEN, List.of()),
                new UnloadedOrder("barcode4", PickupPointReturnReason.OTHER, List.of())
        );
        LockerUnloadScanSummary summary = lockerUnloadValidator.getSummary(task, unloadedOrders);

        //then
        assertSummary(List.of(), List.of(), summary,
                Set.of(), Set.of(),
                List.of("barcode4", "barcode5", "barcode6"));
    }

    private void assertSummary(List<DropoffCargo> expectedReturnCargos,
                               List<DropoffCargo> expectedDirectCargos,
                               LockerUnloadScanSummary summary,
                               Set<LockerUnloadOrderResult> unloadExpectedResultsReturn,
                               Set<LockerUnloadOrderResult> unloadExpectedResultsDirect,
                               List<String> orders) {

        assertThat(summary.getNotFoundExternalOrderIdsS()).hasSize(orders.size());
        assertThat(summary.getNotFoundExternalOrderIdsS()).containsExactlyInAnyOrderElementsOf(orders);


        assertThat(summary.getDropoffCargoReturn()).hasSize(expectedReturnCargos.size());
        var barcodes = summary.getDropoffCargoReturn()
                .stream()
                .map(LockerUnloadScanSummary.DropoffCargo::getBarcode)
                .collect(Collectors.toSet());
        assertThat(barcodes).containsExactlyInAnyOrderElementsOf(expectedReturnCargos.stream().map(DropoffCargo::getBarcode)
                .collect(Collectors.toSet()));

        var unloadDropoffReturnResults =
                summary.getDropoffCargoReturn()
                        .stream()
                        .map(LockerUnloadScanSummary.DropoffCargo::getUnloadResult)
                        .collect(Collectors.toSet());
        assertThat(unloadDropoffReturnResults).containsExactlyInAnyOrderElementsOf(unloadExpectedResultsReturn);

        var unloadDropoffResults = summary.getDropoffCargo()
                .stream()
                .map(LockerUnloadScanSummary.DropoffCargo::getUnloadResult)
                .collect(Collectors.toSet());
        assertThat(unloadDropoffResults).containsExactlyInAnyOrderElementsOf(unloadExpectedResultsDirect);
    }

    private DeliveryTask buildMockedDeliveryTask(Set<Long> cargoReturnIds) {
        return buildMockedDeliveryTask(cargoReturnIds, Set.of());
    }

    private DeliveryTask buildMockedDeliveryTask(Set<Long> cargoReturnIds, Set<Long> cargoDirectIds) {
        Set<LockerSubtask> cargoReturnSubtasks = cargoReturnIds
                .stream()
                .map(this::buildMockedSubtaskDropoffReturn)
                .collect(Collectors.toSet());

        Set<LockerSubtask> cargoDirectSubtasks = cargoDirectIds
                .stream()
                .map(this::buildMockedSubtaskDropoffDirect)
                .collect(Collectors.toSet());

        var mockedDeliveryTask = mock(LockerDeliveryTask.class);
        when(mockedDeliveryTask.streamDropOffReturnSubtasks()).thenReturn(StreamEx.of(cargoReturnSubtasks));
        when(mockedDeliveryTask.streamDeliveryOrderSubtasks()).thenReturn(StreamEx.of(cargoDirectSubtasks));

        var routePoint = mock(RoutePoint.class);
        UserShift userShift = new UserShift();
        userShift.setId(USER_SHIFT_ID);
        when(routePoint.getUserShift()).thenReturn(userShift);
        when(mockedDeliveryTask.getRoutePoint()).thenReturn(routePoint);
        return mockedDeliveryTask;
    }

    private LockerSubtask buildMockedSubtaskDropoffReturn(long cargoId) {
        LockerSubtask mockedSubtask = mock(LockerSubtask.class);
        when(mockedSubtask.isCargoDeliverySubtask()).thenReturn(true);
        when(mockedSubtask.getCargoId()).thenReturn(cargoId);
        return mockedSubtask;
    }

    private LockerSubtask buildMockedSubtaskDropoffDirect(long cargoId) {
        LockerSubtask mockedSubtask = mock(LockerSubtask.class);
        return mockedSubtask;
    }

    private DropoffCargo buildCargo(Long id, String barcode) {
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setId(id);
        dropoffCargo.setBarcode(barcode);
        return dropoffCargo;
    }
}
