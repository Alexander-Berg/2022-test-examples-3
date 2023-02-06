package ru.yandex.market.tpl.dora.domain.assignment;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.dora.domain.assignment.repository.CourseAssignmentHistoryRepository;
import ru.yandex.market.tpl.dora.domain.assignment.repository.CourseAssignmentRepository;
import ru.yandex.market.tpl.dora.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.tpl.dora.test.factory.TestCourseFactory;
import ru.yandex.market.tpl.dora.test.factory.TestPlatformFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static ru.yandex.market.tpl.dora.db.jooq.enums.ECourseAssignmentStatus.ASSIGNED;
import static ru.yandex.market.tpl.dora.db.jooq.enums.ECourseAssignmentStatus.CANCELLED;
import static ru.yandex.market.tpl.dora.db.jooq.enums.ECourseAssignmentStatus.COMPLETED;
import static ru.yandex.market.tpl.dora.db.jooq.enums.ECourseAssignmentStatus.IN_PROGRESS;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CourseAssignmentCommandServiceTest {

    private static final String USER_ID = "1";
    private static final String USER_ID_2 = "2";
    private static final String USER_ID_3 = "3";
    private static final String USER_ID_4 = "4";
    private static final String GROUP_ID = "dora-one";
    private static final String FEATURE = "explorer-one";

    private final TestCourseFactory courseFactory;
    private final TestPlatformFactory platformFactory;
    private final CourseAssignmentCommandService courseAssignmentCommandService;
    private final CourseAssignmentHistoryRepository courseAssignmentHistoryRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;

    @Test
    void assignCourseTest() {
        platformFactory.create();
        var course = courseFactory.create();

        courseAssignmentCommandService.assign(course.getId(), USER_ID);
        courseAssignmentCommandService.assign(course.getId(), USER_ID);

        var history = courseAssignmentHistoryRepository.getCourseAssignmentHistoryByCourseIdAndUserId(
                course.getId(), USER_ID);
        assertEquals(history.size(), 1);
    }

    @Test
    void startCourseTest() {
        platformFactory.create();
        var course = courseFactory.create();
        var course2 = courseFactory.create();
        var course3 = courseFactory.create();
        var course4 = courseFactory.create();
        var course5 = courseFactory.create();

        courseAssignmentCommandService.insertStatus(course.getId(), USER_ID, IN_PROGRESS);
        courseAssignmentCommandService.insertStatus(course2.getId(), USER_ID, ASSIGNED);
        courseAssignmentCommandService.insertStatus(course3.getId(), USER_ID, COMPLETED);
        courseAssignmentCommandService.insertStatus(course4.getId(), USER_ID, CANCELLED);

        courseAssignmentCommandService.start(course.getId(), USER_ID);
        courseAssignmentCommandService.start(course2.getId(), USER_ID);
        courseAssignmentCommandService.start(course3.getId(), USER_ID);
        courseAssignmentCommandService.start(course4.getId(), USER_ID);
        courseAssignmentCommandService.start(course5.getId(), USER_ID);

        var courseAssignments = courseAssignmentRepository.findAll();
        assertEquals(courseAssignments.size(), 5);
        courseAssignments.forEach(ca -> {
                    assertEquals(ca.getStatus(), IN_PROGRESS);
                }
        );
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course.getId(), USER_ID).size(), 1);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course2.getId(), USER_ID).size(), 2);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course3.getId(), USER_ID).size(), 2);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course4.getId(), USER_ID).size(), 2);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course5.getId(), USER_ID).size(), 1);
    }

    @Test
    void completeCourseTest() {
        platformFactory.create();
        var course = courseFactory.create();
        var course2 = courseFactory.create();
        var course3 = courseFactory.create();
        var course4 = courseFactory.create();
        var course5 = courseFactory.create();

        courseAssignmentCommandService.insertStatus(course.getId(), USER_ID, IN_PROGRESS);
        courseAssignmentCommandService.insertStatus(course2.getId(), USER_ID, ASSIGNED);
        courseAssignmentCommandService.insertStatus(course3.getId(), USER_ID, COMPLETED);
        courseAssignmentCommandService.insertStatus(course4.getId(), USER_ID, CANCELLED);

        courseAssignmentCommandService.complete(USER_ID, TestCourseFactory.CourseTestParams.DEFAULT_PROGRAM_ID);

        var courseAssignments = courseAssignmentRepository.findAll();
        assertEquals(courseAssignments.size(), 5);
        courseAssignments.forEach(ca -> {
                    assertEquals(ca.getStatus(), COMPLETED);
                }
        );
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course.getId(), USER_ID).size(), 2);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course2.getId(), USER_ID).size(), 2);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course3.getId(), USER_ID).size(), 1);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course4.getId(), USER_ID).size(), 2);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course5.getId(), USER_ID).size(), 1);
    }

    @Test
    void reassignCourseTest() {
        platformFactory.create();
        var course = courseFactory.create();
        var course2 = courseFactory.create();
        var course3 = courseFactory.create();
        var course4 = courseFactory.create();
        var course5 = courseFactory.create();

        courseAssignmentCommandService.insertStatus(course.getId(), USER_ID, IN_PROGRESS);
        courseAssignmentCommandService.insertStatus(course2.getId(), USER_ID, ASSIGNED);
        courseAssignmentCommandService.insertStatus(course3.getId(), USER_ID, COMPLETED);
        courseAssignmentCommandService.insertStatus(course4.getId(), USER_ID, CANCELLED);

        courseAssignmentCommandService.reassign(course.getId(), USER_ID);
        courseAssignmentCommandService.reassign(course2.getId(), USER_ID);
        courseAssignmentCommandService.reassign(course3.getId(), USER_ID);
        courseAssignmentCommandService.reassign(course4.getId(), USER_ID);
        assertThrowsExactly(TplIllegalArgumentException.class,
                () -> courseAssignmentCommandService.reassign(course5.getId(), USER_ID));

        assertEquals(courseAssignmentRepository.findAll().size(), 4);

        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course.getId(), USER_ID).size(), 3);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course2.getId(), USER_ID).size(), 1);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course3.getId(), USER_ID).size(), 1);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course4.getId(), USER_ID).size(), 1);
        assertEquals(courseAssignmentHistoryRepository
                .getCourseAssignmentHistoryByCourseIdAndUserId(course5.getId(), USER_ID).size(), 0);

        assertEquals(courseAssignmentRepository.findOneByCourseIdAndUserId(course.getId(), USER_ID).getStatus(),
                ASSIGNED);
        assertEquals(courseAssignmentRepository.findOneByCourseIdAndUserId(course2.getId(), USER_ID).getStatus(),
                ASSIGNED);
        assertEquals(courseAssignmentRepository.findOneByCourseIdAndUserId(course3.getId(), USER_ID).getStatus(),
                COMPLETED);
        assertEquals(courseAssignmentRepository.findOneByCourseIdAndUserId(course4.getId(), USER_ID).getStatus(),
                CANCELLED);
    }

    @Test
    void assignAllCourseTest() {
        var platform = platformFactory.createWithFeatures(List.of(FEATURE));
        var course = courseFactory.create();
        var course2 = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .groupIds(List.of(GROUP_ID))
                        .build());
        var course3 = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .featureNames(List.of(FEATURE))
                        .build());
        var course4 = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .groupIds(List.of(GROUP_ID))
                        .featureNames(List.of(FEATURE))
                        .build());

        courseAssignmentCommandService.assignAll(platform.getName(), null, null, USER_ID);
        var assignments = courseAssignmentRepository.findCourseIdsByUserId(USER_ID);
        assertThat(assignments).containsExactlyInAnyOrderElementsOf(List.of(course.getId(), course2.getId(),
                course3.getId()));

        courseAssignmentCommandService.assignAll(platform.getName(), List.of(GROUP_ID), null, USER_ID_2);
        var assignments2 = courseAssignmentRepository.findCourseIdsByUserId(USER_ID_2);
        assertThat(assignments2).containsExactlyInAnyOrderElementsOf(List.of(course.getId(), course2.getId(),
                course4.getId()));

        courseAssignmentCommandService.assignAll(platform.getName(), null, List.of(FEATURE), USER_ID_3);
        var assignments3 = courseAssignmentRepository.findCourseIdsByUserId(USER_ID_3);
        assertThat(assignments3).containsExactlyInAnyOrderElementsOf(List.of(course.getId(), course3.getId(),
                course4.getId()));

        courseAssignmentCommandService.assignAll(platform.getName(), List.of(GROUP_ID), List.of(FEATURE), USER_ID_4);
        var assignments4 = courseAssignmentRepository.findCourseIdsByUserId(USER_ID_4);
        assertThat(assignments4).containsExactlyInAnyOrderElementsOf(List.of(course.getId(), course2.getId(),
                course3.getId(), course4.getId()));
    }

}
