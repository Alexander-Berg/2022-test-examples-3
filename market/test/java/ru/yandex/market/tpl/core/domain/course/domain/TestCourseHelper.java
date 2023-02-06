package ru.yandex.market.tpl.core.domain.course.domain;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author sekulebyakin
 */
@Component
@RequiredArgsConstructor
public class TestCourseHelper {

    private final CourseCommandService courseCommandService;

    @Transactional
    public Course createTestCourse() {
        return createTestCourse(CourseGenerationParams.builder().build());
    }

    @Transactional
    public Course createTestCourse(CourseGenerationParams params) {
        return courseCommandService.createCourse(params.toCreateCommand());
    }

    @Transactional
    public Course createTestCourse(String name) {
        return createTestCourse(CourseGenerationParams.builder().name(name).build());
    }

    @Transactional
    public Course createPublishedTestCourse(CourseGenerationParams params) {
        var course = createTestCourse(params);
        return publishCourse(course.getId());
    }

    @Transactional
    public Course createPublishedTestCourse(String name) {
        return createPublishedTestCourse(CourseGenerationParams.builder().name(name).build());
    }

    @Transactional
    public Course createPublishedTestCourse() {
        return createPublishedTestCourse(CourseGenerationParams.builder().build());
    }

    @Transactional
    public Course publishCourse(long courseId) {
        return courseCommandService.publishCourse(CourseCommand.Publish.builder().courseId(courseId).build());
    }

    @Transactional
    public Course unpublishCourse(long courseId) {
        return courseCommandService.unpublishCourse(CourseCommand.Unpublish.builder().courseId(courseId).build());
    }

    @Transactional
    public Course activateCourseForSC(long courseId, Set<Long> scIds) {
        return courseCommandService.activate(CourseCommand.Activate.builder().courseId(courseId).scIds(scIds).build());
    }

    @Transactional
    public Course deactivateCourseForSC(long courseId, Set<Long> scIds) {
        return courseCommandService.deactivate(CourseCommand.Deactivate.builder().courseId(courseId).scIds(scIds).build());
    }
}
