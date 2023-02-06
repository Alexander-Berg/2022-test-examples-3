package ru.yandex.market.tpl.core.domain.usershift.partner.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTaskFactory;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.TplTestCargoFactory;
import ru.yandex.market.tpl.core.test.factory.TestRoutePointFactory;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor
class PartnerReportUserShiftRepositoryTest extends TplAbstractTest {

    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final TestRoutePointFactory testRoutePointFactory;
    private final TplTestCargoFactory tplTestCargoFactory;

    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;

    private final UserShiftCommandService userShiftCommandService;
    private final PartnerReportUserShiftRepository partnerReportUserShiftRepository;

    private PickupPoint pickupPoint;
    private Movement movementDirectNew;
    private Movement movementDirectOld;
    private Movement movementReturn;
    private Long userShiftId;

    private User user;

    @BeforeEach
    void setUp() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );

        clearAfterTest(pickupPoint);

        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        movementDirectNew = testDataFactory.buildDropOffDirectMovement(pickupPoint.getLogisticPointId().toString());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();

        userShiftId = userShiftCommandService.createUserShift(createCommand);

        //generate Direct task (new verion)
        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        testRoutePointFactory.buildDropoffDirectRoutePointData(movementDirectNew, pickupPoint.getId()),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        //generate Return task
        movementReturn = testDataFactory.buildDropOffReturnMovement(pickupPoint.getLogisticPointId().toString());
        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        testRoutePointFactory.buildDropoffReturnRoutePointData(movementReturn, pickupPoint.getId()),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        //Generate Collect Dropship task (old version)...
        movementDirectOld = testDataFactory.buildDropOffDirectMovement(pickupPoint.getLogisticPointId().toString());
        userShiftCommandService.addCollectDropshipTask(
                null,
                new UserShiftCommand.AddCollectDropshipTask(
                        userShiftId,
                        testRoutePointFactory.buildCollectDropshipRoutePointData(movementDirectOld)
                )
        );

    }

    @Test
    void getRoutingListDataForUserShifts() {
        //given
        var cargoReturn = tplTestCargoFactory.createCargo("cargoReturn",
                movementReturn.getWarehouseTo().getYandexId());
        addDeliveryTask(cargoReturn.getId(), true);

        var cargoDirect = tplTestCargoFactory.createCargoDirect("cargoDirect",
                movementDirectNew.getWarehouse().getYandexId());
        addDeliveryTask(cargoDirect.getId(), false);

        //when
        var routingListData = partnerReportUserShiftRepository.getRoutingListDataForUserShifts(List.of(userShiftId));

        //then
        var routingListDataMap = StreamEx.of(routingListData)
                .toMap(PartnerReportUserShiftRepository.RoutingListData::getMultiOrderId, Function.identity());

        assertThat(routingListDataMap).hasSize(3);

        assertRouingData(cargoReturn, routingListDataMap, movementReturn);
        assertRouingData(cargoDirect, routingListDataMap, movementDirectNew);
        assertRouingData(null, routingListDataMap, movementDirectOld);
    }

    private void assertRouingData(@Nullable DropoffCargo cargo,
                                  Map<String, PartnerReportUserShiftRepository.RoutingListData> routingListDataMap,
                                  Movement movement) {

        var warehouseInfo = movement.isDropOffReturn() ? movement.getWarehouseTo() : movement.getWarehouse();
        assertThat(routingListDataMap).containsKey(movement.getExternalId());
        var returnData = routingListDataMap.get(movement.getExternalId());
        assertThat(returnData.getOrderId()).isEqualTo(cargo == null ? movement.getExternalId() : cargo.getBarcode());
        assertThat(returnData.getCourierUid()).isEqualTo(user.getUid());
        assertThat(returnData.getOrderType()).isEqualTo(OrderType.PVZ);
        assertThat(returnData.getRecipientName()).isEqualTo(warehouseInfo.getContact());
        assertThat(returnData.getDeliveryIntervalFrom()).isEqualTo(movement.getDeliveryIntervalFrom());
        assertThat(returnData.getDeliveryIntervalTo()).isEqualTo(movement.getDeliveryIntervalTo());
        assertThat(returnData.getUserShiftId()).isEqualTo(userShiftId);
    }

    private void addDeliveryTask(Long cargoId, boolean isReturn) {
        var warehouseInfo = isReturn ? movementReturn.getWarehouseTo() : movementDirectNew.getWarehouse();
        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(isReturn ? movementReturn.getId() :
                                                        movementDirectNew.getId())
                                                .dropOffCargoId(cargoId)
                                                .isReturn(isReturn)
                                                .build()
                                )
                                .address(CollectDropshipTaskFactory.fromWarehouseAddress(warehouseInfo.getAddress()))
                                .name(warehouseInfo.getAddress().getAddress())
                                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(pickupPoint.getId())
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );
    }
}
