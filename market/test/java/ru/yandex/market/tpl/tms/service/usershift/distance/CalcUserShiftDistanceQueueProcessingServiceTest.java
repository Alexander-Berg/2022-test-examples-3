package ru.yandex.market.tpl.tms.service.usershift.distance;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocationRepository;
import ru.yandex.market.tpl.core.service.location.distance.async.CalcUserShiftDistancePayload;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
@Disabled
public class CalcUserShiftDistanceQueueProcessingServiceTest extends TplTmsAbstractTest {

    public static final long USER_UID = 1L;
    public static final String REQUEST_ID = "requestId";
    private final CalcUserShiftDistanceQueueProcessingService calcUserShiftDistanceQueueProcessingService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final UserShiftRepository userShiftRepository;
    private final TestUserHelper userHelper;
    private final UserScheduleRuleRepository userScheduleRuleRepository;
    private final UserShiftCommandService commandService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserLocationRepository userLocationRepository;
    private User user;
    private Shift shift;
    private List<UserScheduleRule> rules;

    @DisplayName("Расчет ненулевого пробега в очереди с новым контрактом маппинга точек")
    @Test
    void processUserShiftWithUserLocationWithPostedAtSuccess() {
        LocalDate now = LocalDate.now();
        user = userHelper.findOrCreateUser(USER_UID, now);
        shift = userHelper.findOrCreateOpenShift(now);
        rules = userScheduleRuleRepository.findActiveRulesForInterval(List.of(user.getId()), now, now);
        Long userId = user.getId();

        Optional<UserShift> userShift = userShiftRepository.findByShiftIdAndUserId(shift.getId(), userId);
        Long userShiftId = userShift.map(UserShift::getId).orElse(null);
        CalcUserShiftDistancePayload payload = new CalcUserShiftDistancePayload(
                REQUEST_ID,
                userId,
                userShiftId
        );
        insertUserLocationWithPostedAt();
        insertRoutePoint();

        calcUserShiftDistanceQueueProcessingService.processPayload(payload);

        assertNotNull(userShiftId);
        Optional<UserShift> userShiftOptional = userShiftRepository.findByShiftIdAndUserId(userShiftId, userId);
        assertTrue(userShiftOptional.isPresent());
        BigDecimal transitDistance = userShiftOptional.get().getTransitDistance();
        assertNotNull(transitDistance);
        assertTrue(transitDistance.intValue() > 0);
        dbQueueTestUtil.assertQueueHasSize(QueueType.CALC_USER_SHIFT_DISTANCE, 0);

        clearAfterTest(user);
        clearAfterTest(shift);
        clearAfterTest(rules);
        userLocationRepository.findAll().forEach(this::clearAfterTest);
    }

    @DisplayName("Расчет пустого пробега в очереди")
    @Test
    void processUserShiftWithEmptyUserLocationSuccess() {
        LocalDate now = LocalDate.now();
        user = userHelper.findOrCreateUser(USER_UID, now);
        shift = userHelper.findOrCreateOpenShift(now);
        rules = userScheduleRuleRepository.findActiveRulesForInterval(List.of(user.getId()), now, now);
        Long userId = user.getId();


        Optional<UserShift> userShift = userShiftRepository.findByShiftIdAndUserId(shift.getId(), userId);
        Long userShiftId = userShift.map(UserShift::getId).orElse(null);
        CalcUserShiftDistancePayload payload = new CalcUserShiftDistancePayload(
                REQUEST_ID,
                userId,
                userShiftId
        );

        calcUserShiftDistanceQueueProcessingService.processPayload(payload);

        assertNotNull(userShiftId);
        Optional<UserShift> userShiftOptional = userShiftRepository.findByShiftIdAndUserId(userShiftId, userId);
        assertTrue(userShiftOptional.isPresent());
        BigDecimal transitDistance = userShiftOptional.get().getTransitDistance();
        assertNotNull(transitDistance);
        assertEquals(transitDistance.intValue(), 0);
        dbQueueTestUtil.assertQueueHasSize(QueueType.CALC_USER_SHIFT_DISTANCE, 0);

        clearAfterTest(user);
        clearAfterTest(shift);
        userLocationRepository.findAll().forEach(this::clearAfterTest);
    }

