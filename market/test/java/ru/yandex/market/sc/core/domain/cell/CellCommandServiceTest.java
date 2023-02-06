package ru.yandex.market.sc.core.domain.cell;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.cell.model.CellCargoType;
import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.model.PartnerCellDto;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServicePropertySource;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSite;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSiteRepository;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehousePropertySource;
import ru.yandex.market.sc.core.domain.zone.model.PartnerZoneDto;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static java.util.Collections.emptyList;
import static one.util.streamex.MoreCollectors.onlyOne;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
class CellCommandServiceTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    RouteSoMigrationHelper routeSoMigrationHelper;
    @Autowired
    CellCommandService cellCommandService;
    @Autowired
    ConfigurationService configurationService;
    @MockBean
    Clock clock;
    @Autowired
    SortingCenterPropertySource sortingCenterPropertySource;
    @Autowired
    WarehousePropertySource warehousePropertySource;
    @Autowired
    DeliveryServicePropertySource deliveryServicePropertySource;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    RouteSoSiteRepository routeSoSiteRepository;
    @Autowired
    TransactionTemplate transactionTemplate;

    SortingCenter sortingCenter;
    SortingCenter sortingCenter2;
    Courier courier1;
    Courier courier2;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        sortingCenter2 = testFactory.storedSortingCenter(2);
        courier1 = testFactory.storedCourier(1);
        courier2 = testFactory.storedCourier(2);
        testFactory.setupMockClock(clock);
    }

    @Test
    void allowToSetWarehouseOnlyForReturnOnCreate() {
        testFactory.storedWarehouse("w1");
        for (var cellType : CellType.values()) {
            var number = "c-" + cellType;
            var request = new CellRequestDto(
                    number, CellStatus.ACTIVE, cellType, CellSubType.DEFAULT, "w1", null, null, null
            );
            ThrowableAssert.ThrowingCallable createCell = () -> cellCommandService.createCellAndBindRoutes(sortingCenter, request);
            if (cellType.isWarehouseBindingAvailable()) {
                assertThatCode(createCell).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(createCell).isInstanceOf(ScException.class);
            }
        }
    }

    @Test
    void allowToSetWarehouseOnlyForReturnOnUpdate() {
        testFactory.storedWarehouse("w1");
        for (var cellType : CellType.values()) {
            var number = "c-" + cellType;
            var cell = testFactory.storedCell(sortingCenter, number, cellType);
            var request = new CellRequestDto(
                    number, CellStatus.ACTIVE, cellType, CellSubType.DEFAULT, "w1", null, null, null
            );
            ThrowableAssert.ThrowingCallable updateCell = () -> cellCommandService.updateCell(
                    sortingCenter, cell.getId(), request);
            if (cellType.isWarehouseBindingAvailable()) {
                assertThatCode(updateCell).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(updateCell).isInstanceOf(ScException.class);
            }
        }
    }

    @Test
    void allowToSetWarehouseForCellWithSameWarehouseRoute() {
        var cell = testFactory.storedCell(sortingCenter, "w1", CellType.RETURN);
        var order = testFactory.createOrderForToday(sortingCenter).cancel().accept().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                                    .orElseThrow();

        route.allowReading();
        assertThat(route.getCells(LocalDate.now(clock))).containsOnly(cell);
        Warehouse warehouseTo = route.getWarehouseTo();
        var request = new CellRequestDto(
                cell.getScNumber(), CellStatus.ACTIVE, cell.getType(),
                CellSubType.DEFAULT, Objects.requireNonNull(warehouseTo).getYandexId(), null, null, null
        );
        route.revokeRouteReading();

        assertThat(cellCommandService.updateCell(sortingCenter, cell.getId(), request).getWarehouseYandexId())
                .isEqualTo(Objects.requireNonNull(warehouseTo).getYandexId());
    }

    @Test
    void allowToSetCourierOnlyForCourierOnCreate() {
        testFactory.storedCourier(1L);
        for (var cellType : CellType.values()) {
            var number = "c-" + cellType;
            var request = new CellRequestDto(
                    number, CellStatus.ACTIVE, cellType, CellSubType.DEFAULT, null, 1L, null, null
            );
            ThrowableAssert.ThrowingCallable createCell = () -> cellCommandService.createCellAndBindRoutes(sortingCenter, request);
            if (cellType.isCourierBindingAvailable()) {
                assertThatCode(createCell).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(createCell).isInstanceOf(ScException.class);
            }
        }
    }

    @Test
    void allowToSetCourierOnlyForCourierOnUpdate() {
        testFactory.storedCourier(1L);
        for (var cellType : CellType.values()) {
            var number = "c-" + cellType;
            var cell = testFactory.storedCell(sortingCenter, number, cellType);
            var request = new CellRequestDto(
                    number, CellStatus.ACTIVE, cellType, CellSubType.DEFAULT, null, 1L, null, null
            );
            ThrowableAssert.ThrowingCallable updateCell = () -> cellCommandService.updateCell(
                    sortingCenter, cell.getId(), request);
            if (cellType.isCourierBindingAvailable()) {
                assertThatCode(updateCell).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(updateCell).isInstanceOf(ScException.class);
            }
        }
    }

    @Test
    void allowToSetCourierForCellWithSameCourierRoute() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        route.allowReading();

        assertThat(route.getCells(LocalDate.now(clock))).containsOnly(cell);
        var request = new CellRequestDto(
                cell.getScNumber(), CellStatus.ACTIVE, cell.getType(),
                CellSubType.DEFAULT, null,
                Objects.requireNonNull(route.getCourierTo()).getId(), null, null
        );
        Long courierId = route.getCourierTo().getId();

        route.revokeRouteReading();
        assertThat(cellCommandService.updateCell(sortingCenter, cell.getId(), request).getCourierId())
                .isEqualTo(courierId);
    }

    @Test
    void createCellWithZone() {
        var zone = testFactory.storedZone(sortingCenter);
        var cellDto = cellCommandService.createCellAndBindRoutes(sortingCenter, new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT, null, null, zone.getId(), null
        ));
        assertThat(cellDto).isEqualToIgnoringGivenFields(
                new PartnerCellDto(1L, sortingCenter.getId(), "c1", null, CellStatus.ACTIVE,
                        CellType.COURIER, CellSubType.DEFAULT, null, null, null, null,
                        new PartnerZoneDto(zone.getId(), sortingCenter.getId(), zone.getName(), false, emptyList()),
                        false, true, true, 0, 0),
                "id"
        );
    }

    @Test
    void cellZoneConditionsValidation() {
        testFactory.setSortingCenterProperty(sortingCenter, BUFFER_RETURNS_ENABLED, true);
        var zone = testFactory.storedZone(sortingCenter);
        assertThatCode(() ->
                testFactory.storedCell(sortingCenter, "c1", CellField.builder()
                        .type(CellType.BUFFER)
                        .subType(CellSubType.BUFFER_RETURNS)
                        .status(CellStatus.ACTIVE)
                        .cargoType(CellCargoType.NONE)
                )
        ).doesNotThrowAnyException();
        assertThatCode(() -> cellCommandService.createCellAndBindRoutes(sortingCenter, new CellRequestDto(
                "c2", CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT, null, null, zone.getId(), null
        ))).doesNotThrowAnyException();
        assertDoesNotThrow(() -> cellCommandService.createCellAndBindRoutes(sortingCenter, new CellRequestDto(
                "c3", CellStatus.ACTIVE, CellType.BUFFER, CellSubType.DEFAULT, null, null, zone.getId(), null
        )));
    }

    @Test
    void cantCreateBufferCellWithZone() {
        var zone = testFactory.storedZone(sortingCenter);
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(sortingCenter, new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.BUFFER,
                CellSubType.DEFAULT, "w1", null, zone.getId(), null
        ))).isInstanceOf(ScException.class);
    }

    @Test
    void cantCreateReturnCellWithZone() {
        var zone = testFactory.storedZone(sortingCenter);
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(sortingCenter, new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.RETURN,
                CellSubType.DEFAULT, "w1", null, zone.getId(), null
        ))).isInstanceOf(ScException.class);
    }

    @Test
    void cantCreateCellInsideDeletedZone() {
        var zone = testFactory.storedDeletedZone(sortingCenter);
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(sortingCenter, new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.COURIER,
                CellSubType.DEFAULT, "w1", null, zone.getId(), null
        ))).isInstanceOf(ScException.class);
    }

    @Test
    void updateCellWithZone() {
        var cell = testFactory.storedCell(sortingCenter, null, CellType.COURIER);
        var zone = testFactory.storedZone(sortingCenter);
        PartnerCellDto cellDto = cellCommandService.updateCell(sortingCenter, cell.getId(), new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT, null, null, zone.getId(), null
        ));
        assertThat(cellDto).isEqualToIgnoringGivenFields(
                new PartnerCellDto(1L, sortingCenter.getId(), "c1", null, CellStatus.ACTIVE,
                        CellType.COURIER, CellSubType.DEFAULT, null, null, null, null,
                        new PartnerZoneDto(zone.getId(), sortingCenter.getId(), zone.getName(), false, emptyList()),
                        false, true, true, 0, 0),
                "id"
        );
    }

    @Test
    void cantUpdateCellWhenItUsedOnDifferentRouteReturn() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().get();
        var route = testFactory.findPossibleOutgoingWarehouseRoute(order).get();
        route.allowNextRead();
        Set<RouteCell> routeCells = route.getRouteCells();
        assertThat(routeCells).hasSize(1);
        var cell = routeCells.stream().findFirst().get().getCell();
        var dto = new CellRequestDto("555", CellStatus.ACTIVE, CellType.RETURN, "newWarehouseYandexId");
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell.getId(), dto))
                .isInstanceOf(ScException.class);
    }

    @Test
    void cantUpdateCellWhenItUsedOnDifferentRouteCourier() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).get();
        route.allowNextRead();
        Set<RouteCell> routeCells = route.getRouteCells();
        assertThat(routeCells).hasSize(1);

        var cell = routeCells.stream().findFirst().get().getCell();
        var dto = new CellRequestDto("555", CellStatus.ACTIVE, CellType.COURIER, 777L);
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell.getId(), dto))
                .isInstanceOf(ScException.class);
    }

    @Test
    void updateScNameForReturnCellWhenItUsed() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().get();
        var route = testFactory.findPossibleOutgoingWarehouseRoute(order).get();
        route.allowNextRead();
        Set<RouteCell> routeCells = route.getRouteCells();

        assertThat(routeCells).hasSize(1);
        var cell = routeCells.stream().findFirst().get().getCell();
        var dto = new CellRequestDto("newSuperName", CellStatus.ACTIVE, CellType.RETURN, cell.getWarehouseYandexId());

        assertThat(cellCommandService.updateCell(sortingCenter, cell.getId(), dto))
                .isEqualTo(new PartnerCellDto(
                        cell.getId(), sortingCenter.getId(), "newSuperName", null,
                        CellStatus.ACTIVE, CellType.RETURN, CellSubType.DEFAULT,
                        cell.getWarehouseYandexId(), null, null, null, null, false, true, true, 0, 0
                ));
    }

    @Test
    void canUpdateBufferCellWithZone() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.BUFFER);
        var zone = testFactory.storedZone(sortingCenter);
        assertDoesNotThrow(() -> cellCommandService.updateCell(sortingCenter, cell.getId(), new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.BUFFER,
                CellSubType.DEFAULT, null, null, zone.getId(), null
        )));
    }

    @Test
    void canUpdateReturnCellWithZone() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var zone = testFactory.storedZone(sortingCenter);
        assertDoesNotThrow(() -> cellCommandService.updateCell(sortingCenter, cell.getId(), new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.RETURN,
                CellSubType.DEFAULT, null, null, zone.getId(), null
        )));
    }

    @Test
    void cantUpdateCellSetDeletedZone() {
        var cell = testFactory.storedCell(sortingCenter, null, CellType.COURIER);
        var zone = testFactory.storedDeletedZone(sortingCenter);
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell.getId(), new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.COURIER,
                CellSubType.DEFAULT, "w1", null, zone.getId(), null
        ))).isInstanceOf(ScException.class);
    }

    @Test
    void createCell() {
        assertThat(cellCommandService.createCellAndBindRoutes(sortingCenter, new CellRequestDto("1", null, null)))
                .isNotNull();
    }

    @Test
    void makeCellActive() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN);
        assertThat(cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("2", CellStatus.NOT_ACTIVE, CellType.RETURN)
        ).getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);

        assertThat(cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("2", CellStatus.ACTIVE, CellType.RETURN)
        ).getStatus()).isEqualTo(CellStatus.ACTIVE);
    }

    @Test
    void updateCell() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN);
        assertThat(cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("2", cell.getStatus(), CellType.COURIER)
        )).isEqualTo(new PartnerCellDto(
                cell.getId(), sortingCenter.getId(), "2", null,
                cell.getStatus(), CellType.COURIER, CellSubType.DEFAULT, null,  null, null, null, null,
                false, true, true, 0, 0
        ));
    }

    @Test
    void updateCellSetExistingNumber() {
        testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        var cell2 = testFactory.storedCell(sortingCenter, "2", CellType.COURIER);
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell2.getId(),
                new CellRequestDto("1", CellStatus.ACTIVE, CellType.COURIER))
        )
                .isInstanceOf(ScException.class);
    }

    @Test
    void createCellSetExistingNumber() {
        testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(sortingCenter,
                new CellRequestDto("1", CellStatus.ACTIVE, CellType.COURIER))
        )
                .isInstanceOf(ScException.class);
    }

    @Test
    void updateCellSetIllegalNumber() {
        testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        var cell2 = testFactory.storedCell(sortingCenter, "Ячейка-1", CellType.COURIER);
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell2.getId(),
                new CellRequestDto("1", CellStatus.ACTIVE, CellType.COURIER))
        )
                .isInstanceOf(ScException.class);
    }

    @Test
    void createCellSetIllegalNumber() {
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(sortingCenter,
                new CellRequestDto("Ячейка-1", CellStatus.ACTIVE, CellType.COURIER))
        )
                .isInstanceOf(ScException.class);
    }

    @Test
    void createCellSetExistingNumberManyDeletedCellWithSameNumber() {
        var cell1 = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        cellCommandService.deleteCell(sortingCenter, cell1.getId());
        var cell2 = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        cellCommandService.deleteCell(sortingCenter, cell2.getId());

        testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(sortingCenter,
                new CellRequestDto("1", CellStatus.ACTIVE, CellType.COURIER))
        )
                .isInstanceOf(ScException.class);
    }

    @Test
    void updateCellSetExistingDeletedNumber() {
        var cell1 = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        cellCommandService.deleteCell(sortingCenter, cell1.getId());

        var cell2 = testFactory.storedCell(sortingCenter, "2", CellType.COURIER);
        assertThat(cellCommandService.updateCell(sortingCenter, cell2.getId(),
                new CellRequestDto("1", CellStatus.ACTIVE, CellType.COURIER))

        ).isEqualTo(new PartnerCellDto(
                cell2.getId(), sortingCenter.getId(), "1", null,
                CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT, null, null,
                null, null, null, false, true, true, 0, 0
        ));
    }

    @Test
    void createCellSetExistingDeletedNumber() {
        var cell1 = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        cellCommandService.deleteCell(sortingCenter, cell1.getId());
        assertThat(cellCommandService.createCellAndBindRoutes(sortingCenter,
                new CellRequestDto("1", CellStatus.ACTIVE, CellType.COURIER))

        ).isEqualToIgnoringGivenFields(new PartnerCellDto(
                0L, sortingCenter.getId(), "1", null,
                CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT,  null, null,
                null, null, null, false, true, true, 0, 0
        ), "id");
    }

    @Test
    void createCellSetCourier() {
        var courier = testFactory.storedCourier(1L);
        assertThat(cellCommandService.createCellAndBindRoutes(sortingCenter,
                new CellRequestDto("1", CellStatus.ACTIVE, CellType.COURIER, 1L))

        ).isEqualToIgnoringGivenFields(new PartnerCellDto(
                0L, sortingCenter.getId(), "1", null,
                CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT,
                null, null, 1L, null, null, false, true, true, 0, 0
        ), "id");
    }

    @Test
    void createCellSetCourierAndWarehouse() {
        testFactory.storedCourier(1L);
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(
                sortingCenter,
                new CellRequestDto("1", CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT, "w1", 1L,
                        null, null)
        )).isInstanceOf(ScException.class);
    }

    @Test
    void deactivateCellWithOrdersInside() {
        var cell = createCourierCellWithSingleOrder(sortingCenter, "1");
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto(cell.getScNumber(), CellStatus.NOT_ACTIVE, CellType.COURIER))
        ).isInstanceOf(ScException.class);
    }

    @Test
    void updateUsedCellNumber() {
        var cell = createCourierCellWithSingleOrder(sortingCenter, "1");
        assertThat(cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("2", CellStatus.ACTIVE, CellType.COURIER))
        ).isEqualTo(new PartnerCellDto(
                cell.getId(), sortingCenter.getId(), "2", null,
                CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT,  null, null,
                null, null, null, false, false, true, 1, 0
        ));
    }

    @Test
    void updateUsedCellType() {
        var cell = createCourierCellWithSingleOrder(sortingCenter, "2");
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("2", CellStatus.ACTIVE, CellType.RETURN))
        ).isInstanceOf(ScException.class);
    }

    @Test
    void unbindCellFromZone() {
        var zone = testFactory.storedZone(sortingCenter);
        var cellDto = cellCommandService.createCellAndBindRoutes(sortingCenter, new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT, null, null, zone.getId(), null
        ));
        PartnerCellDto unbindCellDto = cellCommandService.unbindCellFromZone(sortingCenter, cellDto.getId());
        assertThat(unbindCellDto).isEqualToIgnoringGivenFields(
                new PartnerCellDto(1L, sortingCenter.getId(), "c1", null, CellStatus.ACTIVE,
                        CellType.COURIER, CellSubType.DEFAULT, null, null, null, null, null,
                        false, true, true, 0, 0),
                "id"
        );
    }

    @Test
    void unbindCellFromZoneWithoutZone() {
        var cellDto = cellCommandService.createCellAndBindRoutes(sortingCenter, new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT, null, null, null, null
        ));
        PartnerCellDto unbindCellDto = cellCommandService.unbindCellFromZone(sortingCenter, cellDto.getId());
        assertThat(unbindCellDto).isEqualToIgnoringGivenFields(
                new PartnerCellDto(1L, sortingCenter.getId(), "c1", null, CellStatus.ACTIVE,
                        CellType.COURIER, CellSubType.DEFAULT, null, null, null, null, null,
                        false, true, true, 0, 0),
                "id"
        );
    }

    private Cell createCourierCellWithSingleOrder(SortingCenter sortingCenter, String number) {
        var cell = testFactory.storedCell(sortingCenter, number, CellType.COURIER);
        testFactory.createOrderForToday(sortingCenter).accept().sort(cell.getId());
        return cell;
    }

    @Test
    void updateUsedCellStatusFromActiveToNotActive() {
        var cell = createCourierCellWithSinglePlace(sortingCenter, "1");
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("1", CellStatus.NOT_ACTIVE, CellType.COURIER))
        ).isInstanceOf(ScException.class);
    }

    @Test
    void updateUsedCellStatusWithLot() {
        var cell = createCourierCellWithSinglePlace(sortingCenter, "1");
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("1", CellStatus.NOT_ACTIVE, CellType.COURIER))
        ).isInstanceOf(ScException.class);
    }

    @Test
    void updateCellSetNewTypeAndNewSubtypeOk() {
        var cell = testFactory.storedCell(sortingCenter, "cell-1-courier", CellType.COURIER);
        var partnerCellDto = cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("1", CellStatus.ACTIVE,
                        CellType.BUFFER,
                        CellSubType.DROPPED_ORDERS,
                        null,
                        null,
                        null,
                        null
                ));
        assertThat(partnerCellDto.getType()).isEqualTo(CellType.BUFFER);
        assertThat(partnerCellDto.getSubType()).isEqualTo(CellSubType.DROPPED_ORDERS);
    }

    @Test
    void updateCellSetNewTypeAndNewSubtypeBad() {
        var cell = testFactory.storedCell(sortingCenter, "cell-1-courier", CellType.BUFFER);
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto(
                        "1",
                        CellStatus.ACTIVE,
                        CellType.COURIER,
                        CellSubType.DROPPED_ORDERS,
                        null,
                        null,
                        null,
                        null
                )))
                .isInstanceOf(ScException.class).hasMessage("Subtype DROPPED_ORDERS not supported for COURIER");
    }

    @Test
    void updateUsedCellStatusFromNotActiveToActive() {
        var cell = createCourierCellWithSinglePlace(sortingCenter, null);
        assertThat(cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("1", CellStatus.ACTIVE, CellType.COURIER))
        ).isEqualTo(new PartnerCellDto(
                cell.getId(), sortingCenter.getId(), "1", null,
                CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT, null, null,
                null, null, null, false, false, true, 1, 0
        ));
    }

    @Test
    @DisplayName("Ошибка при назначении на ячейку другого курьера")
    void updateUserCellExceptionWhenAssignAnotherCourier() {
        var courier = testFactory.storedCourier(16L);
        var cell = testFactory.storedCell(sortingCenter, "cell-1-courier", CellType.COURIER, courier.getId());
        testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                .shipmentDate(LocalDate.now(clock))
                .sortingCenter(sortingCenter)
                .build()
        ).updateCourier(courier).accept().sort().get();

        courier2 = testFactory.storedCourier(17L);
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("cell-1-courier", CellStatus.ACTIVE, CellType.COURIER, courier2.getId())))
                .isInstanceOf(ScException.class)
                .hasMessage("Can't update cell courier, cell used " + cell);
    }

    @Test
    @DisplayName("Назначение на ячейку того же курьера и удаление route_cell (кроме сегодня, если ячейка не пуста)")
    void updateUserCellWhenAssignSameCourierAndCleanRoutesCellExceptTodayCellIsNotEmpty() {

        var courier = testFactory.storedCourier(17L);
        var cell = testFactory.storedCell(sortingCenter, "cell-2-courier", CellType.COURIER, courier.getId());
        testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                .shipmentDate(LocalDate.now(clock))
                .sortingCenter(sortingCenter)
                .build()
        ).updateCourier(courier).accept().sort().get();

        routeSoMigrationHelper.allowRouteReading();
        var routeCellExpected = StreamEx.of(testFactory.findRoutesCell(cell))
                .filter(routeCell -> routeCell.getRoute().getExpectedDate().equals(LocalDate.now(clock)))
                .collect(onlyOne())
                .orElseThrow();
        LocalDate date = LocalDate.now(clock).plusDays(1);
        testFactory.storedOutgoingCourierRoute(sortingCenter, cell, courier, date, date);
        testFactory.storedOutgoingCourierRoute(sortingCenter, cell, courier, date.plusDays(2), date.plusDays(2));
        testFactory.storedOutgoingCourierRoute(sortingCenter, cell, courier, date.plusDays(3), date.plusDays(3));
        routeSoMigrationHelper.revokeRouteReadingPermission();

        cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("cell-2-courier", CellStatus.ACTIVE, CellType.COURIER, courier.getId()));
        var routesCell = testFactory.findRoutesCell(cell);

        assertThat(routesCell).hasSize(4);
        assertThat(routesCell.stream().map(RouteCell::getCellId).toList())
                .containsOnly(cell.getId());
    }

    @Test
    @DisplayName("Назначение на ячейку другого курьера и удаление всех route_cell (за сегодня и будущие даты)")
    void updateUserCellWhenAssignAnotherCourierAndCleanAllRoutesCellFutureDate() {
        courier1 = testFactory.storedCourier(17L);
        var cell = testFactory.storedCell(sortingCenter, "cell-2-courier", CellType.COURIER, courier1.getId());

        LocalDate now = LocalDate.now(clock);
        testFactory.storedOutgoingCourierRoute(sortingCenter, cell, courier1, now, now);
        testFactory.storedOutgoingCourierRoute(sortingCenter, cell, courier1, now.plusDays(1), now.plusDays(1));
        testFactory.storedOutgoingCourierRoute(sortingCenter, cell, courier1, now.plusDays(2), now.plusDays(2));
        testFactory.storedOutgoingCourierRoute(sortingCenter, cell, courier1, now.plusDays(3), now.plusDays(3));

        courier2 = testFactory.storedCourier(18L);
        cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("cell-2-courier", CellStatus.ACTIVE, CellType.COURIER, courier2.getId()));
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            List<RouteSoSite> routeSoSites = testFactory.findRouteSoSites(cell);
            //todo: кажется, пустота routeSoSites противоречит следующему выражению из теста createCellWithRouteSo:
