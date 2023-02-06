package ru.yandex.market.wms.timetracker.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.timetracker.config.NoAuthTest;
import ru.yandex.market.wms.timetracker.dto.AssignTaskRequest;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.service.AssignTaskService;

@WebMvcTest(AssignTaskController.class)
@ActiveProfiles("test")
@Import(NoAuthTest.class)
class AssignTaskControllerTest {

    @MockBean
    private AssignTaskService assignTaskService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void assignTask() throws Exception {

        AssignTaskRequest contentExpected = AssignTaskRequest.builder()
                .assigner("assigner")
                .duration(15L)
                .status(EmployeeStatus.SHIPPING)
                .userNames(List.of("test1", "test2"))
                .build();

        Mockito.doNothing().when(assignTaskService).save(
                ArgumentMatchers.eq("SOF"), ArgumentMatchers.any(AssignTaskRequest.class));

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/assign-task/SOF")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void assignTaskWhenOnlyOneUser() throws Exception {

        AssignTaskRequest contentExpected = AssignTaskRequest.builder()
                .assigner("assigner")
                .duration(15L)
                .status(EmployeeStatus.SHIPPING)
                .userNames(List.of("test1"))
                .build();

        Mockito.doNothing().when(assignTaskService).save(
                ArgumentMatchers.eq("SOF"), ArgumentMatchers.any(AssignTaskRequest.class));

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/assign-task/SOF")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void assignTaskWhenDurationIsEmpty() throws Exception {

        AssignTaskRequest contentExpected = AssignTaskRequest.builder()
                .assigner("assigner")
                .duration(0L)
                .status(EmployeeStatus.SHIPPING)
                .userNames(List.of("test1"))
                .build();

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/assign-task/SOF")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void assignTaskWhenAssignerIsEmpty() throws Exception {

        AssignTaskRequest contentExpected = AssignTaskRequest.builder()
                .assigner("")
                .duration(15L)
                .status(EmployeeStatus.SHIPPING)
                .userNames(List.of("test1"))
                .build();

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/assign-task/SOF")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void assignTaskWhenUserNamesIsEmpty() throws Exception {

        AssignTaskRequest contentExpected = AssignTaskRequest.builder()
                .assigner("assigner")
                .duration(15L)
                .status(EmployeeStatus.SHIPPING)
                .userNames(Collections.emptyList())
                .build();

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/assign-task/SOF")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void assignTaskWhenUserNamesContainsNull() throws Exception {

        AssignTaskRequest contentExpected = AssignTaskRequest.builder()
                .assigner("assigner")
                .duration(15L)
                .status(EmployeeStatus.SHIPPING)
                .userNames(Arrays.asList(null, "test"))
                .build();

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/assign-task/SOF")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void assignTaskWhenUserNamesContainsEmptyString() throws Exception {

        AssignTaskRequest contentExpected = AssignTaskRequest.builder()
                .assigner("assigner")
                .duration(15L)
                .status(EmployeeStatus.SHIPPING)
                .userNames(Arrays.asList("", "test"))
                .build();

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/assign-task/SOF")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }
}
