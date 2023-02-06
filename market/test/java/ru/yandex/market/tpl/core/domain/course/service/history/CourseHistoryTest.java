package ru.yandex.market.tpl.core.domain.course.service.history;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.course.CourseAssignmentCompleteResultDto;
import ru.yandex.market.tpl.api.model.course.CourseAssignmentStatus;
import ru.yandex.market.tpl.api.model.course.CourseDataRequest;
import ru.yandex.market.tpl.api.model.course.CourseStatus;
import ru.yandex.market.tpl.api.model.course.UserStatusRequirement;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.course.domain.Course;
import ru.yandex.market.tpl.core.domain.course.domain.CourseAssignment;
import ru.yandex.market.tpl.core.domain.course.domain.CourseConfigStatus;
import ru.yandex.market.tpl.core.domain.course.domain.CourseGenerationParams;
import ru.yandex.market.tpl.core.domain.course.domain.CourseScConfig;
import ru.yandex.market.tpl.core.domain.course.domain.TestCourseHelper;
import ru.yandex.market.tpl.core.domain.course.domain.history.CourseEntityDifference;
import ru.yandex.market.tpl.core.domain.course.domain.history.CourseEntityDifference.FieldDifference;
import ru.yandex.market.tpl.core.domain.course.domain.history.CourseHistoryEvent;
import ru.yandex.market.tpl.core.domain.course.domain.history.CourseHistoryEventType;
import ru.yandex.market.tpl.core.domain.course.repository.CourseHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.course.repository.CourseRepository;
import ru.yandex.market.tpl.core.domain.course.service.CourseAssignmentService;
import ru.yandex.market.tpl.core.domain.course.service.CourseService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.course.domain.history.CourseHistoryEventType.ASSIGNMENT_CREATED;
import static ru.yandex.market.tpl.core.domain.course.domain.history.CourseHistoryEventType.ASSIGNMENT_UPDATED;
import static ru.yandex.market.tpl.core.domain.course.domain.history.CourseHistoryEventType.COURSE_CONFIG_UPDATED;
import static ru.yandex.market.tpl.core.domain.course.domain.history.CourseHistoryEventType.COURSE_CREATED;
import static ru.yandex.market.tpl.core.domain.course.domain.history.CourseHistoryEventType.COURSE_UPDATED;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class CourseHistoryTest extends TplAbstractTest {

    private final CourseHistoryEventRepository courseHistoryEventRepository;
    private final CourseRepository courseRepository;
    private final ConfigurationServiceAdapter configuration;
    private final TestCourseHelper testCourseHelper;
    private final TestUserHelper testUserHelper;
    private final CourseService courseService;
    private final CourseAssignmentService courseAssignmentService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;


    @BeforeEach
    void setup() {
        ClockUtil.initFixed(clock);
        configuration.mergeValue(ConfigurationProperties.COURSE_ASSIGNMENT_FILTERS_ENABLED, true);
        configuration.mergeValue(ConfigurationProperties.COURSE_AUTO_ASSIGNMENT_ENABLED, true);
    }

    @Test
    void courseHistoryOnCourseCreatedTest() {
        var course = testCourseHelper.createTestCourse(CourseGenerationParams.builder()
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .build());
        var events = courseHistoryEventRepository.findAllByCourseIdOrderByDate(course.getId());
        assertThat(events).hasSize(1);
        var event = events.get(0);
        checkEvent(event, course, COURSE_CREATED, CourseEntityDifference.CourseDifference.builder()
                .name(FieldDifference.of(course.getName()))
                .programId(FieldDifference.of(course.getProgramId()))
                .imageUrl(FieldDifference.of(course.getImageUrl()))
                .status(FieldDifference.of(CourseStatus.NEW))
                .description(FieldDifference.of(course.getDescription()))
                .expectedDurationMinutes(FieldDifference.of(course.getExpectedDurationMinutes()))
                .userStatusRequirement(FieldDifference.of(course.getUserStatusRequirement()))
                .beta(null)
                .hasExam(null)
                .build());
    }

    @Test
    void courseHistoryOnCourseChangedTest() {
        var courseParams = CourseGenerationParams.builder()
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .build();
        var course = testCourseHelper.createTestCourse(courseParams);
        courseHistoryEventRepository.deleteAll();

        // 0 - обновление курса
        courseService.updateCourse(course.getId(), CourseDataRequest.builder()
                .name("new-name")
                .userStatusRequirement(UserStatusRequirement.ALL)
                .programId(course.getProgramId()) // не меняется
                .imageUrl(course.getImageUrl()) // не меняется
                .expectedDurationMinutes(course.getExpectedDurationMinutes()) // не меняется
                .description(course.getDescription()) // не меняется
                .beta(course.isBeta()) // не меняется
                .hasExam(course.isHasExam()) // не меняется
                .build());

        // Проверяем записанные ивенты
        var events = courseHistoryEventRepository.findAllByCourseIdOrderByDate(course.getId());
        assertThat(events).hasSize(1);
        checkEvent(events.get(0), course, COURSE_UPDATED, CourseEntityDifference.CourseDifference.builder()
                .name(FieldDifference.of(courseParams.getName(), "new-name"))
                .userStatusRequirement(FieldDifference.of(
                        courseParams.getUserStatusRequirement(), UserStatusRequirement.ALL)
                ).build());
    }

    @Test
    void courseHistoryOnCoursePublishingTest() {
        var course = testCourseHelper.createTestCourse(CourseGenerationParams.builder()
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .build());
        courseHistoryEventRepository.deleteAll();

        // Публикация и отмена публикации курса
        courseService.publishCourse(course.getId());
        courseService.unpublish(course.getId());

        var events = courseHistoryEventRepository.findAllByCourseIdOrderByDate(course.getId());
        assertThat(events).hasSize(2);

        // 0 - публикация курса
        checkEvent(events.get(0), course, COURSE_UPDATED, CourseEntityDifference.CourseDifference.builder()
                .status(FieldDifference.of(CourseStatus.NEW, CourseStatus.PUBLISHED))
                .build());

        // 1 - отмена публикации
        checkEvent(events.get(1), course, COURSE_UPDATED, CourseEntityDifference.CourseDifference.builder()
                .status(FieldDifference.of(CourseStatus.PUBLISHED, CourseStatus.UNPUBLISHED))
                .build());
    }

    @Test
    void courseHistoryOnScConfigChangedTest() {
        var course = testCourseHelper.createTestCourse(CourseGenerationParams.builder()
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .build());
        courseHistoryEventRepository.deleteAll();

        // Активация и деактивация курса на СЦ
        courseService.activateCourseForSc(course.getId(), Set.of(SortingCenter.DEFAULT_SC_ID));
        courseService.deactivateCourseForSc(course.getId(), Set.of(SortingCenter.DEFAULT_SC_ID));
        var scConfig = transactionTemplate.execute(status -> courseRepository.findByIdOrThrow(course.getId())
                .getScConfigs().stream().findFirst().orElseThrow());

        var events = courseHistoryEventRepository.findAllByCourseIdOrderByDate(course.getId());
        assertThat(events).hasSize(2);

        // 0 - Активация СЦ
        checkEvent(events.get(0), course, COURSE_CONFIG_UPDATED, scConfig.getId(), CourseScConfig.class,
                CourseEntityDifference.CourseConfigDifference.builder()
                        .scId(SortingCenter.DEFAULT_SC_ID)
                        .status(FieldDifference.of(CourseConfigStatus.ACTIVE))
                        .build());

        // 2 - Деактивация СЦ
        checkEvent(events.get(1), course, COURSE_CONFIG_UPDATED, scConfig.getId(), CourseScConfig.class,
                CourseEntityDifference.CourseConfigDifference.builder()
                        .scId(SortingCenter.DEFAULT_SC_ID)
                        .status(FieldDifference.of(CourseConfigStatus.ACTIVE, CourseConfigStatus.INACTIVE))
                        .build());
    }

    @Test
    void courseHistoryOnAssignmentsChangedTest() {
        var course = testCourseHelper.createPublishedTestCourse(CourseGenerationParams.builder()
                .userStatusRequirement(UserStatusRequirement.ALL)
                .build());
        var user = createTestUser();
        courseHistoryEventRepository.deleteAll();

        // 0 - создание назначения
        var assignment = courseAssignmentService.createCourseAssignments(course, Set.of(user.getId())).get(0);

        // 1 - начало прохождения курса
        courseAssignmentService.startCourseAssignment(user.getId(), assignment.getId());

        // 2 - завершение прохождения курса
        var courseResults = CourseAssignmentCompleteResultDto.builder()
                .assignmentId(assignment.getId())
                .completedAt(5000000000L)
                .externalId(12345L)
                .passed(true)
                .programUuid(course.getProgramId())
                .sessionUuid("2d9fdb5b-c7dd-4327-b8e6-bba27e43c5e3")
                .build();
        courseAssignmentService.completeCourseAssignment(courseResults);

        var events = courseHistoryEventRepository.findAllByCourseIdOrderByDate(course.getId());
        assertThat(events).hasSize(3);

        // 0 - создание назначения
        checkEvent(events.get(0), course, ASSIGNMENT_CREATED, assignment.getId(), CourseAssignment.class,
                CourseEntityDifference.CourseAssignmentDifference.builder()
                        .status(FieldDifference.of(CourseAssignmentStatus.ASSIGNED))
                        .build());

        // 1 - начало прохождения курса
        checkEvent(events.get(1), course, ASSIGNMENT_UPDATED, assignment.getId(), CourseAssignment.class,
                CourseEntityDifference.CourseAssignmentDifference.builder()
                        .userId(user.getId())
                        .status(FieldDifference.of(CourseAssignmentStatus.ASSIGNED, CourseAssignmentStatus.IN_PROGRESS))
                        .build());

        // 2 - завершение прохождения курса
        checkEvent(events.get(2), course, ASSIGNMENT_UPDATED, assignment.getId(), CourseAssignment.class,
                CourseEntityDifference.CourseAssignmentDifference.builder()
                        .userId(user.getId())
                        .status(FieldDifference.of(CourseAssignmentStatus.IN_PROGRESS, CourseAssignmentStatus.COMPLETED))
                        .completedAt(FieldDifference.of(Instant.ofEpochSecond(5000000000L)))
                        .passed(FieldDifference.of(false, courseResults.getPassed()))
                        .sessionId(FieldDifference.of(courseResults.getSessionUuid()))
                        .build());
    }

    @Test
    void courseHistoryOnCancelAssignmentTest() {
        var course = testCourseHelper.createPublishedTestCourse(CourseGenerationParams.builder()
                .userStatusRequirement(UserStatusRequirement.ALL)
                .build());
        var user = createTestUser();
        var assignment = courseAssignmentService.createCourseAssignments(course, Set.of(user.getId())).get(0);
        courseHistoryEventRepository.deleteAll();

        courseAssignmentService.cancelCourseAssignment(course, user.getId());

        var events = courseHistoryEventRepository.findAllByCourseIdOrderByDate(course.getId());
        assertThat(events).hasSize(1);
        checkEvent(events.get(0), course, ASSIGNMENT_UPDATED, assignment.getId(), CourseAssignment.class,
                CourseEntityDifference.CourseAssignmentDifference.builder()
                        .userId(user.getId())
                        .status(FieldDifference.of(CourseAssignmentStatus.ASSIGNED, CourseAssignmentStatus.CANCELLED))
                        .build());
    }

    private void checkEvent(CourseHistoryEvent event, Course course, CourseHistoryEventType type,
                            CourseEntityDifference<?> diff) {
        checkEvent(event, course, type, course.getId(), Course.class, diff);
    }

    private void checkEvent(CourseHistoryEvent event, Course course, CourseHistoryEventType type,
                            Long entityId, Class<?> entityClass, CourseEntityDifference<?> diff) {
        assertThat(event).isNotNull();
        assertThat(event.getCourseId()).isEqualTo(course.getId());
        assertThat(event.getDate()).isEqualTo(Instant.now(clock));
        assertThat(event.getEventType()).isEqualTo(type);
        assertThat(event.getEntityType()).isEqualTo(entityClass.getSimpleName());
        assertThat(event.getEntityId()).isEqualTo(entityId);
        assertThat(event.getDifference()).isEqualTo(diff);
    }

    private User createTestUser() {
        return testUserHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                .userId((long) (Math.random() * 1000000))
                .workdate(LocalDate.now(clock))
                .sortingCenter(TestUserHelper.SortCenterGenerateParam.builder()
                        .id(SortingCenter.DEFAULT_SC_ID)
                        .build())
                .build());
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

}
