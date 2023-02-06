package ru.yandex.market.tpl.dora.test.factory;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.dora.db.jooq.enums.ECourseAssignmentStatus;
import ru.yandex.market.tpl.dora.db.jooq.tables.pojos.Course;
import ru.yandex.market.tpl.dora.db.jooq.tables.pojos.CourseAssignment;
import ru.yandex.market.tpl.dora.domain.assignment.CourseAssignmentCommandService;
import ru.yandex.market.tpl.dora.domain.assignment.repository.CourseAssignmentRepository;
import ru.yandex.market.tpl.dora.domain.course.CourseCommandService;
import ru.yandex.mj.generated.server.model.CourseAssignmentStatusDto;

import static ru.yandex.market.tpl.dora.domain.mapper.EntityMapper.MAPPER;
import static ru.yandex.market.tpl.dora.test.factory.mapper.TestEntityMapper.TEST_MAPPER;

@Transactional
public class TestCourseFactory {

    @Autowired
    private CourseCommandService courseCommandService;

    @Autowired
    private CourseAssignmentCommandService courseAssignmentCommandService;

    @Autowired
    private CourseAssignmentRepository courseAssignmentRepository;

    public Course create() {
        return create(CourseTestParams.builder().build());
    }

    public Course create(CourseTestParams params) {
        return courseCommandService.create(TEST_MAPPER.toCourseCreationRecord(params));
    }

    public CourseAssignment assign(long courseId, String userId) {
        return courseAssignmentCommandService.assign(courseId, userId);
    }

    @Deprecated
    public void changeStatus(long assignmentId, ECourseAssignmentStatus status) {
        courseAssignmentRepository.updateStatus(assignmentId, status);
    }

    @Data
    @Builder
    public static class CourseTestParams {

        public static final String DEFAULT_PROGRAM_ID = "dora-course";
        public static final String DEFAULT_TITLE = "Курс от Даши путешественницы";
        public static final String DEFAULT_DESCRIPTION = null;
        public static final Integer DEFAULT_EXPECTED_DURATION_MINUTES = null;
        public static final String DEFAULT_IMAGE_URL = null;
        public static final Boolean DEFAULT_HAS_EXAM = null;
        public static final List<String> DEFAULT_GROUP_IDS = null;
        public static final List<String> DEFAULT_FEATURE_NAMES = null;

        @Builder.Default
        private String programId = DEFAULT_PROGRAM_ID;

        @Builder.Default
        private String platformName = TestPlatformFactory.PlatformTestParams.DEFAULT_PLATFORM_NAME;

        @Builder.Default
        private String title = DEFAULT_TITLE;

        @Builder.Default
        private String description = DEFAULT_DESCRIPTION;

        @Builder.Default
        private Integer expectedDurationMinutes = DEFAULT_EXPECTED_DURATION_MINUTES;

        @Builder.Default
        private String imageUrl = DEFAULT_IMAGE_URL;

        @Builder.Default
        private Boolean hasExam = DEFAULT_HAS_EXAM;

        @Builder.Default
        private List<String> groupIds = DEFAULT_GROUP_IDS;

        @Builder.Default
        private List<String> featureNames = DEFAULT_FEATURE_NAMES;
    }

}
