package ru.yandex.market.wms.timetracker.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.wms.timetracker.config.NoAuthTest;
import ru.yandex.market.wms.timetracker.model.enums.OperationThreshold;
import ru.yandex.market.wms.timetracker.response.PerformanceByHourResponse;
import ru.yandex.market.wms.timetracker.response.PerformanceByOperationResponse;
import ru.yandex.market.wms.timetracker.response.PerformanceByUserResponse;
import ru.yandex.market.wms.timetracker.service.EmployeePerformanceService;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeePerformanceController.class)
@ActiveProfiles("test")
@Import(NoAuthTest.class)
class EmployeePerformanceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeePerformanceService employeePerformanceService;

    @Test
    @SneakyThrows
    void performanceByUser() {
        when(employeePerformanceService.performanceForDayByUser(any(String.class)))
                .thenReturn(mockOperationPerformanceRequest());

        mockMvc.perform(get("/employee-performance/test"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(
                                FileContentUtils.getFileContent(
                                        "controller/EmployeePerformanceController/response.json"))
                );
    }

    private PerformanceByUserResponse mockOperationPerformanceRequest() {
        final List<PerformanceByOperationResponse> categoriesAtNine = List.of(
                PerformanceByOperationResponse.builder()
                        .name("Приемка")
                        .result(125)
                        .overall(BigDecimal.valueOf(0.50444444))
                        .threshold(OperationThreshold.BAD)
                        .build(),
                PerformanceByOperationResponse.builder()
                        .name("Возвраты")
                        .result(5)
                        .overall(BigDecimal.valueOf(0.1))
                        .threshold(OperationThreshold.BAD)
                        .build(),
                PerformanceByOperationResponse.builder()
                        .name("Консолидация")
                        .result(10)
                        .overall(BigDecimal.valueOf(0.1))
                        .threshold(OperationThreshold.BAD)
                        .build()
        );

        final List<PerformanceByOperationResponse> categoriesAtTen = List.of(
                PerformanceByOperationResponse.builder()
                        .name("Приемка")
                        .result(150)
                        .overall(BigDecimal.valueOf(0.75444444))
                        .threshold(OperationThreshold.GOOD)
                        .build()
        );

        final List<PerformanceByHourResponse> stats = List.of(
                PerformanceByHourResponse.builder()
                        .date(LocalDateTime.of(2021, 9, 1, 9, 0))
                        .overall(BigDecimal.valueOf(0.75444444))
                        .threshold(OperationThreshold.GOOD)
                        .categories(categoriesAtNine)
                        .build(),
                PerformanceByHourResponse.builder()
                        .date(LocalDateTime.of(2021, 9, 1, 10, 0))
                        .overall(BigDecimal.valueOf(1))
                        .threshold(OperationThreshold.GREAT)
                        .categories(categoriesAtTen)
                        .build()
        );

        return PerformanceByUserResponse.builder()
                                        .stats(stats)
                                        .build();
    }
}
