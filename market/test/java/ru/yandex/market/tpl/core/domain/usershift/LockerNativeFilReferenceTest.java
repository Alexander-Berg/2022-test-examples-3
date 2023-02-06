package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.locker.boxbot.request.InitReturnExtraditionDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.CellDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.CellSizeDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.DeliveryItemDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.ExtraditionItemDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.ExtraditionShipmentDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.ReferenceDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.ShipmentDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.SizeCellsDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.SenderWithoutExtId;
import ru.yandex.market.tpl.core.domain.order.SenderWithoutExtIdRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyEntity;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.boxbot.LockerApi;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.service.user.UserPropsType;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class LockerNativeFilReferenceTest extends TplAbstractTest {
    public static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    public static final String EXTERNAL_ORDER_ID_2 = "LO-EXTERNAL_ORDER_ID_2";
    public static final String EXTERNAL_ORDER_ID_3 = "EXTERNAL-LO-ORDER_ID_3";
    public static final String EXTERNAL_EXT_ID_1 = "EXTERNAL-LO-ORDER_ID_4";

    private final LockerDeliveryService service;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final UserPropertyService userPropertyService;
    private final LockerOrderDataHelper lockerOrderDataHelper;
    private final TransactionTemplate transactionTemplate;
    private final SenderWithoutExtIdRepository senderWithoutExtIdRepository;
    private final LockerApi apiClient;

    private User user;
    private Shift shift;
    private Long userShiftId;
    private Order order1;
    private Order order2;
    private Order order3;
    private Order extr1;
    private UserShift userShift;
    private RoutePoint routePoint;
    private LockerDeliveryTask lockerDeliveryTask;


    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order1 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 2);
        order2 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_2, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        order3 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_3, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);

        extr1 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_EXT_ID_1, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);

        var s = new SenderWithoutExtId(order1.getSender().getYandexId());
        senderWithoutExtIdRepository.save(s);

        userShiftReassignManager.assign(userShift, order1);
        userShiftReassignManager.assign(userShift, order2);
        userShiftReassignManager.assign(userShift, order3);

        testUserHelper.checkinAndFinishPickup(userShift);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(3);

        testUserHelper.arriveAtRoutePoint(routePoint);
        UserPropertyEntity up = new UserPropertyEntity();
        up.setUser(user);
        up.setName(UserPropsType.NATIVE_LOCKER_FLOW_ENABLED.getName());
        up.setType(TplPropertyType.BOOLEAN);
        up.setValue(Boolean.toString(true));
        userPropertyService.save(up);

    }

    @Test
    public void shouldFillShipmentsByPlacesBarcode() {
        transactionTemplate.execute(a -> {
            init();
            var reference = mockReference();
            var taskDto = service.getTask(lockerDeliveryTask.getId(), user);
            var marketReference = service.fillReference(reference, taskDto.getId(), user);
            assertThat(marketReference.getShipments().size()).isEqualTo(3);
            return null;
        });
    }

    @Test
    public void shouldReplaceBarcodeInReference() {
        transactionTemplate.execute(a -> {
            init();
            return 0;
        });
        when(apiClient.getReferences(any(), any())).thenReturn(mockReference());
        var reference = service.getReferences("12", "21", lockerDeliveryTask.getId(), user);
        var shipment = reference.getShipments().get(1);
        assertThat(shipment.getBarcode()).isEqualTo(EXTERNAL_ORDER_ID_2 + "-1");
        assertThat(shipment.getExternalOrderId()).isEqualTo(EXTERNAL_ORDER_ID_2 + "-1");
        assertThat(shipment.getItems().get(0).getMainBarcode()).isEqualTo(EXTERNAL_ORDER_ID_2 + "-1");

        shipment = reference.getShipments().get(0);
        assertThat(shipment.getBarcode()).isEqualTo(EXTERNAL_ORDER_ID_1);

        shipment = reference.getShipments().get(2);
        assertThat(shipment.getBarcode()).isEqualTo(EXTERNAL_ORDER_ID_3 + "-1");

        var extradition = reference.getExtradition().get(0);
        assertThat(extradition.getExternalOrderId()).isEqualTo(EXTERNAL_EXT_ID_1 + "-1");

    }

    private ReferenceDto mockReference() {
        String postCert = "cert1";
        String sysCert = "cert2";
        String rootCert = "cert3";
        boolean checkSystemCert = false;
        String sign = "1234567890123456";

        DeliveryItemDto delivery1 = new DeliveryItemDto(1, 1, "EXTERNAL_ORDER_ID_1", "ABC-abc-1234", sign, 0);
        DeliveryItemDto delivery2 = new DeliveryItemDto(1, 1, EXTERNAL_ORDER_ID_2 + "-1", null, sign, 0);
        DeliveryItemDto delivery3 = new DeliveryItemDto(1, 1, "qewr", EXTERNAL_ORDER_ID_3 + "-1", sign, 0);

        CellDto cellDto = new CellDto(3, 1, 3, "A3", 1, 3, 7, 8);
        ExtraditionItemDto extraditionItemDto = new ExtraditionItemDto(1, 2, cellDto, EXTERNAL_EXT_ID_1, sign,
                EXTERNAL_EXT_ID_1);

        CellDto cellDto2 = new CellDto(4, 1, 4, "A4", 1, 4, 7, 8);
        ExtraditionItemDto extraditionItemDto2 = new ExtraditionItemDto(1, 2, cellDto2, "ABC-abc-1236", sign, "ABC" +
                "-abc-1236");

        InitReturnExtraditionDto initRet1 = new InitReturnExtraditionDto(1234, 123L);
        InitReturnExtraditionDto initRet2 = new InitReturnExtraditionDto(1235, 123L);

        CellSizeDto cellSizeDto = new CellSizeDto(1, 1, "asd", "red");
        CellDto cellf1 = new CellDto(3, 1, 1, "A3", 1, 3, 7, 8);
        CellDto cellf2 = new CellDto(3, 1, 2, "A4", 1, 3, 7, 8);
        SizeCellsDto sizeCellsDto = new SizeCellsDto(cellSizeDto, Arrays.asList(cellf1, cellf2));

        ExtraditionShipmentDto extraditionShipmentDto = new ExtraditionShipmentDto(EXTERNAL_EXT_ID_1,
                Arrays.asList(extraditionItemDto));
        ExtraditionShipmentDto extraditionShipmentDto2 = new ExtraditionShipmentDto("ABC-abc-1236",
                Arrays.asList(extraditionItemDto2));

        ShipmentDto shipment1 = new ShipmentDto(33, EXTERNAL_ORDER_ID_1, Arrays.asList(delivery1));
        ShipmentDto shipment2 = new ShipmentDto(34, EXTERNAL_ORDER_ID_2, Arrays.asList(delivery2));
        ShipmentDto shipment3 = new ShipmentDto(35, EXTERNAL_ORDER_ID_3, Arrays.asList(delivery3));

        ReferenceDto dto = new ReferenceDto(
                postCert,
                sysCert,
                rootCert,
                checkSystemCert,

                List.of(delivery1, delivery2, delivery3),
                List.of(extraditionItemDto, extraditionItemDto2),
                List.of(extraditionShipmentDto, extraditionShipmentDto2),
                List.of(initRet1, initRet2),
                List.of(sizeCellsDto),
                null,
                List.of(shipment1, shipment2, shipment3));
        return dto;
    }
}