//            var routeSoSite3 = routeSoSiteRepository
//                    .findByCellAndReservedFromLessThanEqualAndReservedToGreaterThanEqual(cell, now, now);
//            assertThat(routeSoSite3).isNotEmpty();

            assertThat(routeSoSites).hasSize(1);
        } else {
            var routesCell = testFactory.findRoutesCell(cell);
            assertThat(routesCell).isEmpty();
        }


    }

    @Test
    @DisplayName("Назначение на ячейку того же курьера и сохранение всех route_cell (за сегодня и будущие даты)")
    void updateUserCellWhenAssignSameCourierAndCleanAllRoutesCellFutureDate() {
        var courier = testFactory.storedCourier(17L);
        var cell = testFactory.storedCell(sortingCenter, "cell-2-courier", CellType.COURIER, courier.getId());

        LocalDate now = LocalDate.now(clock);
        var route0 = testFactory.storedOutgoingCourierRoute(
                sortingCenter, cell, courier, now, now);
        var route1 = testFactory.storedOutgoingCourierRoute(
                sortingCenter, cell, courier, now.plusDays(1), now.plusDays(1));
        var route2 = testFactory.storedOutgoingCourierRoute(
                sortingCenter, cell, courier, now.plusDays(2), now.plusDays(2));
        var route3 = testFactory.storedOutgoingCourierRoute(
                sortingCenter, cell, courier, now.plusDays(3), now.plusDays(3));

        cellCommandService.updateCell(sortingCenter, cell.getId(),
                new CellRequestDto("cell-2-courier", CellStatus.ACTIVE, CellType.COURIER, courier.getId()));

        route0.allowReading();
        route1.allowReading();
        route2.allowReading();
        route3.allowReading();

        route0 = testFactory.getRoute(route0.getId());
        route1 = testFactory.getRoute(route1.getId());
        route2 = testFactory.getRoute(route2.getId());
        route3 = testFactory.getRoute(route3.getId());

        assertThat(route0.getRouteCells().stream().map(RouteCell::getCellId).toList())
                .isEqualTo(List.of(cell.getId()));
        assertThat(route1.getRouteCells().stream().map(RouteCell::getCellId).toList())
                .isEqualTo(List.of(cell.getId()));
        assertThat(route2.getRouteCells().stream().map(RouteCell::getCellId).toList())
                .isEqualTo(List.of(cell.getId()));
        assertThat(route3.getRouteCells().stream().map(RouteCell::getCellId).toList())
                .isEqualTo(List.of(cell.getId()));
    }

    @Test
    @DisplayName("Назначение на ячейку курьера и сохранение всех route_cell (за сегодня и будущие даты)")
    void createCourierCellUpdateRoutesCellsFutureDate() {
        var courier = testFactory.storedCourier(17L);
        LocalDate now = LocalDate.now(clock);
        var route0 = testFactory.storedOutgoingCourierRoute(
                sortingCenter, null, courier, now, now);
        var route1 = testFactory.storedOutgoingCourierRoute(
                sortingCenter, null, courier, now.plusDays(1), now.plusDays(1));
        var route2 = testFactory.storedOutgoingCourierRoute(
                sortingCenter, null, courier, now.plusDays(2), now.plusDays(2));
        var route3 = testFactory.storedOutgoingCourierRoute(
                sortingCenter, null, courier, now.plusDays(3), now.plusDays(3));
        var cell = testFactory.getCell(
                cellCommandService.createCellAndBindRoutes(sortingCenter,
                                new CellRequestDto("cell-2-courier", CellStatus.ACTIVE,
                                        CellType.COURIER, courier.getId()))
                        .getId()
        );
        var route0new = testFactory.getRoute(route0.getId());
        var route1new = testFactory.getRoute(route1.getId());
        var route2new = testFactory.getRoute(route2.getId());
        var route3new = testFactory.getRoute(route3.getId());

        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            transactionTemplate.execute(t -> {
                assertThat(testFactory.getRouteSo(route0new).getRouteSoSites().stream().map(RouteSoSite::getCellId).toList())
                        .isEqualTo(List.of(cell.getId()));
                assertThat(testFactory.getRouteSo(route1new).getRouteSoSites().stream().map(RouteSoSite::getCellId).toList())
                        .isEqualTo(List.of(cell.getId()));
                assertThat(testFactory.getRouteSo(route2new).getRouteSoSites().stream().map(RouteSoSite::getCellId).toList())
                        .isEqualTo(List.of(cell.getId()));
                assertThat(testFactory.getRouteSo(route3new).getRouteSoSites().stream().map(RouteSoSite::getCellId).toList())
                        .isEqualTo(List.of(cell.getId()));
                return null;
            });

        } else {
            assertThat(route0new.getRouteCells().stream().map(RouteCell::getCellId).toList())
                    .isEqualTo(List.of(cell.getId()));
            assertThat(route1new.getRouteCells().stream().map(RouteCell::getCellId).toList())
                    .isEqualTo(List.of(cell.getId()));
            assertThat(route2new.getRouteCells().stream().map(RouteCell::getCellId).toList())
                    .isEqualTo(List.of(cell.getId()));
            assertThat(route3new.getRouteCells().stream().map(RouteCell::getCellId).toList())
                    .isEqualTo(List.of(cell.getId()));
        }
    }

    @Test
    @DisplayName("Назначение на ячейку склада и сохранение всех route_cell (за сегодня и будущие даты)")
    void createWarehouseCellUpdateRoutesCellsFutureDate() {
        var warehouse = testFactory.storedWarehouse("w1");
        LocalDate now = LocalDate.now(clock);
        var route0 = testFactory.storedOutgoingWarehouseRoute(
                now, sortingCenter, warehouse);
        var route1 = testFactory.storedOutgoingWarehouseRoute(
                now.plusDays(1), sortingCenter, warehouse);
        var route2 = testFactory.storedOutgoingWarehouseRoute(
                now.plusDays(2), sortingCenter, warehouse);
        var route3 = testFactory.storedOutgoingWarehouseRoute(
                now.plusDays(3), sortingCenter, warehouse);
        var cell = testFactory.getCell(
                cellCommandService.createCellAndBindRoutes(sortingCenter,
                                new CellRequestDto("cell-2-warehouse", CellStatus.ACTIVE,
                                        CellType.RETURN, warehouse.getYandexId()))
                        .getId()
        );
        var route0new = testFactory.getRoute(route0.getId());
        var route1new = testFactory.getRoute(route1.getId());
        var route2new = testFactory.getRoute(route2.getId());
        var route3new = testFactory.getRoute(route3.getId());

        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            transactionTemplate.execute(t -> {
                assertThat(testFactory.getRouteSo(route0new).getRouteSoSites().stream().map(RouteSoSite::getCellId).toList())
                        .isEqualTo(List.of(cell.getId()));
                assertThat(testFactory.getRouteSo(route1new).getRouteSoSites().stream().map(RouteSoSite::getCellId).toList())
                        .isEqualTo(List.of(cell.getId()));
                assertThat(testFactory.getRouteSo(route2new).getRouteSoSites().stream().map(RouteSoSite::getCellId).toList())
                        .isEqualTo(List.of(cell.getId()));
                assertThat(testFactory.getRouteSo(route3new).getRouteSoSites().stream().map(RouteSoSite::getCellId).toList())
                        .isEqualTo(List.of(cell.getId()));
                return null;
            });

        } else {
            assertThat(route0new.getRouteCells().stream().map(RouteCell::getCellId).toList())
                    .isEqualTo(List.of(cell.getId()));
            assertThat(route1new.getRouteCells().stream().map(RouteCell::getCellId).toList())
                    .isEqualTo(List.of(cell.getId()));
            assertThat(route2new.getRouteCells().stream().map(RouteCell::getCellId).toList())
                    .isEqualTo(List.of(cell.getId()));
            assertThat(route3new.getRouteCells().stream().map(RouteCell::getCellId).toList())
                    .isEqualTo(List.of(cell.getId()));
        }
    }

    @Test
    void validateUpdateSubTypeReturn() {
        var cell1 = testFactory.storedCell(sortingCenter, "1", CellType.RETURN);
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell1.getId(),
                new CellRequestDto(
                        "2", CellStatus.NOT_ACTIVE, CellType.RETURN, CellSubType.DROPPED_ORDERS, null, null, null, null
                ))).isInstanceOf(ScException.class)
                .hasMessage("Subtype DROPPED_ORDERS not supported for RETURN");
        var cell2 = testFactory.storedCell(sortingCenter, "2", CellType.COURIER);
        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, cell2.getId(),
                new CellRequestDto(
                        "3", CellStatus.NOT_ACTIVE, CellType.COURIER, CellSubType.DROPPED_ORDERS, null, null, null, null
                ))).isInstanceOf(ScException.class)
                .hasMessage("Subtype DROPPED_ORDERS not supported for COURIER");
        var cell3 = testFactory.storedCell(sortingCenter, "3", CellType.BUFFER);
        assertThat(cellCommandService.updateCell(sortingCenter, cell3.getId(),
                new CellRequestDto(
                        "4", CellStatus.NOT_ACTIVE, CellType.BUFFER, CellSubType.DROPPED_ORDERS, null, null, null, null
                )).getSubType()).isEqualTo(CellSubType.DROPPED_ORDERS);
    }

    @Test
    void allowDeleteCellWhenCellIsEmptyButRouteExist() {
        var warehouse = testFactory.storedWarehouse("w1");
        var cell = testFactory.storedCell(sortingCenter, CellType.RETURN, warehouse, "c1");
        var order = testFactory.createForToday(TestFactory.CreateOrderParams.builder()
                .warehouseFromId(warehouse.getYandexId())
                .warehouseReturnId(warehouse.getYandexId())
                .sortingCenter(sortingCenter)
                .build()
        ).accept().cancel().get();
        var routeBefore = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).get();
        routeBefore.allowNextRead();
        Set<RouteCell> routeCells = routeBefore.getRouteCells();
        var routeBeforeCells = routeCells.stream().map(RouteCell::getCell).collect(Collectors.toSet());
        if (!routeBeforeCells.contains(cell)) {
            throw new RuntimeException("Invalid test data");
        }

        assertThat(
                cellCommandService.deleteCell(
                        sortingCenter, routeCells.stream().findFirst().get().getCellId(), false
                )).extracting(PartnerCellDto::isDeleted).isEqualTo(true);
        var routeAfter = testFactory.findOutgoingWarehouseRoute(
                testFactory.getOrderLikeForRouteLookup(order)
        ).get();

        routeAfter.allowNextRead();
        var routeAfterCells = routeAfter.getRouteCells().stream().map(RouteCell::getCell).collect(Collectors.toSet());
        assertThat(routeAfterCells).isEmpty();
    }

    @Test
    @DisplayName("Проверка обязательности указания порядка обхода")
    void cantSaveBufferReturnCellWithoutSequenceNumber() {
        testFactory.setSortingCenterProperty(sortingCenter, BUFFER_RETURNS_ENABLED, true);
        configurationService.mergeValue(ConfigurationProperties.CELL_ADD_SEQUENCE_NUMBER_ENABLED, "true");
        var request = getBufferReturnsRequestDto("BUFFER_RETURNS", null);
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(sortingCenter, request))
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.CELL_SEQUENCE_NUMBER_REQUIRED.name());
    }

    @Test
    @DisplayName("Проверка уникальности порядка обхода в рамках одного СЦ")
    void cantSaveDuplicateSequenceNumber() {
        testFactory.setSortingCenterProperty(sortingCenter, BUFFER_RETURNS_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter2, BUFFER_RETURNS_ENABLED, true);
        configurationService.mergeValue(ConfigurationProperties.CELL_ADD_SEQUENCE_NUMBER_ENABLED, "true");

        long sequenceNumber = 1;
        var request = getBufferReturnsRequestDto("BUFFER_RETURNS", sequenceNumber);
        PartnerCellDto cellDto = cellCommandService.createCellAndBindRoutes(sortingCenter, request);
        assertCreated(sortingCenter.getId(), sequenceNumber, cellDto);
        var request2 = getBufferReturnsRequestDto("BUFFER_RETURNS_2", sequenceNumber);
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(sortingCenter, request2))
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.CELL_SEQUENCE_NUMBER_EXISTS.name());

        PartnerCellDto cellDto2 = cellCommandService.createCellAndBindRoutes(sortingCenter2, request);
        assertCreated(sortingCenter2.getId(), sequenceNumber, cellDto2);
    }

    private CellRequestDto getBufferReturnsRequestDto(String number, Long sequenceNumber) {
        return new CellRequestDto(
                number, CellStatus.ACTIVE,
                CellType.BUFFER, CellSubType.BUFFER_RETURNS, null, null, null,
                sequenceNumber, null, null, null, null, CellCargoType.MGT
        );
    }

    private void assertCreated(long scId, long sequenceNumber, PartnerCellDto cellDto) {
        assertThat(cellDto).isEqualToIgnoringGivenFields(
                new PartnerCellDto(1L, scId, "BUFFER_RETURNS", sequenceNumber, CellStatus.ACTIVE,
                        CellType.BUFFER, CellSubType.BUFFER_RETURNS, null, null, null, null,
                        null, false, true, true, 0, null, null, null, null, 0, CellCargoType.MGT),
                "id"
        );
    }

    @SuppressWarnings("SameParameterValue")
    private Cell createCourierCellWithSinglePlace(SortingCenter sortingCenter, String number) {
        var cell = testFactory.storedCell(sortingCenter, number, CellType.COURIER);
        testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1").sortPlaces("1");
        return cell;
    }

    @Test
    @DisplayName("Создание обезличенной ячейки")
    void createImpermanentCell() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.IMPERMANENT_ENABLED, true);
        CellRequestDto cellRequestDto = new CellRequestDto(
                "cell-impermanence",
                CellStatus.ACTIVE,
                CellType.RETURN,
                CellSubType.IMPERMANENT,
                null,
                null,
                null,
                null);
        cellCommandService.createCellAndBindRoutes(sortingCenter, cellRequestDto);

        var cells = testFactory.findAllCells();
        assertThat(cells).hasSize(1);

        var cellCreated = cells.get(0);
        assertThat(cellCreated.getType()).isEqualTo(CellType.RETURN);
        assertThat(cellCreated.getSubtype()).isEqualTo(CellSubType.IMPERMANENT);
        assertThat(cellCreated.getWarehouseYandexId()).isNull();
        assertThat(cellCreated.getCourierId()).isNull();
    }

    @Test
    @DisplayName("Нельзя закрепить мерчанта за ячейкой")
    void notAssignWarehouseForImpermanentCell() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.IMPERMANENT_ENABLED, true);
        CellRequestDto cellRequestDto = new CellRequestDto(
                "cell-impermanence",
                CellStatus.ACTIVE,
                CellType.RETURN,
                CellSubType.IMPERMANENT,
                "merchant-ivanov",
                null,
                null,
                null);

        assertThatThrownBy(
                () -> cellCommandService.createCellAndBindRoutes(sortingCenter, cellRequestDto),
                CellSubType.IMPERMANENT + " mustn't have reference on warehouse!");
    }

    @Test
    @DisplayName("Не обновлять информацию по ячейке, если привязаны активные лоты")
    void exceptionWhenUpdateCellInfoLinkLots() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        var clientReturnCell = testFactory.storedActiveCell(
                sortingCenter, CellType.RETURN, CellSubType.CLIENT_RETURN, "c1");
        testFactory.createForToday(
                        order(sortingCenter, "o1")
                                .isClientReturn(true)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build()
                )
                .accept()
                .sort()
                .sortToLot("SC_LOT_1", SortableType.PALLET)
                .get();

        CellRequestDto request = CellRequestDto.builder()
                .number(clientReturnCell.getScNumber())
                .status(clientReturnCell.getStatus())
                .type(clientReturnCell.getType())
                .subType(CellSubType.DEFAULT)
                .warehouseYandexId(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId())
                .courierId(clientReturnCell.getCourierId())
                .sequenceNumber(clientReturnCell.getSequenceNumber())
                .build();

        assertThatThrownBy(() -> cellCommandService.updateCell(sortingCenter, clientReturnCell.getId(), request))
                .isInstanceOf(ScException.class)
                .hasMessage(ScErrorCode.CANT_UPDATE_CELL_LINK_LOTS.getMessage());
    }

    @Test
    @DisplayName("Запрет на удаление ячейки, к которой привязаны не отгруженные лоты")
    void exceptionWhenDeleteCellLinkLots() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var order = testFactory.createForToday(order(sortingCenter, "o1")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept()
                .enableSortMiddleMileToLot()
                .get();

        // site_id пересекается с cell_id
        jdbcTemplate.query("select setval('cell_id_seq', 665)", rs -> true);

        var cell = testFactory.storedActiveCell(sortingCenter, CellType.COURIER);
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        route.allowReading();
        testFactory.addRouteCell(route, cell, LocalDate.now(clock));
        route.revokeRouteReading();

        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        testFactory.sortOrderToLot(order, lot, user);

        assertThatThrownBy(() -> cellCommandService.deleteCell(sortingCenter, cell.getId()))
                .isInstanceOf(ScException.class)
                .hasMessage("Can't delete cell with active lots");
    }

    @Test
    @DisplayName("success update cell привязываем ячейку утилизации к утилизатору")
    public void successUpdateCellBindUtilizatorToUtilizationCell() {
        var whYandexId = "w1";
        testFactory.storedWarehouse(whYandexId, WarehouseType.UTILIZATOR);
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN, CellSubType.UTILIZATION);
        var updateCell = cellCommandService.updateCell(sortingCenter, cell.getId(), new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.RETURN,
                CellSubType.UTILIZATION, whYandexId, null, null, null
        ));
        assertThat(updateCell.getType()).isEqualTo(CellType.RETURN);
        assertThat(updateCell.getWarehouseYandexId()).isNotNull();
    }

    @Test
    @DisplayName("fail update cell  не можем привязать мерча к ячейки утилизации")
    public void failUpdateCellBindShopToUtilizationCell() {
        var whYandexId = "w1";
        testFactory.storedWarehouse(whYandexId, WarehouseType.SHOP);
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN, CellSubType.UTILIZATION);
        assertThatThrownBy(
                () -> cellCommandService.updateCell(sortingCenter, cell.getId(), new CellRequestDto(
                        "c1", CellStatus.ACTIVE, CellType.RETURN,
                        CellSubType.UTILIZATION, whYandexId, null, null, null
                ))).isInstanceOf(ScException.class);
    }

    @Test
    @DisplayName("fail update cell не можем привязать к утилизатору к обычную ячейки мерча")
    public void failUpdateCellBindUtilizatorToNonUtilizationCell() {
        var whYandexId = "w1";
        testFactory.storedWarehouse(whYandexId, WarehouseType.UTILIZATOR);
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN, CellSubType.DEFAULT);
        assertThatThrownBy(
                () -> cellCommandService.updateCell(sortingCenter, cell.getId(), new CellRequestDto(
                        "c1", CellStatus.ACTIVE, CellType.RETURN,
                        CellSubType.DEFAULT, whYandexId, null, null, null
                ))).isInstanceOf(ScException.class);
    }

    @Test
    @DisplayName("fail created cell не можем привязать к утилизатору к обычную ячейки мерча")
    void failCreateCellWithUtilizatorToNonUtilizationCell() {
        var number = "c-1";
        var whYandexId = "w1";
        testFactory.storedWarehouse(whYandexId, WarehouseType.UTILIZATOR);
        var request = new CellRequestDto(
                number, CellStatus.ACTIVE, CellType.RETURN, CellSubType.DEFAULT,
                whYandexId, null, null, null
        );
        ThrowableAssert.ThrowingCallable createCell = () -> cellCommandService.createCellAndBindRoutes(sortingCenter, request);
        assertThatThrownBy(createCell).isInstanceOf(ScException.class);
    }

    @Test
    @DisplayName("fail created cell не можем привязать к мерчу ячейку утилизации")
    void failCreateCellWithShopToUtilizationCell() {
        var number = "c-1";
        var whYandexId = "w1";
        testFactory.storedWarehouse(whYandexId, WarehouseType.SHOP);
        var request = new CellRequestDto(
                number, CellStatus.ACTIVE, CellType.RETURN, CellSubType.UTILIZATION,
                whYandexId, null, null, null
        );
        ThrowableAssert.ThrowingCallable createCell = () -> cellCommandService.createCellAndBindRoutes(sortingCenter, request);
        assertThatThrownBy(createCell).isInstanceOf(ScException.class);
    }

    @Test
    @DisplayName("success created cell привязываем утилизатора к ячейки утилизации")
    void successCreateCellWithUtilizatorToUtilizationCell() {
        testFactory.storedCourier(1L);
        var number = "c-1";
        var whYandexId = "w1";
        testFactory.storedWarehouse(whYandexId, WarehouseType.UTILIZATOR);
        var request = new CellRequestDto(
                number, CellStatus.ACTIVE, CellType.RETURN, CellSubType.UTILIZATION,
                whYandexId, null, null, null
        );
        var updateCell = cellCommandService.createCellAndBindRoutes(sortingCenter, request);
        assertThat(updateCell.getType()).isEqualTo(CellType.RETURN);
        assertThat(updateCell.getWarehouseYandexId()).isNotNull();
    }

    @Test
    void createCellWithRouteSo() {
        var createRequest = new CellRequestDto(
                "c1", CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT,
                null, courier1.getId(), null, null
        );
        var now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        var cellDto = cellCommandService.createCellAndBindRoutes(sortingCenter, createRequest);
        Cell cell = testFactory.findCell(cellDto.getId());
        var routeSoSite = routeSoSiteRepository
                .findByCellAndReservedFromLessThanEqualAndReservedToGreaterThanEqual(cell, now, now);
        assertThat(routeSoSite).isNotEmpty();

        //courierId не меняется
        var updateRequest1 = new CellRequestDto(
                "c2", CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT,
                null, courier1.getId(), null, null
        );
        cellCommandService.updateCell(sortingCenter, cell.getId(), updateRequest1);
        var routeSoSite2 = routeSoSiteRepository
                .findByCellAndReservedFromLessThanEqualAndReservedToGreaterThanEqual(cell, now, now);
        assertThat(routeSoSite2).isNotEmpty();
        assertThat(routeSoSite2.size()).isEqualTo(1);
        assertThat(routeSoSite.get(0).getRoute()).isEqualTo(routeSoSite2.get(0).getRoute());

        //courierId меняется
        var updateRequest2 = new CellRequestDto(
                "c_1", CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT,
                null, courier2.getId(), null, null
        );
        cellCommandService.updateCell(sortingCenter, cell.getId(), updateRequest2);
        var routeSoSite3 = routeSoSiteRepository
                .findByCellAndReservedFromLessThanEqualAndReservedToGreaterThanEqual(cell, now, now);
        assertThat(routeSoSite3).isNotEmpty();
        assertThat(routeSoSite3.size()).isEqualTo(1);
        assertThat(routeSoSite.get(0).getRoute()).isNotEqualTo(routeSoSite3.get(0).getRoute());
    }
}
