package ru.yandex.market.tpl.core.domain.course.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Functions;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.course.CourseAssignmentStatus;
import ru.yandex.market.tpl.api.model.course.CourseStatus;
import ru.yandex.market.tpl.api.model.course.UserStatusRequirement;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.course.domain.Course;
import ru.yandex.market.tpl.core.domain.course.domain.CourseAssignment;
import ru.yandex.market.tpl.core.domain.course.domain.CourseCommand;
import ru.yandex.market.tpl.core.domain.course.domain.CourseCommandService;
import ru.yandex.market.tpl.core.domain.course.domain.CourseGenerationParams;
import ru.yandex.market.tpl.core.domain.course.domain.TestCourseHelper;
import ru.yandex.market.tpl.core.domain.course.repository.CourseAssignmentRepository;
import ru.yandex.market.tpl.core.domain.course.repository.CourseRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.partner.UserStatusService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.course.CourseAssignmentStatus.ASSIGNED;
import static ru.yandex.market.tpl.api.model.course.CourseAssignmentStatus.COMPLETED;
import static ru.yandex.market.tpl.core.domain.course.service.CourseAssignmentService.ACTIVE_STATUSES;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class AutoAssignCoursesTest extends TplAbstractTest {

    private final CourseAssignmentService courseAssignmentService;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final ConfigurationServiceAdapter configuration;
    private final CourseCommandService courseCommandService;
    private final UserScheduleService userScheduleService;
    private final TransactionTemplate transactionTemplate;
    private final UserStatusService userStatusService;
    private final TestCourseHelper testCourseHelper;
    private final CourseRepository courseRepository;
    private final TestUserHelper testUserHelper;
    private final UserRepository userRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final UserCommandService userCommandService;
    private final CourseAvailabilityService courseAvailabilityService;
    private final Clock clock;

    @BeforeEach
    void setup() {
        ClockUtil.initFixed(clock);
        configuration.mergeValue(ConfigurationProperties.COURSE_ASSIGNMENT_FILTERS_ENABLED, true);
        configuration.mergeValue(ConfigurationProperties.COURSE_AUTO_ASSIGNMENT_ENABLED, true);
    }

    @Test
    void autoAssignOnCoursePublishedTest() {
        // Создаем юзеров
        var user1 = createTestUser(false, SortingCenter.DEFAULT_SC_ID); // Юзер 1 не новичек
        var user2 = createTestUser(true, SortingCenter.DEFAULT_SC_ID);
        var user3 = createTestUser(true, SortingCenter.DEFAULT_SC_ID);
        var user4 = createTestUser(true, SortingCenter.DEMO_SC_ID); // Для юзера 4 не будет активироваться СЦ

        // Тестовый курс - только для новичков (user1 не подходит)
        var course = testCourseHelper.createTestCourse(CourseGenerationParams.builder()
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .build());

        // Активируем курс на СЦ (user4 не подходит)
        testCourseHelper.activateCourseForSC(course.getId(), Set.of(SortingCenter.DEFAULT_SC_ID));

        assertThat(courseAssignmentRepository.findByCourse(course)).isEmpty();

        // Публикуем курс
        testCourseHelper.publishCourse(course.getId());

        assertThat(courseAssignmentRepository.findByCourse(course)).hasSize(2);
        verifyCourseAssignments(course.getId(), Set.of(user2.getId(), user3.getId()));
        verifyCourseNotAssigned(course.getId(), Set.of(user1.getId(), user4.getId()));

        //завершаем одно прохождение
        completeAssignment(user3, course);

        // Отменяем публикацию курса
        testCourseHelper.unpublishCourse(course.getId());
        assertThat(courseAssignmentRepository.findByCourse(course)).hasSize(2);
        verifyCourseAssignmentsCompleted(course.getId(), Set.of(user3.getId()));
        verifyCourseAssignmentsCancelled(course.getId(), Set.of(user2.getId()));
    }

    private void completeAssignment(User user, Course course) {
        var assignment = courseAssignmentRepository.findAssignmentInStatus(user.getId(),
                course.getProgramId(), Set.of(ASSIGNED)).stream().findFirst().orElseThrow();
        courseAssignmentService.startCourseAssignment(user.getId(), assignment.getId());
        courseAssignmentService.completeCourseAssignment(user.getId(), "session", course.getProgramId(),
                assignment.getId(), clock.instant(), true);
    }

    @Test
    void autoAssignOnCourseUpdatedEvent() {
        // Создаем юзеров
        var user1 = createTestUser(true, SortingCenter.DEFAULT_SC_ID);
        var user2 = createTestUser(false, SortingCenter.DEFAULT_SC_ID);
        var user3 = createTestUser(false, SortingCenter.DEFAULT_SC_ID);
        var user4 = createTestUser(false, SortingCenter.DEMO_SC_ID);

        // Тестовый курс - только для новичков (user1 не подходит), сразу публикуем, без настроек по СЦ назначений быть е должно
        var course = testCourseHelper.createPublishedTestCourse(CourseGenerationParams.builder()
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .build());
        assertThat(courseAssignmentRepository.findByCourse(course)).isEmpty();

        // Добавляем настройку по СЦ, должно создаться назначение на user1, для остальных курс не подходит, они не новички
        testCourseHelper.activateCourseForSC(course.getId(), Set.of(SortingCenter.DEFAULT_SC_ID));
        assertThat(courseAssignmentRepository.findByCourse(course)).hasSize(1);
        verifyCourseAssignments(course.getId(), Set.of(user1.getId()));
        verifyCourseNotAssigned(course.getId(), Set.of(user2.getId(), user3.getId(), user4.getId()));

        // Меняем "для кого курс" - должны создаться назначения на user2 и user3
        courseCommandService.updateCourse(CourseCommand.Update.builder()
                .courseId(course.getId())
                .name(course.getName())
                .programId(course.getProgramId())
                .imageUrl(course.getImageUrl())
                .description(course.getDescription())
                .userStatusRequirement(UserStatusRequirement.ALL)
                .build());
        assertThat(courseAssignmentRepository.findByCourse(course)).hasSize(3);
        verifyCourseAssignments(course.getId(), Set.of(user1.getId(), user2.getId(), user3.getId()));
        verifyCourseNotAssigned(course.getId(), Set.of(user4.getId()));

        // Меняем "для кого курс" обратно на NEWBIE_ONLY - должны отмениться назначения на user2 и user3
        courseCommandService.updateCourse(CourseCommand.Update.builder()
                .courseId(course.getId())
                .name(course.getName())
                .programId(course.getProgramId())
                .imageUrl(course.getImageUrl())
                .description(course.getDescription())
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .build());
        verifyCourseAssignmentsCancelled(course.getId(), Set.of(user2.getId(), user3.getId()));
        assertThat(courseAssignmentRepository.findByCourse(course)).hasSize(3);
    }

    @Test
    void autoAssignOnUserStatusChangedTest() {
        // Создаем не активного юзера
        var user = testUserHelper.findOrCreateUser(12345L, LocalDate.now(clock));
        var userSchedule = userScheduleService.findUserScheduleForDate(user, LocalDate.now(clock));
        var scId = userSchedule.getActiveRules().stream()
                .findFirst()
                .map(UserScheduleRule::getSortingCenterId).orElseThrow();

        updateUserDeleted(user.getId(), true);

        // Тестовые курсы
        var course1 = testCourseHelper.createPublishedTestCourse(CourseGenerationParams.builder()
                .name("course1")
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .build());
        var course2 = testCourseHelper.createPublishedTestCourse("course2");
        var course3 = testCourseHelper.createPublishedTestCourse("course3");
        var course4 = testCourseHelper.createPublishedTestCourse("course4");
        var course5 = testCourseHelper.createTestCourse("course5");
        testCourseHelper.activateCourseForSC(course1.getId(), Set.of(scId));
        testCourseHelper.activateCourseForSC(course2.getId(), Set.of(scId));
        testCourseHelper.activateCourseForSC(course3.getId(), Set.of(scId));
        testCourseHelper.activateCourseForSC(course5.getId(), Set.of(scId));

        // Назначений нет
        assertThat(courseAssignmentRepository.findByUserId(user.getId())).isEmpty();

        // Переводим юзера в статус ACTIVE
        configuration.mergeValue(ConfigurationProperties.SHIFTS_AMOUNT_FOR_GRADE_UP, 0);
        updateUserDeleted(user.getId(), false);

        // Должны появиться назначения
        assertThat(courseAssignmentRepository.findByUserId(user.getId())).hasSize(2);
        verifyCourseAssignments(course2.getId(), Set.of(user.getId()));
        verifyCourseAssignments(course3.getId(), Set.of(user.getId()));

        verifyCourseNotAssigned(course1.getId(), Set.of(user.getId())); // Курс только для новичков
        verifyCourseNotAssigned(course4.getId(), Set.of(user.getId())); // Курс не активирован для СЦ курьера
        verifyCourseNotAssigned(course5.getId(), Set.of(user.getId())); // Курс не опубликован

        //завершаем одно прохождение
        completeAssignment(user, course2);

        // Увольняем юзера, все назначения должны быть отменены
        transactionTemplate.execute(status -> {
            userStatusService.fireUser(user);
            return null;
        });
        assertThat(courseAssignmentRepository.findByUserId(user.getId())).hasSize(2);
        verifyCourseAssignmentsCancelled(course3.getId(), Set.of(user.getId()));
        verifyCourseAssignmentsCompleted(course2.getId(), Set.of(user.getId()));

    }

    @Test
    void autoAssignOnUserScheduleChangedTest() {
        // Создаем юзера без расписания
        var user = testUserHelper.findOrCreateUserWithoutSchedule(12345L);

        // Тестовые курсы для разных СЦ
        var course1 = testCourseHelper.createPublishedTestCourse();
        var course2 = testCourseHelper.createPublishedTestCourse();
        testCourseHelper.activateCourseForSC(course1.getId(), Set.of(SortingCenter.DEFAULT_SC_ID));
        testCourseHelper.activateCourseForSC(course2.getId(), Set.of(SortingCenter.DEMO_SC_ID));

        // создаем юзеру расписание
        var schedule1 = userScheduleService.createRule(user.getId(),
                createRuleDto(SortingCenter.DEFAULT_SC_ID, UserScheduleType.ALWAYS_WORKS), true);

        // Должны появиться назначения
        assertThat(courseAssignmentRepository.findByUserId(user.getId())).hasSize(1);
        verifyCourseAssignments(course1.getId(), Set.of(user.getId()));
        verifyCourseNotAssigned(course2.getId(), Set.of(user.getId())); // Курс для курьеров на другом СЦ

        // Создаем расписание в другом СЦ
        var schedule2 = userScheduleService.scheduleOverride(user.getUid(), LocalDate.now(clock),
                createRuleDto(SortingCenter.DEMO_SC_ID, UserScheduleType.OVERRIDE_WORK), true);

        // Должны добавиться назначения
        assertThat(courseAssignmentRepository.findByUserId(user.getId())).hasSize(2);
        verifyCourseAssignments(course1.getId(), Set.of(user.getId()));
        verifyCourseAssignments(course2.getId(), Set.of(user.getId()));

        // Удаляем расписание
        userScheduleService.deleteRule(user.getId(), schedule2.getId(), true);

        // Назначения по для course2 должны отмениться
        assertThat(courseAssignmentRepository.findByUserId(user.getId())).hasSize(2);
        verifyCourseAssignments(course1.getId(), Set.of(user.getId()));
        verifyCourseAssignmentsCancelled(course2.getId(), Set.of(user.getId()));

        // Меняем СЦ в расписании
        userScheduleService.changeRule(user.getId(), schedule1.getId(),
                createRuleDto(SortingCenter.DEMO_SC_ID, UserScheduleType.FIVE_TWO), true);

        // Назначение для course1 должно отмениться, для course2 создаться новое
        assertThat(courseAssignmentRepository.findByUserId(user.getId())).hasSize(3);
        verifyCourseAssignmentsCancelled(course1.getId(), Set.of(user.getId()));
        verifyCourseAssignments(course2.getId(), Set.of(user.getId()));
    }

    @Test
    void autoAssignScFiltersDisabledTest() {
        var user = createTestUser(false, SortingCenter.DEFAULT_SC_ID);
        var course1 = testCourseHelper.createPublishedTestCourse();
        var course2 = testCourseHelper.createPublishedTestCourse();

        // Активируем курсы на другом СЦ
        testCourseHelper.activateCourseForSC(course1.getId(), Set.of(SortingCenter.DEMO_SC_ID));
        testCourseHelper.activateCourseForSC(course2.getId(), Set.of(SortingCenter.DEMO_SC_ID));

        // Назначения не должны быть созданы
        assertThat(courseAssignmentRepository.findByCourse(course1)).isEmpty();
        assertThat(courseAssignmentRepository.findByCourse(course2)).isEmpty();

        // Отключаем фильтр по СЦ для course1
        courseCommandService.updateCourse(CourseCommand.Update.builder()
                .courseId(course1.getId())
                .name(course1.getName())
                .description(course1.getDescription())
                .imageUrl(course1.getDescription())
                .programId(course1.getProgramId())
                .expectedDurationInMinutes(course1.getExpectedDurationMinutes())
                .userStatusRequirement(course1.getUserStatusRequirement())
                .scFiltersEnabled(false)
                .build());

        // Должно появиться назначение на course1
        assertThat(courseAssignmentRepository.findByCourse(course1)).hasSize(1);
        verifyCourseAssignments(course1.getId(), Set.of(user.getId()));

        // Для course2 ничего не изменилось
        assertThat(courseAssignmentRepository.findByCourse(course2)).isEmpty();
    }

    @Test
    void autoAssignForNotActiveCourier() {
        var user1 = testUserHelper.findOrCreateUserWithoutSchedule(12345L);
        var user2 = createTestUser(true, SortingCenter.DEFAULT_SC_ID);

        // Только для новичков без фильтра по СЦ
        var course1 = testCourseHelper.createPublishedTestCourse(CourseGenerationParams.builder()
                .name("course1")
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .scFiltersEnabled(false)
                .build());
        // Для всех без фильтра по СЦ
        var course2 = testCourseHelper.createPublishedTestCourse(CourseGenerationParams.builder()
                .name("course2")
                .userStatusRequirement(UserStatusRequirement.ALL)
                .scFiltersEnabled(false)
                .build());
        // С фильтром по СЦ
        var course3 = testCourseHelper.createPublishedTestCourse("course3");
        testCourseHelper.activateCourseForSC(course3.getId(), Set.of(SortingCenter.DEFAULT_SC_ID));

        assertThat(courseAssignmentRepository.findByUserId(user1.getId())).hasSize(2);
        assertThat(courseAssignmentRepository.findByUserId(user2.getId())).hasSize(3);
        verifyCourseAssignments(course1.getId(), Set.of(user1.getId(), user2.getId()));
        verifyCourseAssignments(course2.getId(), Set.of(user1.getId(), user2.getId()));
        verifyCourseAssignments(course3.getId(), Set.of(user2.getId()));

        // Переводим user1 в статус ACTIVE
        configuration.mergeValue(ConfigurationProperties.SHIFTS_AMOUNT_FOR_GRADE_UP, 0);
        userCommandService.activate(new UserCommand.Activate(user1.getId()));
        // user2 в статусе NOT_ACTIVE и были смены
        createUserShiftForUser(user2);
        userCommandService.recover(new UserCommand.Recover(user2.getId()));

        assertThat(courseAssignmentRepository.findByUserId(user1.getId())).hasSize(2);
        assertThat(courseAssignmentRepository.findByUserId(user2.getId())).hasSize(3);
        verifyCourseAssignmentsCancelled(course1.getId(), Set.of(user1.getId(), user2.getId()));
        verifyCourseAssignments(course2.getId(), Set.of(user1.getId()));
        verifyCourseAssignmentsCancelled(course2.getId(), Set.of(user2.getId()));
        verifyCourseAssignmentsCancelled(course3.getId(), Set.of(user2.getId()));
    }

    /**
     * Проверка назначений курса курьерам. Выполняются проверки:
     * <li>Курс опубликован</li>
     * <li>Назначение в статусе ASSIGNED</li>
     * <li>Назначение на курьера с переданным ID</li>
     * <li>Статус курьера подходит под требования курса</li>
     * <li>Курс активирован на СЦ, на котором у курьера есть расписание</li>
     * <li>По всем переданным ID курьеров созданы назначения</li>
     */
    private void verifyCourseAssignments(long courseId, Set<Long> userIds) {
        transactionTemplate.execute(status -> {
            var course = courseRepository.findByIdOrThrow(courseId);
            assertThat(course.getStatus()).isEqualTo(CourseStatus.PUBLISHED);

            var usersById = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Functions.identity()));
            var usersWithScheduleOnSC = usersById.values().stream()
                    .collect(Collectors.toMap(
                            User::getId,
                            user -> userRepository.findUserScIdsWithNotClosedSchedules(user.getId(), LocalDate.now(clock))
                    ));

            var assignments = courseAssignmentRepository.findByCourseAndStatusNotIn(
                    course, Set.of(CourseAssignmentStatus.CANCELLED));
            var assignedUserIds = new HashSet<Long>(userIds.size());

            for (var assignment : assignments) {

                assertThat(assignment.getStatus()).isIn(ACTIVE_STATUSES);
                var user = usersById.get(assignment.getUserId());
                assertThat(user).isNotNull();
                assertThat(courseAvailabilityService.isCourseAvailableForUserStatus(course, user)).isTrue();
                assertThat(user.isDeleted()).isFalse();
                var userScIds = usersWithScheduleOnSC.get(user.getId());
                assertThat(course.isAvailableForSCs(userScIds)).isTrue();
                assignedUserIds.add(user.getId());
            }

            assertThat(assignedUserIds).containsAll(userIds);
            return null;
        });
    }

    private void verifyCourseNotAssigned(long courseId, Set<Long> userIds) {
        var course = courseRepository.findByIdOrThrow(courseId);
        var assignedUserIds = courseAssignmentRepository.findByCourse(course).stream()
                .map(CourseAssignment::getUserId)
                .collect(Collectors.toSet());
        assertThat(assignedUserIds).doesNotContainAnyElementsOf(userIds);
    }

    private void verifyCourseAssignmentsCancelled(long courseId, Set<Long> userIds) {
        var course = courseRepository.findByIdOrThrow(courseId);
        var userIdsWithCancelledAssignments = new HashSet<Long>(userIds.size());
        var assignments = courseAssignmentRepository.findByCourse(course);
        for (var assignment : assignments) {
            if (!userIds.contains(assignment.getUserId())) {
                continue;
            }
            assertThat(assignment.getStatus()).isEqualTo(CourseAssignmentStatus.CANCELLED);
            userIdsWithCancelledAssignments.add(assignment.getUserId());
        }
        assertThat(userIdsWithCancelledAssignments).containsAll(userIds);
    }

    private void verifyCourseAssignmentsCompleted(long courseId, Set<Long> userIds) {
        var course = courseRepository.findByIdOrThrow(courseId);
        var assignedUserIds = courseAssignmentRepository.findByCourseIdAndUserIdInAndStatusIn(
            courseId, userIds, EnumSet.of(COMPLETED));
        assertThat(assignedUserIds).extracting("userId").containsAll(userIds);
    }

    private void updateUserDeleted(long userId, boolean deleted) {
        transactionTemplate.execute(status -> {
            var user = userRepository.findByIdOrThrow(userId);
            if (deleted) {
                userStatusService.fireUser(user);
            } else {
                user.updateDeleted(deleted);
                userRepository.save(user);
                userStatusService.activateUser(user);
            }
            return null;
        });
    }

    private User createTestUser(boolean newbie, long scId) {
        var user = testUserHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                .userId((long) (Math.random() * 1000000))
                .workdate(LocalDate.now(clock).plusDays(3))
                .sortingCenter(TestUserHelper.SortCenterGenerateParam.builder()
                        .id(scId)
                        .build())
                .build());
        configuration.mergeValue(ConfigurationProperties.SHIFTS_AMOUNT_FOR_GRADE_UP, newbie ? 99 : 0);
        userStatusService.activateUser(user);
        return user;
    }

    private UserScheduleRuleDto createRuleDto(long scId, UserScheduleType type) {
        var now = LocalDate.now(clock);
        return UserScheduleRuleDto.builder()
                .sortingCenterId(scId)
                .activeFrom(now)
                .activeTo(now.plusDays(60))
                .applyFrom(now)
                .scheduleType(type)
                .build();
    }

    private void createUserShiftForUser(User user) {
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock).plusDays(3));
        userShiftCommandService.createUserShift(UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .build());
    }

}
