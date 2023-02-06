package ru.yandex.market.wms.timetracker.controller;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.timetracker.config.NoAuthTest;
import ru.yandex.market.wms.timetracker.model.enums.AssigmentType;
import ru.yandex.market.wms.timetracker.model.enums.ProcessType;
import ru.yandex.market.wms.timetracker.response.EmployeeProcessTypeRequest;
import ru.yandex.market.wms.timetracker.service.EmployeeProcessTypeService;

@WebMvcTest(EmployeeProcessTypeController.class)
@ActiveProfiles("test")
@Import(NoAuthTest.class)
class EmployeeProcessTypeControllerTest {

    @Autowired
    private EmployeeProcessTypeService employeeProcessTypeService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        EmployeeProcessTypeService employeeProcessTypeService() {
            return Mockito.mock(EmployeeProcessTypeService.class);
        }
    }

    @Test
    void assignUser() throws Exception {

        Mockito.doNothing().when(employeeProcessTypeService)
                .changeProcessType(
                        ArgumentMatchers.eq("sof"), ArgumentMatchers.any(EmployeeProcessTypeRequest.class));

        final EmployeeProcessTypeRequest contentExpected =
                EmployeeProcessTypeRequest.builder()
                        .putAwayZoneName(null)
                        .assigmentType(AssigmentType.SYSTEM)
                        .assigner("assigner")
                        .processType(ProcessType.PLACEMENT)
                        .user("test")
                        .eventTime(Instant.parse("2021-11-01T12:00:00Z"))
                        .build();

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/process-type/assignUser/sof")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void assignUserValidationError() throws Exception {

        Mockito.doNothing().when(employeeProcessTypeService)
                .changeProcessType(
                        ArgumentMatchers.eq("sof"), ArgumentMatchers.any(EmployeeProcessTypeRequest.class));

        final EmployeeProcessTypeRequest contentExpected =
                EmployeeProcessTypeRequest.builder()
                        .putAwayZoneName(null)
                        .assigmentType(AssigmentType.SYSTEM)
                        .assigner("assigner")
                        .processType(ProcessType.PLACEMENT)
                        .user(null)
                        .eventTime(Instant.parse("2021-11-01T12:00:00Z"))
                        .build();

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/process-type/assignUser/sof")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void assignUserServerError() throws Exception {

        Mockito.doThrow(RuntimeException.class).when(employeeProcessTypeService)
                .changeProcessType(
                        ArgumentMatchers.eq("sof"), ArgumentMatchers.any(EmployeeProcessTypeRequest.class));

        final EmployeeProcessTypeRequest contentExpected =
                EmployeeProcessTypeRequest.builder()
                        .putAwayZoneName(null)
                        .assigmentType(AssigmentType.SYSTEM)
                        .assigner("assigner")
                        .processType(ProcessType.PLACEMENT)
                        .user("test")
                        .eventTime(Instant.parse("2021-11-01T12:00:00Z"))
                        .build();

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/process-type/assignUser/sof")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is5xxServerError());
    }
}
