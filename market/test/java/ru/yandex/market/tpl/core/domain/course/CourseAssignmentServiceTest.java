package ru.yandex.market.tpl.core.domain.course;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.course.CourseAssignmentCompleteResultDto;
import ru.yandex.market.tpl.api.model.course.CourseAssignmentStatus;
import ru.yandex.market.tpl.api.model.course.CourseDataRequest;
import ru.yandex.market.tpl.api.model.course.UserStatusRequirement;
import ru.yandex.market.tpl.core.domain.course.domain.Course;
import ru.yandex.market.tpl.core.domain.course.domain.CourseAssignment;
import ru.yandex.market.tpl.core.domain.course.repository.CourseAssignmentRepository;
import ru.yandex.market.tpl.core.domain.course.repository.CourseRepository;
import ru.yandex.market.tpl.core.domain.course.service.CourseAssignmentService;
import ru.yandex.market.tpl.core.domain.course.service.CourseService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.course.CourseAssignmentStatus.IN_PROGRESS;

@RequiredArgsConstructor
public class CourseAssignmentServiceTest extends TplAbstractTest {

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseAssignmentService assignmentService;
    private final CourseAssignmentRepository assignmentRepository;
    private final Clock clock;

    @BeforeEach
    void setUp() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        assignmentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    public void shouldCreateCourseAndAssignment() {
        var course = createTestCourse("Курс 1", "program-uuid-1");
        var assignment = createTestCourseAssignment(course.getId());
        assertThat(assignment).extracting("course.name", "userId", "sessionId", "status")
                .containsOnly("Курс 1", 123L, null, IN_PROGRESS);
    }

    @Test
    public void shouldCompleteAssignmentWebhook() {
        var course = createTestCourse("Курс 2", "program-uuid-2");
        var assignment = createTestCourseAssignment(course.getId());

        var resultDto = new CourseAssignmentCompleteResultDto("session-uuid", course.getProgramId(),
                123L, null, true, 1621508239L, 0L);
        var assignmentDto = assignmentService.completeCourseAssignment(resultDto);

        assertThat(assignmentDto).extracting("course.name", "userId", "sessionId", "status", "completedAt", "passed")
                .containsOnly("Курс 2", 123L, "session-uuid", CourseAssignmentStatus.COMPLETED,
                        Instant.ofEpochSecond(1621508239L), true);
    }

    @Test
    public void shouldCompleteAssignmentWebhookWithEducationCompletedAt() {
        var course = createTestCourse("Курс 2", "program-uuid-2");
        var assignment = createTestCourseAssignment(course.getId());

        var resultDto = new CourseAssignmentCompleteResultDto("session-uuid", course.getProgramId(),
                123L, null, true, 0L, 1621508239L);
        var assignmentDto = assignmentService.completeCourseAssignment(resultDto);

        assertThat(assignmentDto).extracting("course.name", "userId", "sessionId", "status", "completedAt", "passed")
                .containsOnly("Курс 2", 123L, "session-uuid", CourseAssignmentStatus.COMPLETED,
                        Instant.ofEpochSecond(1621508239L), true);
    }

    @Test
    public void shouldCompleteAssignment() {
        var course = createTestCourse("Курс 2", "program-uuid-2");
        var assignment = createTestCourseAssignment(course.getId());

        var assignmentDto = assignmentService.completeCourseAssignment(123L, "session-uuid", "program-uuid-2",
                assignment.getId(), Instant.now(clock), false);

        assertThat(assignmentDto).extracting("course.name", "userId", "sessionId", "status", "completedAt", "passed")
                .containsOnly("Курс 2", 123L, "session-uuid", CourseAssignmentStatus.COMPLETED, Instant.now(clock),
                        false);
    }

    @Test
    public void shouldCompleteNonExistingAssignment() {
        createTestCourse("Курс 2", "program-uuid-2");

        var assignmentDto = assignmentService.completeCourseAssignment(123L, "session-uuid", "program-uuid-2", null,
                Instant.now(clock), true);

        assertThat(assignmentDto).extracting("course.name", "userId", "sessionId", "status", "completedAt", "passed")
                .containsOnly("Курс 2", 123L, "session-uuid", CourseAssignmentStatus.COMPLETED, Instant.now(clock),
                        true);
    }

