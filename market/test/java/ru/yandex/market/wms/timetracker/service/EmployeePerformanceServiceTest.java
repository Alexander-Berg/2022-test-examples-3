package ru.yandex.market.wms.timetracker.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.wms.timetracker.dao.PerformanceDayStandardDao;
import ru.yandex.market.wms.timetracker.dao.postgres.IndirectActivityDao;
import ru.yandex.market.wms.timetracker.dao.postgres.PerfomanceHistoryDAO;
import ru.yandex.market.wms.timetracker.dao.postgres.PerformancePerHourPGDao;
import ru.yandex.market.wms.timetracker.dto.IndirectActivityModel;
import ru.yandex.market.wms.timetracker.mapper.PerformanceByOperationsToRequestConvertor;
import ru.yandex.market.wms.timetracker.model.PerformanceByHourDto;
import ru.yandex.market.wms.timetracker.model.PerformanceByOperationsDto;
import ru.yandex.market.wms.timetracker.model.enums.OperationThreshold;
import ru.yandex.market.wms.timetracker.model.enums.PerformanceUnit;
import ru.yandex.market.wms.timetracker.model.enums.UserActivityStatus;
import ru.yandex.market.wms.timetracker.response.PerformanceByDayResponse;
import ru.yandex.market.wms.timetracker.response.PerformanceByHourResponse;
import ru.yandex.market.wms.timetracker.response.PerformanceByOperationForDayResponse;
import ru.yandex.market.wms.timetracker.response.PerformanceByOperationResponse;
import ru.yandex.market.wms.timetracker.response.PerformanceByUserResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeePerformanceService.class,
        PerformanceByOperationsToRequestConvertor.class,
        OperationThresholdCalculator.class,
        PerformanceByOperationsToRequestConvertor.class,
        PerformanceDayStandardDao.class,
        IndirectActivityProcessor.class
})
@Import({
        EmployeePerformanceServiceTest.CommonTestConfig.class
})
class EmployeePerformanceServiceTest {

