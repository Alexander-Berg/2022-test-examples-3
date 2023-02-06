package ru.yandex.market.wms.timetracker.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import cz.jirutka.rsql.parser.RSQLParser;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.wms.timetracker.dao.EmployeeStatusPlanHistoryDao;
import ru.yandex.market.wms.timetracker.dao.postgres.EmployeeStatusPlanDao;
import ru.yandex.market.wms.timetracker.dao.postgres.EmployeeStatusWithPerformanceDao;
import ru.yandex.market.wms.timetracker.dto.CountByStatusDto;
import ru.yandex.market.wms.timetracker.dto.EmployeeStatusPlanModel;
import ru.yandex.market.wms.timetracker.model.EmployeeStatusPlanHistoryModel;
import ru.yandex.market.wms.timetracker.model.WarehouseModel;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.response.EmployeeCountByStatusHistoryResponse;
import ru.yandex.market.wms.timetracker.response.EmployeeCountByStatusResponse;
import ru.yandex.market.wms.timetracker.specification.rsql.SearchOperators;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PlanFactServiceImplTest.CommonTestConfig.class,
        PlanFactServiceImpl.class,
        OperationThresholdCalculator.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
class PlanFactServiceImplTest {

    @Autowired
    private PlanFactServiceImpl planFactService;

    @MockBean
    private WarehouseService warehouseService;

    @MockBean
    private EmployeeStatusWithPerformanceDao withPerformanceDao;

    @MockBean
    private EmployeeStatusPlanDao employeeStatusPlanDao;

    @MockBean
    private WarehouseTimeZoneConvertorService timeZoneConvertorService;

    @MockBean
    private EmployeeStatusPlanHistoryDao employeeStatusPlanHistoryDao;

    @TestConfiguration
    public static class CommonTestConfig {

