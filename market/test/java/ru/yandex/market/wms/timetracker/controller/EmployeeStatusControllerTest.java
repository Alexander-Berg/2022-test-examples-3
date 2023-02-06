package ru.yandex.market.wms.timetracker.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.model.enums.EnumerationOrder;
import ru.yandex.market.wms.timetracker.model.enums.OperationThreshold;
import ru.yandex.market.wms.timetracker.response.DefaultResponse;
import ru.yandex.market.wms.timetracker.response.EmployeeCountByStatusResponse;
import ru.yandex.market.wms.timetracker.response.EmployeeStatusResponse;
import ru.yandex.market.wms.timetracker.service.EmployeeStatusService;
import ru.yandex.market.wms.timetracker.service.PlanFactService;
import ru.yandex.market.wms.timetracker.specification.rsql.ApiField;

@WebMvcTest(EmployeeStatusController.class)
@ActiveProfiles("test")
@Import(NoAuthTest.class)
class EmployeeStatusControllerTest {

    @MockBean
    private EmployeeStatusService employeeStatusService;

    @MockBean
    private PlanFactService planFactService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getEmployeeStatus() throws Exception {

        final List<EmployeeStatusResponse> contentExpected = List.of(
                EmployeeStatusResponse.builder()
                        .userName("sof-test")
                        .status(EmployeeStatus.PLACEMENT)
                        .lastUpdatedTs(LocalDateTime.parse("2021-11-12T12:00:00"))
                        .assigner("assigner-test")
                        .finishTs(LocalDateTime.parse("2021-11-12T15:00:00"))
                        .threshold(OperationThreshold.BAD)
                        .build()
        );

        final DefaultResponse<List<EmployeeStatusResponse>> requestExpected =
                DefaultResponse.<List<EmployeeStatusResponse>>builder()
                        .limit(20)
                        .offset(0)
                        .content(contentExpected)
                        .build();

        Mockito.when(employeeStatusService.currentEmployeeStatus(ArgumentMatchers.eq("SOF"),
                        ArgumentMatchers.any(Integer.class),
                        ArgumentMatchers.any(Integer.class), ArgumentMatchers.any(String.class),
                        ArgumentMatchers.any(ApiField.class), ArgumentMatchers.any(EnumerationOrder.class),
                        ArgumentMatchers.any()))
                .thenReturn(contentExpected);

        final String jsonModel = mapper.writeValueAsString(requestExpected);

        mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/employee-status/SOF")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonModel));
    }

    @Test
    public void getEmployeeStatusWithRsql() throws Exception {

        final List<EmployeeStatusResponse> contentExpected = List.of(
                EmployeeStatusResponse.builder()
                        .userName("sof-test")
                        .status(EmployeeStatus.PLACEMENT)
                        .lastUpdatedTs(LocalDateTime.parse("2021-11-12T12:00:00"))
                        .assigner("assigner-test")
                        .finishTs(LocalDateTime.parse("2021-11-12T15:00:00"))
                        .threshold(OperationThreshold.BAD)
                        .build()
        );

        final DefaultResponse<List<EmployeeStatusResponse>> requestExpected =
                DefaultResponse.<List<EmployeeStatusResponse>>builder()
                        .limit(1)
                        .offset(10)
                        .content(contentExpected)
                        .build();

        Mockito.when(employeeStatusService.currentEmployeeStatus(ArgumentMatchers.eq("SOF"),
                        ArgumentMatchers.eq(1),
                        ArgumentMatchers.eq(10),
                        ArgumentMatchers.eq("userName==test"),
                        ArgumentMatchers.eq(ApiField.of("userName")),
                        ArgumentMatchers.eq(EnumerationOrder.DESC),
                        ArgumentMatchers.any()))
                .thenReturn(contentExpected);

        final String jsonModel = mapper.writeValueAsString(requestExpected);

        mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/employee-status/SOF?" +
                                        "limit=1" +
                                        "&offset=10" +
                                        "&filter=userName==test" +
                                        "&sort=userName" +
                                        "&order=desc")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonModel));
    }

    @Test
    public void getGroupEmployeeStatusCount() throws Exception {

        final List<EmployeeCountByStatusResponse> expected = List.of(
                EmployeeCountByStatusResponse.builder()
                        .status(EmployeeStatus.PLACEMENT)
                        .plan(13)
                        .fact(10)
                        .build(),
                EmployeeCountByStatusResponse.builder()
                        .status(EmployeeStatus.INVENTORIZATION)
                        .plan(816)
                        .fact(600)
                        .build()
        );

        Mockito.when(planFactService.planFactEmployeeStatus(ArgumentMatchers.eq("SOF")))
                .thenReturn(expected);

        final String jsonModel = mapper.writeValueAsString(expected);

        mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/employee-status/SOF/count")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonModel));
    }

    @Test
    public void getPlanHistory() throws Exception {

        Mockito.when(planFactService.planFactEmployeeStatusHistory(
                        ArgumentMatchers.eq("SOF"),
                        ArgumentMatchers.any(Optional.class),
                        ArgumentMatchers.any(Optional.class)))
                .thenReturn(Collections.emptyList());

        mockMvc
                .perform(
                        MockMvcRequestBuilders.get(
                                "/employee-status/SOF/plan/history?from=2021-12-17T10:00:00&to=2021-12-17T12:00:00"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