    @TestConfiguration
    public static class CommonTestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2021-09-01T12:00:00.00Z"), ZoneOffset.UTC);
        }
    }

    @MockBean
    private PerfomanceHistoryDAO perfomanceHistoryDAO;

    @MockBean
    private PerformancePerHourPGDao performancePerHourDao;

    @Autowired
    private EmployeePerformanceService employeePerformanceService;

    @MockBean
    private IndirectActivityDao indirectActivityDao;

    @Test
    void performanceForDayByUser() {
        String userName = "test";

        when(perfomanceHistoryDAO.getPerfomanceByOperator(any(String.class)))
                .thenReturn(mockOperationPerformanceByUser());

        when(performancePerHourDao.getPerformanceByHour(any(String.class), any(LocalDate.class)))
                .thenReturn(mockPerformanceByHour());

        final PerformanceByUserResponse result =
                employeePerformanceService.performanceForDayByUser(userName);

        final PerformanceByUserResponse expected = PerformanceByUserResponse.builder()
                .dayOverall(BigDecimal.valueOf(1600.0))
                .dayGoal(BigDecimal.valueOf(4048.0))
                .stats(List.of(
                        PerformanceByHourResponse.builder()
                                .date(LocalDateTime.of(2021, 9, 1, 10, 0))
                                .overall(BigDecimal.valueOf(0.1))
                                .threshold(OperationThreshold.UGLY)
                                .categories(List.of(
                                        PerformanceByOperationResponse.builder()
                                                .name("Приемка")
                                                .result(150)
                                                .overall(BigDecimal.valueOf(0.1))
                                                .threshold(OperationThreshold.UGLY)
                                                .unit(PerformanceUnit.PIECE)
                                                .build()
                                ))
                                .build(),

                        PerformanceByHourResponse.builder()
                                .date(LocalDateTime.of(2021, 9, 1, 9, 0))
                                .overall(BigDecimal.valueOf(33.4))
                                .threshold(OperationThreshold.GREAT)
                                .categories(List.of(
                                        PerformanceByOperationResponse.builder()
                                                .name("Приемка")
                                                .result(125)
                                                .overall(BigDecimal.valueOf(100.0))
                                                .threshold(OperationThreshold.GREAT)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationResponse.builder()
                                                .name("Возвраты")
                                                .result(5)
                                                .overall(BigDecimal.valueOf(0.1))
                                                .threshold(OperationThreshold.UGLY)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationResponse.builder()
                                                .name("Консолидация")
                                                .result(10)
                                                .overall(BigDecimal.valueOf(0.1))
                                                .threshold(OperationThreshold.UGLY)
                                                .unit(PerformanceUnit.PIECE)
                                                .build()
                                ))
                                .build()
                ))
                .statsByDay(List.of(
                        PerformanceByDayResponse.builder()
                                .date(LocalDate.of(2021, 9, 1))
                                .categories(List.of(
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("Консолидация")
                                                .result(10)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("Возвраты")
                                                .result(5)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("Приемка")
                                                .result(275)
                                                .unit(PerformanceUnit.PIECE)
                                                .build()
                                ))
                                .build(),
                        PerformanceByDayResponse.builder()
                                .date(LocalDate.of(2021, 8, 31))
                                .categories(List.of(
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("Консолидация")
                                                .result(20)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("Возвраты")
                                                .result(15)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("Приемка")
                                                .result(475)
                                                .unit(PerformanceUnit.PIECE)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        assertThat(result, samePropertyValuesAs(expected));
    }

    @Test
    void performanceForDayByUser2() {
        String userName = "test";

        when(perfomanceHistoryDAO.getPerfomanceByOperator(any(String.class)))
                .thenReturn(mockOperationPerformanceByUser2());

        when(performancePerHourDao.getPerformanceByHour(any(String.class), any(LocalDate.class)))
                .thenReturn(mockPerformanceByHour());

        when(indirectActivityDao.getForHour(
                LocalDateTime.of(2021, 9, 1, 9, 0),
                userName
        )).thenReturn(getIndirectActivities(userName));

        var result = employeePerformanceService.performanceForDayByUser(userName);

        var expected = PerformanceByUserResponse.builder()
                .dayOverall(BigDecimal.valueOf(1600.0))
                .dayGoal(BigDecimal.valueOf(4048.0))
                .stats(List.of(
                        PerformanceByHourResponse.builder()
                                .date(LocalDateTime.of(2021, 9, 1, 10, 0))
                                .overall(BigDecimal.valueOf(0.1))
                                .threshold(OperationThreshold.UGLY)
                                .categories(List.of(
                                        PerformanceByOperationResponse.builder()
                                                .name("Приемка")
                                                .result(150)
                                                .overall(BigDecimal.valueOf(0.1))
                                                .threshold(OperationThreshold.UGLY)
                                                .unit(PerformanceUnit.PIECE)
                                                .build()
                                ))
                                .build(),

                        PerformanceByHourResponse.builder()
                                .date(LocalDateTime.of(2021, 9, 1, 9, 0))
                                .overall(BigDecimal.valueOf(33.4))
                                .threshold(OperationThreshold.GREAT)
                                .categories(List.of(
                                        PerformanceByOperationResponse.builder()
                                                .name("Приемка")
                                                .result(125)
                                                .overall(BigDecimal.valueOf(100.0))
                                                .threshold(OperationThreshold.GREAT)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationResponse.builder()
                                                .name("Консолидация")
                                                .result(10)
                                                .overall(BigDecimal.valueOf(0.1))
                                                .threshold(OperationThreshold.UGLY)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationResponse.builder()
                                                .name("BREAK")
                                                .result(2)
                                                .overall(BigDecimal.valueOf(0.1))
                                                .threshold(OperationThreshold.UGLY)
                                                .unit(PerformanceUnit.MINUTE)
                                                .build(),
                                        PerformanceByOperationResponse.builder()
                                                .name("LUNCH")
                                                .result(3)
                                                .overall(BigDecimal.valueOf(0.1))
                                                .threshold(OperationThreshold.UGLY)
                                                .unit(PerformanceUnit.MINUTE)
                                                .build()
                                ))
                                .build()
                ))
                .statsByDay(List.of(
                        PerformanceByDayResponse.builder()
                                .date(LocalDate.of(2021, 9, 1))
                                .categories(List.of(
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("Консолидация")
                                                .result(10)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("BREAK")
                                                .result(2)
                                                .unit(PerformanceUnit.MINUTE)
                                                .build(),
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("Приемка")
                                                .result(275)
                                                .unit(PerformanceUnit.PIECE)
                                                .build(),
                                        PerformanceByOperationForDayResponse.builder()
                                                .name("LUNCH")
                                                .result(3)
                                                .unit(PerformanceUnit.MINUTE)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        assertThat(result, samePropertyValuesAs(expected));
    }

    private List<IndirectActivityModel> getIndirectActivities(String username) {
        return List.of(
                IndirectActivityModel
                        .builder()
                        .activityName("LUNCH")
                        .eventTime(LocalDateTime.of(2021, 9, 1, 8, 5).toInstant(ZoneOffset.UTC))
                        .status(UserActivityStatus.IN_PROCESS)
                        .userName(username)
                        .assigner(username)
                        .build(),
                IndirectActivityModel
                        .builder()
                        .activityName("LUNCH")
                        .eventTime(LocalDateTime.of(2021, 9, 1, 8, 8).toInstant(ZoneOffset.UTC))
                        .endTime(LocalDateTime.of(2021, 9, 1, 8, 8).toInstant(ZoneOffset.UTC))
                        .status(UserActivityStatus.COMPLETED)
                        .userName(username)
                        .assigner(username)
                        .build(),
                IndirectActivityModel
                        .builder()
                        .activityName("BREAK")
                        .eventTime(LocalDateTime.of(2021, 9, 1, 8, 8).toInstant(ZoneOffset.UTC))
                        .status(UserActivityStatus.IN_PROCESS)
                        .userName(username)
                        .assigner(username)
                        .build(),
                IndirectActivityModel
                        .builder()
                        .activityName("BREAK")
                        .eventTime(LocalDateTime.of(2021, 9, 1, 8, 10).toInstant(ZoneOffset.UTC))
                        .endTime(LocalDateTime.of(2021, 9, 1, 8, 10).toInstant(ZoneOffset.UTC))
                        .status(UserActivityStatus.COMPLETED)
                        .userName(username)
                        .assigner(username)
                        .build()
        );
    }

    private List<PerformanceByOperationsDto> mockOperationPerformanceByUser() {
        return List.of(
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Приемка")
                        .date(LocalDateTime.of(2021, 9, 1, 9, 0))
                        .result(125)
                        .overallByOperation(0.50d)
                        .overall(100)
                        .dayOverall(300)
                        .operDay(LocalDate.of(2021, 9, 1))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Приемка")
                        .date(LocalDateTime.of(2021, 9, 1, 10, 0))
                        .result(150)
                        .overallByOperation(0.50d)
                        .overall(0.1)
                        .dayOverall(500)
                        .operDay(LocalDate.of(2021, 9, 1))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Возвраты")
                        .date(LocalDateTime.of(2021, 9, 1, 9, 0))
                        .result(5)
                        .overallByOperation(50d)
                        .overall(0.1)
                        .dayOverall(300)
                        .operDay(LocalDate.of(2021, 9, 1))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Консолидация")
                        .date(LocalDateTime.of(2021, 9, 1, 9, 0))
                        .result(10)
                        .overallByOperation(0.15d)
                        .overall(0.1)
                        .dayOverall(500)
                        .operDay(LocalDate.of(2021, 9, 1))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Приемка")
                        .date(LocalDateTime.of(2021, 8, 31, 9, 0))
                        .result(225)
                        .overallByOperation(0.50d)
                        .overall(100)
                        .dayOverall(300)
                        .operDay(LocalDate.of(2021, 8, 31))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Приемка")
                        .date(LocalDateTime.of(2021, 8, 31, 10, 0))
                        .result(250)
                        .overallByOperation(0.50d)
                        .overall(0.1)
                        .dayOverall(500)
                        .operDay(LocalDate.of(2021, 8, 31))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Возвраты")
                        .date(LocalDateTime.of(2021, 8, 31, 9, 0))
                        .result(15)
                        .overallByOperation(50d)
                        .overall(0.1)
                        .dayOverall(300)
                        .operDay(LocalDate.of(2021, 8, 31))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Консолидация")
                        .date(LocalDateTime.of(2021, 8, 31, 9, 0))
                        .result(20)
                        .overallByOperation(0.15d)
                        .overall(0.1)
                        .dayOverall(500)
                        .operDay(LocalDate.of(2021, 8, 31))
                        .build()
        );
    }

    private List<PerformanceByHourDto> mockPerformanceByHour() {
        return List.of(
                PerformanceByHourDto.builder()
                        .hour(LocalDateTime.of(2021, 9, 1, 9, 0))
                        .overall(33.4)
                        .build(),
                PerformanceByHourDto.builder()
                        .hour(LocalDateTime.of(2021, 9, 1, 10, 0))
                        .overall(0.1)
                        .build()
        );
    }

    private List<PerformanceByOperationsDto> mockOperationPerformanceByUser2() {
        return List.of(
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Приемка")
                        .date(LocalDateTime.of(2021, 9, 1, 9, 0))
                        .result(125)
                        .overallByOperation(0.50d)
                        .overall(100)
                        .dayOverall(300)
                        .operDay(LocalDate.of(2021, 9, 1))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Приемка")
                        .date(LocalDateTime.of(2021, 9, 1, 10, 0))
                        .result(150)
                        .overallByOperation(0.50d)
                        .overall(0.1)
                        .dayOverall(500)
                        .operDay(LocalDate.of(2021, 9, 1))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Другое")
                        .date(LocalDateTime.of(2021, 9, 1, 9, 0))
                        .result(5)
                        .overallByOperation(50d)
                        .overall(0.1)
                        .dayOverall(300)
                        .operDay(LocalDate.of(2021, 9, 1))
                        .build(),
                PerformanceByOperationsDto.builder()
                        .storageName("SOF")
                        .categoriesName("Консолидация")
                        .date(LocalDateTime.of(2021, 9, 1, 9, 0))
                        .result(10)
                        .overallByOperation(0.15d)
                        .overall(0.1)
                        .dayOverall(500)
                        .operDay(LocalDate.of(2021, 9, 1))
                        .build()
        );
    }
}