    @Test
    public void shouldNotCreateAssignment() {
        var course = createTestCourse("Курс 2", "program-uuid-2");
        var assignment1 = createTestCourseAssignment(course.getId());
        var assignment2 = createTestCourseAssignment(course.getId());

        List<CourseAssignment> assignments = assignmentRepository.findAll();
        assertThat(assignments).hasSize(1).extracting("status").containsOnly(IN_PROGRESS);
        assertThat(assignment1).isEqualTo(assignment2);
    }

    @Test
    public void shouldDeleteOnlySelectedAssignments() {
        var course1 = createTestCourse("Курс 1", "program-uuid-1");
        var course2 = createTestCourse("Курс 2", "program-uuid-2");
        var assignment1 = createTestCourseAssignment(course1.getId());
        var assignment2 = createTestCourseAssignment(course2.getId());

        assignmentService.deleteAssignments(course1.getId(), 123L);
        List<CourseAssignment> assignments = assignmentRepository.findAll();
        assertThat(assignments).hasSize(1)
                .extracting("id").contains(assignment2.getId());
    }

    @Test
    public void shouldNotFailWhenMultipleAssignmentsInProgress() {
        var course = createTestCourse("Курс 1", "program-uuid-1");
        assignmentRepository.save(buildAssignment(course));
        assignmentRepository.save(buildAssignment(course));

        List<CourseAssignment> assignments = assignmentRepository.findAll();
        assertThat(assignments).hasSize(2);

        var assignmentDto = assignmentService.completeCourseAssignment(123L, "session-uuid", "program-uuid-1", null, Instant.now(clock), true);
        assertThat(assignmentDto).extracting("course.name", "userId", "sessionId", "status", "passed")
                .containsOnly("Курс 1", 123L, "session-uuid", CourseAssignmentStatus.COMPLETED, true);
    }

    @Test
    public void shouldUpdateCourseScore() {
        var course = createTestCourse("Курс 3", "program-uuid-3");
        var assignment = createTestCourseAssignment(course.getId());

        var assignmentDto = assignmentService.updateScore(assignment.getId(), 5);

        assertThat(assignmentDto).extracting("course.name", "userId", "sessionId", "status", "score")
                .containsOnly("Курс 3", 123L, null, IN_PROGRESS, 5);
    }

    @Test
    public void shouldStartCourse() {
        var course = createTestCourse("Курс 1", "program-uuid-1");
        var assignment = assignmentService.createCourseAssignment(course, 123L);
        assertThat(assignment.getStatus()).isEqualTo(CourseAssignmentStatus.ASSIGNED);

        var startedAssignment = assignmentService.startCourseAssignment(123L, assignment.getId());
        assertThat(startedAssignment.getId()).isEqualTo(assignment.getId());
        assertThat(startedAssignment.getStatus()).isEqualTo(IN_PROGRESS);
    }

    private Course createTestCourse(String name, String programId) {
        var courseDto = courseService.createCourse(CourseDataRequest.builder()
                .name(name)
                .programId(programId)
                .imageUrl("https://ya.ru")
                .userStatusRequirement(UserStatusRequirement.ALL)
                .build());
        var courseOpt = courseRepository.findById(courseDto.getId());
        assertThat(courseOpt).isNotEmpty();
        return courseOpt.get();
    }

    private CourseAssignment createTestCourseAssignment(Long courseId) {
        var assignmentDto = assignmentService.createCourseAssignment(123L, courseId);
        var assignmentOpt = assignmentRepository.findById(assignmentDto.getId());
        assertThat(assignmentOpt).isNotEmpty();
        return assignmentOpt.get();
    }

    private CourseAssignment buildAssignment(Course course) {
        return CourseAssignment.builder()
                .course(course)
                .userId(123L)
                .status(IN_PROGRESS)
                .build();
    }
}
