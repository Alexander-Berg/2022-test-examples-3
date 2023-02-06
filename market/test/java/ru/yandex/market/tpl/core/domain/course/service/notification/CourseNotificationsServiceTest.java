package ru.yandex.market.tpl.core.domain.course.service.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.api.model.course.CoursePushDto;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.course.domain.Course;
import ru.yandex.market.tpl.core.domain.course.domain.CourseGenerationParams;
import ru.yandex.market.tpl.core.domain.course.domain.TestCourseHelper;
import ru.yandex.market.tpl.core.domain.course.domain.notification.CourseNotificationDayType;
import ru.yandex.market.tpl.core.domain.course.domain.notification.CourseNotificationRule;
import ru.yandex.market.tpl.core.domain.course.domain.notification.CourseNotificationStatus;
import ru.yandex.market.tpl.core.domain.course.repository.CourseNotificationRepository;
import ru.yandex.market.tpl.core.domain.course.repository.CourseNotificationRuleRepository;
import ru.yandex.market.tpl.core.domain.course.service.CourseAssignmentService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.push.notification.PushNotificationRepository;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotification;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.xiva.XivaClient;
import ru.yandex.market.tpl.core.external.xiva.model.PushEvent;
import ru.yandex.market.tpl.core.external.xiva.model.PushSendRequest;
import ru.yandex.market.tpl.core.service.user.partner.UserStatusService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleData;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.api.model.notification.PushRedirectType.COURSE_PAGE;
import static ru.yandex.market.tpl.core.domain.course.domain.notification.CourseNotificationDayType.FREE_DAY;
import static ru.yandex.market.tpl.core.domain.course.domain.notification.CourseNotificationDayType.WORK_DAY;
import static ru.yandex.market.tpl.core.external.xiva.model.PushEvent.COURSE;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class CourseNotificationsServiceTest extends TplAbstractTest {

    private final CourseNotificationRuleRepository courseNotificationRuleRepository;
    private final CourseNotificationRuleAdapter courseNotificationRuleAdapter;
    private final CourseNotificationRepository courseNotificationRepository;
    private final PushNotificationRepository pushNotificationRepository;
    private final CourseNotificationService courseNotificationService;
    private final UserShiftCommandService userShiftCommandService;
    private final CourseAssignmentService courseAssignmentService;
    private final ConfigurationServiceAdapter configuration;
    private final UserScheduleService userScheduleService;
    private final UserStatusService userStatusService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TestCourseHelper courseHelper;
    private final TestUserHelper userHelper;
    private final JdbcTemplate jdbcTemplate;
    private final XivaClient xivaClient;
    private final Clock clock;

    private final LocalDateTime testTime = LocalDateTime.of(2000, 10, 20, 14, 30);

    @BeforeEach
    void setup() {
        setTestTime();
        configuration.mergeValue(ConfigurationProperties.COURSE_ASSIGNMENT_FILTERS_ENABLED, true);
        configuration.mergeValue(ConfigurationProperties.COURSE_AUTO_ASSIGNMENT_ENABLED, true);
        configuration.mergeValue(ConfigurationProperties.COURSE_AUTO_NOTIFICATION_ENABLED, true);
    }

    @Test
    void courseNotificationDifferentUsersSendingTest() {
        // 4 тестовых юзера
        var users = generateTestUsers();
        var course = courseHelper.createPublishedTestCourse();
        var userIds = Set.of(users.get(0).getId(), users.get(1).getId(), users.get(2).getId(), users.get(3).getId());

        // Создаем назначения
        setTestTime(LocalTime.of(11, 30));
        courseAssignmentService.createCourseAssignments(course, userIds);

        // Должны быть созданы оповещения
        var notifications = courseNotificationRepository.findAll();
        assertThat(notifications).hasSize(4);
        for (var notification : notifications) {
            assertThat(notification.getStatus()).isEqualTo(CourseNotificationStatus.NEW);
        }

        // Тестовые настройки для выходного дня и отправляем оповещения
        setTestTime(LocalTime.of(12, 30));
        createRule(FREE_DAY, LocalTime.of(10, 0), LocalTime.of(16, 0)); // только для 1го юзера
        courseNotificationService.sendAutoNotifications();
        var firstPushTime = Instant.now(clock);

        // Должен отправиться пуш только для первого юзера
        checkNotificationSent(course, users.get(0), firstPushTime, CourseNotificationStatus.SENT, 1);
        checkNotificationNotSent(users.get(1));
        checkNotificationNotSent(users.get(2));
        checkNotificationNotSent(users.get(3));
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 1);

        // Тестовые настройки для рабочего дня и отправляем оповещения
        setTestTime(LocalTime.of(15, 30));
        createRule(WORK_DAY, LocalTime.of(14, 0), LocalTime.of(17, 0)); // для 2-4 юзеров
        courseNotificationService.sendAutoNotifications();

        // Оповещение для первого юзера уже было отправлено, для остальных должны отправиться
        checkNotificationSent(course, users.get(0), firstPushTime, CourseNotificationStatus.SENT, 1);
        checkNotificationSent(course, users.get(1), Instant.now(clock), CourseNotificationStatus.SENT, 1);
        checkNotificationSent(course, users.get(2), Instant.now(clock), CourseNotificationStatus.SENT, 1);
        checkNotificationSent(course, users.get(3), Instant.now(clock), CourseNotificationStatus.SENT, 1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 4);
    }

    @Test
    void courseNotificationUserFlowTest() {
        // Тестовый юзер - ближайшие дни выходные
        var nowDate = testTime.toLocalDate();
        var user = createTestUser(false, null, null);
        userScheduleService.scheduleOverride(user.getUid(), nowDate.plusDays(1), false, false);
        userScheduleService.scheduleOverride(user.getUid(), nowDate.plusDays(2), false, false);
        userScheduleService.scheduleOverride(user.getUid(), nowDate.plusDays(3), false, false);

        // Настройки для оповещений для курьера
        createRule(FREE_DAY, LocalTime.of(15, 0), LocalTime.of(20, 0));

        // Создаем тестовый курс и назначаем его курьеру - должно создаться оповещение
        setTestTime(LocalTime.of(14, 0));
        var course = courseHelper.createPublishedTestCourse();
        courseAssignmentService.createCourseAssignments(course, Set.of(user.getId()));
        var notifications = courseNotificationRepository.findAllByCourseAndUserIdIn(course, Set.of(user.getId()));
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getStatus()).isEqualTo(CourseNotificationStatus.NEW);

        // Время не попадает в интервал, оповещения не должны уходить
        courseNotificationService.sendAutoNotifications();
        checkNotificationNotSent(user);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 0);

        // Время попадает в интервал - должен уйти первый пуш
        setTestTime(LocalTime.of(15, 1));
        courseNotificationService.sendAutoNotifications();
        checkNotificationSent(course, user, Instant.now(clock), CourseNotificationStatus.SENT, 1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 1);
        dbQueueTestUtil.clear(QueueType.PUSH_NOTIFICATION_SEND);
        var pushSentTime = Instant.now(clock);

        // Время попадет в интервал, но не прошло минимальное время между пушами
        setTestTime(LocalTime.of(17, 0));
        courseNotificationService.sendAutoNotifications();
        checkNotificationSent(course, user, pushSentTime, CourseNotificationStatus.SENT, 1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 0);

        // Время попадает в интервал и прошло минимальное время между пушами - должен отправться второй пуш
        adjustTestTime(Duration.ofDays(1));
        courseNotificationService.sendAutoNotifications();
        checkNotificationSent(course, user, Instant.now(clock), CourseNotificationStatus.SENT, 2);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 1);
        dbQueueTestUtil.clear(QueueType.PUSH_NOTIFICATION_SEND);

        // Третья попытка - финальная
        adjustTestTime(Duration.ofDays(1));
        courseNotificationService.sendAutoNotifications();
        checkNotificationSent(course, user, Instant.now(clock), CourseNotificationStatus.FINISHED, 3);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 1);
        dbQueueTestUtil.clear(QueueType.PUSH_NOTIFICATION_SEND);
        pushSentTime = Instant.now(clock);

        // После третеь попытки пуши отрпавляться не должны
        adjustTestTime(Duration.ofDays(1));
        courseNotificationService.sendAutoNotifications();
        checkNotificationSent(course, user, pushSentTime, CourseNotificationStatus.FINISHED, 3);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 0);
    }

    @Test
    void courseNotificationUserStartedCourseTest() {
        // Тестовый юзер
        var nowDate = testTime.toLocalDate();
        var user = createTestUser(false, null, null);
        userScheduleService.scheduleOverride(user.getUid(), nowDate.plusDays(1), false, false);

        // Настройки для оповещений для курьера
        createRule(FREE_DAY, LocalTime.of(15, 0), LocalTime.of(20, 0));

        // Создаем тестовый курс и назначаем его курьеру
        setTestTime(LocalTime.of(14, 0));
        var course = courseHelper.createPublishedTestCourse();
        courseAssignmentService.createCourseAssignments(course, Set.of(user.getId()));

        // Время попадает в интервал - должен уйти первый пуш
        setTestTime(LocalTime.of(15, 1));
        courseNotificationService.sendAutoNotifications();
        checkNotificationSent(course, user, Instant.now(clock), CourseNotificationStatus.SENT, 1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 1);
        dbQueueTestUtil.clear(QueueType.PUSH_NOTIFICATION_SEND);
        var pushSentTime = Instant.now(clock);

        // Курьер начал прохождение курса - отправка пушей должна остановиться
        var assignment = courseAssignmentService.getAssignments(user.getId()).stream()
                .filter(a -> a.getCourse().getId().equals(course.getId()))
                .findFirst().orElseThrow();
        courseAssignmentService.startCourseAssignment(user.getId(), assignment.getId());

        adjustTestTime(Duration.ofDays(1));
        courseNotificationService.sendAutoNotifications();
        checkNotificationSent(course, user, pushSentTime, CourseNotificationStatus.FINISHED, 1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 0);
    }

    @Test
    void cancelNotificationsTest() {
        // Тестовый юзер
        var nowDate = testTime.toLocalDate();
        var user = createTestUser(false, null, null);
        userScheduleService.scheduleOverride(user.getUid(), nowDate.plusDays(1), false, false);

        // Настройки для оповещений для курьера
        createRule(FREE_DAY, LocalTime.of(15, 0), LocalTime.of(20, 0));

        // Создаем тестовый курс и назначаем его курьеру
        setTestTime(LocalTime.of(14, 0));
        var course = courseHelper.createPublishedTestCourse();
        courseAssignmentService.createCourseAssignments(course, Set.of(user.getId()));

        // Отменяем назначение - должно отмениться оповещение
        courseAssignmentService.cancelCourseAssignment(course, user.getId());
        var notifications = courseNotificationRepository.findAllByCourseAndUserIdIn(course, Set.of(user.getId()));
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getStatus()).isEqualTo(CourseNotificationStatus.CANCELLED);

        // Отмененное оповещение не должно отправляться
        courseNotificationService.sendAutoNotifications();
        checkNotificationNotSent(user);
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 0);
    }

    @Test
    void shouldSendCourierPushes() {
        userHelper.findOrCreateUser(62985432L);
        userHelper.findOrCreateUser(37530508540L);
        userHelper.findOrCreateUser(347950435L);

        var course = courseHelper.createPublishedTestCourse(CourseGenerationParams.builder()
                .name("Курс 1")
                .programId("program-uuid-1")
                .build());

        courseNotificationService.sendPush(new CoursePushDto(course.getId(), "push message",
                List.of(62985432L, 37530508540L, 347950435L)));

        List<PushNotification> notifications = pushNotificationRepository.findAll();
        assertEquals(3, notifications.size());
        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_NOTIFICATION_SEND, 3);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PUSH_NOTIFICATION_SEND);

        ArgumentCaptor<PushSendRequest> argument = ArgumentCaptor.forClass(PushSendRequest.class);
        verify(xivaClient, times(3)).send(argument.capture());

        assertThat(argument.getAllValues()).extracting("xivaUserId", "event", "title", "body", "ttlSec",
                "payload.redirect.type", "payload.redirect.courseId")
                .hasSize(3)
                .contains(
                        tuple("62985432", COURSE, null, "push message", 3600, COURSE_PAGE, course.getId()),
                        tuple("37530508540", COURSE, null, "push message", 3600, COURSE_PAGE, course.getId()),
                        tuple("347950435", COURSE, null, "push message", 3600, COURSE_PAGE, course.getId()));
        assertThat(pushNotificationRepository.findById(notifications.get(0).getId()).orElseThrow().getXivaUserId())
                .isEqualTo("62985432");
    }

    /**
     * Метод создает юзеров для тестов:
     * 1. Курьер с выходным днем на дату теста
     * 2. Курьер с рабочим днем, смена активна
     * 3. Курьер с рабочим днем, смена закрыта в 15:00
     * 4. Курьер с рабочим днем, смена начнется в 15:00
     */
    private List<User> generateTestUsers() {
        return List.of(
                createTestUser(false, null, null),
                createTestUser(true, null, null),
                createTestUser(true, null, LocalTime.of(15, 0)),
                createTestUser(true, LocalTime.of(15, 0), null)
        );
    }

    private User createTestUser(boolean workDay, LocalTime shiftStart, LocalTime shiftClosed) {
        setTestTime();
        var nowDate = testTime.toLocalDate();
        var user = userHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                .userId((long) (Math.random() * 1000000))
                .workdate(LocalDate.now(clock))
                .sortingCenter(TestUserHelper.SortCenterGenerateParam.builder()
                        .id(SortingCenter.DEFAULT_SC_ID)
                        .build())
                .build());
        configuration.mergeValue(ConfigurationProperties.SHIFTS_AMOUNT_FOR_GRADE_UP, 0);
        userStatusService.activateUser(user);
        userScheduleService.scheduleOverride(user.getUid(), nowDate, workDay, false);
        if (!workDay) {
            userHelper.createEmptyShift(user, nowDate.minusDays(5));
        }

        if (shiftStart != null) {
            var shift = userHelper.findOrCreateOpenShiftForSc(nowDate, SortingCenter.DEFAULT_SC_ID);
            var scheduleData = new UserScheduleData(CourierVehicleType.CAR,
                    new RelativeTimeInterval(shiftStart, shiftStart.plusHours(8)));
            userShiftCommandService.createUserShift(UserShiftCommand.Create.builder()
                    .userId(user.getId())
                    .shiftId(shift.getId())
                    .scheduleData(scheduleData)
                    .build()
            );
        }

        if (shiftClosed != null) {
            var userShift = userHelper.createEmptyShift(user, nowDate);
            setTestTime(shiftClosed);
            userShiftCommandService.closeShift(new UserShiftCommand.Close(userShift.getId()));
            var closedAt = LocalDateTime.ofInstant(DateTimeUtil.todayAt(shiftClosed, clock), ZoneId.of("UTC"));
            // Из-за использования Instant.now() в UserShift.closeShift()
            jdbcTemplate.update("update user_shift set closed_at = ? where id = ?",
                    closedAt, userShift.getId());
            setTestTime();
        }

        return user;
    }

    private void createRule(CourseNotificationDayType dayType, LocalTime start, LocalTime end) {
        createRule(dayType, start, end, Duration.ZERO, Duration.ZERO);
    }

    private void checkNotificationSent(Course course, User user, Instant sentTime, CourseNotificationStatus status,
                                       Integer attempt) {
        var notifications = courseNotificationRepository
                .findAllByCourseAndUserIdIn(course, Set.of(user.getId()));
        assertThat(notifications).hasSize(1);
        var notification = notifications.get(0);
        assertThat(notification.getStatus()).isEqualTo(status);
        assertThat(notification.getLastSentTime()).isEqualTo(sentTime);
        assertThat(notification.getAttempt()).isEqualTo(attempt);

        assertThat(pushNotificationRepository.findAll().stream()
                .filter(push -> push.getEvent() == PushEvent.COURSE)
                .filter(push -> String.valueOf(user.getUid()).equals(push.getXivaUserId()))
                .filter(push -> course.getId().equals(push.getPayload().getRedirect().getCourseId()))
                .findFirst())
                .isPresent();
    }

    private void checkNotificationNotSent(User user) {
        assertThat(pushNotificationRepository.findAll().stream()
                .filter(push -> push.getEvent() == PushEvent.COURSE)
                .filter(push -> push.getBody().contains(String.valueOf(user.getUid())))
                .findFirst())
                .isEmpty();
    }

    private void createRule(CourseNotificationDayType dayType, LocalTime start, LocalTime end,
                            Duration beforeShift, Duration afterShift) {
        var rule = CourseNotificationRule.builder()
                .dayType(dayType)
                .intervalStart(start)
                .intervalEnd(end)
                .minTimeBeforeShift(beforeShift)
                .minTimeAfterShift(afterShift)
                .build();
        courseNotificationRuleRepository.save(rule);
        courseNotificationRuleAdapter.refresh();
    }

    private void setTestTime(LocalTime time) {
        ClockUtil.initFixed(clock, LocalDate.now(clock).atTime(time));
    }

    private void adjustTestTime(Duration interval) {
        ClockUtil.initFixed(clock, LocalDateTime.now(clock).plus(interval));
    }

    private void setTestTime() {
        ClockUtil.initFixed(clock, testTime);
    }
}
