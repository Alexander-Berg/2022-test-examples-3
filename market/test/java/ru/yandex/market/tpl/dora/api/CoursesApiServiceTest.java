package ru.yandex.market.tpl.dora.api;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.dora.db.jooq.tables.pojos.Course;
import ru.yandex.market.tpl.dora.domain.assignment.CourseAssignmentCommandService;
import ru.yandex.market.tpl.dora.test.BaseShallowTest;
import ru.yandex.market.tpl.dora.test.WebLayerTest;
import ru.yandex.market.tpl.dora.test.factory.TestCourseFactory;
import ru.yandex.market.tpl.dora.test.factory.TestPlatformFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.dora.db.jooq.enums.ECourseAssignmentStatus.CANCELLED;
import static ru.yandex.market.tpl.dora.db.jooq.enums.ECourseAssignmentStatus.COMPLETED;
import static ru.yandex.market.tpl.dora.db.jooq.enums.ECourseAssignmentStatus.IN_PROGRESS;
import static ru.yandex.market.tpl.dora.test.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CoursesApiServiceTest extends BaseShallowTest {

    private static final String USER_ID = "dora-the-explorer";
    private static final String GROUP_ID = "dora-one";
    private static final String GROUP_ID_2 = "dora-two";
    private static final String FEATURE = "explorer-one";
    private static final String FEATURE_2 = "explorer-two";

    private final TestCourseFactory courseFactory;
    private final TestPlatformFactory platformFactory;
    private final CourseAssignmentCommandService courseAssignmentCommandService;

    @BeforeEach
    void setup() {
        platformFactory.createWithFeatures(List.of(FEATURE, FEATURE_2));
    }

    @Test
    void assignCourseTest() throws Exception {
        Course course = courseFactory.create();

        mockMvc.perform(post("/courses/" + course.getId() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignment/request_assign_course.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignment/response_assign_course.json")));
    }

    @Test
    void startCourseTest() throws Exception {
        Course course = courseFactory.create();

        mockMvc.perform(put("/courses/" + course.getId() + "/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignment/request_assign_course.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(getFileContent("assignment/response_start_course.json"),
                        course.getId())));
    }

    @Test
    void reassignCourseTest() throws Exception {
        Course course = courseFactory.create();
        courseAssignmentCommandService.start(course.getId(), USER_ID);

        mockMvc.perform(patch("/courses/" + course.getId() + "/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignment/request_assign_course.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignment/response_assign_course.json")));
    }

    @Test
    void getCoursesTestNoFilters() throws Exception {
        var course = courseFactory.create();
        var courseAssignment = courseFactory.assign(course.getId(), USER_ID);

        mockMvc.perform(get("/courses")
                        .param("userId", USER_ID)
                        .param("platformName", TestPlatformFactory.PlatformTestParams.DEFAULT_PLATFORM_NAME)
                        .param("groupIds", GROUP_ID + "," + GROUP_ID_2)
                        .param("features", FEATURE + "," + FEATURE_2))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(getFileContent("course/response_courses.json"),
                        course.getId(), courseAssignment.getId()), true));
    }

    @Test
    void getCoursesTestGroupFilters() throws Exception {
        var course = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .featureNames(List.of(FEATURE, FEATURE_2))
                        .build());
        var courseWithFilter = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .groupIds(List.of(GROUP_ID))
                        .build());
        courseFactory.assign(course.getId(), USER_ID);
        var courseWithFilterAssignment = courseFactory.assign(courseWithFilter.getId(), USER_ID);

        mockMvc.perform(get("/courses")
                        .param("userId", USER_ID)
                        .param("platformName", TestPlatformFactory.PlatformTestParams.DEFAULT_PLATFORM_NAME)
                        .param("groupIds", GROUP_ID + "," + GROUP_ID_2))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(getFileContent("course/response_courses.json"),
                        courseWithFilter.getId(), courseWithFilterAssignment.getId())));
    }

    @Test
    void getCoursesTestFeaturesFilters() throws Exception {
        var course = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .groupIds(List.of(GROUP_ID))
                        .build());
        var courseWithFilter = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .featureNames(List.of(FEATURE, FEATURE_2))
                        .build());
        courseFactory.assign(course.getId(), USER_ID);
        var courseWithFilterAssignment = courseFactory.assign(courseWithFilter.getId(), USER_ID);

        mockMvc.perform(get("/courses")
                        .param("userId", USER_ID)
                        .param("platformName", TestPlatformFactory.PlatformTestParams.DEFAULT_PLATFORM_NAME)
                        .param("features", FEATURE + "," + FEATURE_2))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(getFileContent("course/response_courses.json"),
                        courseWithFilter.getId(), courseWithFilterAssignment.getId())));
    }

    @Test
    void getCoursesTestGroupFeaturesFilters() throws Exception {
        var course = courseFactory.create();
        var courseWithGroupFilter = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .groupIds(List.of(GROUP_ID))
                        .build());
        var courseWithFeatureFilter = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .featureNames(List.of(FEATURE, FEATURE_2))
                        .build());
        var courseWithBothFilter = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .groupIds(List.of(GROUP_ID))
                        .featureNames(List.of(FEATURE, FEATURE_2))
                        .build());
        courseFactory.assign(course.getId(), USER_ID);
        courseFactory.assign(courseWithGroupFilter.getId(), USER_ID);
        courseFactory.assign(courseWithFeatureFilter.getId(), USER_ID);
        courseFactory.assign(courseWithBothFilter.getId(), USER_ID);

        mockMvc.perform(get("/courses")
                        .param("userId", USER_ID)
                        .param("platformName", TestPlatformFactory.PlatformTestParams.DEFAULT_PLATFORM_NAME)
                        .param("features", FEATURE + "," + FEATURE_2)
                        .param("groupIds", GROUP_ID + "," + GROUP_ID_2))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("course/response_courses_group_feature_filters.json")));
    }

    @Test
    void getCoursesTestPageSizeFilters() throws Exception {
        var course = courseFactory.create();
        var course2 = courseFactory.create();
        courseFactory.assign(course.getId(), USER_ID);
        courseFactory.assign(course2.getId(), USER_ID);

        mockMvc.perform(get("/courses")
                        .param("userId", USER_ID)
                        .param("platformName", TestPlatformFactory.PlatformTestParams.DEFAULT_PLATFORM_NAME)
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("course/response_courses_page_size_filters.json")));
    }

    @Test
    void getCoursesTestCommonQueryFilters() throws Exception {
        var course = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .title("Дора")
                        .build());
        var course2 = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .description("Дора")
                        .title("ора")
                        .build());
        var course3 = courseFactory.create(
                TestCourseFactory.CourseTestParams.builder()
                        .description("ора")
                        .title("ора")
                        .build());
        courseFactory.assign(course.getId(), USER_ID);
        courseFactory.assign(course2.getId(), USER_ID);
        courseFactory.assign(course3.getId(), USER_ID);

        mockMvc.perform(get("/courses")
                        .param("userId", USER_ID)
                        .param("platformName", TestPlatformFactory.PlatformTestParams.DEFAULT_PLATFORM_NAME)
                        .param("commonQuery", "д"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("course/response_courses_common_query_filters.json")));
    }

    @Test
    void getCoursesTestStatusSort() throws Exception {
        var course = courseFactory.create();
        var course2 = courseFactory.create();
        var course3 = courseFactory.create();
        var course4 = courseFactory.create();
        var assignment = courseFactory.assign(course.getId(), USER_ID);
        var assignment2 = courseFactory.assign(course2.getId(), USER_ID);
        courseFactory.changeStatus(assignment2.getId(), IN_PROGRESS);
        var assignment3 = courseFactory.assign(course3.getId(), USER_ID);
        courseFactory.changeStatus(assignment3.getId(), COMPLETED);
        var assignment4 = courseFactory.assign(course4.getId(), USER_ID);
        courseFactory.changeStatus(assignment4.getId(), CANCELLED);

        mockMvc.perform(get("/courses")
                        .param("userId", USER_ID)
                        .param("platformName", TestPlatformFactory.PlatformTestParams.DEFAULT_PLATFORM_NAME)
                        .param("sort", "status"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(getFileContent("course/response_courses_status_sort.json"),
                        course2.getId(), assignment2.getId(), assignment2.getUserId(),
                        course.getId(), assignment.getId(), assignment.getUserId(),
                        course3.getId(), assignment3.getId(), assignment3.getUserId()), true));
    }

}
