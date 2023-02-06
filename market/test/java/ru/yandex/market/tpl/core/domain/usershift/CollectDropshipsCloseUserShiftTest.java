package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_SHIFT_CLOSED_CRON_UPDATED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class CollectDropshipsCloseUserShiftTest {

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final MovementGenerator movementGenerator;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftManager userShiftManager;
    private final EntityManager entityManager;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;
    private Shift shift;
    private User user;
    private UserShift userShift;
    private RoutePoint collectDropshipRoutePoint;
    private RoutePoint collectDropshipRoutePoint2;
    private CollectDropshipTask collectDropshipTask;
    private Movement movement;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void init() {
        when(configurationProviderAdapter.isBooleanEnabled(IS_SHIFT_CLOSED_CRON_UPDATED)).thenReturn(true);

        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder().paymentType(OrderPaymentType.CASH).build()
        );
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        movement = movementGenerator.generate(MovementCommand.Create.builder().build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .routePoint(helper.taskCollectDropship(LocalDate.now(clock), movement))
                .build();

        long id = commandService.createUserShift(createCommand);
        userShift = repository.findById(id).orElseThrow();
        List<CollectDropshipTask> collectDropshipTasks = userShift.streamCollectDropshipTasks()
                .collect(Collectors.toList());
        collectDropshipTask = collectDropshipTasks.get(0);
        collectDropshipRoutePoint = collectDropshipTask.getRoutePoint();
        userHelper.checkinAndFinishPickup(userShift);
        userHelper.finishDelivery(userShift.streamDeliveryRoutePoints().findFirst().orElseThrow(), false);

    }

    @Test
    void getUserShiftStatistics() {
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShift.getId(), collectDropshipRoutePoint.getId(),
                        new LocationDto(
                                collectDropshipRoutePoint.getGeoPoint().getLongitude(),
                                collectDropshipRoutePoint.getGeoPoint().getLatitude(),
                                "", userShift.getId())));
        Long routePointId = userShift.streamReturnRoutePoints().findFirst().get().getId();
        jdbcTemplate.update("UPDATE route_point SET updated_at = now() - INTERVAL '3 hours' WHERE id = ?",
                routePointId);

        List<UserShift> userShiftsToClose = getAllShiftsToClose();
        assertThat(userShiftsToClose).isEmpty();
        entityManager.detach(userShift);
        userShift = repository.findById(userShift.getId()).orElseThrow();

        commandService.collectDropships(user, new UserShiftCommand.CollectDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId()
        ));
        routePointId = userShift.getCurrentRoutePoint().getId();
        jdbcTemplate.update("UPDATE route_point SET updated_at = now() - INTERVAL '3 hours' WHERE id = ?",
                routePointId);
        userShiftsToClose = getAllShiftsToClose();
        assertThat(userShiftsToClose).isNotEmpty();

    }

    private List<UserShift> getAllShiftsToClose() {
        return userShiftManager.findShiftsToClose(shift.getShiftDate(),
                Instant.now().plusSeconds(1));
    }
}
