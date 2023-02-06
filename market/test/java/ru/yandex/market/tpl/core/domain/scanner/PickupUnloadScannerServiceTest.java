package ru.yandex.market.tpl.core.domain.scanner;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.scanner.ScannerDisplayMode;
import ru.yandex.market.tpl.api.model.scanner.ScannerOrderDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTaskFactory;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.TplTestCargoFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
@RequiredArgsConstructor
class PickupUnloadScannerServiceTest extends TplAbstractTest {

    private static final String FAKE_LOGISTIC_POINT_ID_TO = "12345";

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandService userShiftCommandService;
    private final OrderGenerateService orderGenerateService;
    private final PickupUnloadScannerService pickupUnloadScannerService;
    private final Clock clock;
    private final ClientReturnGenerator clientReturnGenerator;
    private final TestDataFactory testDataFactory;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final TplTestCargoFactory tplTestCargoFactory;
    private final PickupPointRepository pickupPointRepository;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    private User user;

    @BeforeEach
    void init() {
        Mockito.reset(configurationProviderAdapter);
        user = testUserHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CHECK_CORRECT_ORDER_PICKUP_POINT))
                .thenReturn(Boolean.FALSE);
    }

    @Test
    void deliveryOrder() {
        Order order = orderGenerateService.createOrder();
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(order.getExternalOrderId()),
                user);
        assertThat(scan.getText()).isEqualTo(TplScannerUtils.TEXT_ORDER_FOR_DELIVERY_DO_NOT_RETURN);
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.ERROR);
    }

    @Test
    void returnOrderIncorrectStatus() {
        Order previousOrder = orderGenerateService.createOrder();
        Order order = orderGenerateService.createOrder();
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(previousOrder.getExternalOrderId()),
                user);
        assertThat(scan.getText()).contains("Некорректный статус");
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.ERROR);
    }

    @Test
    void returnOrderCorrectStatus() {
        Order previousOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                .build());
        Order order = orderGenerateService.createOrder();
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(previousOrder.getExternalOrderId()),
                user);
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
    }

    @Test
    void checkCorrectOrderPickupPoint() {
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CHECK_CORRECT_ORDER_PICKUP_POINT))
                .thenReturn(Boolean.TRUE);

        //Создаем две разные pickupPoint, но с одинаковым адресом
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L, false, "ADDRESS000111")
        );
        var pickupPoint2 = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100501L, 1L, false, "ADDRESS000111")
        );

        // Один заказ создан на первый варинт точки, а другой на второй
        Order previousOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                .pickupPoint(pickupPoint)
                .build());
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint2)
                .build());

        //Создаем смену и задачу на доставку одного из заказов
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        //Проверяем, что скан второго заказа прошел успешно, хоть курьер и находится сейчас на "другом" pickupPoint
        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(previousOrder.getExternalOrderId()),
                user);
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
    }

    @Test
    void unknownOrder() {
        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of("NOT_FOUND_ORDER"),
                user);
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.ERROR);
        assertThat(scan.getText()).isEqualTo(TplScannerUtils.TEXT_ITEM_NOT_FOUND);
    }

    @Test
    void scanOrderWithoutInfoAboutPlace() {
        Order previousOrderWithoutInfoAboutPlace =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                        .build());
        Order order = orderGenerateService.createOrder();
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(previousOrderWithoutInfoAboutPlace.getExternalOrderId()),
                user);
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
        assertThat(scan.getExternalOrderId()).isEqualTo(previousOrderWithoutInfoAboutPlace.getExternalOrderId());
        assertThat(scan.getPlaces()).isNotNull().isNotEmpty().hasSize(1);
        assertThat(scan.getPlaces())
                .extracting(PlaceForScanDto::getBarcode)
                .contains(previousOrderWithoutInfoAboutPlace.getExternalOrderId());
    }

    @Test
    void scanSinglePlaceOrder() {
        Order previousSinglePlaceOrder =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .places(
                                List.of(
                                        OrderPlaceDto.builder()
                                                .barcode(new OrderPlaceBarcode("145", "place1"))
                                                .build()
                                )
                        )
                        .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                        .build());
        Order order = orderGenerateService.createOrder();
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(previousSinglePlaceOrder.getExternalOrderId()),
                user);
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
        assertThat(scan.getExternalOrderId()).isEqualTo(previousSinglePlaceOrder.getExternalOrderId());
        assertThat(scan.getPlaces()).isNotNull().isNotEmpty().hasSize(1);
        assertThat(scan.getPlaces())
                .extracting(PlaceForScanDto::getBarcode)
                .contains("place1");
    }

    @Test
    void scanMultiPlaceOrder() {

        Order previousMultiPlaceOrder =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .places(
                                List.of(
                                        OrderPlaceDto.builder()
                                                .barcode(new OrderPlaceBarcode("145", "place1"))
                                                .build(),
                                        OrderPlaceDto.builder()
                                                .barcode(new OrderPlaceBarcode("145", "place2"))
                                                .build()
                                )
                        )
                        .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                        .build());
        Order order = orderGenerateService.createOrder();
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        // сканируем только номер заказа
        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(previousMultiPlaceOrder.getExternalOrderId()),
                user);

        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.MULTI_PLACE_NEED_SCAN_BARCODE);
        assertThat(scan.getPlaces()).isNotNull().isNotEmpty().hasSize(2);
        assertThat(scan.getText()).isEqualTo(TplScannerUtils.TEXT_ORDER_MULTI_PLACE_NEED_SCAN_BARCODE);
        assertThat(scan.getPlaces())
                .extracting(PlaceForScanDto::getBarcode)
                .containsExactlyInAnyOrder("place1", "place2");


        // сканируем номер заказа и ШК коробки
        scan = pickupUnloadScannerService.scan(
                List.of(previousMultiPlaceOrder.getExternalOrderId(), "place1"),
                user);

        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
        assertThat(scan.getPlaces()).isNotNull().isNotEmpty().hasSize(2);
        assertThat(scan.getPlaces())
                .extracting(PlaceForScanDto::getBarcode)
                .containsExactlyInAnyOrder("place1", "place2");


        // сканируем только ШК коробки
        scan = pickupUnloadScannerService.scan(
                List.of("place1"),
                user);

        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
        assertThat(scan.getPlaces()).isNotNull().isNotEmpty().hasSize(2);
        assertThat(scan.getPlaces())
                .extracting(PlaceForScanDto::getBarcode)
                .containsExactlyInAnyOrder("place1", "place2");

        // сканируем ШК коробки и какую-нибудь фигню
        scan = pickupUnloadScannerService.scan(
                List.of("place1", "WRONG_BARCODE"),
                user);

        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
        assertThat(scan.getPlaces()).isNotNull().isNotEmpty().hasSize(2);
        assertThat(scan.getPlaces())
                .extracting(PlaceForScanDto::getBarcode)
                .containsExactlyInAnyOrder("place1", "place2");
    }

    @Test
    void deliveryMultiPlaceOrder() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(
                        List.of(
                                OrderPlaceDto.builder()
                                        .barcode(new OrderPlaceBarcode("145", "place1"))
                                        .build(),
                                OrderPlaceDto.builder()
                                        .barcode(new OrderPlaceBarcode("145", "place2"))
                                        .build()
                        )
                )
                .pickupPoint(testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L))
                .build());
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(order.getExternalOrderId()),
                user);
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.MULTI_PLACE_NEED_SCAN_BARCODE);

        scan = pickupUnloadScannerService.scan(
                List.of("place1"),
                user);
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.NEED_RETURN_REASON);

        scan = pickupUnloadScannerService.scan(
                List.of(order.getExternalOrderId(), "place1"),
                user);
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.NEED_RETURN_REASON);
    }

    @Test
    void returnPlacesAlways() {
        Order order =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .places(
                                List.of(
                                        OrderPlaceDto.builder()
                                                .barcode(new OrderPlaceBarcode("145", "place1"))
                                                .build(),
                                        OrderPlaceDto.builder()
                                                .barcode(new OrderPlaceBarcode("145", "place2"))
                                                .build()
                                )
                        )
                        .build());
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(order.getExternalOrderId()),
                user);
        assertThat(scan.getText()).isEqualTo("Заказ для доставки, НЕ ВОЗВРАЩАТЬ");
        assertThat(scan.getPlaces()).isNotNull().isNotEmpty().hasSize(2);
        assertThat(scan.getPlaces())
                .extracting(PlaceForScanDto::getBarcode)
                .containsExactlyInAnyOrder("place1", "place2");
    }

    @Test
    void pickupClientReturn() {
        String clientReturnBarcode = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(clientReturnBarcode),
                user
        );

        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.ERROR);
    }

    @Test
    void pickupClientReturnScanExistingClientReturn() {
        ClientReturn clientReturn = clientReturnGenerator.generate();
        String clientReturnBarcode = clientReturn.getBarcode();

        ScannerOrderDto scan = pickupUnloadScannerService.scan(
                List.of(clientReturnBarcode),
                user
        );

        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.ERROR);
        assertThat(scan.getText())
                .isEqualTo("Возврат не найден в системе. Просканируйте этикетку ещё раз или введите вручную!");
        assertThat(scan.getExternalOrderId()).isEqualTo(clientReturnBarcode);
    }


    @Test
    void dropoffCargoScan_SuccessExisting() {
        //given
        Set<String> barcodes = Set.of("barcode1", "barcode2");

        var dropoffCargos = barcodes
                .stream()
                .map(barcode -> tplTestCargoFactory.createCargo(barcode, FAKE_LOGISTIC_POINT_ID_TO))
                .collect(Collectors.toSet());

        InitShiftResult initShiftResult = initCargoUserShift(FAKE_LOGISTIC_POINT_ID_TO);

        dropoffCargos.forEach(dropoffCargo -> addDeliveryTask(dropoffCargo, initShiftResult));


        tplTestCargoFactory.initPickupCargoFlow(
                TplTestCargoFactory.CargoPickupContext.of(List.of(), dropoffCargos.stream().map(DropoffCargo::getId)
                        .collect(Collectors.toSet()), Set.of()),
                TplTestCargoFactory.ShiftContext.of(user, initShiftResult.getUserShiftId())
        );

        //when
        String scannedBarcode = "barcode1";
        ScannerOrderDto scan = pickupUnloadScannerService.scan(List.of(scannedBarcode), user);

        //then
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
        assertThat(scan.getText()).isEqualTo(TplScannerUtils.TEXT_DROPOFF_CARGO_RETURN);
        assertThat(scan.getExternalOrderId()).isEqualTo(scannedBarcode);
    }

    @Test
    void dropoffCargoScan_SuccessExisting_ListBarcodes() {
        //given
        Set<String> barcodes = Set.of("barcode1", "barcode2");

        var dropoffCargos = barcodes
                .stream()
                .map(barcode -> tplTestCargoFactory.createCargo(barcode, FAKE_LOGISTIC_POINT_ID_TO))
                .collect(Collectors.toSet());

        InitShiftResult initShiftResult = initCargoUserShift(FAKE_LOGISTIC_POINT_ID_TO);

        dropoffCargos.forEach(dropoffCargo -> addDeliveryTask(dropoffCargo, initShiftResult));


        tplTestCargoFactory.initPickupCargoFlow(
                TplTestCargoFactory.CargoPickupContext.of(List.of(), dropoffCargos.stream().map(DropoffCargo::getId)
                        .collect(Collectors.toSet()), Set.of()),
                TplTestCargoFactory.ShiftContext.of(user, initShiftResult.getUserShiftId())
        );

        //when
        String scannedBarcode = "barcode1";
        ScannerOrderDto scan = pickupUnloadScannerService.scan(List.of(scannedBarcode, "fakeBarcode"), user);

        //then
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
        assertThat(scan.getText()).isEqualTo(TplScannerUtils.TEXT_DROPOFF_CARGO_RETURN);
        assertThat(scan.getExternalOrderId()).isEqualTo(scannedBarcode);
    }


    @Test
    void dropoffCargoScan_notFound() {
        //given
        Set<String> barcodes = Set.of("barcode1", "barcode2");

        var dropoffCargos = barcodes
                .stream()
                .map(barcode -> tplTestCargoFactory.createCargo(barcode, FAKE_LOGISTIC_POINT_ID_TO))
                .collect(Collectors.toSet());

        InitShiftResult initShiftResult = initCargoUserShift(FAKE_LOGISTIC_POINT_ID_TO);

        dropoffCargos.forEach(dropoffCargo -> addDeliveryTask(dropoffCargo, initShiftResult));


        tplTestCargoFactory.initPickupCargoFlow(
                TplTestCargoFactory.CargoPickupContext.of(List.of(), dropoffCargos.stream().map(DropoffCargo::getId)
                        .collect(Collectors.toSet()), Set.of()),
                TplTestCargoFactory.ShiftContext.of(user, initShiftResult.getUserShiftId())
        );


        //when
        String fakeBarcode = "fakeBarcode";
        ScannerOrderDto scan = pickupUnloadScannerService.scan(List.of(fakeBarcode), user);

        //then
        assertThat(scan.getDisplayMode()).isEqualTo(ScannerDisplayMode.ERROR);
        assertThat(scan.getText()).isEqualTo(TplScannerUtils.TEXT_ITEM_NOT_FOUND);
        assertThat(scan.getExternalOrderId()).isEqualTo(fakeBarcode);
    }

    @Test
    void dropoffCargoScan_notUniq_Exception() {
        //given
        Set<String> barcodes = Set.of("barcode1", "barcode2");

        var dropoffCargos = barcodes
                .stream()
                .map(barcode -> tplTestCargoFactory.createCargo(barcode, FAKE_LOGISTIC_POINT_ID_TO))
                .collect(Collectors.toSet());

        InitShiftResult initShiftResult = initCargoUserShift(FAKE_LOGISTIC_POINT_ID_TO);

        dropoffCargos.forEach(dropoffCargo -> addDeliveryTask(dropoffCargo, initShiftResult));


        tplTestCargoFactory.initPickupCargoFlow(
                TplTestCargoFactory.CargoPickupContext.of(List.of(), dropoffCargos.stream().map(DropoffCargo::getId)
                        .collect(Collectors.toSet()), Set.of()),
                TplTestCargoFactory.ShiftContext.of(user, initShiftResult.getUserShiftId())
        );

        //when
        assertThrows(TplInvalidParameterException.class, () -> pickupUnloadScannerService.scan(List.copyOf(barcodes),
                user));
    }

    private InitShiftResult initCargoUserShift(String logisticPointIdTo) {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );

        clearAfterTest(pickupPoint);

        Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Movement movement = testDataFactory.buildDropOffReturnMovement(logisticPointIdTo);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();

        var userShiftId = userShiftCommandService.createUserShift(createCommand);

        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(movement.getId())
                                                .build()
                                )
                                .address(CollectDropshipTaskFactory.fromWarehouseAddress(movement.getWarehouseTo().getAddress()))
                                .name(movement.getWarehouseTo().getAddress().getAddress())
                                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(pickupPoint.getId())
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        return InitShiftResult.of(
                pickupPoint.getId(),
                movement.getId(),
                userShiftId
        );
    }

    private void addDeliveryTask(DropoffCargo cargo, InitShiftResult initShiftResult) {
        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        initShiftResult.getUserShiftId(),
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(initShiftResult.getMovementId())
                                                .dropOffCargoId(cargo.getId())
                                                .build()
                                )
                                .name("fakeName")
                                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(initShiftResult.getPickupPointId())
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );
    }

    @Value(staticConstructor = "of")
    private static class InitShiftResult {
        private Long pickupPointId;
        private Long movementId;
        private Long userShiftId;
    }
}
