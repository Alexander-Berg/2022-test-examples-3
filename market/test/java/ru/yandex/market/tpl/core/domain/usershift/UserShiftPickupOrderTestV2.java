package ru.yandex.market.tpl.core.domain.usershift;


import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstance;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceType;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.service.usershift.additionaldata.vehicle.AdditionalVehicleDataService;
import ru.yandex.market.tpl.core.service.usershift.additionaldata.vehicle.UpdateAdditionalVehicleDataDto;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus.FINISHED;
import static ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus.UPDATING_VEHICLE_INFO;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.IN_PROGRESS;


@RequiredArgsConstructor
public class UserShiftPickupOrderTestV2 extends TplAbstractTest {

    private final Clock clock;

    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper helper;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final VehicleGenerateService vehicleGenerateService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final UserRepository userRepository;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;
    private final AdditionalVehicleDataService additionalVehicleDataService;
    private final UserPropertyService userPropertyService;
    private final TrackingService trackingService;

    private User user;
    private UserShift userShift;
    private Order order;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(3523601L);

        initTasks();
    }

    void initTasks() {
        transactionTemplate.execute(ts -> {
            Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

            order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .paymentType(OrderPaymentType.CASH)
                    .paymentStatus(OrderPaymentStatus.UNPAID)
                    .build());

            var createCommand = UserShiftCommand.Create.builder()
                    .userId(user.getId())
                    .shiftId(shift.getId())
                    .routePoint(helper.taskOrderPickup(clock.instant()))
                    .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                    .mergeStrategy(SimpleStrategies.NO_MERGE)
                    .build();

            userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
            commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
            return null;
        });
    }

    @ParameterizedTest
    @MethodSource("updateVehicleInfoParams")
    void shouldUpdateVehicleInfoBeforeFinish(VehicleInstanceType vehicleInstanceType, Instant infoUpdatedAt,
                                             OrderPickupTaskStatus expectedStatus) {
        assignVehicleToUser(vehicleInstanceType, infoUpdatedAt);

        PickupTaskInfo info = arriveAtPickupReturnInfo();

        List<Long> expectedPickupOrders = List.of(order.getId());
        List<Long> expectedSkippedOrders = List.of();
        var pickupOrdersCommand = getPickupOrdersCommand(info, expectedPickupOrders, expectedSkippedOrders);

        commandService.startOrderPickup(user, info.getPickupStartCommand());
        commandService.pickupOrders(user, pickupOrdersCommand);

        finishLoading(info);
        assertPickupTaskHasStatus(expectedStatus);
    }

    @Test
    void shouldUpdateVehicleInfoBeforeFinishIdempotent() {
        assignVehicleToUser(VehicleInstanceType.PUBLIC, null);

        PickupTaskInfo info = arriveAtPickupReturnInfo();

        List<Long> expectedPickupOrders = List.of(order.getId());
        List<Long> expectedSkippedOrders = List.of();
        var pickupOrdersCommand = getPickupOrdersCommand(info, expectedPickupOrders, expectedSkippedOrders);

        commandService.startOrderPickup(user, info.getPickupStartCommand());
        commandService.pickupOrders(user, pickupOrdersCommand);

        finishLoading(info);

        assertPickupTaskHasStatus(UPDATING_VEHICLE_INFO);
        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        finishLoading(info);

        assertPickupTaskHasStatus(UPDATING_VEHICLE_INFO);
    }

    @Test
    void shouldShowTrackingWhenCourierOnUpdateVehicle() {
        assignVehicleToUser(VehicleInstanceType.PUBLIC, null);

        PickupTaskInfo info = arriveAtPickupReturnInfo();

        List<Long> expectedPickupOrders = List.of(order.getId());
        List<Long> expectedSkippedOrders = List.of();
        var pickupOrdersCommand = getPickupOrdersCommand(info, expectedPickupOrders, expectedSkippedOrders);

        commandService.startOrderPickup(user, info.getPickupStartCommand());
        commandService.pickupOrders(user, pickupOrdersCommand);

        finishLoading(info);

        assertPickupTaskHasStatus(UPDATING_VEHICLE_INFO);

        String trackingId = trackingService.getTrackingLinkByOrder(order.getExternalOrderId()).orElseThrow();
        TrackingDto trackingDto = trackingService.getTrackingDto(trackingId);
        assertThat(trackingDto.getDelivery().getStatus()).isEqualTo(IN_PROGRESS);
    }

    @Test
    void shouldFinishLoadAfterUpdateVehicleData() {
        var vehicleInstance = assignVehicleToUser(VehicleInstanceType.PUBLIC, null);

        PickupTaskInfo info = arriveAtPickupReturnInfo();

        List<Long> expectedPickupOrders = List.of(order.getId());
        List<Long> expectedSkippedOrders = List.of();
        var pickupOrdersCommand = getPickupOrdersCommand(info, expectedPickupOrders, expectedSkippedOrders);

        commandService.startOrderPickup(user, info.getPickupStartCommand());
        commandService.pickupOrders(user, pickupOrdersCommand);

        finishLoading(info);

        assertPickupTaskHasStatus(UPDATING_VEHICLE_INFO);
        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        String registrationNumber = "A098BC";
        String registrationNumberRegion = "000";

        var dto = UpdateAdditionalVehicleDataDto.builder()
                .userShiftId(userShift.getId())
                .vehicleDataDto(UpdateAdditionalVehicleDataDto.UpdateVehicleDataDto.builder()
                        .vehicleInstanceId(vehicleInstance.getId())
                        .registrationNumber(registrationNumber)
                        .vehicle(vehicleInstance.getVehicle())
                        .registrationNumberRegion(registrationNumberRegion)
                        .build())
                .build();
        additionalVehicleDataService.updateVehicleData(user, dto);

        assertPickupTaskHasStatus(FINISHED);
    }


    private UserShiftCommand.FinishScan getPickupOrdersCommand(PickupTaskInfo info, List<Long> expectedPickupOrders,
                                                               List<Long> expectedSkippedOrders) {
        return new UserShiftCommand.FinishScan(
                userShift.getId(),
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(expectedPickupOrders)
                        .skippedOrders(expectedSkippedOrders)
                        .comment("")
                        .finishedAt(Instant.now(clock))
                        .build()
        );
    }

    private PickupTaskInfo arriveAtPickupReturnInfo() {
        return transactionTemplate.execute(ts -> {
            user = userRepository.findByIdOrThrow(user.getId());
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            var info = new PickupTaskInfo(userShift);
            commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                    userShift.getId(), info.getRoutePoint().getId(),
                    new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId())
            ));
            return info;
        });
    }

    private VehicleInstance assignVehicleToUser(VehicleInstanceType vehicleInstanceType, Instant infoUpdatedAt) {
        var vehicle = vehicleGenerateService.generateVehicle();
        return vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                .type(vehicleInstanceType)
                .infoUpdatedAt(infoUpdatedAt)
                .users(List.of(user))
                .vehicle(vehicle)
                .build());
    }

    private void assertPickupTaskHasStatus(OrderPickupTaskStatus updatingVehicleInfo) {
        transactionTemplate.execute(ts -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            var pickupTask = userShift.streamPickupTasks()
                    .findFirst()
                    .orElseThrow(() ->
                            new TplEntityNotFoundException(OrderPickupTask.class));
            assertThat(pickupTask.getStatus()).isEqualTo(updatingVehicleInfo);
            return null;
        });
    }

    private void finishLoading(PickupTaskInfo info) {
        commandService.finishLoading(
                user,
                new UserShiftCommand.FinishLoading(
                        userShift.getId(),
                        info.getRoutePoint().getId(),
                        info.getTask().getId()));
    }

    private static Stream<Arguments> updateVehicleInfoParams() {
        Instant instant = LocalDateTime.of(LocalDate.of(2021, Month.DECEMBER, 15), LocalTime.of(12,30)).toInstant(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(VehicleInstanceType.PUBLIC, instant, UPDATING_VEHICLE_INFO),
                Arguments.of(VehicleInstanceType.PERSONAL, null, UPDATING_VEHICLE_INFO),
                Arguments.of(VehicleInstanceType.PERSONAL, instant.plusSeconds(3600 * 24), OrderPickupTaskStatus.FINISHED)
        );
    }

    @Value
    private static class PickupTaskInfo {

        RoutePoint routePoint;
        OrderPickupTask task;
        UserShiftCommand.StartScan pickupStartCommand;

        PickupTaskInfo(UserShift userShift) {
            routePoint = userShift.getRoutePoints().get(0);
            task = routePoint.streamPickupTasks().collect(Collectors.toList()).get(0);
            pickupStartCommand = new UserShiftCommand.StartScan(
                    userShift.getId(),
                    routePoint.getId(),
                    task.getId()
            );
        }

    }

}
