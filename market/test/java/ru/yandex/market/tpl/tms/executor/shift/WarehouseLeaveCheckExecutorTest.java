package ru.yandex.market.tpl.tms.executor.shift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.service.location.LocationService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor
class WarehouseLeaveCheckExecutorTest extends TplTmsAbstractTest {
    private static final String DEVICE_ID = "device_id";

    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper helper;

    private final LocationService locationService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final WarehouseLeaveCheckExecutor executor;

    private final Clock clock;

    private User user;
    private UserShift userShift;
    private GeoPoint scLocation;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);

        user = userHelper.findOrCreateUser(234682L);
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(Instant.now()))
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        scLocation = userShift.getShift().getSortingCenter().getGeoPoint();
    }

    @Test
    void testNotLeaveWarehouseBeforeCheckIn() {
        locationService.addUserLocation(user, new LocationDto(
                scLocation.getLongitude().add(BigDecimal.TEN),
                scLocation.getLatitude().add(BigDecimal.TEN),
                DEVICE_ID,
                userShift.getId()
        ));
        executor.doRealJob(null);

        assertThat(userShift.getWarehouseAreaLeaveTime()).isNull();
    }

    @Test
    void testNotLeaveWarehouseIfTooClose() {
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift, true);

        locationService.addUserLocation(user, new LocationDto(
                scLocation.getLongitude().add(BigDecimal.valueOf(0.0001)),
                scLocation.getLatitude().add(BigDecimal.valueOf(0.0001)),
                DEVICE_ID,
                userShift.getId()
        ));
        executor.doRealJob(null);

        assertThat(userShift.getWarehouseAreaLeaveTime()).isNull();
    }

    @Test
    void testLeaveWarehouseIfFarAway() {
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift, true);

        locationService.addUserLocation(user, new LocationDto(
                scLocation.getLongitude().add(BigDecimal.TEN),
                scLocation.getLatitude().add(BigDecimal.TEN),
                DEVICE_ID,
                userShift.getId()
        ));
        executor.doRealJob(null);

        userShift = repository.findByIdOrThrow(userShift.getId());
        assertThat(userShift.getWarehouseAreaLeaveTime()).isNotNull();
    }

    @Test
    void testLeaveTimeNotUpdated() {
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift, true);

        locationService.addUserLocation(user, new LocationDto(
                scLocation.getLongitude().add(BigDecimal.TEN),
                scLocation.getLatitude().add(BigDecimal.TEN),
                DEVICE_ID,
                userShift.getId()
        ));
        executor.doRealJob(null);

        Instant firstLeaveTime = userShift.getWarehouseAreaLeaveTime();

        locationService.addUserLocation(user, new LocationDto(
                scLocation.getLongitude().add(BigDecimal.TEN),
                scLocation.getLatitude().add(BigDecimal.TEN),
                DEVICE_ID,
                userShift.getId()
        ));
        executor.doRealJob(null);

        assertThat(userShift.getWarehouseAreaLeaveTime()).isEqualTo(firstLeaveTime);
    }
}
