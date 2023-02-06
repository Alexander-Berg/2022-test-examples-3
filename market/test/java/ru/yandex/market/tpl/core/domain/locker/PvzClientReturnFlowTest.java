package ru.yandex.market.tpl.core.domain.locker;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.LockerSubtaskType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.CreatedSource;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.query.usershift.mapper.TaskDtoMapper;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PvzClientReturnFlowTest extends TplAbstractTest {
    private final TestUserHelper userHelper;
    private final Clock clock;
    private final UserShiftCommandService commandService;
    private final OrderGenerateService orderGenerateService;
    private final ClientReturnCommandService clientReturnCommandService;
    private final ClientReturnRepository clientReturnRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TaskDtoMapper taskDtoMapper;
    private final LockerSubtaskRepository lockerSubtaskRepository;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    @AfterEach
    void after() {
        ClockUtil.initFixed(clock);
    }

    @Test
    @Transactional
    void clientReturnInPvzFlowTest() {
        String prefix = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix();
        ClockUtil.initFixed(clock, LocalDateTime.of(LocalDate.now().plusYears(1000), LocalTime.now()));
        //Крутой курьер, на которого джоба не назначила возврата, так как отключена
        User modernUser = userHelper.findOrCreateUser(47205239L);
        //Застрявший в прошлом курьер, на которого джоба успела назначить возврат до отключения
        User retardedUser = userHelper.findOrCreateUser(47205237L);
        UserShift modernShift = userHelper.createEmptyShift(modernUser, LocalDate.now(clock));
        UserShift retardedShift = userHelper.createEmptyShift(retardedUser, LocalDate.now(clock));
        PickupPoint pickupPoint = PickupPointGenerator.generatePickupPoint(3463476346L);
        pickupPointRepository.save(pickupPoint);

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        LockerDeliveryTask modernLockerDeliveryTask = userHelper.addLockerDeliveryTaskToShift(modernUser,
                modernShift, order1);

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        RoutePointAddress address = new RoutePointAddress("my_address2", geoPoint);
        Instant deliveryTime = Instant.now(clock);
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        LockerDeliveryTask retardedLockerDeliveryTask = (LockerDeliveryTask) commandService.addDeliveryTask(
                retardedUser,
                new UserShiftCommand.AddDeliveryTask(retardedShift.getId(),
                        NewDeliveryRoutePointData.builder()
                                .address(address)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name2")
                                .withOrderReferenceFromOrder(order2, false, false)
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(pickupPoint.getId())
                                .build(),
                        SimpleStrategies.NO_MERGE,
                        GeoPoint.GEO_POINT_SCALE
                )
        );


        //Назначенный джобой возврат для устаревшего курьера, который он не успеет забрать
        ClientReturnCommand.Create createCommand = ClientReturnCommand.Create.builder()
                .pickupPoint(pickupPoint)
                .barcode(prefix + "123")
                .returnId("4567")
                .createdSource(CreatedSource.SELF)
                .source(Source.SYSTEM)
                .build();
        ClientReturn badClientReturnWithTaskForRetarded = clientReturnCommandService.create(createCommand);
        badClientReturnWithTaskForRetarded.setStatus(ClientReturnStatus.ASSIGNED_TO_COURIER);
        clientReturnRepository.save(badClientReturnWithTaskForRetarded);

        //Назначенный джобой возврат для устаревшего курьера, который он успеет забрать
        ClientReturnCommand.Create createCommand15 = ClientReturnCommand.Create.builder()
                .pickupPoint(pickupPoint)
                .barcode(prefix + "1232")
                .returnId("456700")
                .createdSource(CreatedSource.SELF)
                .source(Source.SYSTEM)
                .build();
        ClientReturn goodClientReturnWithTaskForRetarded = clientReturnCommandService.create(createCommand15);
        goodClientReturnWithTaskForRetarded.setStatus(ClientReturnStatus.ASSIGNED_TO_COURIER);
        clientReturnRepository.save(goodClientReturnWithTaskForRetarded);

        //Обычный возврат на устаревшего курьера
        ClientReturnCommand.Create createCommand2 = ClientReturnCommand.Create.builder()
                .pickupPoint(pickupPoint)
                .barcode(prefix + "1234")
                .returnId("45671")
                .createdSource(CreatedSource.SELF)
                .source(Source.SYSTEM)
                .build();
        ClientReturn clientReturnForRetarded = clientReturnCommandService.create(createCommand2);
        clientReturnForRetarded.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
        clientReturnRepository.save(clientReturnForRetarded);

        //Обычный возврат на крутого курьера
        ClientReturnCommand.Create createCommand3 = ClientReturnCommand.Create.builder()
                .pickupPoint(pickupPoint)
                .barcode(prefix + "12345")
                .returnId("456711")
                .createdSource(CreatedSource.SELF)
                .source(Source.SYSTEM)
                .build();
        ClientReturn clientReturnForModern = clientReturnCommandService.create(createCommand3);
        clientReturnForModern.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
        clientReturnRepository.save(clientReturnForModern);

        //Заказ, который уже забрал другой курьер
        ClientReturnCommand.Create createCommand4 = ClientReturnCommand.Create.builder()
                .pickupPoint(pickupPoint)
                .barcode(prefix + "123456")
                .returnId("4567111")
                .createdSource(CreatedSource.SELF)
                .source(Source.SYSTEM)
                .build();
        ClientReturn clientReturnBad = clientReturnCommandService.create(createCommand4);
        clientReturnBad.setStatus(ClientReturnStatus.RECEIVED);
        clientReturnRepository.save(clientReturnBad);


        commandService.createPickupSubtaskClientReturn(
                null,
                new UserShiftCommand.CreatePickupSubtaskClientReturn(
                        retardedShift.getId(),
                        retardedLockerDeliveryTask.getRoutePoint().getId(),
                        retardedLockerDeliveryTask.getId(),
                        badClientReturnWithTaskForRetarded.getId(),
                        LockerDeliverySubtaskStatus.FINISHED,
                        Source.SYSTEM
                )
        );
        commandService.createPickupSubtaskClientReturn(
                null,
                new UserShiftCommand.CreatePickupSubtaskClientReturn(
                        retardedShift.getId(),
                        retardedLockerDeliveryTask.getRoutePoint().getId(),
                        retardedLockerDeliveryTask.getId(),
                        goodClientReturnWithTaskForRetarded.getId(),
                        LockerDeliverySubtaskStatus.FINISHED,
                        Source.SYSTEM
                )
        );

        //Курьеры добрались до пвз
        userHelper.checkinAndFinishPickup(modernShift);
        userHelper.checkinAndFinishPickup(retardedShift);
        userHelper.arriveAtRoutePoint(retardedLockerDeliveryTask.getRoutePoint());
        userHelper.arriveAtRoutePoint(modernLockerDeliveryTask.getRoutePoint());

        //Курьеры выгрузили заказы
        commandService.finishLoadingLocker(modernUser,
                new UserShiftCommand.FinishLoadingLocker(modernShift.getId(),
                        modernShift.getCurrentRoutePoint().getId(), modernLockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "Не вышло"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of())
                                .build()));
        commandService.finishLoadingLocker(retardedUser,
                new UserShiftCommand.FinishLoadingLocker(retardedShift.getId(),
                        retardedShift.getCurrentRoutePoint().getId(), retardedLockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "Не вышло"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of())
                                .build()));


        //Современный курьер начинает процесс забора возвратов и получает перечень доступных для сканирования баркодов
        List<TaskDto> list = taskDtoMapper.mapTasks(modernLockerDeliveryTask.getRoutePoint(), Set.of(order1),
                modernUser);
        assertThat(list).hasSize(1);
        LockerDeliveryTaskDto taskDto = (LockerDeliveryTaskDto) list.get(0);
        List<ClientReturnDto> clientReturns = taskDto.getClientReturns();
        assertThat(clientReturns).hasSize(4);
        Set<Long> modernIds = clientReturns.stream().map(ClientReturnDto::getId).collect(Collectors.toSet());
        assertThat(modernIds).contains(badClientReturnWithTaskForRetarded.getId(),
                goodClientReturnWithTaskForRetarded.getId(), clientReturnForRetarded.getId(),
                clientReturnForModern.getId());

        //Современный курьер забирает два возврата, один из которых был назначен джобой на второго курьера
        commandService.finishUnloadingLocker(modernUser,
                new UserShiftCommand.FinishUnloadingLocker(modernShift.getId(),
                        modernLockerDeliveryTask.getRoutePoint().getId(),
                        modernLockerDeliveryTask.getId(),
                        Set.of(new UnloadedOrder(badClientReturnWithTaskForRetarded.getBarcode(), null, List.of()),
                                new UnloadedOrder(clientReturnForModern.getBarcode(), null, List.of()))));

        assertThat(clientReturnRepository.findAllByBarcodeIn(Set.of(badClientReturnWithTaskForRetarded.getBarcode(),
                        clientReturnForModern.getBarcode())).stream()
                .filter(clientReturn -> clientReturn.getStatus() == ClientReturnStatus.RECEIVED)
                .count()).isEqualTo(2);

        List<LockerSubtask> modernSubtask = lockerSubtaskRepository.findByTaskId(modernLockerDeliveryTask.getId());
        assertThat(modernSubtask.stream()
                .filter(lockerSubtask -> lockerSubtask.getType() == LockerSubtaskType.PICKUP_CLIENT_RETURN
                        && lockerSubtask.getStatus() == LockerDeliverySubtaskStatus.FINISHED)
                .count()).isEqualTo(2);

        //Устаревший курьер пытается забрать три возврата, из которых один назначен на него джобой и на месте,
        //а второй забрал крутой курьер. В итоге получает ошибку
        Assertions.assertThrows(TplInvalidParameterException.class,
                () -> commandService.finishUnloadingLocker(retardedUser,
                        new UserShiftCommand.FinishUnloadingLocker(retardedShift.getId(),
                                retardedLockerDeliveryTask.getRoutePoint().getId(),
                                retardedLockerDeliveryTask.getId(),
                                Set.of(new UnloadedOrder(badClientReturnWithTaskForRetarded.getBarcode(), null,
                                                List.of()),
                                        new UnloadedOrder(goodClientReturnWithTaskForRetarded.getBarcode(), null,
                                                List.of()),
                                        new UnloadedOrder(clientReturnForRetarded.getBarcode(), null, List.of())))));

        //Крутой курьер решает пожалеть своего коллегу и переоткрывает свое задание на заборку,
        //чтобы отдать ему свои возвраты
        commandService.reopenDeliveryTask(modernUser,
                new UserShiftCommand.ReopenOrderDeliveryTask(modernShift.getId(),
                        modernLockerDeliveryTask.getRoutePoint().getId(),
                        modernLockerDeliveryTask.getId(),
                        Source.COURIER));
        assertThat(clientReturnRepository.findAllByBarcodeIn(Set.of(badClientReturnWithTaskForRetarded.getBarcode(),
                        clientReturnForModern.getBarcode())).stream()
                .filter(clientReturn -> clientReturn.getStatus() == ClientReturnStatus.READY_FOR_RECEIVED)
                .count()).isEqualTo(2);
        List<LockerSubtask> modernSubtaskAfterReopen = lockerSubtaskRepository
                .findByTaskId(modernLockerDeliveryTask.getId());
        assertThat(modernSubtaskAfterReopen.stream()
                .filter(lockerSubtask -> lockerSubtask.getType() == LockerSubtaskType.PICKUP_CLIENT_RETURN
                        && lockerSubtask.getStatus() == LockerDeliverySubtaskStatus.FINISHED)
                .count()).isEqualTo(0);

        //Устаревший курьер еще раз получает перечень всех доступных возвратов и с радостью загружает в машину все
        commandService.reopenDeliveryTask(retardedUser,
                new UserShiftCommand.ReopenOrderDeliveryTask(retardedShift.getId(),
                        retardedLockerDeliveryTask.getRoutePoint().getId(),
                        retardedLockerDeliveryTask.getId(),
                        Source.COURIER));
        userHelper.arriveAtRoutePoint(retardedLockerDeliveryTask.getRoutePoint());
        commandService.finishLoadingLocker(retardedUser,
                new UserShiftCommand.FinishLoadingLocker(retardedShift.getId(),
                        retardedShift.getCurrentRoutePoint().getId(), retardedLockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "Не вышло"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of())
                                .build()));

        List<TaskDto> listRetarded = taskDtoMapper.mapTasks(retardedLockerDeliveryTask.getRoutePoint(), Set.of(order2),
                retardedUser);
        assertThat(listRetarded).hasSize(1);
        LockerDeliveryTaskDto taskDtoRetarded = (LockerDeliveryTaskDto) listRetarded.get(0);
        List<ClientReturnDto> clientReturnsRetarded = taskDtoRetarded.getClientReturns();
        assertThat(clientReturnsRetarded).hasSize(4);
        Set<Long> retardedIds = clientReturnsRetarded.stream().map(ClientReturnDto::getId).collect(Collectors.toSet());
        assertThat(retardedIds).contains(badClientReturnWithTaskForRetarded.getId(),
                goodClientReturnWithTaskForRetarded.getId(), clientReturnForRetarded.getId(),
                clientReturnForModern.getId());

        commandService.finishUnloadingLocker(retardedUser,
                new UserShiftCommand.FinishUnloadingLocker(retardedShift.getId(),
                        retardedLockerDeliveryTask.getRoutePoint().getId(),
                        retardedLockerDeliveryTask.getId(),
                        Set.of(new UnloadedOrder(badClientReturnWithTaskForRetarded.getBarcode(), null, List.of()),
                                new UnloadedOrder(goodClientReturnWithTaskForRetarded.getBarcode(), null, List.of()),
                                new UnloadedOrder(clientReturnForRetarded.getBarcode(), null, List.of()),
                                new UnloadedOrder(clientReturnForModern.getBarcode(), null, List.of()))));

        assertThat(clientReturnRepository.findAllByBarcodeIn(Set.of(badClientReturnWithTaskForRetarded.getBarcode(),
                        clientReturnForModern.getBarcode(), clientReturnForRetarded.getBarcode(),
                        goodClientReturnWithTaskForRetarded.getBarcode())).stream()
                .filter(clientReturn -> clientReturn.getStatus() == ClientReturnStatus.RECEIVED)
                .count()).isEqualTo(4);

        List<LockerSubtask> retardedSubtasks = lockerSubtaskRepository.findByTaskId(retardedLockerDeliveryTask.getId());
        assertThat(retardedSubtasks.stream()
                .filter(lockerSubtask -> lockerSubtask.getType() == LockerSubtaskType.PICKUP_CLIENT_RETURN
                        && lockerSubtask.getStatus() == LockerDeliverySubtaskStatus.FINISHED)
                .count()).isEqualTo(4);
    }
}
