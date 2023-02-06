package ru.yandex.market.tpl.tms.executor.shift;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.push.notification.PushNotificationRepository;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotification;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotificationPayload;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.xiva.model.PushEvent;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@RequiredArgsConstructor
class PushLostCouriersExecutorTest extends TplTmsAbstractTest {

    private final PushLostCouriersExecutor executor;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final PushNotificationRepository pushNotificationRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final Clock clock;

    @BeforeEach
    void initClock() {
        ClockUtil.initFixed(clock, LocalDate.now().plusDays(1L).atTime(11, 49));
    }

    @Test
    void noPushIfPickupNotFinished() {
        long uid = 18347650L;
        UserShift userShift = openUserShift(uid);
        executor.doRealJob(null);
        assertNoEventSent(uid, userShift.getId());
    }

    @Test
    void pushSimple() {
        long uid = 18347650L;
        var userShift = openUserShift(uid);
        finishPickup(userShift);
        executor.setLocationExpirationMs(0L);
        executor.doRealJob(null);
        assertEventSent(uid, userShift.getId());
    }

    @Test
    void noPushIfHasRecentNotification() {
        long uid = 18347650L;
        createPush(uid, clock.instant().minus(10, ChronoUnit.MINUTES));
        var userShift = openUserShift(uid);
        finishPickup(userShift);
        executor.setLocationExpirationMs(0L);
        executor.doRealJob(null);
        assertNoEventSent(uid, userShift.getId());
    }

    @Test
    void pushIfHasOldNotification() {
        long uid = 18347650L;
        createPush(uid, clock.instant().minus(1, ChronoUnit.HOURS));
        var userShift = openUserShift(uid);
        finishPickup(userShift);
        executor.setLocationExpirationMs(0L);
        executor.doRealJob(null);
        assertEventSent(uid, userShift.getId());

    }

    private UserShift openUserShift(long uid) {
        LocalDate date = LocalDate.now(clock);
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(date)
                .build());
        var user = testUserHelper.findOrCreateUser(uid, date);
        testUserHelper.findOrCreateOpenShift(date);
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(testUserHelper.findOrCreateOpenShift(date).getId())
                .routePoint(helper.taskPrepaid("my address", 12, order.getId()))
                .build();
        long userShiftId = commandService.createUserShift(createCommand);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
        return userShiftRepository.findByIdOrThrow(userShiftId);
    }

    private void finishPickup(UserShift userShift) {
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
    }

    private void assertEventSent(long uid, Long userShiftId) {
        assertThat(
                pushNotificationRepository.findByXivaUserIdAndUserShiftIdOrderByIdDesc(
                                String.valueOf(uid), userShiftId).stream()
                        .filter(pn -> pn.getEvent() == PushEvent.COURIER_LOST)
                        .findAny()
        ).isPresent();
    }

    private void assertNoEventSent(long uid, long userShiftId) {
        assertThat(pushNotificationRepository.findByXivaUserIdAndUserShiftIdOrderByIdDesc(
                        String.valueOf(uid), userShiftId).stream()
                .filter(pn -> pn.getEvent() == PushEvent.COURIER_LOST)
                .findAny()
        ).isNotPresent();
    }

    private void createPush(long uid, Instant createdAt) {
        PushNotification notification = new PushNotification(
                String.valueOf(uid),
                PushEvent.COURIER_LOST,
                "",
                "",
                0,
                PushNotificationPayload.EMPTY,
                0,
                createdAt,
                null,
                null,
                null,
                null
        );
        PushNotification saveNotification = pushNotificationRepository.save(notification);
        jdbcTemplate.update("UPDATE push_notification SET created_at = :createdAt WHERE id in (:ids)",
                new MapSqlParameterSource()
                        .addValue("createdAt", Date.from(createdAt))
                        .addValue("ids", List.of(saveNotification.getId()))
        );
    }

}