    private void insertUserLocationWithPostedAt() {
        jdbcTemplate.execute(getInsertUserLocationWithPostedAtSql(), PreparedStatement::execute);
    }

    private void insertRoutePoint() {
        jdbcTemplate.execute(getInsertRoutePoint(), PreparedStatement::execute);
    }

    private String getInsertUserLocationWithPostedAtSql() {
        return "INSERT INTO user_location (id, user_id, longitude, latitude, device_id, " +
                "route_point_id, route_point_status_after, created_at, updated_at, user_shift_id, posted_at) VALUES " +
                "(60963109, 1, 39.8271984, 47.1046228, null, null, null, '2021-02-18 23:41:26.177288', '2021-02-18" +
                " 23:41:26.177220', 1, '2021-02-18 23:41:26.177288'),\n" +
                "(60963931, 1, 39.8271984, 47.1046228, null, null, null, '2021-02-18 23:55:50.554986', '2021-02-18" +
                " 23:55:50.554936', 1, '2021-02-18 23:55:50.554986'),\n" +
                "(60964369, 1, 39.8271984, 47.1046228, null, null, null, '2021-02-19 00:04:03.803065', '2021-02-19" +
                " 00:04:03.803025', 1, '2021-02-19 00:04:03.803065'),\n" +
                "(60964796, 1, 39.9028902, 47.1276918, null, null, null, '2021-02-19 00:11:42.054716', '2021-02-19" +
                " 00:11:42.054660', 1, '2021-02-19 00:11:42.054716'),\n" +
                "(60961957, 1, 39.9028887, 47.1276855, null, null, null, '2021-02-18 23:19:20.407609', '2021-02-18" +
                " 23:19:20.407553', 1, '2021-02-18 23:19:20.407609'),\n" +
                "(60964048, 1, 39.8880441, 47.1298755, null, null, null, '2021-02-18 23:57:53.801722', '2021-02-18" +
                " 23:57:53.801648', 1, '2021-02-18 23:57:53.801722'),\n" +
                "(60962063, 1, 39.9029131, 47.1277256, null, null, null, '2021-02-18 23:21:23.752650', '2021-02-18" +
                " 23:21:23.752539', 1, '2021-02-18 23:21:23.752650'),\n" +
                "(60963083, 1, 39.9029072, 47.1277257, null, null, null, '2021-02-18 23:40:55.856552', '2021-02-18" +
                " 23:40:55.856494', 1, '2021-02-18 23:40:55.856552'),\n" +
                "(60963282, 1, 39.9028881, 47.1276806, null, null, null, '2021-02-18 23:44:30.908710', '2021-02-18" +
                " 23:44:30.908658', 1, '2021-02-18 23:44:30.908710'),\n" +
                "(60963396, 1, 39.9028785, 47.1276603, null, null, null, '2021-02-18 23:46:34.580583', '2021-02-18" +
                " 23:46:34.580532', 1, '2021-02-18 23:46:34.580583');";
    }

    private String getInsertRoutePoint() {
        return "INSERT INTO route_point (id, user_shift_id, name, status, expected_date_time, address, " +
                "longitude, latitude, created_at, updated_at, type, arrival_time, wrong_location_comment, " +
                "explicit_order_number) " +
                "VALUES (209092, 1, 'ул. улица Цюрупы, д. 8, кв. 275', 'FINISHED', " +
                "'2020-04-25 07:53:59.000000', 'ул. улица Цюрупы, д. 8, кв. 275', 37.57739, 55.670386, '2020-04-24 " +
                "21:04:37.440647', '2020-04-25 08:06:27.478633', 'DELIVERY', '2020-04-25 08:02:50.816153', null, " +
                "null);";
    }

}
