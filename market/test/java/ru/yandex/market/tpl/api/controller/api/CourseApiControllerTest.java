package ru.yandex.market.tpl.api.controller.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.model.course.CourierCourseDto;
import ru.yandex.market.tpl.api.model.course.CourseAssignmentDto;
import ru.yandex.market.tpl.api.model.course.CourseDto;
import ru.yandex.market.tpl.core.domain.course.service.CourseAssignmentService;
import ru.yandex.market.tpl.core.domain.course.service.CourseService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.api.ResourcesUtil.getFileContent;
import static ru.yandex.market.tpl.api.model.course.CourseAssignmentStatus.COMPLETED;
import static ru.yandex.market.tpl.api.model.course.CourseAssignmentStatus.IN_PROGRESS;
import static ru.yandex.market.tpl.api.model.course.CourseStatus.PUBLISHED;

@WebLayerTest(CourseApiController.class)
public class CourseApiControllerTest extends BaseShallowTest {

    @MockBean
    private CourseService courseService;
    @MockBean
    private CourseAssignmentService assignmentService;

    @Test
    void shouldReturnCourierCourses() throws Exception {
        when(courseService.getCourierCourses(eq(UID))).thenReturn(getCoursesDto());

        mockMvc.perform(
                        get("/api/courses")
                                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("course/response_course.json")
                ));
    }

    @Test
    void shouldReturnCourseAssignment() throws Exception {
        when(assignmentService.createCourseAssignment(eq(UID), eq(5L))).thenReturn(getAssignmentDto());

        mockMvc.perform(
                        post("/api/courses/{courseId}", 5L)
                                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("course/response_assignment.json")
                ));
    }

    @Test
    void shouldReturnStartedAssignment() throws Exception {
        when(assignmentService.startCourseAssignment(eq(UID), eq(10L))).thenReturn(getAssignmentDto());

        mockMvc.perform(
                        patch("/api/courses/assignments/{assignmentId}", 10L)
                                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("course/response_assignment.json")
                ));
    }

    @Test
    void shouldDeleteAssignments() throws Exception {
        doNothing().when(assignmentService).deleteAssignments(any(), any());

        mockMvc.perform(
                        delete("/api/courses/1/assignments")
                                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                                .with(httpBasic("1", "1"))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAcceptCompleteCourseRequest() throws Exception {
        when(blackboxClient.oauth(anyString(), anyString())).thenThrow(RuntimeException.class);
        when(assignmentService.completeCourseAssignment(any())).thenReturn(getCompletedAssignmentDto());

        mockMvc.perform(
                        post("/api/courses/kiosk/complete-assignment")
                                .with(httpBasic("1", "1"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("course/complete_course_request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("course/complete_course_response.json")
                ));
    }

    @Test
    void shouldFailCompleteCourseRequest() throws Exception {
        when(blackboxClient.oauth(anyString(), anyString())).thenThrow(RuntimeException.class);
        when(assignmentService.completeCourseAssignment(any())).thenReturn(getCompletedAssignmentDto());

        mockMvc.perform(
                        post("/api/courses/kiosk/complete-assignment")
                                .with(httpBasic("1", "1"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("course/invalid_complete_course_request.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldFailAuthentication() throws Exception {
        when(blackboxClient.oauth(anyString(), anyString())).thenThrow(RuntimeException.class);
        when(assignmentService.completeCourseAssignment(any())).thenReturn(getCompletedAssignmentDto());

        mockMvc.perform(
                        post("/api/courses/kiosk/complete-assignment")
                                .with(httpBasic("123", "123"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("course/complete_course_request.json")))
                .andExpect(status().isUnauthorized());
    }


    private List<CourierCourseDto> getCoursesDto() {
        return List.of(CourierCourseDto.builder()
                .id(1L)
                .name("Курс 1")
                .courseUrl("https://ya.ru")
                .imageUrl("https://ya.ru")
                .status(PUBLISHED)
                .description("Описание")
                .expectedDurationMinutes(20)
                .isNewCourse(true)
                .courseAssignment(getCompletedAssignmentDto())
                .currentAssignment(getAssignmentDto())
                .isHasExam(true)
                .build()
        );
    }

    private CourseAssignmentDto getCompletedAssignmentDto() {
        return CourseAssignmentDto.builder()
                .id(10L)
                .course(getCourseDto())
                .userId(345L)
                .sessionId("session-uuid")
                .status(COMPLETED)
                .build();
    }

    private CourseDto getCourseDto() {
        return CourseDto.builder()
                .id(5L)
                .name("Курс 1")
                .programId("program-uuid")
                .imageUrl("https://ya.ru")
                .status(PUBLISHED)
                .description("Курс первый")
                .expectedDurationMinutes(25)
                .hasExam(true)
                .build();
    }

    private CourseAssignmentDto getAssignmentDto() {
        return CourseAssignmentDto.builder()
                .id(10L)
                .course(getCourseDto())
                .userId(345L)
                .status(IN_PROGRESS)
                .build();
    }
}
