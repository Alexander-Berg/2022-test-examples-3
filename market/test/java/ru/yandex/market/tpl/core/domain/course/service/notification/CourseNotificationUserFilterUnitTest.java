package ru.yandex.market.tpl.core.domain.course.service.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.core.domain.course.domain.notification.CourseNotificationDayType;
import ru.yandex.market.tpl.core.domain.course.domain.notification.CourseNotificationRule;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserSchedule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleData;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class CourseNotificationUserFilterUnitTest {

    @InjectMocks
    private CourseNotificationUserFilter courseNotificationUserFilter;

    @Mock
    private UserScheduleRuleRepository scheduleRuleRepository;

    @Mock
    private CourseNotificationRuleAdapter ruleAdapter;

    @Mock
    private UserShiftRepository userShiftRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    private Map<Long, UserSchedule> userSchedules;
    private List<CourseNotificationRule> rules;
    private List<UserShiftRepository.UserShiftRawDataDto> userShifts;
    private List<User> users;

    @BeforeEach
    void setup() {
        ClockUtil.initFixed(clock);
        mockRepositories();
        mockTestRules();
    }

    @Test
    void filterUsersTimeZoneTest() {
        // Проверка попадает в интервал для выходных дней
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(18, 0)));

        mockUserData(1L, 3, false);
        mockUserData(2L, 4, false);
        mockUserData(3L, 6, false); // не подходит по времени, у него уже 21:00

        var filteredUsers = courseNotificationUserFilter.filterUsersToNotify(Set.of(1L, 2L, 3L));
        assertThat(filteredUsers).hasSize(2);
        assertThat(filteredUsers).contains(1L, 2L);
    }

    @Test
    void filterUsersDayTypeTest() {
        // Проверка попадет в интервал для рабочих дней
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(12, 0)));

        mockUserData(1L, 3, false); // не подходит, у него выходной
        mockUserData(2L, 3, true);

        var filteredUsers = courseNotificationUserFilter.filterUsersToNotify(Set.of(1L, 2L));
        assertThat(filteredUsers).hasSize(1);
        assertThat(filteredUsers).contains(2L);
    }

    @Test
    void filterUsersOnTaskTest() {
        // Проверка попадет в интервал для рабочих дней
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(12, 0)));

        mockUserData(1L, 3, true);
        var userOnTask = mockUserData(2L, 3, true);
        mockInProgressShift(userOnTask); // userOnTask не подходит, он на смене

        var filteredUsers = courseNotificationUserFilter.filterUsersToNotify(Set.of(1L, 2L));
        assertThat(filteredUsers).hasSize(1);
        assertThat(filteredUsers).contains(1L);
    }

    @Test
    void filterUsersBeforeShiftTest() {
        // Проверка попадет в интервал для рабочих дней
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(12, 0)));

        var user1 = mockUserData(1L, 3, true);
        var user2 = mockUserData(2L, 3, true);
        mockFutureShift(user1, LocalTime.of(13, 0)); // не подходит, у него скоро начнется смена
        mockFutureShift(user2, LocalTime.of(15, 0));

        var filteredUsers = courseNotificationUserFilter.filterUsersToNotify(Set.of(1L, 2L));
        assertThat(filteredUsers).hasSize(1);
        assertThat(filteredUsers).contains(2L);
    }

    @Test
    void filterUsersAfterShiftTest() {
        // Проверка попадет в интервал для рабочих дней
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(12, 0)));

        var user1 = mockUserData(1L, 3, true);
        var user2 = mockUserData(2L, 3, true);
        mockClosedShift(user1, LocalTime.of(10, 0));
        mockClosedShift(user2, LocalTime.of(11, 30)); // не подходит, у него недавно завершилась смена

        var filteredUsers = courseNotificationUserFilter.filterUsersToNotify(Set.of(1L, 2L));
        assertThat(filteredUsers).hasSize(1);
        assertThat(filteredUsers).contains(1L);
    }

    @Test
    void filterUsersDoubleIntervalTest() {
        // Проверка попадет в два интервала
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(14, 0)));
        mockUserData(1L, 3, true); // подходит для интервала для рабочих дней
        mockUserData(2L, 3, false); // подходит для интервала для не рабочих дней

        var filteredUsers = courseNotificationUserFilter.filterUsersToNotify(Set.of(1L, 2L));

        // Оба юзера должны проходить фильтр, один попадает в интервал для рабочих дней, второй - для не рабочих
        assertThat(filteredUsers).hasSize(2);
        assertThat(filteredUsers).contains(1L, 2L);
    }

    @Test
    void filterUsersOnTaskAllowedTest() {
        // Проверка попадет в интервал для рабочих дней
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(12, 0)));
        var user = mockUserData(1L, 3, true);
        mockInProgressShift(user);

        // Без правила оповещений с разрешенной отправкой в течение смены юзер не должен отбираться
        assertThat(courseNotificationUserFilter.filterUsersToNotify(Set.of(1L))).hasSize(0);

        var ruleForInProgressShift = mock(CourseNotificationRule.class);
        lenient().doReturn(CourseNotificationDayType.WORK_DAY_ON_TASK_ALLOWED).when(ruleForInProgressShift).getDayType();
        lenient().doReturn(LocalTime.of(11, 0)).when(ruleForInProgressShift).getIntervalStart();
        lenient().doReturn(LocalTime.of(13, 0)).when(ruleForInProgressShift).getIntervalEnd();
        rules.add(ruleForInProgressShift);

        // С новым правилом должен пройти фильтр
        assertThat(courseNotificationUserFilter.filterUsersToNotify(Set.of(1L))).hasSize(1);
    }

    @Test
    void filerDeletedUsersTest() {
        // Проверка попадет в интервал для рабочих дней
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(12, 0)));
        mockUserData(1L, 3, true, true);

        assertThat(courseNotificationUserFilter.filterUsersToNotify(Set.of(1L))).hasSize(0);
    }

    @Test
    void filterUsersWithDifferentTimeZoneAndOverrideSkip() {
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(15, 0)));
        var user = mockUserData(1L, 8, false, true, false);
        assertThat(courseNotificationUserFilter.filterUsersToNotify(Set.of(1L))).isEmpty();

        // есть смена на СЦ с таймзоной +07:00
        var userShift = mockClosedShiftEntity(user, LocalDateTime.now().minusDays(5));
        var sortingCenter = mock(SortingCenter.class);
        var offset = ZoneOffset.ofHours(7);
        doReturn(offset).when(sortingCenter).getZoneOffset();
        doReturn(sortingCenter).when(userShift).getSortingCenter();
        doReturn(Optional.of(userShift)).when(userShiftRepository).findTopByUserOrderByIdDesc(user);

        // Таймзона при отсутствии СЦ в расписании определяется по последней смене
        assertThat(courseNotificationUserFilter.filterUsersToNotify(Set.of(1L))).hasSize(1);

        // Время с учетом таймзоны выходит за интервал
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(LocalTime.of(18, 0)));
        assertThat(courseNotificationUserFilter.filterUsersToNotify(Set.of(1L))).isEmpty();
    }

    /**
     * Интервалы оповещений для тестов:
     * 11:00 - 15:00 для рабочих дней, не раньше чем за 2 часа до смены, не раньше чем через 1 час после смены
     * 13:00 - 20:00 для не рабочих дней
     */
    private void mockTestRules() {
        var ruleForWorkDays = mock(CourseNotificationRule.class);
        lenient().doReturn(CourseNotificationDayType.WORK_DAY).when(ruleForWorkDays).getDayType();
        lenient().doReturn(LocalTime.of(11, 0)).when(ruleForWorkDays).getIntervalStart();
        lenient().doReturn(LocalTime.of(15, 0)).when(ruleForWorkDays).getIntervalEnd();
        lenient().doReturn(Duration.ofHours(2)).when(ruleForWorkDays).getMinTimeBeforeShift();
        lenient().doReturn(Duration.ofHours(1)).when(ruleForWorkDays).getMinTimeAfterShift();

        var ruleForFreeDays = mock(CourseNotificationRule.class);
        lenient().doReturn(CourseNotificationDayType.FREE_DAY).when(ruleForFreeDays).getDayType();
        lenient().doReturn(LocalTime.of(13, 0)).when(ruleForFreeDays).getIntervalStart();
        lenient().doReturn(LocalTime.of(20, 0)).when(ruleForFreeDays).getIntervalEnd();

        rules.add(ruleForWorkDays);
        rules.add(ruleForFreeDays);
    }

    private User mockUserData(long userId, int offsetHours, boolean workDay) {
        return mockUserData(userId, offsetHours, workDay, false);
    }

    private User mockUserData(long userId, int offsetHours, boolean workDay, boolean deleted) {
        return mockUserData(userId, offsetHours, workDay, false, deleted);
    }

    private User mockUserData(long userId, int offsetHours, boolean workDay, boolean overrideSkip, boolean deleted) {
        var nowDate = LocalDate.now(clock);
        var user = mock(User.class);
        lenient().doReturn(deleted).when(user).isDeleted();
        lenient().doReturn(userId).when(user).getId();
        users.add(user);

        var schedule = mock(UserSchedule.class);
        lenient().doReturn(user).when(schedule).getUser();
        lenient().doReturn(workDay).when(schedule).isWorkDay(eq(nowDate));
        var scheduleRule = mock(UserScheduleRule.class);
        lenient().doReturn(Optional.of(scheduleRule)).when(schedule).getPrimaryActiveRule(eq(nowDate));
        lenient().doReturn(user).when(scheduleRule).getUser();

        if (!overrideSkip) {
            var sc = mock(SortingCenter.class);
            var zoneOffset = ZoneOffset.ofHours(offsetHours);
            lenient().doReturn(zoneOffset).when(sc).getZoneOffset();
            lenient().doReturn(sc).when(scheduleRule).getSortingCenter();
        }

        userSchedules.put(userId, schedule);
        return user;
    }

    private void mockClosedShift(User user, LocalTime closedAt) {
        mockClosedShift(user, LocalDate.now(clock).atTime(closedAt));
    }

    private UserShiftRepository.UserShiftRawDataDto mockClosedShift(User user, LocalDateTime closedAt) {
        var userShift = mock(UserShiftRepository.UserShiftRawDataDto.class);
        lenient().doReturn(user.getId()).when(userShift).getUserId();
        lenient().doReturn(UserShiftStatus.SHIFT_CLOSED).when(userShift).getStatus();
        var closeInstant = closedAt.toInstant(ZoneOffset.of("+03:00"));
        lenient().doReturn(closeInstant).when(userShift).getClosedAt();
        userShifts.add(userShift);
        return userShift;
    }

    private UserShift mockClosedShiftEntity(User user, LocalDateTime closedAt) {
        var userShift = mock(UserShift.class);
        lenient().doReturn(user).when(userShift).getUser();
        lenient().doReturn(UserShiftStatus.SHIFT_CLOSED).when(userShift).getStatus();
        var closeInstant = closedAt.toInstant(ZoneOffset.of("+03:00"));
        lenient().doReturn(closeInstant).when(userShift).getClosedAt();
        return userShift;
    }

    private void mockFutureShift(User user, LocalTime startsAt) {
        var userShift = mock(UserShiftRepository.UserShiftRawDataDto.class);
        lenient().doReturn(user.getId()).when(userShift).getUserId();
        lenient().doReturn(UserShiftStatus.SHIFT_CREATED).when(userShift).getStatus();
        var shiftScheduleData = mock(UserScheduleData.class);
        lenient().doReturn(startsAt).when(userShift).getLoadingStartTime();
        userShifts.add(userShift);
    }

    private void mockInProgressShift(User user) {
        var userShift = mock(UserShiftRepository.UserShiftRawDataDto.class);
        lenient().doReturn(user.getId()).when(userShift).getUserId();
        lenient().doReturn(UserShiftStatus.ON_TASK).when(userShift).getStatus();
        userShifts.add(userShift);
    }

    @SuppressWarnings("unchecked")
    private void mockRepositories() {
        userSchedules = new HashMap<>();
        userShifts = new ArrayList<>();
        rules = new ArrayList<>();
        users = new ArrayList<>();
        var nowDate = LocalDate.now(clock);

        lenient().doAnswer(invocation -> {
            var userIds = (Collection<Long>) invocation.getArgument(0);
            return userSchedules.entrySet().stream()
                    .filter(entry -> userIds.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }).when(scheduleRuleRepository).findSchedulesByUser(any(), eq(nowDate), eq(nowDate));

        lenient().doAnswer(invocation -> {
            var userIds = (Collection<Long>) invocation.getArgument(0);
            return userShifts.stream()
                    .filter(us -> userIds.contains(us.getUserId()))
                    .collect(Collectors.toList());
        }).when(userShiftRepository).findRawDataByUsersAndShiftDate(any(), eq(nowDate));

        lenient().doAnswer(invocation -> {
            var userIds = (Collection<Long>) invocation.getArgument(0);
            return users.stream()
                    .filter(user -> userIds.contains(user.getId()))
                    .collect(Collectors.toList());
        }).when(userRepository).findAllById(any());

        lenient().doReturn(rules).when(ruleAdapter).getRules();
    }
}
