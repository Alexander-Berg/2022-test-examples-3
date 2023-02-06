package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.lot.jdbc.LotJdbcRepository;
import ru.yandex.market.sc.core.domain.lot.model.ApiSortableDto;
import ru.yandex.market.sc.core.domain.lot.repository.LotSize;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.lot.tm.PushDoSortableToTmService;
import ru.yandex.market.sc.core.domain.order.jdbc.OrderJdbcRepository;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.place.jdbc.PlaceJdbcRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.scan_log.repository.OrderScanLogEntryRepository;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.BarcodeType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableBarcodeRepository;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.external.tm.TmClient;
import ru.yandex.market.sc.core.external.tm.model.PutScStateRequest;
import ru.yandex.market.sc.core.external.tm.model.TmSortable;
import ru.yandex.market.sc.core.external.tm.model.TmSortableType;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.util.JacksonUtil;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StampAndLotControllerTest {


    private static final long UID = 124L;
    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final SortableRepository sortableRepository;
    private final SortableQueryService sortableQueryService;
    private final OutboundRepository outboundRepository;
    private final SortableLotService sortableLotService;
    private final SortingCenterRepository sortingCenterRepository;
    private final SortingCenterPropertySource sortingCenterPropertySource;
    private final LotJdbcRepository lotJdbcRepository;
    private final OrderJdbcRepository orderJdbcRepository;
    private final PlaceJdbcRepository placeJdbcRepository;
    private final OrderScanLogEntryRepository orderScanLogEntryRepository;
    private final SortableBarcodeRepository sortableBarcodeRepository;
    private final TmClient tmClient;
    PushDoSortableToTmService pushDoSortableToTmService;

    @Autowired
    private ConfigurationProvider configurationProvider;
    private User user;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    TestFactory.CourierWithDs courierWithDs;
    private final long sortingCenterToId = 111746283111L;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, UID, UserRole.SENIOR_STOCKMAN);
        TestFactory.setupMockClock(clock);
        testFactory.setSortingCenterProperty(sortingCenter.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);
        testFactory.increaseScOrderId();
        pushDoSortableToTmService = new PushDoSortableToTmService(
                sortingCenterRepository,
                sortingCenterPropertySource,
                configurationProvider,
                lotJdbcRepository,
                placeJdbcRepository,
                tmClient,
                clock
        );
        courierWithDs = testFactory.magistralCourier(String.valueOf(sortingCenterToId));
    }


    @DisplayName("Добавление пломбы на лот. Настройка включена")
    @Test
    void addStamp() throws Exception {
        testFactory.increaseScOrderId();
        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                lot.getLot().getSize()
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        var sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        var barcode = sortable.getSortableBarcode("simpleStamp").orElseThrow();
        assertThat(barcode.getSortable()).isEqualTo(sortable);
        assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);
    }

    @DisplayName("Добавление пломбы на лот, попытка отменить заказ (не даем это сделать). Настройка включена")
    @Test
    void addStampAndTryToCancelOrder() throws Exception {
        testFactory.increaseScOrderId();
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, true);
        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                lot.getLot().getSize()
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        var sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        var barcode = sortable.getSortableBarcode("simpleStamp").orElseThrow();
        assertThat(barcode.getSortable()).isEqualTo(sortable);
        assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);
        final ScOrder order = order1;
        assertThatThrownBy(() -> testFactory.cancelOrder(order.getId()));
        order1 = testFactory.getOrder(order1.getId());
        assertThat(order1.getStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("Добавление пломбы на лот, попытка отменить заказ (не даем это сделать). Одноместный заказ. " +
            "Настройка включена")
    @Test
    void addStampAndTryToCancelOrderNonMultiPlaceOrder() throws Exception {
        testFactory.increaseScOrderId();
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, true);
        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).accept().sort().get();
        testFactory.sortOrderToLot(order1, lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                lot.getLot().getSize()
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        var sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        var barcode = sortable.getSortableBarcode("simpleStamp").orElseThrow();
        assertThat(barcode.getSortable()).isEqualTo(sortable);
        assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);
        final ScOrder order = order1;
        assertThatThrownBy(() -> testFactory.cancelOrder(order.getId()));
        order1 = testFactory.getOrder(order1.getId());
        assertThat(order1.getStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("Нельзя отсортировать отмененный заказ в лот на дропоффе")
    @Test
    void cantSortCanceledOrderToLot() {
        testFactory.increaseScOrderId();
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, true);
        Cell parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.COURIER);
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1")
                        .dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().cancel().get();
        assertThatThrownBy(() -> testFactory.sortOrderToLot(order1, lot, user))
                .hasMessage(ScErrorCode.CANT_SORT_CANCELED_ORDER_TO_LOT.getMessage());
    }

    @DisplayName("Нельзя отсортировать неполный многоместный в лот на дропоффе")
    @Test
    void cantSortIncompleteMultiPlaceOrderToLotOnDropoff() {
        testFactory.increaseScOrderId();
        Cell parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.COURIER);
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlace("11").sortPlace("11").get();
        assertThatThrownBy(() -> testFactory.sortToLot(order1, "11", lot, user))
                .hasMessage(ScErrorCode.CANT_SORT_INCOMPLETE_MULTIPLACE_TO_LOT.getMessage());
    }

    @DisplayName("Добавление пломбы на лот и затем ее же удаление. Настройка включена")
    @Test
    void addStampAndThenDeleteStamp() throws Exception {
        testFactory.increaseScOrderId();
        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService()).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                lot.getLot().getSize()
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        var sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        var barcode = sortable.getSortableBarcode("simpleStamp").orElseThrow();
        assertThat(barcode.getSortable()).isEqualTo(sortable);
        assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);
        //теперь удаляем пломбу
        expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.PROCESSING,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.ADD_STAMP),
                false,
                lot.getLot().getSize()
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/deleteStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        sortable = sortableRepository.findByIdOrThrow(sortable.getId());
        assertThat(sortable.hasBarcode(BarcodeType.STAMP)).isFalse();
    }

    @SneakyThrows
    @DisplayName("Попробовать запломбировать лот когда еще не все посылки заказа находятся в лотах")
    @Test
    void addStampWhenNotAllPlacesAreInLots() {
        testFactory.increaseScOrderId();
        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        testFactory.sortToLot(order1, "11", lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                LotSize.NORMAL
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value(String.format("Нельзя запломбировать лот. " +
                                "Есть заказы которые помещены в лот неполностью. " +
                                "Проблемные заказы %s, ...", order1.getExternalId())));
        var barcode = sortableBarcodeRepository
                .findBySortingCenterAndBarcode(sortingCenter, "simpleStamp");
        assertThat(barcode.isEmpty()).isTrue();
        var sortable = sortableRepository.findBySortingCenterAndBarcode(sortingCenter, lot.getBarcode())
                .orElseThrow();
        assertThat(sortable.getStatus()).isEqualTo(SortableStatus.KEEPED_DIRECT);
    }


    @DisplayName("Добавить пломбу на лот и затем попробовать удалить несуществующую пломбу. Настройка включена")
    @Test
    void addStampAndThenDeleteNonExistingStamp() throws Exception {
        testFactory.increaseScOrderId();

        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                null
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        var sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        var barcode = sortable.getSortableBarcode("simpleStamp").orElseThrow();
        assertThat(barcode.getSortable()).isEqualTo(sortable);
        assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);
        //теперь удаляем пломбу
        expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.PROCESSING,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.ADD_STAMP),
                false,
                LotSize.NORMAL
        );
        String errorMessage = String.format("cant delete stamp %s for sortable %s, because there is no such stamp",
                "nonExistingStamp", lot.getSortable().getId());
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/deleteStamp?stampId=nonExistingStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value(errorMessage));
        sortable = sortableRepository.findByIdOrThrow(sortable.getId());
        assertThat(sortable.hasBarcode(BarcodeType.STAMP)).isTrue();
    }

    @DisplayName("Добавление пломбы на лот и затем ее же удаление. Потом добавляем новую пломбу. Настройка включена")
    @Test
    void addStampAndThenDeleteStampAndThenAddNewONe() throws Exception {
        testFactory.increaseScOrderId();
        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                lot.getLot().getSize()
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        var sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        var barcode = sortable.getSortableBarcode("simpleStamp").orElseThrow();
        assertThat(barcode.getSortable()).isEqualTo(sortable);
        assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);
        //теперь удаляем пломбу
        expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.PROCESSING,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.ADD_STAMP),
                false,
                lot.getLot().getSize()
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/deleteStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        sortable = sortableRepository.findByIdOrThrow(sortable.getId());
        assertThat(sortable.hasBarcode(BarcodeType.STAMP)).isFalse();
        //добавляем новую пломбу
        expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                lot.getLot().getSize()
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=anotherOneStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        barcode = sortable.getSortableBarcode("anotherOneStamp").orElseThrow();
        assertThat(barcode.getSortable()).isEqualTo(sortable);
        assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);
    }

    @SneakyThrows
    @DisplayName("Отправка стейта в тм со списком лотов, которые готовы к отгрузке")
    @Test
    void pustStateToTmFromDropoff() {
        long logisticPointToId = 5378264623L;
        testFactory.increaseScOrderId();
        testFactory.setDeliveryServiceProperty(courierWithDs.deliveryService(),
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(sortingCenter.getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId(String.valueOf(logisticPointToId))
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);

        testFactory.increaseScOrderId();
        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .deliveryService(courierWithDs.deliveryService()).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                lot.getLot().getSize()
        );
        final String stamp = "simpleStamp";
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=" + stamp)
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        var sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        var barcode = sortable.getSortableBarcode("simpleStamp").orElseThrow();
        AssertionsForClassTypes.assertThat(barcode.getSortable()).isEqualTo(sortable);
        AssertionsForClassTypes.assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);

        PutScStateRequest actualRequest = pushDoSortableToTmService.prepareDropoffRequest(sortingCenter.getId(), LocalDateTime.now(clock));
        PutScStateRequest expectedRequest = PutScStateRequest.ofSortables(List.of(new TmSortable(
                stamp,
                null,
                Map.of("referenceId", lot.getBarcode()),
                TmSortableType.LOT,
                logisticPointToId,
                LocalDateTime.now(clock)
        )));
        Assertions.assertThat(actualRequest).isEqualTo(expectedRequest);
    }

    @SneakyThrows
    @DisplayName("Отправка стейта в тм со списком лотов и посылок, которые готовы к отгрузке")
    @Test
    void pustStateToTmFromDropoffWithPlaces() {
        long logisticPointToId = 5378264623L;
        testFactory.increaseScOrderId();
        testFactory.setDeliveryServiceProperty(courierWithDs.deliveryService(),
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(sortingCenter.getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId(String.valueOf(logisticPointToId))
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);


        testFactory.increaseScOrderId();

        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .deliveryService(courierWithDs.deliveryService()).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var order2 = testFactory.createForToday(
                order(sortingCenter, "2").places("21", "22")
                        .deliveryService(courierWithDs.deliveryService()).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("21", "22").sortPlaces("21", "22").get();
        var order3 = testFactory.createForToday(
                order(sortingCenter, "3").places("31", "32")
                        .deliveryService(courierWithDs.deliveryService()).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("31", "32").sortPlaces("31").get();
        var order4 = testFactory.createForToday(
                order(sortingCenter, "4").places("41", "42")
                        .deliveryService(courierWithDs.deliveryService()).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("41").sortPlaces("41").get();
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                lot.getLot().getSize()
        );
        final String stamp = "simpleStamp";
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=" + stamp)
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        var sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        var barcode = sortable.getSortableBarcode("simpleStamp").orElseThrow();
        AssertionsForClassTypes.assertThat(barcode.getSortable()).isEqualTo(sortable);
        AssertionsForClassTypes.assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);


        PutScStateRequest actualRequest = pushDoSortableToTmService.prepareDropoffRequest(sortingCenter.getId(), LocalDateTime.now(clock));
        Assertions.assertThat(actualRequest.getBags()).isNull();
        Assertions.assertThat(actualRequest.getPallets()).isNull();
        Assertions.assertThat(actualRequest.getSortables()).containsExactlyInAnyOrder(
                new TmSortable(
                        "21",
                        null,
                        Map.of("referenceId", "2"),
                        TmSortableType.BOX,
                        logisticPointToId,
                        LocalDateTime.now(clock)),
                new TmSortable(
                        "22",
                        null,
                        Map.of("referenceId", "2"),
                        TmSortableType.BOX,
                        logisticPointToId,
                        LocalDateTime.now(clock)
                ),
                new TmSortable(
                        "31",
                        null,
                        Map.of("referenceId", "3"),
                        TmSortableType.BOX,
                        logisticPointToId,
                        LocalDateTime.now(clock)
                ),
                new TmSortable(
                        "41",
                        null,
                        Map.of("referenceId", "4"),
                        TmSortableType.BOX,
                        logisticPointToId,
                        LocalDateTime.now(clock)
                ),
                new TmSortable(
                        stamp,
                        null,
                        Map.of("referenceId", lot.getBarcode()),
                        TmSortableType.LOT,
                        logisticPointToId,
                        LocalDateTime.now(clock))
        );
    }

    @DisplayName("Нельзя отсортировать посылку в опломбированый лот. Настройка включена")
    @Test
    void cantSortPlaceToLotWhenStampAdded() throws Exception {
        testFactory.increaseScOrderId();

        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var order2 = testFactory.createForToday(
                order(sortingCenter, "2").places("21", "22")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).acceptPlaces("21", "22").sortPlaces("21", "22").get();
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        ApiSortableDto expected = new ApiSortableDto(lot.getLotId(),
                SortableType.PALLET,
                lot.getBarcode(),
                lot.getBarcode() + " " + parentCell.getScNumber(),
                LotStatus.READY,
                SortableStatus.KEEPED_DIRECT,
                CellSubType.DEFAULT,
                Set.of(SortableAPIAction.DELETE_STAMP),
                true,
                lot.getLot().getSize()
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        var sortable = sortableQueryService.findOrThrow(sortingCenter, lot.getBarcode());
        var barcode = sortable.getSortableBarcode("simpleStamp").orElseThrow();
        assertThat(barcode.getSortable()).isEqualTo(sortable);
        assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeType.STAMP);
        assertThatThrownBy(() -> testFactory.sortToLot(order1, "11", lot, user))
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.LOT_INVALID_STATE.name());
    }

    @SneakyThrows
    @DisplayName("Отгрузка лота с пломбой. Настройка включена")
    @Test
    public void addStampAndShipLot() {
        testFactory.increaseScOrderId();

        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setDeliveryServiceProperty(courierWithDs.deliveryService(),
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(sortingCenter.getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();

        Cell parentCell = testFactory.storedMagistralCell(sortingCenter, "r1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .deliveryService(courierWithDs.deliveryService()).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order1))
                .orElseThrow();
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/" + lot.getBarcode()
                                        + "/addStamp?stampId=simpleStamp")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk());
        String outboundExternalId = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/outbounds/bindLotWithStamp"
                                        + "?stampId=simpleStamp&routeId="
                                        + testFactory.getRouteIdForSortableFlow(route))
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var palletAsSortable = sortableRepository.findById(lot.getSortableId()).orElseThrow();
        outbound = outboundRepository.findByIdOrThrow(outbound.getId());
        order1 = testFactory.getOrder(order1.getId());
        Assertions.assertThat(palletAsSortable.getOutbound()).isEqualTo(outbound);
        Assertions.assertThat(palletAsSortable.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        lot = sortableLotService.findByLotIdOrThrow(lot.getLotId());
        Assertions.assertThat(lot.getLotStatus()).isEqualTo(LotStatus.SHIPPED);
        Assertions.assertThat(order1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        Assertions.assertThat(testFactory.orderPlaces(order1.getId()).stream()
                .map(Place::getStatus)
                .toList()).containsOnly(PlaceStatus.SHIPPED);
    }

}
