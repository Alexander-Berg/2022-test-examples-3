package ru.yandex.market.sc.core.domain.scan;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.OrderScanLogSomeIdConstraintCheckListener;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.OrderFlowService;
import ru.yandex.market.sc.core.domain.order.OrderQueryService;
import ru.yandex.market.sc.core.domain.order.model.ApiLotBaseDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.ApiPlaceLotDto;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.PlaceCommandService;
import ru.yandex.market.sc.core.domain.place.model.PlaceId;
import ru.yandex.market.sc.core.domain.place.model.PlaceScRequest;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinishOrder;
import ru.yandex.market.sc.core.domain.scan.model.AcceptLotRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.AcceptReturnedOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogOperation;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogResult;
import ru.yandex.market.sc.core.domain.scan_log.repository.OrderScanLogEntry;
import ru.yandex.market.sc.core.domain.scan_log.repository.OrderScanLogEntryRepository;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.filter;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix.CLIENT_RETURN_FBS_BUGR;
import static ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix.CLIENT_RETURN_FBS_DZER;
import static ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix.CLIENT_RETURN_FBS_KRASN;
import static ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix.CLIENT_RETURN_FBS_SHUSH;
import static ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix.CLIENT_RETURN_FBS_TAR;
import static ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix.CLIENT_RETURN_FSN;
import static ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix.CLIENT_RETURN_PS;
import static ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ScanServiceTest {

    @Mock
    private PlaceCommandService placeCommandService;
    private final TestFactory testFactory;
    private final ScOrderRepository orderRepository;
    private final ScanService scanService;
    private final PlaceRepository placeRepository;
    private final OrderScanLogEntryRepository orderScanLogEntryRepository;
    private final ConfigurationService configurationService;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @MockBean
    private Clock clock;
    private SortingCenter sortingCenter;
    private SortableLot lot;
    private User user;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        Cell parentCell = testFactory.storedCell(sortingCenter, "cell-1", CellType.RETURN);
        lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        user = testFactory.storedUser(sortingCenter, 456L);
    }

    @Disabled("получать cellId на инвентаризации от фронта MARKETTPLSC-3401")
    @Test
    void logInventarizationCreatedNoCellId() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        testFactory.storedCell(sortingCenter);

        scanService.getOrder(order.getExternalId(), null, null,
                new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
        );

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(1);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isEqualTo(ScErrorCode.ORDER_NOT_FROM_CURRENT_CELL);
    }

    @Test
    void logInventarizationInCellNoCellId() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort(cell.getId()).get();

        scanService.getOrder(order.getExternalId(), order.getExternalId(), null,
                new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
        );

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(1);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.OK);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isEqualTo(cell.getId());
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isEqualTo(cell.getId());
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isNull();
    }

    @Test
    void logInventarizationFromDifferentScNoCellId() {
        var differentSortingCenter = testFactory.storedSortingCenter(456L);
        var order = testFactory.createOrderForToday(differentSortingCenter).get();

        testFactory.storedCell(sortingCenter);
        assertThatThrownBy(
                () -> scanService.getOrder(order.getExternalId(), null, null,
                        new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
                )
        ).isInstanceOf(ScException.class);

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(1);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isEqualTo(ScErrorCode.ORDER_FROM_ANOTHER_SC);
    }

    @Test
    void logInventarizationUnknownBarcodeNoCellId() {
        testFactory.storedCell(sortingCenter);
        scanService.getOrder("unknown", null, null,
                new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
        );

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of("unknown")
        );
        assertThat(orderScanLogEntries).hasSize(1);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isEqualTo(ScErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void logInventarizationShippedToCourierNoCellId() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort(cell.getId()).ship().get();

        scanService.getOrder(order.getExternalId(), null, null,
                new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
        );

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(2);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SHIP);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.COURIER_SHIP);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.OK);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isEqualTo(cell.getId());
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();

        assertThat(orderScanLogEntries.get(1).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(1).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(1).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(1).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(1).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(1).getErrorCode())
                .isEqualTo(ScErrorCode.ORDER_NOT_FROM_CURRENT_CELL_SHIPPED_DIRECT);
    }

    @Test
    void logInventarizationReturnedToWarehouseNoCellId() {
        var cell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN, "w1");
        var order = testFactory.create(order(sortingCenter).warehouseReturnId("w1").build())
                .accept().cancel().sort(cell.getId()).ship().get();

        scanService.getOrder(order.getExternalId(), null, null,
                new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
        );

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(2);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SHIP);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.RETURN_SHIP);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.OK);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isEqualTo(cell.getId());
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isNull();

        assertThat(orderScanLogEntries.get(1).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(1).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(1).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(1).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(1).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(1).getErrorCode())
                .isEqualTo(ScErrorCode.ORDER_NOT_FROM_CURRENT_CELL_SHIPPED_RETURN);
    }

    @Test
    @Disabled("MARKETTPLSC-5120")
    void logInitialAcceptanceReturnedToWarehouseNoCellId() {
        var cell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN, "w1");
        var order = testFactory.create(order(sortingCenter).warehouseReturnId("w1").build())
                .accept().cancel().sort(cell.getId()).ship().get();


        assertThatThrownBy(() -> scanService.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), null),
                new ScContext(user, ScanLogContext.INITIAL_ACCEPTANCE)))
                .isInstanceOf(ScException.class);

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(2);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SHIP);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.RETURN_SHIP);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.OK);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isEqualTo(cell.getId());
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isNull();

        assertThat(orderScanLogEntries.get(1).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(1).getContext()).isEqualTo(ScanLogContext.INITIAL_ACCEPTANCE);
        assertThat(orderScanLogEntries.get(1).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(1).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(1).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(1).getErrorCode())
                .isEqualTo(ScErrorCode.PLACE_SHIPPED_ON_RETURN_STREAM);
    }


    @Test
    void logInventarizationCreated() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var cell = testFactory.storedCell(sortingCenter);

        assertThatThrownBy(
                () -> scanService.getOrder(order.getExternalId(), null, cell.getId(),
                        new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
                )
        ).isInstanceOf(ScException.class);

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(1);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isEqualTo(ScErrorCode.ORDER_NOT_FROM_CURRENT_CELL);
    }

    @Test
    void logInventarizationInCell() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort(cell.getId()).get();

        scanService.getOrder(order.getExternalId(), order.getExternalId(), cell.getId(),
                new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
        );

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(1);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.OK);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isEqualTo(cell.getId());
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isEqualTo(cell.getId());
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isNull();
    }

    @Test
    void logInventarizationFromDifferentSc() {
        var differentSortingCenter = testFactory.storedSortingCenter(456L);
        var order = testFactory.createOrderForToday(differentSortingCenter).get();

        var cell = testFactory.storedCell(sortingCenter);
        assertThatThrownBy(
                () -> scanService.getOrder(order.getExternalId(), order.getExternalId(), cell.getId(),
                        new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
                )
        ).isInstanceOf(ScException.class);

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(1);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isEqualTo(ScErrorCode.ORDER_FROM_ANOTHER_SC);
    }

    @Test
    void logInventarizationUnknownBarcode() {
        var cell = testFactory.storedCell(sortingCenter);
        assertThatThrownBy(
                () -> scanService.getOrder("unknown", null, cell.getId(),
                        new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
                )
        ).isInstanceOf(ScException.class);

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of("unknown")
        );
        assertThat(orderScanLogEntries).hasSize(1);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isEqualTo(ScErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void logInventarizationShippedToCourier() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort(cell.getId()).ship().get();

        assertThatThrownBy(
                () -> scanService.getOrder(order.getExternalId(), null, cell.getId(),
                        new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
                )
        ).isInstanceOf(ScException.class);

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(2);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SHIP);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.COURIER_SHIP);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.OK);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isEqualTo(cell.getId());
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();

        assertThat(orderScanLogEntries.get(1).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(1).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(1).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(1).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(1).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(1).getErrorCode())
                .isEqualTo(ScErrorCode.ORDER_NOT_FROM_CURRENT_CELL_SHIPPED_DIRECT);
    }

    @Test
    void logInventarizationReturnedToWarehouse() {
        var cell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN, "w1");
        var order = testFactory.create(order(sortingCenter).warehouseReturnId("w1").build())
                .accept().cancel().sort(cell.getId()).ship().get();

        assertThatThrownBy(
                () -> scanService.getOrder(order.getExternalId(), null, cell.getId(),
                        new ScContext(user, ScanLogContext.INVENTORY_RESORTING)
                )
        ).isInstanceOf(ScException.class);

        var orderScanLogEntries = orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(), List.of(order.getExternalId())
        );
        assertThat(orderScanLogEntries).hasSize(2);
        assertThat(orderScanLogEntries.get(0).getOperation()).isEqualTo(ScanLogOperation.SHIP);
        assertThat(orderScanLogEntries.get(0).getContext()).isEqualTo(ScanLogContext.RETURN_SHIP);
        assertThat(orderScanLogEntries.get(0).getResult()).isEqualTo(ScanLogResult.OK);
        assertThat(orderScanLogEntries.get(0).getCellBeforeId()).isEqualTo(cell.getId());
        assertThat(orderScanLogEntries.get(0).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(0).getErrorCode()).isNull();

        assertThat(orderScanLogEntries.get(1).getOperation()).isEqualTo(ScanLogOperation.SCAN);
        assertThat(orderScanLogEntries.get(1).getContext()).isEqualTo(ScanLogContext.INVENTORY_RESORTING);
        assertThat(orderScanLogEntries.get(1).getResult()).isEqualTo(ScanLogResult.ERROR);
        assertThat(orderScanLogEntries.get(1).getCellBeforeId()).isNull();
        assertThat(orderScanLogEntries.get(1).getCellAfterId()).isNull();
        assertThat(orderScanLogEntries.get(1).getErrorCode())
                .isEqualTo(ScErrorCode.ORDER_NOT_FROM_CURRENT_CELL_SHIPPED_RETURN);
    }

    @Test
    @DisplayName("Клиентский возврат привез другой курьер, создаем маршрут на него и завершаем")
    void acceptReturnedFromAnotherCourier() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();
        var oldCourierDto = new CourierDto(1003L, "Курьер с возвратами", null);
        var order = testFactory.createClientReturnForToday(
                        sortingCenter,
                        oldCourierDto,
                        CLIENT_RETURN_PVZ.getAnyPrefix())
                .get();
        var newCourier = testFactory.storedCourier(745L);

        ApiOrderDto apiOrderDto = scanService.acceptReturnedOrder(
                // нужно создать плейс, чтобы создался маршрут на нового курьра
                buildAcceptReturnedRequest(order.getExternalId(), newCourier.getId(), true),
                new ScContext(user)
        );

        var actualOrder = testFactory.getOrder(apiOrderDto.getId());
        assertThat(order).isEqualTo(actualOrder);

        var newCourierRoute = testFactory.findPossibleIncomingCourierRoute(actualOrder, newCourier).orElseThrow();
        assertThat(newCourierRoute.getCourierFromId()).isEqualTo(newCourier.getId());
        var newCourierRouteFinishOrders = newCourierRoute.getAllRouteFinishOrders()
                .stream()
                .map(RouteFinishOrder::getOrderId)
                .collect(Collectors.toSet());
        assertThat(newCourierRouteFinishOrders).containsExactly(actualOrder.getId());

        var oldCourierRoute = testFactory.findPossibleIncomingCourierRoute(actualOrder).orElseThrow();
        assertThat(oldCourierRoute.getRouteFinishes()).isEmpty();
    }

    @Test
    void createClientReturnOnAcceptReturn() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();
        var requestDto =
                new AcceptReturnedOrderRequestDto(CLIENT_RETURN_PVZ.getAnyPrefix() + "-1",
                        null, null);
        ApiOrderDto orderDto = scanService.acceptReturnedOrder(requestDto, new ScContext(user));
        assertThat(orderDto.getId()).isNotNull();
        assertThat(orderDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
    }

    @Test
    void createClientReturnOnAcceptReturnDisabled() {
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.DISABLE_CLIENT_RETURN_CREATE, "true");
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();
        var requestDto =
                new AcceptReturnedOrderRequestDto(CLIENT_RETURN_PVZ.getAnyPrefix() + "-1",
                        null, null);
        ApiOrderDto orderDto = scanService.acceptReturnedOrder(requestDto, new ScContext(user));
        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    void doNotCreateClientReturnWithGoodPrefixAndBadBarcode() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();
        var requestDto =
                new AcceptReturnedOrderRequestDto(CLIENT_RETURN_PVZ.getAnyPrefix() + " -1",
                        null, null);
        ApiOrderDto orderDto = scanService.acceptReturnedOrder(requestDto, new ScContext(user));
        assertThat(orderDto.getId()).isNull();
        assertThat(orderDto.getStatus()).isEqualTo(ApiOrderStatus.ERROR);
    }

    @Test
    @DisplayName("Создаем клиентский возврат на неизвестный заказ, создаем маршрут на курьера и завершаем его")
    void createClientReturnOnAcceptReturnFromCourier() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();

        var courier = testFactory.storedCourier(765L);
        var requestDto = buildAcceptReturnedRequest(
                CLIENT_RETURN_PVZ.getAnyPrefix(),
                courier.getId(),
                false
        );
        ApiOrderDto orderDto = scanService.acceptReturnedOrder(requestDto, new ScContext(user));

        assertThat(orderDto.getId()).isNotNull();
        assertThat(orderDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
        testFactory.getOrder(orderDto.getId());
        var clientReturn = testFactory.getOrder(orderDto.getId());
        var route = testFactory.findPossibleIncomingCourierRoute(clientReturn, courier).orElseThrow();
        assertThat(route.getCourierFromId()).isEqualTo(courier.getId());
    }

    @Test
    void createFbsClientReturnOnAcceptReturnPs() {
        createClientReturnOnScanTest(CLIENT_RETURN_PS, "10001794073");
    }

    @Test
    void createFbsClientReturnOnAcceptReturnPvz() {
        createClientReturnOnScanTest(CLIENT_RETURN_PVZ, "10001794073");
    }

    @Test
    void createFbsClientReturnOnAcceptReturnFsn() {
        createClientReturnOnScanTest(CLIENT_RETURN_FSN, "10000010736");
    }

    @Test
    void createFbsClientReturnOnAcceptReturnTar() {
        createClientReturnOnScanTest(CLIENT_RETURN_FBS_TAR, "10001700279");
    }

    @Test
    void createFbsClientReturnOnAcceptReturnDzer() {
        createClientReturnOnScanTest(CLIENT_RETURN_FBS_DZER, "10001804390");
    }

    @Test
    void createFbsClientReturnOnAcceptReturnBugr() {
        createClientReturnOnScanTest(CLIENT_RETURN_FBS_BUGR, "10001896827");
    }

    @Test
    void createFbsClientReturnOnAcceptReturnShush() {
        createClientReturnOnScanTest(CLIENT_RETURN_FBS_SHUSH, "10001855034");
    }

    @Test
    void createFbsClientReturnOnAcceptReturnKrasn() {
        createClientReturnOnScanTest(CLIENT_RETURN_FBS_KRASN, "10001742420");
    }

    private void createClientReturnOnScanTest(ClientReturnBarcodePrefix prefix, String expectedWarehouseId) {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns(prefix.getWarehouseReturnId());
        var requestDto =
                new AcceptReturnedOrderRequestDto(prefix.getAnyPrefix() + "-1", null, null);
        ApiOrderDto orderDto = scanService.acceptReturnedOrder(requestDto, new ScContext(user));
        assertThat(orderDto.getId()).isNotNull();
        assertThat(orderDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(orderDto.getWarehouseReturnYandexId()).isPresent();
        assertThat(orderDto.getWarehouseReturnYandexId().orElseThrow()).isEqualTo(expectedWarehouseId);
    }

    @Test
    void lotSortOrderWhenSinglePlaceOrder() {
        Place place = testFactory.createOrderForToday(sortingCenter).accept().cancel().sort().getPlace();
        testFactory.sortToLot(place, lot, user);
        place = testFactory.getPlace(place.getId());

        assertThat(place.getParent()).isNotNull();
        assertThat(place.getParent()).isEqualTo(lot.getSortable());
        assertThat(place.getCell()).isNull();
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void lotSortOrderWhenMultiPlaceOrder() {
        var order = testFactory.createOrder(order(sortingCenter)
                        .places("p1", "p2")
                        .externalId("o1")
                        .build())
                .acceptPlaces()
                .makeReturn()
                .getPlaces();
        testFactory.sortPlace(order.get("p1"));
        testFactory.sortPlace(order.get("p2"));

        testFactory.sortToLot(order.get("p1"), lot,  user);

        List<Place> actualPlaces = placeRepository.findAllByOrderIdOrderById(order.get("p1").getOrderId());
        assertThat(filter(actualPlaces).with("lot").notEqualsTo(null).get())
                .extracting(p -> p.getLot().getId())
                .containsOnly(lot.getLotId());
    }

    @Test
    void lotSortOrderWhenException() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().getPlace();

        PlaceScRequest placeScRequest = new PlaceScRequest(PlaceId.of(place), user);

        doThrow(RuntimeException.class).when(placeCommandService).sortPlaceToLot(placeScRequest, lot.getLotId());

        assertThatThrownBy(() -> testFactory.sortToLot(place, lot, user))
                .isInstanceOf(RuntimeException.class);
        assertThat(orderScanLogEntryRepository.findAllBySortingCenterAndExternalOrderIdInOrderById(
                sortingCenter.getId(),
                List.of(place.getExternalId())))
                .filteredOn(orderScanLogEntry -> orderScanLogEntry.getOperation().equals(ScanLogOperation.SORT))
                .hasSize(1);
    }

    @Test
    void getOrderWithLotsIsNotMultiPlace() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .cancel()
                .sort()
                .getPlace();

        var apiOrderWithLotsDto = scanService.getOrderWithLots(place.getExternalId(), null,
                new ScContext(user, ScanLogContext.SORT_LOT));
        assertThat(apiOrderWithLotsDto.getPlaces()).hasSize(1);
        ApiPlaceLotDto placeLotDto = apiOrderWithLotsDto.getPlaces().get(0);
        assertThat(placeLotDto.getCell().getId())
                .isEqualTo(place.getCellId().orElse(null));
        assertThat(apiOrderWithLotsDto.getLotsTo()).isNotEmpty();
        assertThat(apiOrderWithLotsDto.getLotsTo()).containsOnly(new ApiLotBaseDto(
                lot.getLotId(),
                lot.getNameForApi(),
                lot.getBarcode()));
    }

    @Test
        // https://st.yandex-team.ru/MARKETTPLSC-4456
    void transferToLot() {
        OrderScanLogSomeIdConstraintCheckListener.resetHitCounter();

        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().getPlace();

        SortableLot lotFrom = testFactory.storedLot(sortingCenter, SortableType.ORPHAN_PALLET, place.getCell());
        SortableLot lotTo = testFactory.storedLot(sortingCenter, SortableType.ORPHAN_PALLET, place.getCell());
        testFactory.sortPlaceToLot(place, lotFrom, testFactory.storedUser(sortingCenter, 1L));

        //Проверяем, что нет исключения
        scanService.transferFromLotToLot(lotFrom.getBarcode(), lotTo.getBarcode(), new ScContext(user));

        place = testFactory.updated(place);
        assertThat(place.getLot()).isEqualTo(lotTo.getLot());
        assertThat(OrderScanLogSomeIdConstraintCheckListener.getHitCounter()).isEqualTo(1);
    }

    @Test
    void getOrderWithLotsIsMultiPlace() {
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var deliveryService = testFactory.storedDeliveryService("123");
        TestFactory.CreateOrderParams createOrderParams =
                order(sortingCenter)
                        .places("p1", "p2")
                        .externalId("o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build();
        var places = testFactory.createOrder(createOrderParams)
                .cancel()
                .acceptPlaces("p1", "p2")
                .makeReturn().getPlaces();
        var apiOrderWithLotsDto = scanService.getOrderWithLots(places.get("p1").getExternalId(), null,
                new ScContext(user, ScanLogContext.SORT_LOT));
        assertThat(apiOrderWithLotsDto.getLotsTo()).isNotEmpty();
        assertThat(apiOrderWithLotsDto.getLotsTo()).containsOnly(new ApiLotBaseDto(
                lot.getLotId(),
                lot.getNameForApi(),
                lot.getBarcode()));
        assertThat(apiOrderWithLotsDto.getPlaces())
                .extracting(p -> p.getCell() != null ? p.getCell().getId() : null)
                .allMatch(Objects::isNull);
        assertThat(
                apiOrderWithLotsDto.getPlaces().stream().map(ApiPlaceLotDto::getExternalId).toList()
        ).containsExactlyInAnyOrder("p1", "p2");
    }

    @Test
    void prepareToShipLotScanLogOk() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept().cancel().accept().getPlace();
        Route route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();
        Cell cell = testFactory.determineRouteCell(route, place);
        lot = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);
        testFactory.sortPlaceToLot(place, lot, user);

        scanService.prepareToShipSortable(
                lot.getLotId(),
                SortableType.PALLET,
                SortableAPIAction.READY_FOR_SHIPMENT,
                new ScContext(user, ScanLogContext.PALLETIZATION)
        );
        List<OrderScanLogEntry> logs = orderScanLogEntryRepository.findAll().stream()
                .filter(o -> Objects.equals(o.getLotBeforeId(), lot.getLotId()))
                .toList();

        assertThat(logs.size()).isEqualTo(1);
        assertThat(logs.get(0).getLotId()).isNotNull();
        assertThat(logs.get(0).getSortableId()).isNotNull();
        assertThat(logs.get(0).getType()).isEqualTo(SortableType.PALLET);
        assertThat(logs.get(0).getOperation()).isEqualTo(ScanLogOperation.PREPARE);
        assertThat(logs.get(0).getResult()).isEqualTo(ScanLogResult.OK);
    }

    @Test
    void prepareToShipLotScanLogError() {
        assertThatThrownBy(() -> scanService.prepareToShipSortable(
                lot.getLotId(),
                SortableType.PALLET,
                SortableAPIAction.READY_FOR_SHIPMENT,
                new ScContext(user, ScanLogContext.PALLETIZATION)
        ));

        List<OrderScanLogEntry> logs = orderScanLogEntryRepository.findAll().stream()
                .filter(o -> Objects.equals(o.getLotBeforeId(), lot.getLotId()))
                .toList();

        assertThat(logs.size()).isEqualTo(1);
        assertThat(logs.get(0).getLotId()).isNotNull();
        assertThat(logs.get(0).getOperation()).isEqualTo(ScanLogOperation.PREPARE);
        assertThat(logs.get(0).getResult()).isEqualTo(ScanLogResult.ERROR);
    }

    @Test
    @SneakyThrows
    void acceptReturnedOrderWhenOrderNotFound() {
        String externalId = "o1";
        var expectedWarehouseReturn = testFactory.storedWarehouse("return-1");
        List<String> numberPlaces = List.of("o1-1", "o1-2", "o2-3");
        var o1 = testFactory.createForToday(order(sortingCenter, externalId)
                        .places(numberPlaces)
                        .warehouseReturnId(expectedWarehouseReturn.getYandexId()).build())
                .acceptPlaces(numberPlaces).sortPlaces(numberPlaces).ship().makeReturn().get();

        var actualSortingCenter = testFactory.storedSortingCenter(665L);
        var actualUser = testFactory.storedUser(actualSortingCenter, 199L);
        setMisdeliveryProcessingProperies(expectedWarehouseReturn, actualSortingCenter);
        var courier = testFactory.storedCourier(2452L);
        ApiOrderDto apiOrderDto = scanService.acceptReturnedOrder(
                buildAcceptReturnedRequest(externalId, courier.getId(), false),
                new ScContext(actualUser)
        );
        var expectedOrder = testFactory.getOrder(o1.getId());
        var expectedPlaces = testFactory.orderPlaces(expectedOrder);
        var actualOrder = testFactory.getOrder(apiOrderDto.getId());
        var actualPlaces = testFactory.orderPlaces(actualOrder);

        assertThat(apiOrderDto)
                .isNotEqualTo(OrderQueryService.notFoundDto(externalId, sortingCenter));
        assertThat(expectedOrder).isNotNull();
        assertThat(expectedPlaces).hasSize(3);
        assertThat(actualOrder).isNotNull();
        assertThat(actualPlaces).hasSize(3);
    }

    private void setMisdeliveryProcessingProperies(Warehouse expectedWarehouseReturn,
                                                   SortingCenter actualSortingCenter) throws JsonProcessingException {
        var misdeliveryProcessingScs = Map.of(
                sortingCenter.getId(), new OrderFlowService.MisdeliveryProcessingProperties(
                        "MSK", 1, OrderFlowService.MisdeliveryReturnDirection.SORTING_CENTER,
                        Objects.requireNonNull(expectedWarehouseReturn.getYandexId()), List.of()
                ),
                actualSortingCenter.getId(), new OrderFlowService.MisdeliveryProcessingProperties(
                        "MSK", 2, OrderFlowService.MisdeliveryReturnDirection.SORTING_CENTER,
                        expectedWarehouseReturn.getYandexId(), List.of()
                )
        );

        configurationService.insertValue(
                ConfigurationProperties.MISDELIVERY_RETURNS_MAPPINGS,
                new ObjectMapper().writeValueAsString(misdeliveryProcessingScs)
        );
    }

    @Test // объяснение тут MARKETTPLSC-2441
    @DisplayName("При создании засыла не указан СД, устанавливаем фэйковую СД")
    @SneakyThrows
    void acceptReturnedOrderWhenOrderNotFoundAndCourierIsNull() {
        var expectedWarehouseReturn = testFactory.storedWarehouse("return-1");
        var p1 = testFactory.createForToday(order(sortingCenter, "o1")
                        .warehouseReturnId(expectedWarehouseReturn.getYandexId()).build())
                .accept().sort().ship()
                .makeReturn()
                .getPlace();

        namedJdbcTemplate.update("update orders set courier = null where id = :id", Map.of("id", p1.getOrderId()));
        namedJdbcTemplate.update("update place set courier = null, in_route_id = null where id = :id", Map.of("id", p1.getId()));
        testFactory.storedCourier(3L, OrderCommandService.MISDELIVERY_COURIER_NAME);

        var actualSortingCenter = testFactory.storedSortingCenter(665L);
        var actualUser = testFactory.storedUser(actualSortingCenter, 199L);
        setMisdeliveryProcessingProperies(expectedWarehouseReturn, actualSortingCenter);

        ApiOrderDto apiOrderDto = scanService.acceptReturnedOrder(
                new AcceptReturnedOrderRequestDto("o1", "o1", null),
                new ScContext(actualUser)
        );

        assertThat(apiOrderDto)
                .isNotEqualTo(OrderQueryService.notFoundDto("o1", sortingCenter));
        assertThat(testFactory.getOrder(apiOrderDto.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

    }

    private AcceptReturnedOrderRequestDto buildAcceptReturnedRequest(String externalId, long courierId,
                                                                     boolean needPlace) {
        return new AcceptReturnedOrderRequestDto(
                externalId,
                needPlace
                        ? testFactory.orderPlace(orderRepository.findAllByExternalId(externalId).get(0)).getExternalId()
                        : null,
                courierId
        );
    }

    @Test
    void acceptLotWithPlaces() {
        var zone = testFactory.storedZone(sortingCenter, "Зона приемки лотов");
        var order = testFactory.create(order(sortingCenter).externalId("o-1")
                .dsType(DeliveryServiceType.TRANSIT)
                .places("p-1", "p-2")
                .deliveryDate(LocalDate.now(clock))
                .shipmentDate(LocalDate.now(clock))
                .build()).get();
        testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.DS_SC)
                .sortingCenter(sortingCenter)
                .inboundExternalId("in-1")
                .registryMap(Map.of("registry_1", List.of(Pair.of("o-1", "p-1"))))
                .placeInPallets(Map.of("p-1", "SC_LOT_1"))
                .palletToStamp(Map.of("SC_LOT_1", "stamp-1"))
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .build()
        );

        ScContext ctx = ScContext.builder()
                .sortingCenter(sortingCenter).user(user).context(ScanLogContext.ACCEPT_LOTS).zone(zone).build();
        scanService.acceptLotWithPlaces(new AcceptLotRequestDto("stamp-1"), ctx);
        OrderScanLogEntry scanLogEntry = StreamEx.of(orderScanLogEntryRepository.findAll())
                .findFirst(log -> log.getContext() == ScanLogContext.ACCEPT_LOTS)
                .orElseThrow();

        assertThat(scanLogEntry.getContext()).isEqualTo(ScanLogContext.ACCEPT_LOTS);
        assertThat(scanLogEntry.getZoneId()).isEqualTo(zone.getId());
    }
}
