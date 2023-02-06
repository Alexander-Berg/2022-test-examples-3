package ru.yandex.market.wms.timetracker.controller;

import java.util.List;

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
import ru.yandex.market.wms.timetracker.dto.EmployeeStatusPlanRequest;
import ru.yandex.market.wms.timetracker.model.WarehouseModel;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.service.EmployeeStatusPlanService;
import ru.yandex.market.wms.timetracker.service.WarehouseService;

@WebMvcTest(EmployeeStatusPlanController.class)
@ActiveProfiles("test")
@Import(NoAuthTest.class)
class EmployeeStatusPlanControllerTest {

    @Autowired
    private EmployeeStatusPlanService employeeStatusPlanService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public EmployeeStatusPlanService employeeStatusPlanService() {
            return Mockito.mock(EmployeeStatusPlanService.class);
        }

        @Bean
        public WarehouseService warehouseService() {
            return Mockito.mock(WarehouseService.class);
        }
    }

    @Test
    void save() throws Exception {

        List<EmployeeStatusPlanRequest> contentExpected = List.of(
                EmployeeStatusPlanRequest.builder()
                        .status(EmployeeStatus.CONSOLIDATION)
                        .count(30L)
                        .build(),
                EmployeeStatusPlanRequest.builder()
                        .status(EmployeeStatus.PRECONSOLIDATION)
                        .count(1L)
                        .build()
        );

        Mockito.when(warehouseService.readWarehouse(ArgumentMatchers.eq("SOF")))
                        .thenReturn(WarehouseModel.builder()
                                .id(1L)
                                .build());

        Mockito.doNothing()
                .when(employeeStatusPlanService).save(ArgumentMatchers.anyCollection());

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/employee-status-plan/sof")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void saveWhenStatusIsNull() throws Exception {

        List<EmployeeStatusPlanRequest> contentExpected = List.of(
                EmployeeStatusPlanRequest.builder()
                        .status(null)
                        .count(30L)
                        .build()
        );

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/employee-status-plan/sof")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

}
