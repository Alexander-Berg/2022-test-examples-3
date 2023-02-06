package ru.yandex.market.tpl.core.query.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.order.PlaceDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatisticsDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlace;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.DeliverySubtask;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
@Slf4j
class BatchOrderUserShiftQueryServiceTest {

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftQueryService queryService;
    private final Clock clock;
    private final TestDataFactory testDataFactory;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private User user;
    private UserShift userShift;

    @BeforeEach
    void init() {
        transactionTemplate.execute(st -> {
            user = testUserHelper.findOrCreateUser(1L);
            return null;
        });
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());

        Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();
        RoutePointAddress myAddress = new RoutePointAddress("my_address", geoPoint);

        NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order1, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery1)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        userShift = userShiftRepository.findByIdWithRoutePoints(userShiftId).orElseThrow();
    }

    @Test
    void getUserShiftStatistics() {
        UserShiftStatisticsDto statisticsDto = queryService.getUserShiftStatisticsDto(user, userShift.getId());
        assertThat(statisticsDto).isNotNull();
        assertThat(statisticsDto.getNumberOfAllTasks()).isEqualTo(1);
        assertThat(statisticsDto.getNumberOfFinishedTasks()).isEqualTo(0);
    }

    @SneakyThrows
    @Test
    void getLockerTasksInfo() {

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);

        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .places(List.of(
                        OrderPlaceDto.builder()
                                .barcode(new OrderPlaceBarcode("123", "place1"))
                                .build(),
                        OrderPlaceDto.builder()
                                .barcode(new OrderPlaceBarcode("123", "place2"))
                                .build()
                ))
                .withBatch(true)
                .build());
//        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
//                .build());

        OrderPlace placeInsideBatch = StreamEx.of(order.getPlaces())
                .filter(op -> op.getCurrentBatch().isPresent())
                .findFirst()
                .orElseThrow();
        String placeOutsideBatchBarcode = StreamEx.of(order.getPlaces())
                .remove(op -> op.getCurrentBatch().isPresent())
                .map(OrderPlace::getBarcode)
                .map(OrderPlaceBarcode::getBarcode)
                .findFirst()
                .orElseThrow();
        Order orderToReturn = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("123")
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        Order orderToReturn2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("456")
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        String unknownOrderExternalOrderId = "unknown";
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
//        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order2);
        DeliveryTask lockerDeliveryTask = userShift.streamDeliveryTasks()
                .filter(dt -> dt.streamDeliveryOrderSubtasks()
                        .map(DeliverySubtask::getOrderId)
                        .toList().contains(order.getId()))
                .findFirst()
                .orElseThrow();

        testUserHelper.checkinAndFinishPickup(userShift);

        RemainingOrderDeliveryTasksDto remainingTasksInfo = queryService.getRemainingTasksInfo(user);

        assertThat(remainingTasksInfo.getOrders()).hasSize(3);
        assertThat(remainingTasksInfo.getOrders()).extracting(OrderSummaryDto::getOrdinalNumber).containsExactly(
                1, // <-- client order
                2, // <-- locker order, outside batch
                2  // <-- locker order, inside batch
        );
        OrderSummaryDto pickupPointBatchSummary = StreamEx.of(remainingTasksInfo.getOrders())
                .filter(o -> o.getBatch() != null)
                .findFirst()
                .orElseThrow();

        var batchBarcode = placeInsideBatch.getCurrentBatch().get().getBarcode();
        String placeInsideBatchBarcode = placeInsideBatch.getBarcode().getBarcode();
        assertThat(pickupPointBatchSummary.getBatch()).isEqualTo(
                OrderBatchDto.builder()
                        .barcode(batchBarcode)
                        .orders(Set.of(
                                OrderDto.builder()
                                        .externalOrderId(order.getExternalOrderId())
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode(placeInsideBatchBarcode)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );
        String multiOrderId = pickupPointBatchSummary.getMultiOrderId();
        OrderSummaryDto pickupPointOutsideBatchSummary = StreamEx.of(remainingTasksInfo.getOrders())
                .filter(summary -> summary.getMultiOrderId().equals(multiOrderId))
                .filter(summary -> summary.getBatch() == null)
                .findFirst()
                .orElseThrow();
        assertThat(pickupPointOutsideBatchSummary.getPlaces()).isEqualTo(
                List.of(
                        PlaceForScanDto.builder()
                                .barcode(placeOutsideBatchBarcode)
                                .build()
                )
        );

        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());
        commandService.finishLoadingLocker(user, new UserShiftCommand.FinishLoadingLocker(userShift.getId(),
                lockerDeliveryTask.getRoutePoint().getId(), lockerDeliveryTask.getId(), null, ScanRequest.builder()
                .successfullyScannedOrders(List.of(order.getId()))
                .build()));
        commandService.finishUnloadingLocker(user, new UserShiftCommand.FinishUnloadingLocker(
                userShift.getId(),
                lockerDeliveryTask.getRoutePoint().getId(),
                lockerDeliveryTask.getId(),
                Set.of(
                        new UnloadedOrder(orderToReturn.getExternalOrderId(), null, List.of()),
                        new UnloadedOrder(orderToReturn2.getExternalOrderId(), null, List.of()),
                        new UnloadedOrder(unknownOrderExternalOrderId, null, List.of())
                )
        ));

        OrderDeliveryTasksDto tasksInfo = queryService.getTasksInfo(user, null);
        List<OrderSummaryDto> lockerSummaries = tasksInfo.getTasks().stream()
                .filter(s -> s.getType() == TaskType.LOCKER_DELIVERY)
                .collect(Collectors.toList());
        Map<Boolean, List<OrderSummaryDto>> isReturnToLockerOrderSummaries = lockerSummaries.stream()
                .collect(Collectors.partitioningBy(OrderSummaryDto::getHasReturn));

        assertThat(lockerSummaries)
                .hasSize(5)
                .extracting(OrderSummaryDto::getMultiOrderId)
                .containsOnly("pickup" + lockerDeliveryTask.getId());
        OrderSummaryDto batchDeliverySummary = StreamEx.of(isReturnToLockerOrderSummaries.get(false))
                .filter(summary -> summary.getBatch() != null)
                .findFirst()
                .orElseThrow();
        assertThat(batchDeliverySummary.getBatch()).isEqualTo(
                OrderBatchDto.builder()
                        .barcode(batchBarcode)
                        .orders(Set.of(
                                OrderDto.builder()
                                        .externalOrderId(order.getExternalOrderId())
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode(placeInsideBatchBarcode)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );
        assertThat(isReturnToLockerOrderSummaries.get(true))
                .hasSize(3)
                .extracting(OrderSummaryDto::getExternalOrderId, OrderSummaryDto::isCanBeGrouped)
                .containsExactlyInAnyOrder(
                        tuple(orderToReturn.getExternalOrderId(), true),
                        tuple(orderToReturn2.getExternalOrderId(), true),
                        tuple(unknownOrderExternalOrderId, true)
                );

    }

}
