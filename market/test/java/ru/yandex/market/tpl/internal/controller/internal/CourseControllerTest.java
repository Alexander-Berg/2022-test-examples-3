package ru.yandex.market.tpl.internal.controller.internal;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.model.course.CourseDataRequest;
import ru.yandex.market.tpl.api.model.course.CourseDto;
import ru.yandex.market.tpl.api.model.course.CoursePushDto;
import ru.yandex.market.tpl.api.model.course.CourseType;
import ru.yandex.market.tpl.api.model.course.UserStatusRequirement;
import ru.yandex.market.tpl.core.domain.course.service.CourseAssignmentService;
import ru.yandex.market.tpl.core.domain.course.service.CourseService;
import ru.yandex.market.tpl.core.domain.course.service.notification.CourseNotificationService;
import ru.yandex.market.tpl.core.util.ObjectMappers;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(CourseController.class)
public class CourseControllerTest extends BaseShallowTest {

    @MockBean
    private CourseService courseService;

    @MockBean
    private CourseNotificationService courseNotificationService;

    @MockBean
    private CourseAssignmentService courseAssignmentService;

    @Test
    void shouldSendCourierPushes() throws Exception {
        mockMvc.perform(
                        post("/internal/courses/send-push")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "    \"courseId\": 1,\n" +
                                        "    \"message\": \"push message\",\n" +
                                        "    \"userUids\": [3,4,5]\n" +
                                        "}\n"))
                .andExpect(status().isOk());

        verify(courseNotificationService, times(1))
                .sendPush(new CoursePushDto(1L, "push message", List.of(3L, 4L, 5L)));

    }

    @Test
    void shouldActivateCourseForSC() throws Exception {
        mockMvc.perform(
                        patch("/internal/courses/12345/config/sc")
                                .param("scIds", "5", "7", "9"))
                .andExpect(status().isOk());
        verify(courseService).activateCourseForSc(eq(12345L), eq(Set.of(5L, 7L, 9L)));
    }

    @Test
    void shouldDeactivateCourseForSC() throws Exception {
        mockMvc.perform(
                        delete("/internal/courses/12345/config/sc")
                                .param("scIds", "5", "7", "9"))
                .andExpect(status().isOk());
        verify(courseService).deactivateCourseForSc(eq(12345L), eq(Set.of(5L, 7L, 9L)));
    }

    @Test
    void shouldReturnCourse() throws Exception {
        var course = createTestCourseDto();
        doReturn(course).when(courseService).getCourseById(eq(12345L));
        mockMvc.perform(get("/internal/courses/12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(course.getId()))
                .andExpect(jsonPath("$.name").value(course.getName()))
                .andExpect(jsonPath("$.description").value(course.getDescription()))
                .andExpect(jsonPath("$.programId").value(course.getProgramId()))
                .andExpect(jsonPath("$.imageUrl").value(course.getImageUrl()))
                .andExpect(jsonPath("$.expectedDurationMinutes").value(course.getExpectedDurationMinutes()))
                .andExpect(jsonPath("$.userStatusRequirement").value(course.getUserStatusRequirement().toString()))
                .andExpect(jsonPath("$.beta").value(course.isBeta()))
                .andExpect(jsonPath("$.scFiltersEnabled").value(course.isScFiltersEnabled()))
                .andExpect(jsonPath("$.hasExam").value(course.isHasExam()))
                .andExpect(jsonPath("$.createdAt").value(course.getCreatedAt().toString()));
    }

    @Test
    void shouldCallUpdateCourse() throws Exception {
        var course = createTestCourseDto();
        var courseData = CourseDataRequest.builder()
                .name("new name")
                .description("new-description")
                .programId("e57b51b3-5592-4463-9e36-a77e2f0956cb")
                .imageUrl("https://new-test-course-url")
                .courseType(CourseType.PRO_APPLICATION)
                .expectedDurationMinutes(10)
                .userStatusRequirement(UserStatusRequirement.ALL)
                .hasExam(false)
                .build();
        var body = ObjectMappers.baseObjectMapper().writeValueAsString(courseData);
        doReturn(course).when(courseService).updateCourse(eq(course.getId()), eq(courseData));
        mockMvc.perform(
                        put("/internal/courses/12345")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
        verify(courseService).updateCourse(eq(course.getId()), eq(courseData));
    }

    @Test
    void shouldCallCreateCourse() throws Exception {
        var course = createTestCourseDto();
        var courseData = CourseDataRequest.builder()
                .name("new name")
                .description("new-description")
                .programId("e57b51b3-5592-4463-9e36-a77e2f0956cb")
                .imageUrl("https://new-test-course-url")
                .expectedDurationMinutes(10)
                .userStatusRequirement(UserStatusRequirement.ALL)
                .build();
        var body = ObjectMappers.baseObjectMapper().writeValueAsString(courseData);
        doReturn(course).when(courseService).createCourse(eq(courseData));
        mockMvc.perform(
                        post("/internal/courses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
        verify(courseService).createCourse(eq(courseData));
    }

    @Test
    void shouldCallPublishCourse() throws Exception {
        var course = createTestCourseDto();
        doReturn(course).when(courseService).publishCourse(eq(12345L));
        mockMvc.perform(
                        put("/internal/courses/12345/publishing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12345L));
        verify(courseService).publishCourse(eq(12345L));
    }

    @Test
    void shouldCallUnpublishCourse() throws Exception {
        var course = createTestCourseDto();
        doReturn(course).when(courseService).unpublish(eq(12345L));
        mockMvc.perform(
                        delete("/internal/courses/12345/publishing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12345L));
        verify(courseService).unpublish(eq(12345L));
    }

    @Test
    void shouldCallUpdateAssignments() throws Exception {
        mockMvc.perform(
                        put("/internal/courses/assignments/auto")
                                .param("courseIds", "123", "234")
                                .param("userIds", "555", "333"))
                .andExpect(status().isOk());
        verify(courseAssignmentService).updateCourseAssignments(123L);
        verify(courseAssignmentService).updateCourseAssignments(234L);
        verify(courseAssignmentService).updateUserAssignments(555L);
        verify(courseAssignmentService).updateUserAssignments(333L);
    }

    private CourseDto createTestCourseDto() {
        return CourseDto.builder()
                .id(12345L)
                .name("test-course")
                .description("test-course-description")
                .programId("b8d64e8c-2b40-4e92-a967-54be56a99b18")
                .imageUrl("https://test-course-url")
                .expectedDurationMinutes(15)
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .beta(false)
                .scFiltersEnabled(true)
                .hasExam(true)
                .createdAt(LocalDateTime.of(2000, 10, 15, 14, 30).toInstant(ZoneOffset.ofHours(3)))
                .build();
    }
}