        @Bean
        public RSQLParser rsqlParser() {
            return new RSQLParser(SearchOperators.OPERATORS);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2021-12-17T15:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Test
    public void planFactEmployeeStatus() {

        Mockito.when(warehouseService.readWarehouse(ArgumentMatchers.eq("SOF"))).thenReturn(
                WarehouseModel.builder()
                        .id(1L)
                        .build()
        );

        Mockito.when(withPerformanceDao.countGroupByStatus(ArgumentMatchers.eq(1L)))
                .thenReturn(List.of(
                        CountByStatusDto.builder()
                                .status(EmployeeStatus.PLACEMENT)
                                .count(300L)
                                .build(),

                        CountByStatusDto.builder()
                                .status(EmployeeStatus.INVENTORIZATION)
                                .count(10L)
                                .build()
                ));

        Mockito.when(employeeStatusPlanDao.findAll(ArgumentMatchers.eq(1L)))
                .thenReturn(List.of(
                        EmployeeStatusPlanModel.builder()
                                .warehouseId(1L)
                                .status(EmployeeStatus.PLACEMENT)
                                .count(8)
                                .build(),
                        EmployeeStatusPlanModel.builder()
                                .warehouseId(1L)
                                .status(EmployeeStatus.INVENTORIZATION)
                                .count(250)
                                .build()
                ));

        final EmployeeCountByStatusResponse expectedPlacement = EmployeeCountByStatusResponse.builder()
                .status(EmployeeStatus.PLACEMENT)
                .fact(300)
                .plan(8)
                .build();

        final EmployeeCountByStatusResponse expectedInventorization = EmployeeCountByStatusResponse.builder()
                .status(EmployeeStatus.INVENTORIZATION)
                .fact(10)
                .plan(250)
                .build();

        final List<EmployeeCountByStatusResponse> result = planFactService.planFactEmployeeStatus("SOF");

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, result.size()),
                () -> MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expectedPlacement,
                        expectedInventorization)));
    }

    @ParameterizedTest
    @MethodSource("providePlanFactEmployeeStatusHistory")
    public void planFactEmployeeStatusHistory(Optional<LocalDateTime> from, Optional<LocalDateTime> to) {

        setupMockPlanFactStatusHistory();

        var result =
                planFactService.planFactEmployeeStatusHistory("sof", from, to);

        var expected15 =
                EmployeeCountByStatusHistoryResponse.builder()
                        .period(LocalDateTime.parse("2021-12-17T15:00:00"))
                        .data(List.of(EmployeeCountByStatusResponse.builder()
                                        .status(EmployeeStatus.SHIPPING)
                                        .plan(10)
                                        .fact(15)
                                        .build(),
                                EmployeeCountByStatusResponse.builder()
                                        .status(EmployeeStatus.CONSOLIDATION)
                                        .plan(10)
                                        .fact(15)
                                        .build()))
                        .build();

        var expected16 =
                EmployeeCountByStatusHistoryResponse.builder()
                        .period(LocalDateTime.parse("2021-12-17T16:00:00"))
                        .data(List.of(EmployeeCountByStatusResponse.builder()
                                .status(EmployeeStatus.SHIPPING)
                                .plan(10)
                                .fact(15)
                                .build()))
                        .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, result.size()),
                () -> MatcherAssert.assertThat(result, Matchers.contains(expected15, expected16))
        );
    }

    @Test
    public void planFactEmployeeStatusHistoryWhenFromIsEmptyAndToNotEmpty() {

        final IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> planFactService.planFactEmployeeStatusHistory("sof",
                        Optional.empty(),
                        Optional.of(LocalDateTime.parse("2021-12-17T15:00:00")))
        );

        Assertions.assertTrue(exception.getMessage().contains("Argument to is not empty but from is empty"));
    }

    @Test
    public void planFactEmployeeStatusHistoryWhenFromAfterThanTo() {

        final IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> planFactService.planFactEmployeeStatusHistory("sof",
                        Optional.of(LocalDateTime.parse("2021-12-17T16:00:00")),
                        Optional.of(LocalDateTime.parse("2021-12-17T15:00:00")))
        );

        Assertions.assertTrue(exception.getMessage().contains("Argument from is after to argument"));
    }

    private static Stream<Arguments> providePlanFactEmployeeStatusHistory() {
        return Stream.of(
                Arguments.of(Optional.empty(), Optional.empty()),
                Arguments.of(Optional.of(LocalDateTime.parse("2021-12-17T00:00:00")), Optional.empty()),
                Arguments.of(
                        Optional.of(LocalDateTime.parse("2021-12-17T00:00:00")),
                        Optional.of(LocalDateTime.parse("2021-12-17T23:00:00")))
        );
    }

    private void setupMockPlanFactStatusHistory() {
        Mockito.when(warehouseService.readWarehouse(ArgumentMatchers.eq("sof")))
                .thenReturn(WarehouseModel.builder()
                        .id(1L)
                        .build());

        Mockito.when(timeZoneConvertorService.fromUtc(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(Instant.parse("2021-12-17T15:00:00Z"))
        )).thenReturn(LocalDateTime.parse("2021-12-17T15:00:00"));

        Mockito.when(timeZoneConvertorService.fromUtc(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(Instant.parse("2021-12-17T16:00:00Z"))
        )).thenReturn(LocalDateTime.parse("2021-12-17T16:00:00"));

        Mockito.when(timeZoneConvertorService.toUtcInstant(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(LocalDateTime.parse("2021-12-17T00:00:00")
                ))).thenReturn(Instant.parse("2021-12-17T15:00:00Z"));

        Mockito.when(timeZoneConvertorService.toUtcInstant(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(LocalDateTime.parse("2021-12-17T23:00:00")
                ))).thenReturn(Instant.parse("2021-12-17T16:00:00Z"));

        Mockito.when(warehouseService.readWarehouse(ArgumentMatchers.eq("sof")))
                .thenReturn(WarehouseModel.builder()
                        .id(1L)
                        .build());

        Mockito.when(employeeStatusPlanHistoryDao.findAvgByWarehouseIdAndEventTimeBetween(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.any(Instant.class),
                ArgumentMatchers.any(Instant.class)
        )).thenReturn(
                List.of(
                        EmployeeStatusPlanHistoryModel.builder()
                                .eventTime(Instant.parse("2021-12-17T15:00:00Z"))
                                .status(EmployeeStatus.SHIPPING)
                                .plan(10)
                                .fact(15)
                                .build(),
                        EmployeeStatusPlanHistoryModel.builder()
                                .eventTime(Instant.parse("2021-12-17T15:00:00Z"))
                                .status(EmployeeStatus.CONSOLIDATION)
                                .plan(10)
                                .fact(15)
                                .build(),
                        EmployeeStatusPlanHistoryModel.builder()
                                .eventTime(Instant.parse("2021-12-17T16:00:00Z"))
                                .status(EmployeeStatus.SHIPPING)
                                .plan(10)
                                .fact(15)
                                .build()
                )
        );
    }

}
