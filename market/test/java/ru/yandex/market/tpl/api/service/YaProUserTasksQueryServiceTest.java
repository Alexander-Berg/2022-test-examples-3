package ru.yandex.market.tpl.api.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.api.test.TplApiAbstractTest;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayCallTaskProjection;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayLockerDeliveryTaskProjection;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayOrderDeliveryTaskProjection;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayRoutePointProjection;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayShiftProjection;
import ru.yandex.market.tpl.server.model.RoutePointsGroupType;
import ru.yandex.market.tpl.server.model.TaskCargoTypeDto;
import ru.yandex.market.tpl.server.model.TodayAllTasksDto;
import ru.yandex.market.tpl.server.model.TodayRoutePointDto;
import ru.yandex.market.tpl.server.model.TodayRoutePointsGroupDto;
import ru.yandex.market.tpl.server.model.TodayTaskDto;
import ru.yandex.market.tpl.server.model.TodayTaskStatus;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class YaProUserTasksQueryServiceTest extends TplApiAbstractTest {
    private final YaProUserTasksQueryService yaProUserTasksQueryService;
    private final Clock clock;

    @Test
    void getAllTodayTasks() {
        Long callTaskId = 1235L;

        UserTodayShiftProjection userTodayShiftProjection = UserTodayShiftProjection
                .builder()
                .isCanBeOpenedOutOfTurn(true)
                .zoneOffset(ZoneOffset.UTC)
                .routePointProjections(List.of(

                        UserTodayRoutePointProjection
                                .builder()
                                .routePointType(RoutePointType.LOCKER_DELIVERY)
                                .lockerDeliveryType(PartnerSubType.LAVKA)
                                .id(3463463L)
                                .addressString("Улица Пушкина дом Колотушкина LAVKA")
                                .expectedDateTime(Instant.now(clock))
                                .userTodayOrderDeliveryTaskProjections(List.of())
                                .userTodayLockerDeliveryTaskProjections(List.of(
                                        UserTodayLockerDeliveryTaskProjection.builder()
                                                .pickupPointId(34646L)
                                                .id(34564564354L)
                                                .ordinalNumber(33453665)
                                                .lockerDeliveryTaskStatus(LockerDeliveryTaskStatus.ORDERS_LOADED)
                                                .intervalFrom(LocalDateTime.of(2020, 1, 2, 5, 20))
                                                .intervalTo(LocalDateTime.of(2020, 1, 2, 10, 20))
                                                .build()
                                ))
                                .build(),

                        UserTodayRoutePointProjection
                                .builder()
                                .routePointType(RoutePointType.LOCKER_DELIVERY)
                                .lockerDeliveryType(PartnerSubType.LOCKER)
                                .id(34634634L)
                                .addressString("Улица Пушкина дом Колотушкина LOCKER")
                                .expectedDateTime(Instant.now(clock))
                                .userTodayOrderDeliveryTaskProjections(List.of())
                                .userTodayLockerDeliveryTaskProjections(List.of(
                                        UserTodayLockerDeliveryTaskProjection.builder()
                                                .pickupPointId(346461L)
                                                .id(345645643542L)
                                                .ordinalNumber(345358763)
                                                .lockerDeliveryTaskStatus(LockerDeliveryTaskStatus.NOT_STARTED)
                                                .intervalFrom(LocalDateTime.of(2020, 1, 2, 5, 20))
                                                .intervalTo(LocalDateTime.of(2020, 1, 2, 10, 20))
                                                .build()
                                ))
                                .build(),

                        UserTodayRoutePointProjection
                                .builder()
                                .routePointType(RoutePointType.LOCKER_DELIVERY)
                                .lockerDeliveryType(PartnerSubType.PVZ)
                                .id(34634632L)
                                .addressString("Улица Пушкина дом Колотушкина PVZ")
                                .expectedDateTime(Instant.now(clock))
                                .userTodayOrderDeliveryTaskProjections(List.of())
                                .userTodayLockerDeliveryTaskProjections(List.of(
                                        UserTodayLockerDeliveryTaskProjection.builder()
                                                .pickupPointId(3464612L)
                                                .ordinalNumber(3343785)
                                                .id(3456456435423L)
                                                .lockerDeliveryTaskStatus(LockerDeliveryTaskStatus.STARTED)
                                                .intervalFrom(LocalDateTime.of(2020, 1, 2, 5, 20))
                                                .intervalTo(LocalDateTime.of(2020, 1, 2, 10, 20))
                                                .build()
                                ))
                                .build(),

                        UserTodayRoutePointProjection
                                .builder()
                                .routePointType(RoutePointType.DELIVERY)
                                .id(347457474764L)
                                .addressString("15 улица")
                                .expectedDateTime(Instant.now(clock))
                                .userTodayLockerDeliveryTaskProjections(List.of())
                                .userTodayOrderDeliveryTaskProjections(List.of(
                                        UserTodayOrderDeliveryTaskProjection.builder()
                                                .isClientReturn(false)
                                                .multiOrderId("3453345308")
                                                .isFashion(false)
                                                .orderFlowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                                                .recipientFio("Аброгим Абромович Аброгов")
                                                .ordinalNumber(3353453)
                                                .orderDeliveryTaskStatus(OrderDeliveryTaskStatus.DELIVERED)
                                                .callTask(
                                                        UserTodayCallTaskProjection.builder()
                                                                .id(callTaskId)
                                                                .build()
                                                )
                                                .intervalFrom(LocalDateTime.of(2020, 1, 2, 5, 20))
                                                .intervalTo(LocalDateTime.of(2020, 1, 2, 10, 20))
                                                .build(),

                                        UserTodayOrderDeliveryTaskProjection.builder()
                                                .orderFlowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                                                .orderDeliveryTaskStatus(OrderDeliveryTaskStatus.DELIVERED)
                                                .build(),

                                        UserTodayOrderDeliveryTaskProjection.builder()
                                                .isClientReturn(false)
                                                .multiOrderId("3453345308")
                                                .isFashion(false)
                                                .ordinalNumber(334533)
                                                .recipientFio("Абром Аброгимович Аброгимов")
                                                .orderDeliveryTaskStatus(OrderDeliveryTaskStatus.DELIVERED)
                                                .callTask(
                                                        UserTodayCallTaskProjection.builder()
                                                                .id(callTaskId)
                                                                .build()
                                                )
                                                .build(),

                                        UserTodayOrderDeliveryTaskProjection.builder()
                                                .isClientReturn(true)
                                                .multiOrderId("3453345308")
                                                .isFashion(false)
                                                .ordinalNumber(33534535)
                                                .recipientFio("Аброгим Аброгмович Аброгимов")
                                                .id(463579043541L)
                                                .orderDeliveryTaskStatus(OrderDeliveryTaskStatus.DELIVERED)
                                                .callTask(
                                                        UserTodayCallTaskProjection.builder()
                                                                .id(callTaskId)
                                                                .build()
                                                )
                                                .build(),

                                        UserTodayOrderDeliveryTaskProjection.builder()
                                                .isClientReturn(true)
                                                .multiOrderId("3453345308")
                                                .isFashion(false)
                                                .ordinalNumber(3353435)
                                                .recipientFio("Аброгим Аброгимович Абр")
                                                .id(463579043541L)
                                                .orderDeliveryTaskStatus(OrderDeliveryTaskStatus.DELIVERED)
                                                .callTask(
                                                        UserTodayCallTaskProjection.builder()
                                                                .id(callTaskId)
                                                                .build()
                                                )
                                                .build(),

                                        UserTodayOrderDeliveryTaskProjection.builder()
                                                .isClientReturn(false)
                                                .multiOrderId("3453345308")
                                                .ordinalNumber(33534)
                                                .isFashion(true)
                                                .recipientFio("Аброгим Аброгимович Аброгим")
                                                .orderDeliveryTaskStatus(OrderDeliveryTaskStatus.DELIVERY_FAILED)
                                                .callTask(
                                                        UserTodayCallTaskProjection.builder()
                                                                .id(callTaskId)
                                                                .build()
                                                )
                                                .build(),

                                        UserTodayOrderDeliveryTaskProjection.builder()
                                                .isClientReturn(false)
                                                .multiOrderId("3453345308")
                                                .isFashion(true)
                                                .ordinalNumber(3530)
                                                .recipientFio("Аброгим Аброгимович Аброгимо")
                                                .orderDeliveryTaskStatus(OrderDeliveryTaskStatus.DELIVERY_FAILED)
                                                .callTask(
                                                        UserTodayCallTaskProjection.builder()
                                                                .id(callTaskId)
                                                                .build()
                                                )
                                                .build()
                                ))
                                .build()
                ))
                .build();


        TodayAllTasksDto result = yaProUserTasksQueryService.getAllTodayTasks(userTodayShiftProjection);
        assertThat(result.getIsCanBeOpenedOutOfTurn()).isEqualTo(userTodayShiftProjection.isCanBeOpenedOutOfTurn());
        assertThat(result.getActiveRoutePointsGroup().size()).isEqualTo(3L);
        assertThat(result.getFinishedRoutePointsGroup().size()).isEqualTo(1L);

        TodayRoutePointsGroupDto groupDtoLavkaPosition0 = result.getActiveRoutePointsGroup().get(0);
        assertGroup(groupDtoLavkaPosition0, RoutePointsGroupType.LAVKA, 1);
        TodayRoutePointDto routePointLavkaPosition0 = groupDtoLavkaPosition0.getRoutePoints().get(0);
        UserTodayRoutePointProjection routePointProjectionLavkaPosition0 =
                userTodayShiftProjection.getRoutePointProjections().get(0);
        assertRoutePoint(routePointLavkaPosition0, routePointProjectionLavkaPosition0);
        assertThat(routePointLavkaPosition0.getTasks().size()).isEqualTo(1);
        assertTask(routePointLavkaPosition0.getTasks().get(0),
                routePointProjectionLavkaPosition0.getUserTodayLockerDeliveryTaskProjections().get(0),
                TaskCargoTypeDto.DELIVERY, TodayTaskStatus.NEUTRAL, routePointProjectionLavkaPosition0);

        TodayRoutePointsGroupDto groupDtoLockerPosition1 = result.getActiveRoutePointsGroup().get(1);
        assertGroup(groupDtoLockerPosition1, RoutePointsGroupType.LOCKER, 1);
        TodayRoutePointDto routePointLockerPosition1 = groupDtoLockerPosition1.getRoutePoints().get(0);
        UserTodayRoutePointProjection routePointProjectionLockerPosition1 =
                userTodayShiftProjection.getRoutePointProjections().get(1);
        assertRoutePoint(routePointLockerPosition1, routePointProjectionLockerPosition1);
        assertThat(routePointLockerPosition1.getTasks().size()).isEqualTo(1);
        assertTask(routePointLockerPosition1.getTasks().get(0),
                routePointProjectionLockerPosition1.getUserTodayLockerDeliveryTaskProjections().get(0),
                TaskCargoTypeDto.DELIVERY, TodayTaskStatus.NEUTRAL, routePointProjectionLockerPosition1);

        TodayRoutePointsGroupDto groupDtoPvzPosition2 = result.getActiveRoutePointsGroup().get(2);
        assertGroup(groupDtoPvzPosition2, RoutePointsGroupType.PVZ, 1);
        TodayRoutePointDto routePointPvzPosition2 = groupDtoPvzPosition2.getRoutePoints().get(0);
        UserTodayRoutePointProjection routePointProjectionPvzPosition2 =
                userTodayShiftProjection.getRoutePointProjections().get(2);
        assertRoutePoint(routePointPvzPosition2, routePointProjectionPvzPosition2);
        assertThat(routePointPvzPosition2.getTasks().size()).isEqualTo(1);
        assertTask(routePointPvzPosition2.getTasks().get(0),
                routePointProjectionPvzPosition2.getUserTodayLockerDeliveryTaskProjections().get(0),
                TaskCargoTypeDto.DELIVERY, TodayTaskStatus.NEUTRAL, routePointProjectionPvzPosition2);


        TodayRoutePointsGroupDto groupDtoClientFinishedPosition0 = result.getFinishedRoutePointsGroup().get(0);
        assertGroup(groupDtoClientFinishedPosition0, RoutePointsGroupType.CLIENT, 1);
        TodayRoutePointDto routePointClientPosition3 = groupDtoClientFinishedPosition0.getRoutePoints().get(0);
        UserTodayRoutePointProjection routePointProjectionClientPosition3 =
                userTodayShiftProjection.getRoutePointProjections().get(3);
        assertRoutePoint(routePointClientPosition3, routePointProjectionClientPosition3);
        assertThat(routePointClientPosition3.getTasks().size()).isEqualTo(4);

        assertTask(routePointClientPosition3.getTasks().get(0),
                routePointProjectionClientPosition3.getUserTodayOrderDeliveryTaskProjections().get(0),
                TaskCargoTypeDto.DELIVERY, TodayTaskStatus.FINISHED);
        assertTask(routePointClientPosition3.getTasks().get(1),
                routePointProjectionClientPosition3.getUserTodayOrderDeliveryTaskProjections().get(3),
                TaskCargoTypeDto.RETURN, TodayTaskStatus.FINISHED);
        assertTask(routePointClientPosition3.getTasks().get(2),
                routePointProjectionClientPosition3.getUserTodayOrderDeliveryTaskProjections().get(4),
                TaskCargoTypeDto.RETURN, TodayTaskStatus.FINISHED);
        assertTask(routePointClientPosition3.getTasks().get(3),
                routePointProjectionClientPosition3.getUserTodayOrderDeliveryTaskProjections().get(5),
                TaskCargoTypeDto.FASHION, TodayTaskStatus.CANCELLED);
    }

    private void assertTask(TodayTaskDto taskDto, UserTodayOrderDeliveryTaskProjection projectionDto,
                            TaskCargoTypeDto taskCargoTypeDto, TodayTaskStatus todayTaskStatus) {
        assertThat(taskDto.getRecepientName()).isEqualTo(projectionDto.getRecipientFio());
        assertThat(taskDto.getTaskCargoTypeDto()).isEqualTo(taskCargoTypeDto);
        assertThat(taskDto.getTaskStatus()).isEqualTo(todayTaskStatus);
        assertThat(taskDto.getMultiOrderId()).isEqualTo(Long.toString(projectionDto.getCallTask().getId()));
        assertThat(taskDto.getTaskId()).isEqualTo(projectionDto.getId());
        assertThat(taskDto.getOrdinalNumber()).isEqualTo(projectionDto.getOrdinalNumber());
    }

    private void assertTask(TodayTaskDto taskDto, UserTodayLockerDeliveryTaskProjection projectionDto,
                            TaskCargoTypeDto taskCargoTypeDto, TodayTaskStatus todayTaskStatus,
                            UserTodayRoutePointProjection routePointProjectionDto) {
        assertThat(taskDto.getRecepientName()).isEqualTo(routePointProjectionDto.getLockerDeliveryType().getDescription());
        assertThat(taskDto.getTaskCargoTypeDto()).isEqualTo(taskCargoTypeDto);
        assertThat(taskDto.getTaskStatus()).isEqualTo(todayTaskStatus);
        assertThat(taskDto.getTaskId()).isEqualTo(projectionDto.getId());
        assertThat(taskDto.getOrdinalNumber()).isEqualTo(projectionDto.getOrdinalNumber());
    }

    private void assertGroup(TodayRoutePointsGroupDto groupDto, RoutePointsGroupType groupType, int size) {
        assertThat(groupDto.getRoutePointsGroupType()).isEqualTo(groupType);
        assertThat(groupDto.getRoutePoints().size()).isEqualTo(size);
    }

    private void assertRoutePoint(TodayRoutePointDto routePointDto, UserTodayRoutePointProjection projectionDto) {
        assertThat(routePointDto.getRoutePointId()).isEqualTo(projectionDto.getId());
        assertThat(routePointDto.getAddress()).isEqualTo(projectionDto.getAddressString());

        if (CollectionUtils.isNotEmpty(projectionDto.getUserTodayOrderDeliveryTaskProjections())) {
            var task = projectionDto.getUserTodayOrderDeliveryTaskProjections().get(0);
            assertThat(routePointDto.getIntervalFrom()).isEqualTo(task.getIntervalFrom());
            assertThat(routePointDto.getIntervalTo()).isEqualTo(task.getIntervalTo());
        } else if (CollectionUtils.isNotEmpty(projectionDto.getUserTodayLockerDeliveryTaskProjections())) {
            var task = projectionDto.getUserTodayLockerDeliveryTaskProjections().get(0);
            assertThat(routePointDto.getIntervalFrom()).isEqualTo(task.getIntervalFrom());
            assertThat(routePointDto.getIntervalTo()).isEqualTo(task.getIntervalTo());
        }
    }
}
